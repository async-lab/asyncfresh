package club.muimi.backend.service.direction;

import club.muimi.backend.dto.admin.DirectionUpsertRequest;
import club.muimi.backend.entity.Direction;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.NotFoundException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.DirectionRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.vo.admin.AdminDirectionTreeVo;
import club.muimi.backend.vo.direction.DirectionTreeVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DirectionService {

    private static final int LEVEL_ROOT = 1;
    private static final int LEVEL_CHILD = 2;

    private final DirectionRepository directionRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;

    public DirectionService(
            DirectionRepository directionRepository,
            ApplicationRepository applicationRepository,
            RecruitmentGroupRepository recruitmentGroupRepository
    ) {
        this.directionRepository = directionRepository;
        this.applicationRepository = applicationRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<DirectionTreeVo> listPublicTree(boolean enabledOnly) {
        List<Direction> directions = enabledOnly
                ? directionRepository.findAllByEnabledTrueOrderByLevelAscSortOrderAscIdAsc()
                : directionRepository.findAllByOrderByLevelAscSortOrderAscIdAsc();
        return buildPublicTree(directions);
    }

    @Transactional(readOnly = true)
    public List<AdminDirectionTreeVo> listAdminTree() {
        return buildAdminTree(directionRepository.findAllByOrderByLevelAscSortOrderAscIdAsc());
    }

    @Transactional
    public AdminDirectionTreeVo createDirection(DirectionUpsertRequest request) {
        Direction direction = Direction.builder().build();
        applyDirectionChange(direction, request, null);
        Direction saved = directionRepository.save(direction);
        return getAdminDirectionTree(saved.getId());
    }

    @Transactional
    public AdminDirectionTreeVo updateDirection(Long directionId, DirectionUpsertRequest request) {
        Direction direction = getDirectionOrThrow(directionId);
        applyDirectionChange(direction, request, directionId);
        directionRepository.save(direction);
        return getAdminDirectionTree(directionId);
    }

    @Transactional
    public void deleteDirection(Long directionId) {
        Direction direction = getDirectionOrThrow(directionId);
        if (direction.getLevel() == LEVEL_ROOT && directionRepository.existsByParentId(directionId)) {
            throw new ConflictException("该一级方向下仍存在二级方向，暂不允许删除");
        }
        if (direction.getLevel() == LEVEL_ROOT && applicationRepository.existsByDirectionLevel1Id(directionId)) {
            throw new ConflictException("该方向已被报名申请使用，暂不允许删除");
        }
        if (direction.getLevel() == LEVEL_CHILD && applicationRepository.existsByDirectionLevel2Id(directionId)) {
            throw new ConflictException("该方向已被报名申请使用，暂不允许删除");
        }
        if (direction.getLevel() == LEVEL_ROOT && recruitmentGroupRepository.existsByDirectionLevel1Id(directionId)) {
            throw new ConflictException("该方向已被分组使用，暂不允许删除");
        }
        if (direction.getLevel() == LEVEL_CHILD && recruitmentGroupRepository.existsByDirectionLevel2Id(directionId)) {
            throw new ConflictException("该方向已被分组使用，暂不允许删除");
        }
        directionRepository.delete(direction);
    }

    private void applyDirectionChange(Direction direction, DirectionUpsertRequest request, Long currentDirectionId) {
        Long normalizedParentId = request.parentId();
        String normalizedName = request.name().trim();

        if (currentDirectionId != null && !Objects.equals(direction.getParentId(), normalizedParentId)) {
            validateParentChangeAllowed(currentDirectionId);
        }

        Direction parent = null;
        int level = LEVEL_ROOT;
        if (normalizedParentId != null) {
            parent = getDirectionOrThrow(normalizedParentId);
            if (Objects.equals(normalizedParentId, currentDirectionId)) {
                throw new ConflictException("方向不能将自己设置为父级");
            }
            if (parent.getLevel() != LEVEL_ROOT) {
                throw new ConflictException("二级方向只能挂载在一级方向下");
            }
            level = LEVEL_CHILD;
        }

        if (currentDirectionId != null
                && direction.getLevel() == LEVEL_ROOT
                && level == LEVEL_CHILD
                && directionRepository.existsByParentId(currentDirectionId)) {
            throw new ConflictException("存在下级方向的一级方向不能直接改为二级方向");
        }

        boolean duplicated = currentDirectionId == null
                ? directionRepository.existsByParentIdAndNameIgnoreCase(normalizedParentId, normalizedName)
                : directionRepository.existsByParentIdAndNameIgnoreCaseAndIdNot(normalizedParentId, normalizedName, currentDirectionId);
        if (duplicated) {
            throw new ConflictException("同级方向名称已存在");
        }

        direction.setParentId(parent == null ? null : parent.getId());
        direction.setName(normalizedName);
        direction.setLevel(level);
        direction.setSortOrder(request.sortOrder());
        direction.setEnabled(request.enabled());
    }

    private void validateParentChangeAllowed(Long directionId) {
        if (directionRepository.existsByParentId(directionId)) {
            throw new ConflictException("存在下级方向的一级方向不能修改层级归属");
        }
        if (applicationRepository.existsByDirectionLevel1Id(directionId)
                || applicationRepository.existsByDirectionLevel2Id(directionId)
                || recruitmentGroupRepository.existsByDirectionLevel1Id(directionId)
                || recruitmentGroupRepository.existsByDirectionLevel2Id(directionId)) {
            throw new ConflictException("已被业务数据使用的方向不能修改层级归属");
        }
    }

    private Direction getDirectionOrThrow(Long directionId) {
        return directionRepository.findById(directionId)
                .orElseThrow(() -> new NotFoundException("方向不存在"));
    }

    private AdminDirectionTreeVo getAdminDirectionTree(Long directionId) {
        List<AdminDirectionTreeVo> roots = buildAdminTree(directionRepository.findAllByOrderByLevelAscSortOrderAscIdAsc());
        return findAdminDirection(roots, directionId)
                .orElseThrow(() -> new NotFoundException("方向不存在"));
    }

    private Optional<AdminDirectionTreeVo> findAdminDirection(List<AdminDirectionTreeVo> roots, Long directionId) {
        for (AdminDirectionTreeVo root : roots) {
            if (Objects.equals(root.id(), directionId)) {
                return Optional.of(root);
            }
            Optional<AdminDirectionTreeVo> child = findAdminDirection(root.children(), directionId);
            if (child.isPresent()) {
                return child;
            }
        }
        return Optional.empty();
    }

    private List<DirectionTreeVo> buildPublicTree(List<Direction> directions) {
        Map<Long, List<Direction>> childrenMap = buildChildrenMap(directions);
        return directions.stream()
                .filter(direction -> direction.getParentId() == null)
                .map(direction -> new DirectionTreeVo(
                        direction.getId(),
                        direction.getName(),
                        direction.getLevel(),
                        buildPublicChildren(direction.getId(), childrenMap)
                ))
                .toList();
    }

    private List<DirectionTreeVo> buildPublicChildren(Long parentId, Map<Long, List<Direction>> childrenMap) {
        return childrenMap.getOrDefault(parentId, List.of()).stream()
                .map(direction -> new DirectionTreeVo(
                        direction.getId(),
                        direction.getName(),
                        direction.getLevel(),
                        List.of()
                ))
                .toList();
    }

    private List<AdminDirectionTreeVo> buildAdminTree(List<Direction> directions) {
        Map<Long, List<Direction>> childrenMap = buildChildrenMap(directions);
        return directions.stream()
                .filter(direction -> direction.getParentId() == null)
                .map(direction -> toAdminVo(direction, childrenMap))
                .toList();
    }

    private AdminDirectionTreeVo toAdminVo(Direction direction, Map<Long, List<Direction>> childrenMap) {
        List<AdminDirectionTreeVo> children = childrenMap.getOrDefault(direction.getId(), List.of()).stream()
                .map(child -> toAdminVo(child, childrenMap))
                .toList();
        return new AdminDirectionTreeVo(
                direction.getId(),
                direction.getParentId(),
                direction.getName(),
                direction.getLevel(),
                direction.getSortOrder(),
                direction.getEnabled(),
                children
        );
    }

    private Map<Long, List<Direction>> buildChildrenMap(List<Direction> directions) {
        Map<Long, List<Direction>> result = new HashMap<>();
        for (Direction direction : directions) {
            if (direction.getParentId() != null) {
                result.computeIfAbsent(direction.getParentId(), ignored -> new ArrayList<>()).add(direction);
            }
        }
        return result;
    }
}
