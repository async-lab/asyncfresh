package club.muimi.backend.service.admin;

import club.muimi.backend.common.api.PageResult;
import club.muimi.backend.common.enums.AuditModule;
import club.muimi.backend.common.enums.AuditSeverity;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.dto.admin.CreateAdminUserRequest;
import club.muimi.backend.dto.admin.UpdateAdminUserRequest;
import club.muimi.backend.dto.admin.UpdateUserRoleRequest;
import club.muimi.backend.dto.admin.UpdateUserStatusRequest;
import club.muimi.backend.entity.Application;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.NotFoundException;
import club.muimi.backend.exception.ValidationException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.audit.AuditLogCommand;
import club.muimi.backend.service.audit.AuditLogService;
import club.muimi.backend.service.user.CurrentUserService;
import club.muimi.backend.vo.admin.AdminUserDetailVo;
import club.muimi.backend.vo.admin.AdminUserSummaryVo;
import club.muimi.backend.vo.auth.GroupSimpleVo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final UserReferenceChecker userReferenceChecker;

    public AdminUserService(
            UserRepository userRepository,
            ApplicationRepository applicationRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder,
            UserReferenceChecker userReferenceChecker
    ) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.userReferenceChecker = userReferenceChecker;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminUserSummaryVo> listUsers(
            int page,
            int size,
            Role role,
            UserStatus status,
            String keyword
    ) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = userRepository.searchUsers(role, status, normalizeKeyword(keyword), pageable);
        List<User> users = result.getContent();
        List<Long> userIds = users.stream().map(User::getId).toList();

        Map<Long, Long> applicationCountMap = buildApplicationCountMap(userIds);
        Map<Long, List<GroupSimpleVo>> groupsMap = buildUserGroupsMap(userIds);
        Map<Long, Long> leaderGroupCountMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : recruitmentGroupRepository.findAllByLeaderUserIdIn(userIds)
                .stream()
                .collect(Collectors.groupingBy(RecruitmentGroup::getLeaderUserId, Collectors.counting()));

        Page<AdminUserSummaryVo> mappedPage = result.map(user -> {
            List<GroupSimpleVo> groups = groupsMap.getOrDefault(user.getId(), List.of());
            return new AdminUserSummaryVo(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    Boolean.TRUE.equals(user.getEmailVerified()),
                    leaderGroupCountMap.getOrDefault(user.getId(), 0L),
                    applicationCountMap.getOrDefault(user.getId(), 0L),
                    groups.size(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    groups
            );
        });
        return PageResult.from(mappedPage);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailVo getUserDetail(Long userId) {
        User user = getUserOrThrow(userId);
        return toDetailVo(user);
    }

    @Transactional
    public AdminUserDetailVo createUser(CreateAdminUserRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        String username = normalizeRequiredText(request.username(), "用户名不能为空");
        String email = normalizeRequiredText(request.email(), "邮箱不能为空");
        validatePasswordStrength(request.password());
        Role role = request.role() == null ? Role.FRESHMAN : request.role();
        validateManagedRole(role);
        UserStatus status = request.status() == null ? UserStatus.ACTIVE : request.status();
        boolean emailVerified = request.emailVerified() == null || Boolean.TRUE.equals(request.emailVerified());

        ensureUsernameNotExists(username);
        ensureEmailNotExists(email);

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(emailVerified)
                .role(role)
                .status(status)
                .tokenVersion(0L)
                .lastLoginAt(null)
                .build();
        saveUser(user);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("username", user.getUsername());
        detail.put("email", user.getEmail());
        detail.put("role", user.getRole());
        detail.put("status", user.getStatus());
        detail.put("emailVerified", user.getEmailVerified());
        auditLogService.record(AuditLogCommand.builder(
                        AuditModule.AUTH,
                        "CREATE_USER",
                        AuditSeverity.IMPORTANT,
                        "创建用户"
                ).actor(currentUser)
                .target("USER", user.getId())
                .detail(detail)
                .build());
        return toDetailVo(user);
    }

    @Transactional
    public AdminUserDetailVo updateUser(Long userId, UpdateAdminUserRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw new ForbiddenException("管理员不能修改自己的账号信息");
        }

        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("管理员账号不允许通过该接口修改");
        }

        String username = normalizeRequiredText(request.username(), "用户名不能为空");
        String email = normalizeRequiredText(request.email(), "邮箱不能为空");
        validateManagedRole(request.role());
        ensureLeaderCanBeDemoted(userId, user.getRole(), request.role());

        Role previousRole = user.getRole();
        UserStatus previousStatus = user.getStatus();
        boolean invalidateTokens = false;

        if (!username.equals(user.getUsername())) {
            ensureUsernameAvailable(username, userId);
            user.setUsername(username);
        }
        if (!email.equals(user.getEmail())) {
            ensureEmailAvailable(email, userId);
            user.setEmail(email);
        }
        if (hasText(request.password())) {
            validatePasswordStrength(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            invalidateTokens = true;
        }
        if (previousRole != request.role()) {
            user.setRole(request.role());
            invalidateTokens = true;
        }
        user.setStatus(request.status());
        if (request.emailVerified() != null) {
            user.setEmailVerified(request.emailVerified());
        }
        if (invalidateTokens) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        saveUser(user);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("previousRole", previousRole);
        detail.put("currentRole", user.getRole());
        detail.put("previousStatus", previousStatus);
        detail.put("currentStatus", user.getStatus());
        detail.put("username", user.getUsername());
        detail.put("email", user.getEmail());
        detail.put("passwordChanged", hasText(request.password()));
        detail.put("tokenVersion", user.getTokenVersion());
        auditLogService.record(AuditLogCommand.builder(
                        AuditModule.AUTH,
                        "UPDATE_USER",
                        request.status() == UserStatus.DISABLED ? AuditSeverity.MAJOR : AuditSeverity.IMPORTANT,
                        "更新用户信息"
                ).actor(currentUser)
                .target("USER", userId)
                .detail(detail)
                .build());
        return toDetailVo(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw new ForbiddenException("管理员不能删除自己的账号");
        }

        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("管理员账号不允许通过该接口删除");
        }
        userReferenceChecker.ensureDeletable(userId);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("username", user.getUsername());
        detail.put("email", user.getEmail());
        detail.put("role", user.getRole());
        detail.put("status", user.getStatus());
        try {
            userRepository.delete(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("该用户存在关联业务数据，无法直接删除");
        }

        auditLogService.record(AuditLogCommand.builder(
                        AuditModule.AUTH,
                        "DELETE_USER",
                        AuditSeverity.MAJOR,
                        "删除用户"
                ).actor(currentUser)
                .target("USER", userId)
                .detail(detail)
                .build());
    }

    @Transactional
    public AdminUserDetailVo updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw new ForbiddenException("管理员不能修改自己的状态");
        }

        User user = getUserOrThrow(userId);
        UserStatus previousStatus = user.getStatus();
        user.setStatus(request.status());
        userRepository.save(user);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("previousStatus", previousStatus);
        detail.put("currentStatus", request.status());
        detail.put("targetRole", user.getRole());
        auditLogService.record(AuditLogCommand.builder(
                        AuditModule.AUTH,
                        "UPDATE_USER_STATUS",
                        request.status() == UserStatus.DISABLED ? AuditSeverity.MAJOR : AuditSeverity.IMPORTANT,
                        "更新用户状态"
                ).actor(currentUser)
                .target("USER", userId)
                .detail(detail)
                .build());
        return toDetailVo(user);
    }

    @Transactional
    public AdminUserDetailVo updateUserRole(Long userId, UpdateUserRoleRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw new ForbiddenException("管理员不能修改自己的角色");
        }
        validateManagedRole(request.role());

        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("管理员账号角色不允许通过该接口修改");
        }
        Role previousRole = user.getRole();
        if (previousRole == request.role()) {
            return toDetailVo(user);
        }
        ensureLeaderCanBeDemoted(userId, previousRole, request.role());

        user.setRole(request.role());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("previousRole", previousRole);
        detail.put("currentRole", request.role());
        detail.put("tokenVersion", user.getTokenVersion());
        auditLogService.record(AuditLogCommand.builder(
                        AuditModule.AUTH,
                        "UPDATE_USER_ROLE",
                        AuditSeverity.IMPORTANT,
                        "更新用户角色"
                ).actor(currentUser)
                .target("USER", userId)
                .detail(detail)
                .build());
        return toDetailVo(user);
    }

    private AdminUserDetailVo toDetailVo(User user) {
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(user.getId());
        Set<Long> groupIds = groupMembers.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toSet());
        List<GroupSimpleVo> groups = groupIds.isEmpty()
                ? List.of()
                : recruitmentGroupRepository.findAllByIdIn(groupIds)
                .stream()
                .map(group -> new GroupSimpleVo(group.getId(), group.getName()))
                .toList();
        List<GroupSimpleVo> leaderGroups = recruitmentGroupRepository.findAllByLeaderUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(group -> new GroupSimpleVo(group.getId(), group.getName()))
                .toList();

        return new AdminUserDetailVo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                leaderGroups,
                applicationRepository.countByUserId(user.getId()),
                groupMembers.size(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                groups
        );
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private void validateManagedRole(Role role) {
        if (role != Role.FRESHMAN && role != Role.LEADER) {
            throw new ValidationException("用户角色只能在 FRESHMAN 与 LEADER 之间调整");
        }
    }

    private void ensureLeaderCanBeDemoted(Long userId, Role previousRole, Role nextRole) {
        if (previousRole == Role.LEADER && nextRole == Role.FRESHMAN
                && recruitmentGroupRepository.findAllByLeaderUserId(userId).stream().findAny().isPresent()) {
            throw new ConflictException("该负责人仍绑定负责的分组，不能降级为新生");
        }
    }

    private void ensureUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("用户名已存在");
        }
    }

    private void ensureEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("邮箱已被注册");
        }
    }

    private void ensureUsernameAvailable(String username, Long userId) {
        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new ConflictException("用户名已存在");
        }
    }

    private void ensureEmailAvailable(String email, Long userId) {
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ConflictException("邮箱已被注册");
        }
    }

    private void saveUser(User user) {
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("用户名或邮箱已存在");
        }
    }

    private void validatePasswordStrength(String password) {
        boolean valid = password != null
                && password.length() >= 8
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw new ValidationException("密码至少 8 位，且必须同时包含字母和数字");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<Long, Long> buildApplicationCountMap(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return applicationRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.groupingBy(Application::getUserId, Collectors.counting()));
    }

    private Map<Long, List<GroupSimpleVo>> buildUserGroupsMap(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GroupMember> members = groupMemberRepository.findAllByUserIdIn(userIds);
        if (members.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> groupIds = members.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, GroupSimpleVo> groupMap = recruitmentGroupRepository.findAllByIdIn(groupIds).stream()
                .collect(Collectors.toMap(
                        RecruitmentGroup::getId,
                        group -> new GroupSimpleVo(group.getId(), group.getName()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, List<GroupSimpleVo>> groupsByUserId = new LinkedHashMap<>();
        for (GroupMember member : members) {
            GroupSimpleVo group = groupMap.get(member.getGroupId());
            if (group == null) {
                continue;
            }
            List<GroupSimpleVo> userGroups = groupsByUserId.computeIfAbsent(member.getUserId(), ignored -> new ArrayList<>());
            boolean alreadyPresent = userGroups.stream().anyMatch(existing -> existing.id().equals(group.id()));
            if (!alreadyPresent) {
                userGroups.add(group);
            }
        }
        return groupsByUserId;
    }
}
