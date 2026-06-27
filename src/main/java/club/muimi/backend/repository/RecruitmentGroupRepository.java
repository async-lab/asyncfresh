package club.muimi.backend.repository;

import club.muimi.backend.entity.RecruitmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RecruitmentGroupRepository extends JpaRepository<RecruitmentGroup, Long> {

    List<RecruitmentGroup> findAllByIdIn(Collection<Long> ids);

    List<RecruitmentGroup> findAllByLeaderUserId(Long leaderUserId);

    List<RecruitmentGroup> findAllByLeaderUserIdIn(Collection<Long> leaderUserIds);

    boolean existsByIdAndLeaderUserId(Long id, Long leaderUserId);
}
