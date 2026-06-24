package club.muimi.backend.entity;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class User {
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private Role role = Role.FRESHMAN;

    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private Long tokenVersion = 0L;

    @Column(nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
