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
import org.springframework.stereotype.Component;

@Component
public class UserReferenceChecker {

    private final RecruitmentGroupRepository recruitmentGroupRepository;
    private final ApplicationRepository applicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final RecruitmentTaskRepository recruitmentTaskRepository;
    private final AnnouncementRepository announcementRepository;
    private final LearningMaterialRepository learningMaterialRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadSessionRepository fileUploadSessionRepository;

    public UserReferenceChecker(
            RecruitmentGroupRepository recruitmentGroupRepository,
            ApplicationRepository applicationRepository,
            GroupMemberRepository groupMemberRepository,
            TaskSubmissionRepository taskSubmissionRepository,
            RecruitmentTaskRepository recruitmentTaskRepository,
            AnnouncementRepository announcementRepository,
            LearningMaterialRepository learningMaterialRepository,
            StoredFileRepository storedFileRepository,
            FileUploadSessionRepository fileUploadSessionRepository
    ) {
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.applicationRepository = applicationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.recruitmentTaskRepository = recruitmentTaskRepository;
        this.announcementRepository = announcementRepository;
        this.learningMaterialRepository = learningMaterialRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadSessionRepository = fileUploadSessionRepository;
    }

    public void ensureDeletable(Long userId) {
        if (recruitmentGroupRepository.existsByLeaderUserId(userId)) {
            throw new ConflictException("该负责人仍绑定负责的分组，不能删除");
        }
        if (applicationRepository.existsByUserId(userId) || groupMemberRepository.existsByUserId(userId)) {
            throw new ConflictException("该用户仍有报名申请或分组成员记录，不能删除");
        }
        if (taskSubmissionRepository.existsByUserId(userId) || taskSubmissionRepository.existsByReviewerUserId(userId)) {
            throw new ConflictException("该用户仍存在任务提交或评测记录，不能删除");
        }
        if (recruitmentTaskRepository.existsByPublisherUserId(userId)
                || announcementRepository.existsByPublisherUserId(userId)
                || learningMaterialRepository.existsByPublisherUserId(userId)) {
            throw new ConflictException("该用户仍有已发布的任务、公告或资料，不能删除");
        }
        if (storedFileRepository.existsByUploaderUserId(userId)
                || fileUploadSessionRepository.existsByUploaderUserId(userId)) {
            throw new ConflictException("该用户仍存在已上传的文件或上传会话，不能删除");
        }
    }
}
