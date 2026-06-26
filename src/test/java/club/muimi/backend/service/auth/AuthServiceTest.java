package club.muimi.backend.service.auth;

import club.muimi.backend.common.enums.EmailCodeScene;
import club.muimi.backend.common.enums.Role;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.config.AuthProperties;
import club.muimi.backend.dto.auth.ChangePasswordRequest;
import club.muimi.backend.dto.auth.LoginRequest;
import club.muimi.backend.dto.auth.RegisterRequest;
import club.muimi.backend.dto.auth.ResetPasswordRequest;
import club.muimi.backend.dto.auth.SendEmailCodeRequest;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.UnauthorizedException;
import club.muimi.backend.vo.auth.CurrentUserVo;
import club.muimi.backend.repository.GroupMemberRepository;
import club.muimi.backend.repository.RecruitmentGroupRepository;
import club.muimi.backend.repository.UserRepository;
import club.muimi.backend.security.auth.LoginUser;
import club.muimi.backend.security.cookie.AuthCookieService;
import club.muimi.backend.security.jwt.JwtClaims;
import club.muimi.backend.security.jwt.JwtTokenService;
import club.muimi.backend.service.period.PeriodService;
import club.muimi.backend.service.user.CurrentUserService;
import club.muimi.backend.support.mail.MailService;
import club.muimi.backend.support.redis.AuthCacheService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private RecruitmentGroupRepository recruitmentGroupRepository;
    @Mock
    private PeriodService periodService;
    @Mock
    private AuthCacheService authCacheService;
    @Mock
    private MailService mailService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private AuthCookieService authCookieService;
    @Mock
    private CurrentUserService currentUserService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getEmailCode().setTtlSeconds(300);
        authProperties.getEmailCode().setSendCooldownSeconds(60);
        authProperties.getLogin().setMaxFailCount(5);
        authProperties.getLogin().setFailLockSeconds(900);
        authService = new AuthService(
                userRepository,
                groupMemberRepository,
                recruitmentGroupRepository,
                periodService,
                authCacheService,
                mailService,
                passwordEncoder,
                jwtTokenService,
                authCookieService,
                authProperties,
                currentUserService
        );
    }

    @Test
    void registerShouldCreateFreshmanUserWhenCodeValid() {
        RegisterRequest request = new RegisterRequest(
                "zhangsan",
                "user@example.com",
                "Pass1234",
                "Pass1234",
                "123456"
        );
        doNothing().when(periodService).ensureRegistrationOpen();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(authCacheService.getEmailCode(EmailCodeScene.REGISTER, request.email())).thenReturn(Optional.of("123456"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        var result = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(Role.FRESHMAN);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches("Pass1234", savedUser.getPasswordHash())).isTrue();
        assertThat(result.id()).isEqualTo(1L);
        verify(authCacheService).deleteEmailCode(EmailCodeScene.REGISTER, request.email());
    }

    @Test
    void loginShouldWriteCookieWhenCredentialsCorrect() {
        User user = User.builder()
                .id(1L)
                .username("zhangsan")
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Pass1234"))
                .emailVerified(true)
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        when(authCacheService.isLoginLocked(user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenService.generateToken(user, true)).thenReturn("token-value");

        var result = authService.login(new LoginRequest(user.getEmail(), "Pass1234", true), new MockHttpServletResponse());

        assertThat(result.email()).isEqualTo(user.getEmail());
        verify(authCacheService).clearLoginFailCount(user.getEmail());
        verify(authCookieService).writeLoginCookie(any(MockHttpServletResponse.class), anyString(), anyBoolean());
        verify(authCookieService).writeCsrfCookie(any(MockHttpServletResponse.class), anyString());
    }

    @Test
    void loginShouldRecordFailureWhenPasswordIncorrect() {
        User user = User.builder()
                .id(1L)
                .username("zhangsan")
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Pass1234"))
                .emailVerified(true)
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        when(authCacheService.isLoginLocked(user.getEmail())).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(authCacheService.incrementLoginFailCount(anyString(), any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong", false), new MockHttpServletResponse()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("邮箱或密码错误");

        verify(authCacheService).incrementLoginFailCount(anyString(), any());
    }

    @Test
    void changePasswordShouldIncreaseTokenVersionAndClearCookie() {
        User user = User.builder()
                .id(1L)
                .username("zhangsan")
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("OldPass123"))
                .emailVerified(true)
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(3L)
                .build();
        LoginUser loginUser = new LoginUser(1L, "zhangsan", "user@example.com", user.getPasswordHash(), Role.FRESHMAN, UserStatus.ACTIVE, 3L, "jti-1");
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(currentUserService.requireCurrentUser()).thenReturn(loginUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(authCookieService.resolveToken(httpServletRequest)).thenReturn(Optional.of("token"));
        when(jwtTokenService.parse("token")).thenReturn(new JwtClaims(1L, "FRESHMAN", 3L, "jti-1", Instant.now().plusSeconds(600)));
        when(jwtTokenService.remainingValidity(any())).thenReturn(Duration.ofMinutes(10));

        authService.changePassword(
                new ChangePasswordRequest("OldPass123", "NewPass123", "NewPass123"),
                httpServletRequest,
                new MockHttpServletResponse()
        );

        assertThat(user.getTokenVersion()).isEqualTo(4L);
        assertThat(passwordEncoder.matches("NewPass123", user.getPasswordHash())).isTrue();
        verify(authCacheService).blacklistToken(anyString(), any());
        verify(authCookieService).clearLoginCookie(any(MockHttpServletResponse.class));
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndDeleteVerificationCode() {
        User user = User.builder()
                .id(1L)
                .username("zhangsan")
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("OldPass123"))
                .emailVerified(true)
                .role(Role.FRESHMAN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(2L)
                .build();
        ResetPasswordRequest request = new ResetPasswordRequest("user@example.com", "654321", "NewPass123", "NewPass123");
        when(authCacheService.getEmailCode(EmailCodeScene.RESET_PASSWORD, request.email())).thenReturn(Optional.of("654321"));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        authService.resetPassword(request);

        assertThat(user.getTokenVersion()).isEqualTo(3L);
        assertThat(passwordEncoder.matches("NewPass123", user.getPasswordHash())).isTrue();
        verify(authCacheService).deleteEmailCode(EmailCodeScene.RESET_PASSWORD, request.email());
    }

    @Test
    void sendEmailCodeShouldNotLeakWhetherResetAccountExists() {
        SendEmailCodeRequest request = new SendEmailCodeRequest("missing@example.com", EmailCodeScene.RESET_PASSWORD);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(authCacheService.hasEmailCooldown(request.scene(), request.email())).thenReturn(false);

        authService.sendEmailCode(request);

        verify(authCacheService).markEmailCooldown(any(), anyString(), any());
    }

    @Test
    void getCurrentUserShouldReturnEmptyGroupsForAdminWithoutMembership() {
        User admin = User.builder()
                .id(99L)
                .username("admin")
                .email("admin@example.com")
                .passwordHash("hashed")
                .emailVerified(true)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .build();
        LoginUser loginUser = new LoginUser(99L, "admin", "admin@example.com", "hashed", Role.ADMIN, UserStatus.ACTIVE, 0L, "jti-admin");
        when(currentUserService.requireCurrentUser()).thenReturn(loginUser);
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(groupMemberRepository.findAllByUserId(99L)).thenReturn(List.of());
        when(recruitmentGroupRepository.findByLeaderUserId(99L)).thenReturn(Optional.empty());

        CurrentUserVo result = authService.getCurrentUser();

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        assertThat(result.groups()).isEmpty();
    }
}
