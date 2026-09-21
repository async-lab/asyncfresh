package club.muimi.backend.service.group;

import club.muimi.backend.common.enums.ApplicationStatus;
import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.dto.admin.AdminAddGroupMemberRequest;
import club.muimi.backend.entity.Application;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.DirectionRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.audit.AuditLogService;
import club.muimi.backend.service.notification.NotificationService;
import club.muimi.backend.service.period.PeriodService;
import club.muimi.backend.service.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupManagementServiceTest {

    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private DirectionRepository directionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PeriodService periodService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    private GroupManagementService groupManagementService;

    @BeforeEach
    void setUp() {
        groupManagementService = new GroupManagementService(
                recruitmentGroupRepository,
                groupMemberRepository,
                applicationRepository,
                directionRepository,
                userRepository,
                currentUserService,
                periodService,
                notificationService,
                auditLogService,
                Clock.fixed(Instant.parse("2026-06-28T03:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    @Test
    void leaderShouldNotAssignApplicationToOtherLeadersGroup() {
        LoginUser leader = buildLoginUser(10L, Role.LEADER);
        RecruitmentGroup group = buildGroup();

        when(currentUserService.requireCurrentUser()).thenReturn(leader);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupManagementService.assignApplicationToGroup(20L, 30L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("无权操作该分组");
    }

    @Test
    void assignApplicationShouldRejectWhenApplicationDoesNotMatchGroup() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        Application application = Application.builder()
                .id(30L)
                .userId(7L)
                .status(ApplicationStatus.SUBMITTED)
                .directionLevel1Id(1L)
                .directionLevel2Id(3L)
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .build();

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(group));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));
        when(groupMemberRepository.findByApplicationId(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupManagementService.assignApplicationToGroup(20L, 30L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("报名申请与目标分组条件不匹配");
    }

    @Test
    void assignApplicationShouldPersistGroupMemberAndUpdateStatus() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        Application application = buildSubmittedApplication(30L, 7L);

        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(group));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));
        when(groupMemberRepository.findByApplicationId(30L)).thenReturn(Optional.empty());
        when(groupMemberRepository.countByGroupIdForUpdate(20L)).thenReturn(5L);

        groupManagementService.assignApplicationToGroup(20L, 30L);

        ArgumentCaptor<club.muimi.backend.entity.GroupMember> groupMemberCaptor =
                ArgumentCaptor.forClass(club.muimi.backend.entity.GroupMember.class);
        verify(groupMemberRepository).save(groupMemberCaptor.capture());
        assertThat(groupMemberCaptor.getValue().getGroupId()).isEqualTo(20L);
        assertThat(groupMemberCaptor.getValue().getApplicationId()).isEqualTo(30L);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.GROUPED);
        verify(applicationRepository).save(application);
    }

    @Test
    void addMemberShouldCreateApplicationThenAssign() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.ACTIVE);
        stubAdminAddMemberPrerequisites(admin, group, target);
        when(applicationRepository.findByUserIdAndDirectionLevel2Id(7L, 2L)).thenReturn(Optional.empty());
        Application[] persisted = new Application[1];
        when(applicationRepository.saveAndFlush(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setId(30L);
            persisted[0] = application;
            return application;
        });
        when(applicationRepository.findByIdForUpdate(30L)).thenAnswer(invocation -> Optional.ofNullable(persisted[0]));
        when(groupMemberRepository.findByApplicationId(30L)).thenReturn(Optional.empty());

        groupManagementService.addMemberToGroup(20L, buildAddMemberRequest());

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(applicationCaptor.capture());
        Application created = applicationCaptor.getValue();
        assertThat(created.getUserId()).isEqualTo(7L);
        assertThat(created.getDirectionLevel1Id()).isEqualTo(1L);
        assertThat(created.getDirectionLevel2Id()).isEqualTo(2L);
        assertThat(created.getGrade()).isEqualTo(Grade.YEAR_1);
        assertThat(created.getAdmissionYear()).isEqualTo(2026);
        assertThat(created.getRealName()).isEqualTo("张三");
        assertThat(created.getStatus()).isEqualTo(ApplicationStatus.GROUPED);
        verify(groupMemberRepository).save(any());
    }

    @Test
    void addMemberShouldReviveWithdrawnApplicationThenAssign() {
        addMemberShouldReviveThenAssign(ApplicationStatus.WITHDRAWN);
    }

    @Test
    void addMemberShouldReviveRejectedApplicationThenAssign() {
        addMemberShouldReviveThenAssign(ApplicationStatus.REJECTED);
    }

    @Test
    void addMemberShouldAssignMatchingSubmittedApplication() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.ACTIVE);
        Application application = buildSubmittedApplication(30L, 7L);
        stubAdminAddMemberPrerequisites(admin, group, target);
        when(applicationRepository.findByUserIdAndDirectionLevel2Id(7L, 2L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMemberRepository.findByApplicationId(30L)).thenReturn(Optional.empty());

        groupManagementService.addMemberToGroup(20L, buildAddMemberRequest());

        assertThat(application.getRealName()).isEqualTo("张三");
        assertThat(application.getPhoneNumber()).isEqualTo("13800000001");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.GROUPED);
        verify(groupMemberRepository).save(any());
    }

    @Test
    void addMemberShouldRejectMismatchedSubmittedApplication() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.ACTIVE);
        Application application = Application.builder()
                .id(30L)
                .userId(7L)
                .status(ApplicationStatus.SUBMITTED)
                .directionLevel1Id(1L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_2)
                .admissionYear(2026)
                .build();
        stubAdminAddMemberPrerequisites(admin, group, target);
        when(applicationRepository.findByUserIdAndDirectionLevel2Id(7L, 2L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> groupManagementService.addMemberToGroup(20L, buildAddMemberRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该用户该方向已有待分组申请，但与当前分组条件不匹配");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMemberShouldRejectAlreadyGroupedApplication() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.ACTIVE);
        Application application = Application.builder()
                .id(30L)
                .userId(7L)
                .status(ApplicationStatus.GROUPED)
                .directionLevel1Id(1L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .build();
        stubAdminAddMemberPrerequisites(admin, group, target);
        when(applicationRepository.findByUserIdAndDirectionLevel2Id(7L, 2L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> groupManagementService.addMemberToGroup(20L, buildAddMemberRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该用户该方向已完成分组，请先取消原分组");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMemberShouldRejectAdminUser() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.ADMIN, UserStatus.ACTIVE);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(group));
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> groupManagementService.addMemberToGroup(20L, buildAddMemberRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("不能将管理员加入分组");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMemberShouldRejectDisabledUser() {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.DISABLED);
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(group));
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> groupManagementService.addMemberToGroup(20L, buildAddMemberRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("该用户已被禁用，无法加入分组");
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void addMemberShouldRejectNonAdminActor() {
        LoginUser leader = buildLoginUser(10L, Role.LEADER);
        when(currentUserService.requireCurrentUser()).thenReturn(leader);

        assertThatThrownBy(() -> groupManagementService.addMemberToGroup(20L, buildAddMemberRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("当前角色无权添加分组成员");
        verify(groupMemberRepository, never()).save(any());
    }

    private void addMemberShouldReviveThenAssign(ApplicationStatus originalStatus) {
        LoginUser admin = buildLoginUser(1L, Role.ADMIN);
        RecruitmentGroup group = buildGroup();
        User target = buildUser(7L, Role.FRESHMAN, UserStatus.ACTIVE);
        Application application = Application.builder()
                .id(30L)
                .userId(7L)
                .status(originalStatus)
                .statusRemark("旧备注")
                .directionLevel1Id(9L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_2)
                .admissionYear(2024)
                .realName("旧姓名")
                .phoneNumber("13900000000")
                .college("旧学院")
                .major("旧专业")
                .className("旧班级")
                .build();
        stubAdminAddMemberPrerequisites(admin, group, target);
        when(applicationRepository.findByUserIdAndDirectionLevel2Id(7L, 2L)).thenReturn(Optional.of(application));
        when(applicationRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(application));
        when(applicationRepository.saveAndFlush(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMemberRepository.findByApplicationId(30L)).thenReturn(Optional.empty());

        groupManagementService.addMemberToGroup(20L, buildAddMemberRequest());

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.GROUPED);
        assertThat(application.getStatusRemark()).isNull();
        assertThat(application.getDirectionLevel1Id()).isEqualTo(1L);
        assertThat(application.getGrade()).isEqualTo(Grade.YEAR_1);
        assertThat(application.getAdmissionYear()).isEqualTo(2026);
        assertThat(application.getRealName()).isEqualTo("张三");
        verify(groupMemberRepository).save(any());
    }

    private void stubAdminAddMemberPrerequisites(LoginUser admin, RecruitmentGroup group, User target) {
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        doNothing().when(periodService).ensureSelectionOpenForGrouping();
        when(recruitmentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(groupMemberRepository.existsByUserIdAndGroupId(target.getId(), group.getId())).thenReturn(false);
        when(groupMemberRepository.countByGroupIdForUpdate(group.getId())).thenReturn(5L);
    }

    private RecruitmentGroup buildGroup() {
        return RecruitmentGroup.builder()
                .id(20L)
                .name("Java一组")
                .leaderUserId(99L)
                .directionLevel1Id(1L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .maxSize(10)
                .build();
    }

    private Application buildSubmittedApplication(Long applicationId, Long userId) {
        return Application.builder()
                .id(applicationId)
                .userId(userId)
                .status(ApplicationStatus.SUBMITTED)
                .directionLevel1Id(1L)
                .directionLevel2Id(2L)
                .grade(Grade.YEAR_1)
                .admissionYear(2026)
                .realName("原姓名")
                .phoneNumber("13700000000")
                .college("原学院")
                .major("原专业")
                .className("原班级")
                .build();
    }

    private User buildUser(Long userId, Role role, UserStatus status) {
        return User.builder()
                .id(userId)
                .username("user" + userId)
                .email("user" + userId + "@example.com")
                .passwordHash("hashed")
                .role(role)
                .status(status)
                .build();
    }

    private AdminAddGroupMemberRequest buildAddMemberRequest() {
        return new AdminAddGroupMemberRequest(
                7L,
                "张三",
                "13800000001",
                "计算机学院",
                "软件工程",
                "1班",
                "补录"
        );
    }

    private LoginUser buildLoginUser(Long userId, Role role) {
        return new LoginUser(
                userId,
                "tester",
                "tester@example.com",
                "hashed",
                role,
                UserStatus.ACTIVE,
                0L,
                "jti"
        );
    }
}
