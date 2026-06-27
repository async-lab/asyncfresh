package club.muimi.backend.service.admin;

import club.muimi.backend.common.api.PageResult;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.dto.admin.UpdateUserStatusRequest;
import club.muimi.backend.entity.Application;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.NotFoundException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.user.CurrentUserService;
import club.muimi.backend.vo.admin.AdminUserDetailVo;
import club.muimi.backend.vo.admin.AdminUserSummaryVo;
import club.muimi.backend.vo.auth.GroupSimpleVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
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

    public AdminUserService(
            UserRepository userRepository,
            ApplicationRepository applicationRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.currentUserService = currentUserService;
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
        Map<Long, Long> groupCountMap = buildGroupCountMap(userIds);
        Map<Long, Long> leaderGroupMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : recruitmentGroupRepository.findAllByLeaderUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        RecruitmentGroup::getLeaderUserId,
                        RecruitmentGroup::getId,
                        Math::min
                ));

        Page<AdminUserSummaryVo> mappedPage = result.map(user -> new AdminUserSummaryVo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                leaderGroupMap.get(user.getId()),
                applicationCountMap.getOrDefault(user.getId(), 0L),
                groupCountMap.getOrDefault(user.getId(), 0L),
                user.getLastLoginAt(),
                user.getCreatedAt()
        ));
        return PageResult.from(mappedPage);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailVo getUserDetail(Long userId) {
        User user = getUserOrThrow(userId);
        return toDetailVo(user);
    }

    @Transactional
    public AdminUserDetailVo updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getUserId().equals(userId)) {
            throw new ForbiddenException("管理员不能修改自己的状态");
        }

        User user = getUserOrThrow(userId);
        user.setStatus(request.status());
        userRepository.save(user);
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
        Long leaderGroupId = recruitmentGroupRepository.findAllByLeaderUserId(user.getId())
                .stream()
                .map(RecruitmentGroup::getId)
                .min(Long::compareTo)
                .orElse(null);

        return new AdminUserDetailVo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                leaderGroupId,
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

    private Map<Long, Long> buildGroupCountMap(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return groupMemberRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.groupingBy(GroupMember::getUserId, Collectors.counting()));
    }
}
