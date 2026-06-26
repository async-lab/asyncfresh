package club.muimi.backend.repository;

import club.muimi.backend.entity.RecruitmentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecruitmentGroupRepository extends JpaRepository<RecruitmentGroup, Long> {

    List<RecruitmentGroup> findAllByIdIn(Collection<Long> ids);

    Optional<RecruitmentGroup> findByLeaderUserId(Long leaderUserId);

    boolean existsByIdAndLeaderUserId(Long id, Long leaderUserId);
}
