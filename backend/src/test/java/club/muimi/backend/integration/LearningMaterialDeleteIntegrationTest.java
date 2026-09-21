package club.muimi.backend.integration;

import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.StoredFilePurpose;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.LearningMaterial;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.StoredFile;
import club.muimi.backend.entity.User;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.StoredFileRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.material.LearningMaterialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 在真实 MySQL 上验证「删除带附件的学习资料」：
 * 附件记录（stored_file）必须在主体行删除之后才能删除，
 * 否则会撞上 fk_learning_material_attachment_file_id 外键约束，
 * 对外统一表现为 40900「数据冲突」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3307/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB}",
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
        "app.bootstrap.default-admin.enabled=false"
})
class LearningMaterialDeleteIntegrationTest {

    @Autowired
    private LearningMaterialService learningMaterialService;
    @Autowired
    private LearningMaterialRepository learningMaterialRepository;
    @Autowired
    private StoredFileRepository storedFileRepository;
    @Autowired
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String suffix;
    private User leader;
    private RecruitmentGroup group;
    private StoredFile storedFile;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());

        leader = userRepository.save(User.builder()
                .username("mat_del_leader_" + suffix)
                .email("mat_del_leader_" + suffix + "@example.com")
                .passwordHash(passwordEncoder.encode("Leader1234"))
                .emailVerified(true)
                .role(Role.LEADER)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .lastLoginAt(LocalDateTime.now())
                .build());

        group = recruitmentGroupRepository.save(RecruitmentGroup.builder()
                .name("删除资料集成测试组_" + suffix)
                .directionLevel1Id(1L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .maxSize(10)
                .leaderUserId(leader.getId())
                .build());

        storedFile = storedFileRepository.save(StoredFile.builder()
                .purpose(StoredFilePurpose.MATERIAL_ATTACHMENT)
                .originalFileName("delete-material-it.txt")
                .contentType("text/plain")
                .sizeBytes(3L)
                .storagePath("files/delete-material-it-" + suffix + ".txt")
                .uploaderUserId(leader.getId())
                .build());

        material = learningMaterialRepository.save(LearningMaterial.builder()
                .groupId(group.getId())
                .title("带附件的集成测试资料_" + suffix)
                .contentMarkdown("集成测试正文")
                .attachmentFileId(storedFile.getId())
                .publisherUserId(leader.getId())
                .build());

        // 与业务创建流程一致：附件创建后绑定到资料
        storedFile.setBindingType("MATERIAL");
        storedFile.setBindingId(material.getId());
        storedFileRepository.save(storedFile);

        LoginUser loginUser = new LoginUser(
                leader.getId(),
                leader.getUsername(),
                leader.getEmail(),
                "hashed",
                Role.LEADER,
                UserStatus.ACTIVE,
                0L,
                "jti-" + suffix
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_LEADER"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        // 清理顺序同样要遵守外键：资料 -> 附件 -> 分组 -> 用户
        if (material != null) {
            learningMaterialRepository.findById(material.getId()).ifPresent(learningMaterialRepository::delete);
        }
        if (storedFile != null) {
            storedFileRepository.findById(storedFile.getId()).ifPresent(storedFileRepository::delete);
        }
        if (group != null) {
            recruitmentGroupRepository.findById(group.getId()).ifPresent(recruitmentGroupRepository::delete);
        }
        if (leader != null) {
            userRepository.findById(leader.getId()).ifPresent(userRepository::delete);
        }
    }

    @Test
    void deleteMaterialWithAttachmentShouldNotViolateForeignKey() {
        assertThatCode(() -> learningMaterialService.deleteMaterial(group.getId(), material.getId()))
                .doesNotThrowAnyException();

        assertThat(learningMaterialRepository.findById(material.getId())).isEmpty();
        assertThat(storedFileRepository.findById(storedFile.getId())).isEmpty();
    }
}
