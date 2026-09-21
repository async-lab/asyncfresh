package club.muimi.backend.service.admin;

import club.muimi.backend.common.api.PageResult;
import club.muimi.backend.common.enums.ApplicationStatus;
import club.muimi.backend.common.enums.Grade;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.entity.Application;
import club.muimi.backend.entity.Direction;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.DirectionRepository;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.service.user.CurrentUserService;
import club.muimi.backend.vo.admin.AdminApplicationVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final DirectionRepository directionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;
    private final CurrentUserService currentUserService;
    private final Clock appClock;

    public AdminApplicationService(
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            DirectionRepository directionRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository,
            CurrentUserService currentUserService,
            Clock appClock
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.directionRepository = directionRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.currentUserService = currentUserService;
        this.appClock = appClock;
    }

    @Transactional(readOnly = true)
    public PageResult<AdminApplicationVo> listApplications(
            int page,
            int size,
            String keyword,
            ApplicationStatus status,
            Long directionLevel1Id,
            Long directionLevel2Id,
            Grade grade,
            Integer admissionYear
    ) {
        LoginUser currentUser = currentUserService.requireCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("只有管理员可以查看全部报名申请");
        }

        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Application> result = applicationRepository.searchApplications(
                status,
                directionLevel1Id,
                directionLevel2Id,
                grade,
                admissionYear,
                normalizeKeyword(keyword),
                pageable
        );
        List<Application> applications = result.getContent();
        if (applications.isEmpty()) {
            return PageResult.from(result.map(application -> toVo(application, Map.of(), Map.of(), Map.of(), Map.of())));
        }

        Map<Long, User> userMap = userRepository.findAllById(
                        applications.stream().map(Application::getUserId).collect(Collectors.toCollection(LinkedHashSet::new))
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Direction> directionMap = loadDirections(applications);
        Map<Long, GroupMember> memberMap = groupMemberRepository.findAllByApplicationIdIn(
                        applications.stream().map(Application::getId).toList()
                ).stream()
                .collect(Collectors.toMap(GroupMember::getApplicationId, Function.identity(), (left, right) -> left));
        Map<Long, RecruitmentGroup> groupMap = loadGroups(memberMap.values());

        return PageResult.from(result.map(application -> toVo(application, userMap, directionMap, memberMap, groupMap)));
    }

    private AdminApplicationVo toVo(
            Application application,
            Map<Long, User> userMap,
            Map<Long, Direction> directionMap,
            Map<Long, GroupMember> memberMap,
            Map<Long, RecruitmentGroup> groupMap
    ) {
        User user = userMap.get(application.getUserId());
        Direction level1 = directionMap.get(application.getDirectionLevel1Id());
        Direction level2 = directionMap.get(application.getDirectionLevel2Id());
        GroupMember member = memberMap.get(application.getId());
        RecruitmentGroup group = member == null ? null : groupMap.get(member.getGroupId());
        return new AdminApplicationVo(
                application.getId(),
                application.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getEmail(),
                application.getRealName(),
                application.getPhoneNumber(),
                application.getCollege(),
                application.getMajor(),
                application.getClassName(),
                application.getGrade(),
                application.getAdmissionYear(),
                application.getDirectionLevel1Id(),
                level1 == null ? null : level1.getName(),
                application.getDirectionLevel2Id(),
                level2 == null ? null : level2.getName(),
                application.getIntroduction(),
                application.getStatus(),
                application.getStatusRemark(),
                group == null ? null : group.getId(),
                group == null ? null : group.getName(),
                toOffsetDateTime(application.getCreatedAt()),
                toOffsetDateTime(application.getUpdatedAt())
        );
    }

    private Map<Long, Direction> loadDirections(Collection<Application> applications) {
        Set<Long> directionIds = new LinkedHashSet<>();
        for (Application application : applications) {
            directionIds.add(application.getDirectionLevel1Id());
            directionIds.add(application.getDirectionLevel2Id());
        }
        return directionRepository.findAllById(directionIds).stream()
                .collect(Collectors.toMap(Direction::getId, Function.identity()));
    }

    private Map<Long, RecruitmentGroup> loadGroups(Collection<GroupMember> members) {
        if (members.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> groupIds = members.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return recruitmentGroupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(RecruitmentGroup::getId, Function.identity()));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private OffsetDateTime toOffsetDateTime(java.time.LocalDateTime value) {
        return value.atZone(appClock.getZone()).toOffsetDateTime();
    }
}
