package club.muimi.backend.security.auth;

import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final UserStatus status;
    private final Long tokenVersion;
    private final String tokenJti;
    private final List<GrantedAuthority> authorities;

    public LoginUser(
            Long userId,
            String username,
            String email,
            String passwordHash,
            Role role,
            UserStatus status,
            Long tokenVersion,
            String tokenJti
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.tokenVersion = tokenVersion;
        this.tokenJti = tokenJti;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getTokenVersion() {
        return tokenVersion;
    }

    public String getTokenJti() {
        return tokenJti;
    }

    public String getDisplayUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
