package club.muimi.backend.repository;

import club.muimi.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findAllByUserId(Long userId);

    List<GroupMember> findAllByUserIdIn(Collection<Long> userIds);

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    Optional<GroupMember> findByApplicationId(Long applicationId);

    long countByUserId(Long userId);
}
