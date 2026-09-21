package club.muimi.backend.integration;

import club.muimi.backend.common.enums.AnnouncementScope;
import club.muimi.backend.common.enums.NotificationType;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.Announcement;
import club.muimi.backend.entity.Notification;
import club.muimi.backend.entity.User;
import club.muimi.backend.repository.AnnouncementRepository;
import club.muimi.backend.repository.NotificationRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.service.notification.NotificationService;
import club.muimi.backend.service.notification.OrphanNotificationCleanupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证「删除资源时同步清理关联通知」在真实 MySQL 上的行为：
 * 批量 JPQL 删除可执行、且只删除指向已删除资源的通知。
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
        "app.bootstrap.default-admin.enabled=false"
})
class NotificationCleanupIntegrationTest {

    /** 远大于自增值的 id，确保一定不存在对应业务实体。 */
    private static final long MISSING_RELATED_ID = 9_000_000_000_000L;

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private OrphanNotificationCleanupService orphanNotificationCleanupService;

    private String suffix;
    private User primaryUser;
    private User secondaryUser;

    private final List<Long> createdNotificationIds = new ArrayList<>();
    private final List<Long> createdAnnouncementIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        primaryUser = createUser("notif_primary_");
        secondaryUser = createUser("notif_secondary_");
    }

    @AfterEach
    void cleanUp() {
        createdNotificationIds.forEach(id -> notificationRepository.findById(id).ifPresent(notificationRepository::delete));
        createdNotificationIds.clear();
        createdAnnouncementIds.forEach(id -> announcementRepository.findById(id).ifPresent(announcementRepository::delete));
        createdAnnouncementIds.clear();
        if (primaryUser != null) {
            userRepository.findById(primaryUser.getId()).ifPresent(userRepository::delete);
            primaryUser = null;
        }
        if (secondaryUser != null) {
            userRepository.findById(secondaryUser.getId()).ifPresent(userRepository::delete);
            secondaryUser = null;
        }
    }

    @Test
    void orphanCleanupShouldOnlyPurgeNotificationsPointingToMissingResources() {
        Announcement liveAnnouncement = createAnnouncement("仍存在的公告");

        Notification liveNotification = createNotification(
                primaryUser, "it.keep.announcement", NotificationType.ANNOUNCEMENT_PUBLISHED,
                "ANNOUNCEMENT", liveAnnouncement.getId());
        Notification orphanAnnouncement = createNotification(
                primaryUser, "it.orphan.announcement", NotificationType.ANNOUNCEMENT_PUBLISHED,
                "ANNOUNCEMENT", MISSING_RELATED_ID);
        Notification orphanTask = createNotification(
                primaryUser, "it.orphan.task", NotificationType.TASK_PUBLISHED,
                "TASK", MISSING_RELATED_ID);
        Notification orphanMaterial = createNotification(
                primaryUser, "it.orphan.material", NotificationType.MATERIAL_PUBLISHED,
                "MATERIAL", MISSING_RELATED_ID);
        Notification applicationNotification = createNotification(
                primaryUser, "it.application", NotificationType.APPLICATION_REJECTED,
                "APPLICATION", MISSING_RELATED_ID);
        Notification nullRelatedNotification = createNotification(
                primaryUser, "it.null.related", NotificationType.TASK_PUBLISHED,
                "TASK", null);

        orphanNotificationCleanupService.cleanOrphanNotifications();

        assertThat(notificationRepository.findById(liveNotification.getId())).isPresent();
        assertThat(notificationRepository.findById(orphanAnnouncement.getId())).isEmpty();
        assertThat(notificationRepository.findById(orphanTask.getId())).isEmpty();
        assertThat(notificationRepository.findById(orphanMaterial.getId())).isEmpty();
        // 未纳入巡检范围的 relatedType 不会被误删
        assertThat(notificationRepository.findById(applicationNotification.getId())).isPresent();
        // relatedId 为空的通知不属于孤儿
        assertThat(notificationRepository.findById(nullRelatedNotification.getId())).isPresent();
    }

    @Test
    void deleteByRelatedShouldRemoveNotificationsOfEveryRecipientForThatResourceOnly() {
        Announcement deletedAnnouncement = createAnnouncement("待删除公告");
        Announcement keptAnnouncement = createAnnouncement("保留公告");

        Notification firstRecipient = createNotification(
                primaryUser, "it.delete.first", NotificationType.ANNOUNCEMENT_PUBLISHED,
                "ANNOUNCEMENT", deletedAnnouncement.getId());
        Notification secondRecipient = createNotification(
                secondaryUser, "it.delete.second", NotificationType.ANNOUNCEMENT_PUBLISHED,
                "ANNOUNCEMENT", deletedAnnouncement.getId());
        Notification anotherResource = createNotification(
                primaryUser, "it.delete.other", NotificationType.ANNOUNCEMENT_PUBLISHED,
                "ANNOUNCEMENT", keptAnnouncement.getId());

        notificationService.deleteByRelated("ANNOUNCEMENT", deletedAnnouncement.getId());

        assertThat(notificationRepository.findById(firstRecipient.getId())).isEmpty();
        assertThat(notificationRepository.findById(secondRecipient.getId())).isEmpty();
        assertThat(notificationRepository.findById(anotherResource.getId())).isPresent();
    }

    private User createUser(String prefix) {
        return userRepository.save(User.builder()
                .username(prefix + suffix)
                .email(prefix + suffix + "@example.com")
                .passwordHash(passwordEncoder.encode("Notif1234"))
                .emailVerified(true)
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .lastLoginAt(LocalDateTime.now())
                .build());
    }

    private Announcement createAnnouncement(String title) {
        Announcement announcement = announcementRepository.save(Announcement.builder()
                .title(title)
                .contentMarkdown("集成测试内容")
                .scope(AnnouncementScope.GLOBAL)
                .groupId(null)
                .publisherUserId(primaryUser.getId())
                .build());
        createdAnnouncementIds.add(announcement.getId());
        return announcement;
    }

    private Notification createNotification(
            User recipient,
            String bizKey,
            NotificationType type,
            String relatedType,
            Long relatedId
    ) {
        Notification notification = notificationRepository.save(Notification.builder()
                .recipientUserId(recipient.getId())
                .type(type)
                .title("集成测试通知")
                .content("集成测试内容")
                .bizKey(bizKey + "." + suffix)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .build());
        createdNotificationIds.add(notification.getId());
        return notification;
    }
}
