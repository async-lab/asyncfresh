package club.muimi.backend.service.direction;

import club.muimi.backend.dto.admin.DirectionUpsertRequest;
import club.muimi.backend.entity.Direction;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.repository.ApplicationRepository;
import club.muimi.backend.repository.DirectionRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectionServiceTest {

    @Mock
    private DirectionRepository directionRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;

    @Test
    void listPublicTreeShouldOnlyReturnEnabledTreeNodes() {
        Direction enabledRoot = Direction.builder()
                .id(1L)
                .name("Backend")
                .level(1)
                .sortOrder(1)
                .enabled(true)
                .build();
        Direction enabledChild = Direction.builder()
                .id(2L)
                .parentId(1L)
                .name("Java")
                .level(2)
                .sortOrder(1)
                .enabled(true)
                .build();
        Direction orphanChild = Direction.builder()
                .id(3L)
                .parentId(99L)
                .name("Orphan")
                .level(2)
                .sortOrder(1)
                .enabled(true)
                .build();
        when(directionRepository.findAllByEnabledTrueOrderByLevelAscSortOrderAscIdAsc())
                .thenReturn(List.of(enabledRoot, enabledChild, orphanChild));

        DirectionService service = new DirectionService(
                directionRepository,
                applicationRepository,
                recruitmentGroupRepository
        );

        var result = service.listPublicTree(true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
        assertThat(result.getFirst().children()).hasSize(1);
        assertThat(result.getFirst().children().getFirst().id()).isEqualTo(2L);
    }

    @Test
    void createDirectionShouldRejectChildOfSecondLevelDirection() {
        Direction secondLevel = Direction.builder()
                .id(2L)
                .parentId(1L)
                .name("Java")
                .level(2)
                .sortOrder(1)
                .enabled(true)
                .build();
        when(directionRepository.findById(2L)).thenReturn(Optional.of(secondLevel));

        DirectionService service = new DirectionService(
                directionRepository,
                applicationRepository,
                recruitmentGroupRepository
        );

        assertThatThrownBy(() -> service.createDirection(new DirectionUpsertRequest(2L, "Spring", 1, true)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("二级方向只能挂载在一级方向下");
    }

    @Test
    void updateDirectionShouldRejectParentChangeWhenDirectionIsInUse() {
        Direction existing = Direction.builder()
                .id(2L)
                .parentId(1L)
                .name("Java")
                .level(2)
                .sortOrder(1)
                .enabled(true)
                .build();
        when(directionRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(applicationRepository.existsByDirectionLevel1Id(2L)).thenReturn(false);
        when(applicationRepository.existsByDirectionLevel2Id(2L)).thenReturn(true);

        DirectionService service = new DirectionService(
                directionRepository,
                applicationRepository,
                recruitmentGroupRepository
        );

        assertThatThrownBy(() -> service.updateDirection(2L, new DirectionUpsertRequest(null, "Java", 1, true)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("已被业务数据使用的方向不能修改层级归属");
    }
}
