package club.muimi.backend.service.notification;

import club.muimi.backend.entity.Announcement;
import club.muimi.backend.entity.LearningMaterial;
import club.muimi.backend.entity.RecruitmentTask;
import club.muimi.backend.repository.AnnouncementRepository;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.NotificationRepository;
import club.muimi.backend.repository.RecruitmentTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 清理孤儿通知：当业务实体（公告/任务/学习资料）已被删除，
 * 但指向它的通知仍留在 notification 表中时，将其批量删除。
 * 启动后执行一次，之后每 6 小时巡检一次。
 */
@Component
public class OrphanNotificationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(OrphanNotificationCleanupService.class);

    private final NotificationRepository notificationRepository;
    private final AnnouncementRepository announcementRepository;
    private final RecruitmentTaskRepository recruitmentTaskRepository;
    private final LearningMaterialRepository learningMaterialRepository;

    public OrphanNotificationCleanupService(
            NotificationRepository notificationRepository,
            AnnouncementRepository announcementRepository,
            RecruitmentTaskRepository recruitmentTaskRepository,
            LearningMaterialRepository learningMaterialRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.announcementRepository = announcementRepository;
        this.recruitmentTaskRepository = recruitmentTaskRepository;
        this.learningMaterialRepository = learningMaterialRepository;
    }

    @Scheduled(initialDelay = 30_000L, fixedDelay = 6 * 60 * 60 * 1000L)
    @Transactional
    public void cleanOrphanNotifications() {
        clean("ANNOUNCEMENT", ids -> announcementRepository.findAllById(ids).stream()
                .map(Announcement::getId).collect(java.util.stream.Collectors.toCollection(HashSet::new)));
        clean("TASK", ids -> recruitmentTaskRepository.findAllById(ids).stream()
                .map(RecruitmentTask::getId).collect(java.util.stream.Collectors.toCollection(HashSet::new)));
        clean("MATERIAL", ids -> learningMaterialRepository.findAllById(ids).stream()
                .map(LearningMaterial::getId).collect(java.util.stream.Collectors.toCollection(HashSet::new)));
    }

    private void clean(String relatedType, Function<List<Long>, Set<Long>> existingIdLoader) {
        List<Long> relatedIds = notificationRepository.findRelatedIdsByRelatedType(relatedType);
        if (relatedIds.isEmpty()) {
            return;
        }
        Set<Long> existingIds = existingIdLoader.apply(relatedIds);
        List<Long> orphanIds = relatedIds.stream()
                .filter(relatedId -> !existingIds.contains(relatedId))
                .toList();
        if (orphanIds.isEmpty()) {
            return;
        }
        int deleted = notificationRepository.deleteAllByRelatedTypeAndRelatedIdIn(relatedType, orphanIds);
        log.info("已清理 {} 条指向已删除{}的通知（relatedType={}）", deleted, relatedType, relatedType);
    }
}
