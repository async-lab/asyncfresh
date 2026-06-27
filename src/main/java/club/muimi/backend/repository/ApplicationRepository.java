package club.muimi.backend.repository;

import club.muimi.backend.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByUserIdIn(Collection<Long> userIds);

    long countByUserId(Long userId);
}
