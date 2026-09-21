package club.muimi.backend.service.admin;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.dto.admin.CreateAdminUserRequest;
import club.muimi.backend.dto.admin.UpdateAdminUserRequest;
import club.muimi.backend.dto.admin.UpdateUserRoleRequest;
import club.muimi.backend.dto.admin.UpdateUserStatusRequest;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.ValidationException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.audit.AuditLogService;
import club.muimi.backend.service.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserReferenceChecker userReferenceChecker;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository,
                applicationRepository,
                groupMemberRepository,
                recruitmentGroupRepository,
                currentUserService,
                auditLogService,
                passwordEncoder,
                userReferenceChecker
        );
    }

    @Test
    void updateUserStatusShouldRejectSelfStatusChange() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.updateUserStatus(1L, new UpdateUserStatusRequest(UserStatus.DISABLED)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员不能修改自己的状态");
    }

    @Test
    void updateUserRoleShouldPromoteFreshmanToLeaderAndInvalidateOldTokens() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        User freshman = User.builder()
                .id(2L)
                .username("freshman")
                .email("freshman@example.com")
                .passwordHash("hashed")
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(freshman));
        when(groupMemberRepository.findAllByUserId(2L)).thenReturn(List.of());
        when(recruitmentGroupRepository.findAllByLeaderUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
        when(applicationRepository.countByUserId(2L)).thenReturn(0L);

        var result = adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.LEADER));

        assertThat(result.role()).isEqualTo(Role.LEADER);
        assertThat(freshman.getRole()).isEqualTo(Role.LEADER);
        assertThat(freshman.getTokenVersion()).isEqualTo(4L);
    }

    @Test
    void updateUserRoleShouldRejectLeaderDemotionWhenUserStillOwnsGroup() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        User leader = User.builder()
                .id(2L)
                .username("leader")
                .email("leader@example.com")
                .passwordHash("hashed")
                .role(Role.LEADER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(leader));
        when(recruitmentGroupRepository.findAllByLeaderUserId(2L)).thenReturn(List.of(
                RecruitmentGroup.builder().id(10L).leaderUserId(2L).name("g1").directionLevel1Id(1L).directionLevel2Id(2L).grade(club.muimi.backend.common.enums.Grade.YEAR_1).admissionYear(2026).maxSize(10).build()
        ));

        assertThatThrownBy(() -> adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.FRESHMAN)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该负责人仍绑定负责的分组，不能降级为新生");
        assertThat(leader.getRole()).isEqualTo(Role.LEADER);
        assertThat(leader.getTokenVersion()).isEqualTo(3L);
    }

    @Test
    void updateUserRoleShouldRejectAdminRoleRequest() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.ADMIN)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("用户角色只能在 FRESHMAN 与 LEADER 之间调整");
    }

    @Test
    void updateUserRoleShouldRejectChangingAdminAccount() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        User targetAdmin = User.builder()
                .id(2L)
                .username("admin2")
                .email("admin2@example.com")
                .passwordHash("hashed")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() -> adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.LEADER)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员账号角色不允许通过该接口修改");
        assertThat(targetAdmin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(targetAdmin.getTokenVersion()).isEqualTo(3L);
    }

    @Test
    void updateUserRoleShouldDemoteLeaderWithoutOwnedGroup() {
        LoginUser admin = new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        User leader = User.builder()
                .id(2L)
                .username("leader")
                .email("leader@example.com")
                .passwordHash("hashed")
                .role(Role.LEADER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(leader));
        when(recruitmentGroupRepository.findAllByLeaderUserId(2L)).thenReturn(List.of());
        when(groupMemberRepository.findAllByUserId(2L)).thenReturn(List.of());
        when(recruitmentGroupRepository.findAllByLeaderUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
        when(applicationRepository.countByUserId(2L)).thenReturn(0L);

        var result = adminUserService.updateUserRole(2L, new UpdateUserRoleRequest(Role.FRESHMAN));

        assertThat(result.role()).isEqualTo(Role.FRESHMAN);
        assertThat(leader.getRole()).isEqualTo(Role.FRESHMAN);
        assertThat(leader.getTokenVersion()).isEqualTo(4L);
    }

    @Test
    void listUsersShouldTolerateLeaderOwningMultipleGroups() {
        User leader = User.builder()
                .id(2L)
                .username("leader")
                .email("leader@example.com")
                .passwordHash("hashed")
                .role(Role.LEADER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.searchUsers(null, null, null, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(leader)));
        when(applicationRepository.findAllByUserIdIn(List.of(2L))).thenReturn(List.of());
        when(groupMemberRepository.findAllByUserIdIn(List.of(2L))).thenReturn(List.of());
        when(recruitmentGroupRepository.findAllByLeaderUserIdIn(List.of(2L))).thenReturn(List.of(
                RecruitmentGroup.builder().id(20L).leaderUserId(2L).name("g2").directionLevel1Id(1L).directionLevel2Id(2L).grade(club.muimi.backend.common.enums.Grade.YEAR_1).admissionYear(2026).maxSize(10).build(),
                RecruitmentGroup.builder().id(10L).leaderUserId(2L).name("g1").directionLevel1Id(1L).directionLevel2Id(2L).grade(club.muimi.backend.common.enums.Grade.YEAR_1).admissionYear(2026).maxSize(10).build()
        ));

        var result = adminUserService.listUsers(1, 10, null, null, null);

        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().leaderGroupCount()).isEqualTo(2L);
        assertThat(result.list().getFirst().groups()).isEmpty();
        assertThat(result.list().getFirst().groupCount()).isEqualTo(0L);
    }

    @Test
    void listUsersShouldIncludeMemberGroupNames() {
        User freshman = User.builder()
                .id(2L)
                .username("freshman")
                .email("freshman@example.com")
                .passwordHash("hashed")
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.searchUsers(null, null, null, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(freshman)));
        when(applicationRepository.findAllByUserIdIn(List.of(2L))).thenReturn(List.of());
        when(groupMemberRepository.findAllByUserIdIn(List.of(2L))).thenReturn(List.of(
                club.muimi.backend.entity.GroupMember.builder()
                        .id(1L)
                        .groupId(9L)
                        .userId(2L)
                        .applicationId(3L)
                        .build()
        ));
        when(recruitmentGroupRepository.findAllByIdIn(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(
                RecruitmentGroup.builder()
                        .id(9L)
                        .name("前端一组")
                        .directionLevel1Id(1L)
                        .directionLevel2Id(2L)
                        .grade(club.muimi.backend.common.enums.Grade.YEAR_1)
                        .admissionYear(2026)
                        .maxSize(10)
                        .build()
        ));
        when(recruitmentGroupRepository.findAllByLeaderUserIdIn(List.of(2L))).thenReturn(List.of());

        var result = adminUserService.listUsers(1, 10, null, null, null);

        assertThat(result.list()).hasSize(1);
        assertThat(result.list().getFirst().groups())
                .extracting(club.muimi.backend.vo.auth.GroupSimpleVo::name)
                .containsExactly("前端一组");
        assertThat(result.list().getFirst().groupCount()).isEqualTo(1L);
    }

    @Test
    void getUserDetailShouldTolerateLeaderOwningMultipleGroups() {
        User leader = User.builder()
                .id(2L)
                .username("leader")
                .email("leader@example.com")
                .passwordHash("hashed")
                .role(Role.LEADER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(leader));
        when(groupMemberRepository.findAllByUserId(2L)).thenReturn(List.of());
        when(recruitmentGroupRepository.findAllByLeaderUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(
                RecruitmentGroup.builder().id(20L).leaderUserId(2L).name("g2").directionLevel1Id(1L).directionLevel2Id(2L).grade(club.muimi.backend.common.enums.Grade.YEAR_1).admissionYear(2026).maxSize(10).build(),
                RecruitmentGroup.builder().id(10L).leaderUserId(2L).name("g1").directionLevel1Id(1L).directionLevel2Id(2L).grade(club.muimi.backend.common.enums.Grade.YEAR_1).admissionYear(2026).maxSize(10).build()
        ));
        when(applicationRepository.countByUserId(2L)).thenReturn(0L);

        var result = adminUserService.getUserDetail(2L);

        assertThat(result.leaderGroups())
                .extracting(club.muimi.backend.vo.auth.GroupSimpleVo::id)
                .containsExactly(20L, 10L);
    }

    @Test
    void createUserShouldCreateFreshmanByDefault() {
        LoginUser admin = adminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(userRepository.existsByEmail("newbie@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass1234")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        stubUserDetail(10L);

        var result = adminUserService.createUser(new CreateAdminUserRequest(
                "newbie",
                "newbie@example.com",
                "Pass1234",
                null,
                null,
                null
        ));

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.username()).isEqualTo("newbie");
        assertThat(result.role()).isEqualTo(Role.FRESHMAN);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.emailVerified()).isTrue();
    }

    @Test
    void createUserShouldRejectDuplicateUsername() {
        LoginUser admin = adminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.existsByUsername("newbie")).thenReturn(true);

        assertThatThrownBy(() -> adminUserService.createUser(new CreateAdminUserRequest(
                "newbie",
                "newbie@example.com",
                "Pass1234",
                Role.FRESHMAN,
                UserStatus.ACTIVE,
                true
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void createUserShouldRejectAdminRole() {
        LoginUser admin = adminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.createUser(new CreateAdminUserRequest(
                "newbie",
                "newbie@example.com",
                "Pass1234",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true
        )))
                .isInstanceOf(ValidationException.class)
                .hasMessage("用户角色只能在 FRESHMAN 与 LEADER 之间调整");
    }

    @Test
    void updateUserShouldUpdateProfileAndResetPassword() {
        LoginUser admin = adminUser();
        User freshman = managedUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(freshman));
        when(userRepository.existsByUsernameAndIdNot("freshman2", 2L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("freshman2@example.com", 2L)).thenReturn(false);
        when(passwordEncoder.encode("NewPass123")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubUserDetail(2L);

        var result = adminUserService.updateUser(2L, new UpdateAdminUserRequest(
                "freshman2",
                "freshman2@example.com",
                "NewPass123",
                Role.LEADER,
                UserStatus.DISABLED,
                true
        ));

        assertThat(result.username()).isEqualTo("freshman2");
        assertThat(result.email()).isEqualTo("freshman2@example.com");
        assertThat(result.role()).isEqualTo(Role.LEADER);
        assertThat(result.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(freshman.getPasswordHash()).isEqualTo("encoded-new");
        assertThat(freshman.getTokenVersion()).isEqualTo(4L);
    }

    @Test
    void updateUserShouldRejectSelfUpdate() {
        LoginUser admin = adminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.updateUser(1L, new UpdateAdminUserRequest(
                "admin",
                "admin@example.com",
                null,
                Role.FRESHMAN,
                UserStatus.ACTIVE,
                true
        )))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员不能修改自己的账号信息");
    }

    @Test
    void deleteUserShouldRemoveManagedUser() {
        LoginUser admin = adminUser();
        User freshman = managedUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(freshman));

        adminUserService.deleteUser(2L);

        verify(userReferenceChecker).ensureDeletable(2L);
        verify(userRepository).delete(freshman);
    }

    @Test
    void deleteUserShouldRejectSelfDeletion() {
        LoginUser admin = adminUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);

        assertThatThrownBy(() -> adminUserService.deleteUser(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员不能删除自己的账号");
    }

    @Test
    void deleteUserShouldRejectAdminAccount() {
        LoginUser admin = adminUser();
        User targetAdmin = User.builder()
                .id(2L)
                .username("admin2")
                .email("admin2@example.com")
                .passwordHash("hashed")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetAdmin));

        assertThatThrownBy(() -> adminUserService.deleteUser(2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("管理员账号不允许通过该接口删除");
    }

    @Test
    void deleteUserShouldRejectWhenRelatedDataExists() {
        LoginUser admin = adminUser();
        User freshman = managedUser();
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(freshman));
        doThrow(new ConflictException("该负责人仍绑定负责的分组，不能删除"))
                .when(userReferenceChecker).ensureDeletable(2L);

        assertThatThrownBy(() -> adminUserService.deleteUser(2L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该负责人仍绑定负责的分组，不能删除");
    }

    private LoginUser adminUser() {
        return new LoginUser(1L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
    }

    private User managedUser() {
        return User.builder()
                .id(2L)
                .username("freshman")
                .email("freshman@example.com")
                .passwordHash("hashed")
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tokenVersion(3L)
                .build();
    }

    private void stubUserDetail(Long userId) {
        when(groupMemberRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(recruitmentGroupRepository.findAllByLeaderUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(applicationRepository.countByUserId(userId)).thenReturn(0L);
    }
}
