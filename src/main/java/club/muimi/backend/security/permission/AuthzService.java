package club.muimi.backend.security.permission;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.entity.Application;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
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
        return currentUserId() != null && currentUserId().equals(userId);
    }

    public boolean canAccessApplication(Authentication authentication, Long applicationId) {
        if (isAdmin(authentication)) {
            return true;
        }
        Application application = applicationRepository.findById(applicationId).orElse(null);
        return application != null && application.getUserId().equals(currentUserId(authentication));
    }

    public boolean canViewGroup(Authentication authentication, Long groupId) {
        if (isAdmin(authentication)) {
            return true;
        }
        Long currentUserId = currentUserId(authentication);
        return currentUserId != null && groupMemberRepository.existsByUserIdAndGroupId(currentUserId, groupId);
    }

    public boolean canManageGroup(Authentication authentication, Long groupId) {
        if (isAdmin(authentication)) {
            return true;
        }
        Long currentUserId = currentUserId(authentication);
        return currentUserId != null && recruitmentGroupRepository.existsByIdAndLeaderUserId(groupId, currentUserId);
    }

    public boolean canPublishGroupAnnouncement(Authentication authentication, Long groupId) {
        return canManageGroup(authentication, groupId);
    }

    private boolean isAdmin(Authentication authentication) {
        LoginUser loginUser = extractLoginUser(authentication);
        return loginUser != null && loginUser.getRole() == Role.ADMIN;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return currentUserId(authentication);
    }

    private Long currentUserId(Authentication authentication) {
        LoginUser loginUser = extractLoginUser(authentication);
        return loginUser == null ? null : loginUser.getUserId();
    }

    private LoginUser extractLoginUser(Authentication authentication) {
        Authentication actualAuthentication = authentication;
        if (actualAuthentication == null) {
            return null;
        }
        if (actualAuthentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }
}
