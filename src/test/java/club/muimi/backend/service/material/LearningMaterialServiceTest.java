package club.muimi.backend.service.material;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.StoredFilePurpose;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.LearningMaterial;
import club.muimi.backend.entity.StoredFile;
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
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
