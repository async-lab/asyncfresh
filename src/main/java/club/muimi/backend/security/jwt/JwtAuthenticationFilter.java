package club.muimi.backend.security.jwt;

import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.entity.User;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.security.cookie.AuthCookieService;
import club.muimi.backend.support.redis.AuthCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthCookieService authCookieService;
    private final JwtTokenService jwtTokenService;
    private final AuthCacheService authCacheService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            AuthCookieService authCookieService,
            JwtTokenService jwtTokenService,
            AuthCacheService authCacheService,
            UserRepository userRepository
    ) {
        this.authCookieService = authCookieService;
        this.jwtTokenService = jwtTokenService;
        this.authCacheService = authCacheService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateFromCookie(request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateFromCookie(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> tokenOptional = authCookieService.resolveToken(request);
        if (tokenOptional.isEmpty()) {
            return;
        }

        try {
            JwtClaims claims = jwtTokenService.parse(tokenOptional.get());
            if (authCacheService.isTokenBlacklisted(claims.jti())) {
                clearContextAndCookie(response);
                return;
            }

            User user = userRepository.findById(claims.userId()).orElse(null);
            if (user == null || user.getStatus() != UserStatus.ACTIVE || !user.getTokenVersion().equals(claims.tokenVersion())) {
                clearContextAndCookie(response);
                return;
            }

            // 这里同时校验 tokenVersion，确保改密/重置密码后旧 Token 立即失效。
            LoginUser loginUser = new LoginUser(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    user.getRole(),
                    user.getStatus(),
                    user.getTokenVersion(),
                    claims.jti()
            );
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    loginUser,
                    null,
                    loginUser.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            clearContextAndCookie(response);
        }
    }

    private void clearContextAndCookie(HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        authCookieService.clearLoginCookie(response);
    }
}
