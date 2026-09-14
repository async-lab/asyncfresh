package club.muimi.backend.service.admin;

import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.repository.AnnouncementRepository;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.FileUploadSessionRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.LearningMaterialRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.RecruitmentTaskRepository;
import club.muimi.backend.repository.StoredFileRepository;
import club.muimi.backend.repository.TaskSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReferenceCheckerTest {

    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;
    @Mock
    private RecruitmentTaskRepository recruitmentTaskRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private LearningMaterialRepository learningMaterialRepository;
    @Mock
    private StoredFileRepository storedFileRepository;
    @Mock
    private FileUploadSessionRepository fileUploadSessionRepository;

    private UserReferenceChecker userReferenceChecker;

    @BeforeEach
    void setUp() {
        userReferenceChecker = new UserReferenceChecker(
                recruitmentGroupRepository,
                applicationRepository,
                groupMemberRepository,
                taskSubmissionRepository,
                recruitmentTaskRepository,
                announcementRepository,
                learningMaterialRepository,
                storedFileRepository,
                fileUploadSessionRepository
        );
    }

    @Test
    void ensureDeletableShouldPassWhenUserHasNoReferences() {
        stubNoReferences();

        assertThatCode(() -> userReferenceChecker.ensureDeletable(2L)).doesNotThrowAnyException();
    }

    @Test
    void ensureDeletableShouldRejectGroupLeader() {
        when(recruitmentGroupRepository.existsByLeaderUserId(2L)).thenReturn(true);

        assertThatThrownBy(() -> userReferenceChecker.ensureDeletable(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该负责人仍绑定负责的分组，不能删除");
    }

    @Test
    void ensureDeletableShouldRejectUserWithApplication() {
        when(recruitmentGroupRepository.existsByLeaderUserId(2L)).thenReturn(false);
        when(applicationRepository.existsByUserId(2L)).thenReturn(true);

        assertThatThrownBy(() -> userReferenceChecker.ensureDeletable(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该用户仍有报名申请或分组成员记录，不能删除");
    }

    private void stubNoReferences() {
        when(recruitmentGroupRepository.existsByLeaderUserId(2L)).thenReturn(false);
        when(applicationRepository.existsByUserId(2L)).thenReturn(false);
        when(groupMemberRepository.existsByUserId(2L)).thenReturn(false);
        when(taskSubmissionRepository.existsByUserId(2L)).thenReturn(false);
        when(taskSubmissionRepository.existsByReviewerUserId(2L)).thenReturn(false);
        when(recruitmentTaskRepository.existsByPublisherUserId(2L)).thenReturn(false);
        when(announcementRepository.existsByPublisherUserId(2L)).thenReturn(false);
        when(learningMaterialRepository.existsByPublisherUserId(2L)).thenReturn(false);
        when(storedFileRepository.existsByUploaderUserId(2L)).thenReturn(false);
        when(fileUploadSessionRepository.existsByUploaderUserId(2L)).thenReturn(false);
    }
}
