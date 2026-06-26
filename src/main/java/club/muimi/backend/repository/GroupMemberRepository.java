package club.muimi.backend.repository;

import club.muimi.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findAllByUserId(Long userId);

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);
}
