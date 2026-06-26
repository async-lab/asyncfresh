package club.muimi.backend.repository;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    Optional<User> findByEmail(String email);
}
