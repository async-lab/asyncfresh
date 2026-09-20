package club.muimi.backend.service.notification;

import club.muimi.backend.entity.Announcement;
import club.muimi.backend.repository.AnnouncementRepository;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.NotificationRepository;
import club.muimi.backend.repository.RecruitmentTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrphanNotificationCleanupServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private RecruitmentTaskRepository recruitmentTaskRepository;
    @Mock
    private LearningMaterialRepository learningMaterialRepository;

    private OrphanNotificationCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new OrphanNotificationCleanupService(
                notificationRepository,
                announcementRepository,
                recruitmentTaskRepository,
                learningMaterialRepository
        );
    }

    @Test
    void shouldOnlyDeleteRelatedIdsWhoseResourceNoLongerExists() {
        when(notificationRepository.findRelatedIdsByRelatedType("ANNOUNCEMENT")).thenReturn(List.of(1L, 2L, 3L));
        when(notificationRepository.findRelatedIdsByRelatedType("TASK")).thenReturn(List.of(10L, 11L));
        when(notificationRepository.findRelatedIdsByRelatedType("MATERIAL")).thenReturn(List.of());
        when(announcementRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(announcement(1L), announcement(3L)));
        when(recruitmentTaskRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of());

        cleanupService.cleanOrphanNotifications();

        verify(notificationRepository).deleteAllByRelatedTypeAndRelatedIdIn("ANNOUNCEMENT", List.of(2L));
        verify(notificationRepository).deleteAllByRelatedTypeAndRelatedIdIn("TASK", List.of(10L, 11L));
        verify(notificationRepository, never()).deleteAllByRelatedTypeAndRelatedIdIn(eq("MATERIAL"), any());
        verify(learningMaterialRepository, never()).findAllById(any());
    }

    @Test
    void shouldNotDeleteWhenEveryRelatedResourceStillExists() {
        when(notificationRepository.findRelatedIdsByRelatedType("ANNOUNCEMENT")).thenReturn(List.of(1L));
        when(notificationRepository.findRelatedIdsByRelatedType("TASK")).thenReturn(List.of());
        when(notificationRepository.findRelatedIdsByRelatedType("MATERIAL")).thenReturn(List.of());
        when(announcementRepository.findAllById(List.of(1L))).thenReturn(List.of(announcement(1L)));

        cleanupService.cleanOrphanNotifications();

        verify(notificationRepository, never()).deleteAllByRelatedTypeAndRelatedIdIn(anyString(), any());
    }

    @Test
    void shouldSkipRepositoryLookupsWhenNoNotificationReferencesTheType() {
        when(notificationRepository.findRelatedIdsByRelatedType("ANNOUNCEMENT")).thenReturn(List.of());
        when(notificationRepository.findRelatedIdsByRelatedType("TASK")).thenReturn(List.of());
        when(notificationRepository.findRelatedIdsByRelatedType("MATERIAL")).thenReturn(List.of());

        cleanupService.cleanOrphanNotifications();

        verifyNoInteractions(announcementRepository, recruitmentTaskRepository, learningMaterialRepository);
        verify(notificationRepository, never()).deleteAllByRelatedTypeAndRelatedIdIn(anyString(), any());
    }

    private Announcement announcement(Long id) {
        return Announcement.builder().id(id).build();
    }
}
