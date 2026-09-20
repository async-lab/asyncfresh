package club.muimi.backend.service.material;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.StoredFilePurpose;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.LearningMaterial;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.StoredFileRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.audit.AuditLogService;
import club.muimi.backend.service.file.FileStorageService;
import club.muimi.backend.service.notification.NotificationService;
import club.muimi.backend.service.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningMaterialServiceTest {

    @Mock
    private LearningMaterialRepository learningMaterialRepository;
    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private StoredFileRepository storedFileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    private LearningMaterialService learningMaterialService;

    @BeforeEach
    void setUp() {
        learningMaterialService = new LearningMaterialService(
                learningMaterialRepository,
                recruitmentGroupRepository,
                groupMemberRepository,
                storedFileRepository,
                userRepository,
                currentUserService,
                fileStorageService,
                notificationService,
                auditLogService,
                Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void getMaterialAttachmentFileShouldRejectUserOutsideGroup() {
        LoginUser freshman = new LoginUser(8L, "freshman", "freshman@example.com", "hashed", Role.FRESHMAN, UserStatus.ACTIVE, 0L, "jti-8");
        LearningMaterial material = LearningMaterial.builder()
                .id(3L)
                .groupId(12L)
                .title("资料")
                .attachmentFileId(99L)
                .publisherUserId(2L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(freshman);
        when(learningMaterialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(groupMemberRepository.existsByUserIdAndGroupId(8L, 12L)).thenReturn(false);
        when(recruitmentGroupRepository.existsByIdAndLeaderUserId(12L, 8L)).thenReturn(false);

        assertThatThrownBy(() -> learningMaterialService.getMaterialAttachmentFile(3L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("当前用户无权查看该学习资料");

        verify(storedFileRepository, never()).findById(99L);
    }

    @Test
    void getMaterialAttachmentFileShouldAllowVisibleGroupMember() {
        LoginUser freshman = new LoginUser(8L, "freshman", "freshman@example.com", "hashed", Role.FRESHMAN, UserStatus.ACTIVE, 0L, "jti-8");
        LearningMaterial material = LearningMaterial.builder()
                .id(3L)
                .groupId(12L)
                .title("资料")
                .attachmentFileId(99L)
                .publisherUserId(2L)
                .build();
        StoredFile file = StoredFile.builder()
                .id(99L)
                .purpose(StoredFilePurpose.MATERIAL_ATTACHMENT)
                .originalFileName("guide.pdf")
                .sizeBytes(1024)
                .storagePath("materials/guide.pdf")
                .uploaderUserId(2L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(freshman);
        when(learningMaterialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(groupMemberRepository.existsByUserIdAndGroupId(8L, 12L)).thenReturn(true);
        when(storedFileRepository.findById(99L)).thenReturn(Optional.of(file));

        learningMaterialService.getMaterialAttachmentFile(3L);

        verify(storedFileRepository).findById(99L);
    }

    @Test
    void listVisibleMaterialsShouldNotQueryFilesWhenMaterialsHaveNoAttachments() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        LocalDateTime now = LocalDateTime.parse("2026-06-29T08:00:00");
        LearningMaterial material = LearningMaterial.builder()
                .id(3L)
                .groupId(12L)
                .title("资料")
                .contentMarkdown("内容")
                .attachmentFileId(null)
                .publisherUserId(2L)
                .createdAt(now)
                .updatedAt(now)
                .build();
        RecruitmentGroup group = RecruitmentGroup.builder()
                .id(12L)
                .name("后端组")
                .build();
        User publisher = User.builder()
                .id(2L)
                .username("leader")
                .email("leader@example.com")
                .passwordHash("hashed")
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(learningMaterialRepository.findAll()).thenReturn(List.of(material));
        when(recruitmentGroupRepository.findAllByIdIn(any())).thenReturn(List.of(group));
        when(userRepository.findAllById(any())).thenReturn(List.of(publisher));

        var materials = learningMaterialService.listVisibleMaterials();

        assertThat(materials).hasSize(1);
        assertThat(materials.getFirst().attachment()).isNull();
        verify(storedFileRepository, never()).findAllByIdIn(any());
    }

    @Test
    void deleteMaterialShouldCleanRelatedNotifications() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        RecruitmentGroup group = RecruitmentGroup.builder()
                .id(12L)
                .name("后端组")
                .build();
        LearningMaterial material = LearningMaterial.builder()
                .id(3L)
                .groupId(12L)
                .title("资料")
                .publisherUserId(2L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(recruitmentGroupRepository.findById(12L)).thenReturn(Optional.of(group));
        when(learningMaterialRepository.findById(3L)).thenReturn(Optional.of(material));

        learningMaterialService.deleteMaterial(12L, 3L);

        verify(learningMaterialRepository).delete(material);
        verify(notificationService).deleteByRelated("MATERIAL", 3L);
    }

    @Test
    void deleteMaterialShouldCleanRelatedNotificationsBeforeRemoval() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        RecruitmentGroup group = RecruitmentGroup.builder()
                .id(12L)
                .name("后端组")
                .build();
        LearningMaterial material = LearningMaterial.builder()
                .id(3L)
                .groupId(12L)
                .title("资料")
                .publisherUserId(2L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(recruitmentGroupRepository.findById(12L)).thenReturn(Optional.of(group));
        when(learningMaterialRepository.findById(3L)).thenReturn(Optional.of(material));

        learningMaterialService.deleteMaterial(12L, 3L);

        // 顺序约定与公告删除一致：先清理关联通知，再删除资料本体，避免约束冲突（40900 数据冲突）
        inOrder(notificationService, learningMaterialRepository)
                .verify(notificationService)
                .deleteByRelated("MATERIAL", 3L);
        inOrder(notificationService, learningMaterialRepository)
                .verify(learningMaterialRepository)
                .delete(material);
        verify(auditLogService).record(any());
    }
}
