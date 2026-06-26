package club.muimi.backend.security.permission;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.Application;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.security.auth.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;

    @Test
    void adminShouldAccessAnyApplication() {
        AuthzService authzService = new AuthzService(applicationRepository, groupMemberRepository, recruitmentGroupRepository);

        boolean result = authzService.canAccessApplication(authentication(Role.ADMIN, 99L), 123L);

        assertThat(result).isTrue();
    }

    @Test
    void ownerShouldAccessOwnApplication() {
        AuthzService authzService = new AuthzService(applicationRepository, groupMemberRepository, recruitmentGroupRepository);
        Application application = Application.builder().id(10L).userId(1L).build();
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        boolean result = authzService.canAccessApplication(authentication(Role.FRESHMAN, 1L), 10L);

        assertThat(result).isTrue();
    }

    @Test
    void memberShouldViewOwnGroup() {
        AuthzService authzService = new AuthzService(applicationRepository, groupMemberRepository, recruitmentGroupRepository);
        when(groupMemberRepository.existsByUserIdAndGroupId(1L, 200L)).thenReturn(true);

        boolean result = authzService.canViewGroup(authentication(Role.FRESHMAN, 1L), 200L);

        assertThat(result).isTrue();
    }

    @Test
    void leaderShouldManageOwnedGroup() {
        AuthzService authzService = new AuthzService(applicationRepository, groupMemberRepository, recruitmentGroupRepository);
        when(recruitmentGroupRepository.existsByIdAndLeaderUserId(300L, 2L)).thenReturn(true);

        boolean result = authzService.canManageGroup(authentication(Role.LEADER, 2L), 300L);

        assertThat(result).isTrue();
    }

    private Authentication authentication(Role role, Long userId) {
        LoginUser loginUser = new LoginUser(userId, "zhangsan", "user@example.com", "N/A", role, UserStatus.ACTIVE, 0L, "test-jti");
        return UsernamePasswordAuthenticationToken.authenticated(loginUser, null, loginUser.getAuthorities());
    }
}
