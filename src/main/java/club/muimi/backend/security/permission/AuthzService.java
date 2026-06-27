package club.muimi.backend.security.permission;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.security.auth.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("authzService")
public class AuthzService {

    private final ApplicationRepository applicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;

    public AuthzService(
            ApplicationRepository applicationRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
    }

    public boolean isCurrentUser(Long userId) {
        LoginUser loginUser = extractLoginUser(SecurityContextHolder.getContext().getAuthentication());
        return loginUser != null && loginUser.getUserId().equals(userId);
    }

    public boolean canAccessApplication(Authentication authentication, Long applicationId) {
        LoginUser loginUser = extractLoginUser(authentication);
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getRole() == Role.ADMIN) {
            return true;
        }

        return applicationRepository.findById(applicationId)
                .map(application -> {
                    if (application.getUserId().equals(loginUser.getUserId())) {
                        return true;
                    }
                    if (loginUser.getRole() != Role.LEADER) {
                        return false;
                    }
                    return groupMemberRepository.findByApplicationId(applicationId)
                            .map(groupMember -> recruitmentGroupRepository.existsByIdAndLeaderUserId(
                                    groupMember.getGroupId(),
                                    loginUser.getUserId()
                            ))
                            .orElse(false);
                })
                .orElse(false);
    }

    public boolean canViewGroup(Authentication authentication, Long groupId) {
        LoginUser loginUser = extractLoginUser(authentication);
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getRole() == Role.ADMIN) {
            return true;
        }
        if (recruitmentGroupRepository.existsByIdAndLeaderUserId(groupId, loginUser.getUserId())) {
            return true;
        }
        return groupMemberRepository.existsByUserIdAndGroupId(loginUser.getUserId(), groupId);
    }

    public boolean canManageGroup(Authentication authentication, Long groupId) {
        LoginUser loginUser = extractLoginUser(authentication);
        if (loginUser == null) {
            return false;
        }
        if (loginUser.getRole() == Role.ADMIN) {
            return true;
        }
        return loginUser.getRole() == Role.LEADER
                && recruitmentGroupRepository.existsByIdAndLeaderUserId(groupId, loginUser.getUserId());
    }

    public boolean canPublishGroupAnnouncement(Authentication authentication, Long groupId) {
        return canManageGroup(authentication, groupId);
    }

    private LoginUser extractLoginUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }
}
