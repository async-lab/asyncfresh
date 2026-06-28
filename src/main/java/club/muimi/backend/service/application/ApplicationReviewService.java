package club.muimi.backend.service.application;

import club.muimi.backend.common.enums.ApplicationStatus;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.dto.admin.RejectApplicationRequest;
import club.muimi.backend.dto.admin.UnassignGroupApplicationRequest;
import club.muimi.backend.entity.Application;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.NotFoundException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.period.PeriodService;
import club.muimi.backend.service.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationReviewService {

    private final ApplicationRepository applicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;
    private final CurrentUserService currentUserService;
    private final PeriodService periodService;

    public ApplicationReviewService(
            ApplicationRepository applicationRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository,
            CurrentUserService currentUserService,
            PeriodService periodService
    ) {
        this.applicationRepository = applicationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.currentUserService = currentUserService;
        this.periodService = periodService;
    }

    @Transactional
    public void rejectApplication(Long applicationId, RejectApplicationRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        periodService.ensureSelectionOpenForGrouping();

        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new NotFoundException("报名申请不存在"));
        ensureCanReview(currentUser, application);
        ensureRejectable(application);

        application.setStatus(ApplicationStatus.REJECTED);
        application.setStatusRemark(request.remark().trim());
        applicationRepository.save(application);
    }

    @Transactional
    public void unassignApplicationFromGroup(
            Long groupId,
            Long applicationId,
            UnassignGroupApplicationRequest request
    ) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        periodService.ensureSelectionOpenForGrouping();

        RecruitmentGroup group = recruitmentGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new NotFoundException("分组不存在"));
        ensureCanManageGroup(currentUser, group);

        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new NotFoundException("报名申请不存在"));
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndApplicationId(groupId, applicationId)
                .orElseThrow(() -> new ConflictException("该报名申请当前不在目标分组中"));

        if (application.getStatus() != ApplicationStatus.GROUPED) {
            throw new ConflictException("当前报名申请状态不允许取消分组");
        }

        groupMemberRepository.delete(groupMember);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setStatusRemark(normalizeNullableRemark(request.remark()));
        applicationRepository.save(application);
    }

    private void ensureRejectable(Application application) {
        if (application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new ConflictException("当前报名申请状态不允许拒绝");
        }
        if (groupMemberRepository.findByApplicationId(application.getId()).isPresent()) {
            throw new ConflictException("已分组的申请不允许拒绝");
        }
    }

    private void ensureCanReview(LoginUser currentUser, Application application) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.LEADER) {
            throw new ForbiddenException("当前角色无权审核报名申请");
        }
        boolean manageable = recruitmentGroupRepository
                .existsByLeaderUserIdAndDirectionLevel1IdAndDirectionLevel2IdAndGradeAndAdmissionYear(
                        currentUser.getUserId(),
                        application.getDirectionLevel1Id(),
                        application.getDirectionLevel2Id(),
                        application.getGrade(),
                        application.getAdmissionYear()
                );
        if (!manageable) {
            throw new ForbiddenException("无权审核该报名申请");
        }
    }

    private void ensureCanManageGroup(LoginUser currentUser, RecruitmentGroup group) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.LEADER || !java.util.Objects.equals(group.getLeaderUserId(), currentUser.getUserId())) {
            throw new ForbiddenException("无权操作该分组");
        }
    }

    private String normalizeNullableRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String trimmed = remark.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
