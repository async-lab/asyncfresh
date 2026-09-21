package club.muimi.backend.integration;

import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.common.enums.PeriodType;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.StoredFilePurpose;
import club.muimi.backend.common.enums.TaskSubmissionStatus;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.Direction;
import club.muimi.backend.entity.LearningMaterial;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.RecruitmentPeriod;
import club.muimi.backend.entity.RecruitmentTask;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.entity.TaskSubmission;
import club.muimi.backend.entity.User;
import club.muimi.backend.repository.DirectionRepository;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.RecruitmentPeriodRepository;
import club.muimi.backend.repository.RecruitmentTaskRepository;
import club.muimi.backend.repository.StoredFileRepository;
import club.muimi.backend.repository.TaskSubmissionRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.material.LearningMaterialService;
import club.muimi.backend.service.task.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 带附件的资源删除必须能通过外键校验：
 * 附件（stored_file）被主体（learning_material / recruitment_task / task_submission）引用时，
 * 删除顺序错误会撞外键约束，对外表现为 40900「数据冲突」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB}",
        "spring.datasource.username=${DB_USERNAME:epoch}",
        "spring.datasource.password=${DB_PASSWORD:123456}",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6379",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test-mail@example.com",
        "spring.mail.password=test-auth-code",
        "spring.mail.from=test-mail@example.com",
        "app.security.jwt.secret=test-jwt-secret-key-with-at-least-32-bytes-long",
        "app.security.jwt.cookie-secure=false",
        "app.security.jwt.cookie-same-site=Lax",
        "app.auth.cache-type=redis",
        "app.storage.root-path=./target/test-storage",
        "app.bootstrap.default-admin.enabled=false"
})
class AttachmentDeleteIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DirectionRepository directionRepository;
    @Autowired
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Autowired
    private RecruitmentPeriodRepository recruitmentPeriodRepository;
    @Autowired
    private StoredFileRepository storedFileRepository;
    @Autowired
    private LearningMaterialRepository learningMaterialRepository;
    @Autowired
    private RecruitmentTaskRepository recruitmentTaskRepository;
    @Autowired
    private TaskSubmissionRepository taskSubmissionRepository;
    @Autowired
    private LearningMaterialService learningMaterialService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String suffix;
    private User admin;
    private Direction level1;
    private Direction level2;
    private RecruitmentGroup group;
    private RecruitmentPeriod originalSelectionSnapshot;
    private Long managedSelectionPeriodId;
    private boolean createdSelectionPeriodForTest;

    private final List<Long> materialIds = new ArrayList<>();
    private final List<Long> taskIds = new ArrayList<>();
    private final List<Long> storedFileIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        admin = userRepository.save(User.builder()
                .username("attach_" + suffix)
                .email("attach_" + suffix + "@example.com")
                .passwordHash(passwordEncoder.encode("Attach1234"))
                .emailVerified(true)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .lastLoginAt(LocalDateTime.now())
                .build());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(admin.getId(), admin.getUsername(), admin.getEmail(), admin.getPasswordHash(),
                        admin.getRole(), admin.getStatus(), admin.getTokenVersion(), "jti-attachment-test"),
                null,
                List.of()
        ));

        level1 = directionRepository.save(Direction.builder()
                .name("attach-l1-" + suffix).level(1).sortOrder(1).enabled(true).build());
        level2 = directionRepository.save(Direction.builder()
                .parentId(level1.getId()).name("attach-l2-" + suffix).level(2).sortOrder(1).enabled(true).build());
        group = recruitmentGroupRepository.save(RecruitmentGroup.builder()
                .name("attach-group-" + suffix)
                .directionLevel1Id(level1.getId())
                .directionLevel2Id(level2.getId())
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .maxSize(10)
                .leaderUserId(admin.getId())
                .build());

        openSelectionPeriod();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();

        materialIds.forEach(id -> learningMaterialRepository.findById(id).ifPresent(learningMaterialRepository::delete));
        materialIds.clear();
        taskIds.forEach(id -> {
            taskSubmissionRepository.findAllByTaskId(id).forEach(taskSubmissionRepository::delete);
            recruitmentTaskRepository.findById(id).ifPresent(recruitmentTaskRepository::delete);
        });
        taskIds.clear();
        storedFileIds.forEach(id -> storedFileRepository.findById(id).ifPresent(storedFileRepository::delete));
        storedFileIds.clear();
        if (group != null) {
            recruitmentGroupRepository.findById(group.getId()).ifPresent(recruitmentGroupRepository::delete);
            group = null;
        }
        if (level2 != null) {
            directionRepository.findById(level2.getId()).ifPresent(directionRepository::delete);
            level2 = null;
        }
        if (level1 != null) {
            directionRepository.findById(level1.getId()).ifPresent(directionRepository::delete);
            level1 = null;
        }
        if (admin != null) {
            userRepository.findById(admin.getId()).ifPresent(userRepository::delete);
            admin = null;
        }
        restoreSelectionPeriod();
    }

    @Test
    void deleteMaterialWithAttachmentShouldNotHitForeignKeyConflict() {
        StoredFile attachment = createStoredFile(StoredFilePurpose.MATERIAL_ATTACHMENT);
        LearningMaterial material = learningMaterialRepository.save(LearningMaterial.builder()
                .groupId(group.getId())
                .title("带附件资料")
                .contentMarkdown("内容")
                .attachmentFileId(attachment.getId())
                .publisherUserId(admin.getId())
                .build());
        materialIds.add(material.getId());

        learningMaterialService.deleteMaterial(group.getId(), material.getId());

        assertThat(learningMaterialRepository.findById(material.getId())).isEmpty();
        assertThat(storedFileRepository.findById(attachment.getId())).isEmpty();
    }

    @Test
    void deleteTaskWithAttachmentShouldNotHitForeignKeyConflict() {
        StoredFile attachment = createStoredFile(StoredFilePurpose.TASK_ATTACHMENT);
        RecruitmentTask task = recruitmentTaskRepository.save(RecruitmentTask.builder()
                .groupId(group.getId())
                .title("带附件任务")
                .contentMarkdown("内容")
                .attachmentFileId(attachment.getId())
                .maxScore(100)
                .deadlineAt(LocalDateTime.now().plusDays(3))
                .publisherUserId(admin.getId())
                .build());
        taskIds.add(task.getId());

        taskService.deleteTask(group.getId(), task.getId());

        assertThat(recruitmentTaskRepository.findById(task.getId())).isEmpty();
        assertThat(storedFileRepository.findById(attachment.getId())).isEmpty();
    }

    @Test
    void deleteTaskWithSubmissionAttachmentShouldNotHitForeignKeyConflict() {
        StoredFile taskAttachment = createStoredFile(StoredFilePurpose.TASK_ATTACHMENT);
        StoredFile submissionAttachment = createStoredFile(StoredFilePurpose.TASK_SUBMISSION_ATTACHMENT);
        RecruitmentTask task = recruitmentTaskRepository.save(RecruitmentTask.builder()
                .groupId(group.getId())
                .title("带提交附件的任务")
                .contentMarkdown("内容")
                .attachmentFileId(taskAttachment.getId())
                .maxScore(100)
                .deadlineAt(LocalDateTime.now().plusDays(3))
                .publisherUserId(admin.getId())
                .build());
        taskIds.add(task.getId());
        taskSubmissionRepository.save(TaskSubmission.builder()
                .taskId(task.getId())
                .userId(admin.getId())
                .status(TaskSubmissionStatus.SUBMITTED)
                .contentMarkdown("提交内容")
                .attachmentFileId(submissionAttachment.getId())
                .submittedAt(LocalDateTime.now())
                .build());

        taskService.deleteTask(group.getId(), task.getId());

        assertThat(recruitmentTaskRepository.findById(task.getId())).isEmpty();
        assertThat(storedFileRepository.findById(taskAttachment.getId())).isEmpty();
        assertThat(storedFileRepository.findById(submissionAttachment.getId())).isEmpty();
    }

    private StoredFile createStoredFile(StoredFilePurpose purpose) {
        StoredFile storedFile = storedFileRepository.save(StoredFile.builder()
                .purpose(purpose)
                .originalFileName("attach-" + suffix + ".bin")
                .contentType("application/octet-stream")
                .sizeBytes(1024L)
                .storagePath("files/attach-" + suffix + "-" + purpose + ".bin")
                .uploaderUserId(admin.getId())
                .build());
        storedFileIds.add(storedFile.getId());
        return storedFile;
    }

    private void openSelectionPeriod() {
        RecruitmentPeriod period = recruitmentPeriodRepository.findAll().stream()
                .filter(item -> item.getPeriodType() == PeriodType.SELECTION)
                .findFirst()
                .orElse(null);
        if (period == null) {
            RecruitmentPeriod created = recruitmentPeriodRepository.save(RecruitmentPeriod.builder()
                    .periodType(PeriodType.SELECTION)
                    .startTime(LocalDateTime.now().minusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1))
                    .enabled(true)
                    .build());
            managedSelectionPeriodId = created.getId();
            createdSelectionPeriodForTest = true;
            return;
        }
        managedSelectionPeriodId = period.getId();
        createdSelectionPeriodForTest = false;
        originalSelectionSnapshot = RecruitmentPeriod.builder()
                .id(period.getId())
                .periodType(period.getPeriodType())
                .startTime(period.getStartTime())
                .endTime(period.getEndTime())
                .enabled(period.getEnabled())
                .createdAt(period.getCreatedAt())
                .updatedAt(period.getUpdatedAt())
                .build();
        period.setEnabled(true);
        period.setStartTime(LocalDateTime.now().minusDays(1));
        period.setEndTime(LocalDateTime.now().plusDays(1));
        recruitmentPeriodRepository.save(period);
    }

    private void restoreSelectionPeriod() {
        if (managedSelectionPeriodId == null) {
            return;
        }
        if (createdSelectionPeriodForTest) {
            recruitmentPeriodRepository.deleteById(managedSelectionPeriodId);
        } else if (originalSelectionSnapshot != null) {
            recruitmentPeriodRepository.findById(managedSelectionPeriodId).ifPresent(period -> {
                period.setEnabled(originalSelectionSnapshot.getEnabled());
                period.setStartTime(originalSelectionSnapshot.getStartTime());
                period.setEndTime(originalSelectionSnapshot.getEndTime());
                recruitmentPeriodRepository.save(period);
            });
        }
        managedSelectionPeriodId = null;
        createdSelectionPeriodForTest = false;
        originalSelectionSnapshot = null;
    }
}
