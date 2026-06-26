package club.muimi.backend.service.auth;

import club.muimi.backend.common.enums.EmailCodeScene;
import club.muimi.backend.common.enums.UserStatus;
import club.muimi.backend.config.AuthProperties;
import club.muimi.backend.dto.auth.ChangePasswordRequest;
import club.muimi.backend.dto.auth.ForgotPasswordRequest;
import club.muimi.backend.dto.auth.LoginRequest;
import club.muimi.backend.dto.auth.RegisterRequest;
import club.muimi.backend.dto.auth.ResetPasswordRequest;
import club.muimi.backend.dto.auth.SendEmailCodeRequest;
import club.muimi.backend.entity.GroupMember;
import club.muimi.backend.entity.RecruitmentGroup;
import club.muimi.backend.entity.User;
import club.muimi.backend.exception.ConflictException;
import club.muimi.backend.exception.ForbiddenException;
import club.muimi.backend.exception.NotFoundException;
import club.muimi.backend.exception.TooManyRequestsException;
import club.muimi.backend.exception.UnauthorizedException;
import club.muimi.backend.exception.ValidationException;
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
import club.muimi.backend.vo.auth.CurrentUserVo;
import club.muimi.backend.vo.auth.GroupSimpleVo;
import club.muimi.backend.vo.auth.LoginResultVo;
import club.muimi.backend.vo.auth.RegisterResultVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RecruitmentGroupRepository recruitmentGroupRepository;
    private final PeriodService periodService;
    private final AuthCacheService authCacheService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthCookieService authCookieService;
    private final AuthProperties authProperties;
    private final CurrentUserService currentUserService;

    public AuthService(
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository,
            RecruitmentGroupRepository recruitmentGroupRepository,
            PeriodService periodService,
            AuthCacheService authCacheService,
            MailService mailService,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthCookieService authCookieService,
            AuthProperties authProperties,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.recruitmentGroupRepository = recruitmentGroupRepository;
        this.periodService = periodService;
        this.authCacheService = authCacheService;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authCookieService = authCookieService;
        this.authProperties = authProperties;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void sendEmailCode(SendEmailCodeRequest request) {
        boolean shouldSendMail = true;
        if (request.scene() == EmailCodeScene.REGISTER) {
            periodService.ensureRegistrationOpen();
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("该邮箱已被注册");
            }
        } else {
            shouldSendMail = userRepository.existsByEmail(request.email());
        }

        if (authCacheService.hasEmailCooldown(request.scene(), request.email())) {
            throw new TooManyRequestsException("验证码发送过于频繁，请稍后再试");
        }

        Duration cooldownTtl = Duration.ofSeconds(authProperties.getEmailCode().getSendCooldownSeconds());
        if (!shouldSendMail) {
            // 找回密码场景不应暴露邮箱是否注册，未命中时直接返回统一成功结果。
            authCacheService.markEmailCooldown(request.scene(), request.email(), cooldownTtl);
            return;
        }

        String code = generateVerificationCode();
        Duration codeTtl = Duration.ofSeconds(authProperties.getEmailCode().getTtlSeconds());
        authCacheService.saveEmailCode(request.scene(), request.email(), code, codeTtl);
        authCacheService.markEmailCooldown(request.scene(), request.email(), cooldownTtl);
        mailService.sendVerificationCode(request.email(), code, request.scene());
    }

    @Transactional
    public RegisterResultVo register(RegisterRequest request) {
        periodService.ensureRegistrationOpen();
        validatePasswordPair(request.password(), request.confirmPassword());
        validatePasswordStrength(request.password());
        ensureUsernameNotExists(request.username());
        ensureEmailNotExists(request.email());
        validateEmailCode(EmailCodeScene.REGISTER, request.email(), request.code());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(true)
                .tokenVersion(0L)
                .lastLoginAt(null)
                .build();
        userRepository.save(user);
        authCacheService.deleteEmailCode(EmailCodeScene.REGISTER, request.email());
        return new RegisterResultVo(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional
    public LoginResultVo login(LoginRequest request, HttpServletResponse response) {
        if (authCacheService.isLoginLocked(request.email())) {
            throw new TooManyRequestsException("登录失败次数过多，请稍后再试");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    recordLoginFailure(request.email());
                    return new UnauthorizedException("邮箱或密码错误");
                });

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLoginFailure(request.email());
            throw new UnauthorizedException("邮箱或密码错误");
        }

        authCacheService.clearLoginFailCount(request.email());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenService.generateToken(user, request.rememberMeOrDefault());
        authCookieService.writeLoginCookie(response, token, request.rememberMeOrDefault());
        authCookieService.writeCsrfCookie(response, token);
        return new LoginResultVo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified())
        );
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieService.resolveToken(request)
                .map(jwtTokenService::parse)
                .ifPresent(this::blacklistTokenIfNecessary);
        authCookieService.clearLoginCookie(response);
    }

    @Transactional(readOnly = true)
    public CurrentUserVo getCurrentUser() {
        LoginUser loginUser = currentUserService.requireCurrentUser();
        User user = userRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("当前登录状态已失效"));
        List<GroupMember> groupMembers = groupMemberRepository.findAllByUserId(loginUser.getUserId());
        List<Long> groupIds = groupMembers.stream().map(GroupMember::getGroupId).distinct().toList();
        List<GroupSimpleVo> groups = recruitmentGroupRepository.findAllByIdIn(groupIds)
                .stream()
                .map(group -> new GroupSimpleVo(group.getId(), group.getName()))
                .toList();
        Long leaderGroupId = recruitmentGroupRepository.findByLeaderUserId(loginUser.getUserId())
                .map(RecruitmentGroup::getId)
                .orElse(null);
        return new CurrentUserVo(
                loginUser.getUserId(),
                loginUser.getDisplayUsername(),
                loginUser.getEmail(),
                loginUser.getRole(),
                loginUser.getStatus(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                leaderGroupId,
                groups
        );
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        sendEmailCode(new SendEmailCodeRequest(request.email(), EmailCodeScene.RESET_PASSWORD));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordPair(request.newPassword(), request.confirmPassword());
        validatePasswordStrength(request.newPassword());
        validateEmailCode(EmailCodeScene.RESET_PASSWORD, request.email(), request.code());

        User user = ensureUserExists(request.email());
        // 递增 tokenVersion，强制全部旧登录态失效。
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        authCacheService.deleteEmailCode(EmailCodeScene.RESET_PASSWORD, request.email());
    }

    @Transactional
    public void changePassword(
            ChangePasswordRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        LoginUser loginUser = currentUserService.requireCurrentUser();
        User user = userRepository.findById(loginUser.getUserId())
                .orElseThrow(() -> new UnauthorizedException("当前登录状态已失效"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new ValidationException("旧密码不正确");
        }

        validatePasswordPair(request.newPassword(), request.confirmPassword());
        validatePasswordStrength(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        authCookieService.resolveToken(httpServletRequest)
                .map(jwtTokenService::parse)
                .ifPresent(this::blacklistTokenIfNecessary);
        authCookieService.clearLoginCookie(httpServletResponse);
    }

    private void validateEmailCode(EmailCodeScene scene, String email, String code) {
        String cachedCode = authCacheService.getEmailCode(scene, email)
                .orElseThrow(() -> new ValidationException("验证码不存在或已过期"));
        if (!cachedCode.equals(code)) {
            throw new ValidationException("验证码错误");
        }
    }

    private User ensureUserExists(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private void ensureUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("用户名已存在");
        }
    }

    private void ensureEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("邮箱已被注册");
        }
    }

    private void validatePasswordPair(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("两次输入的密码不一致");
        }
    }

    private void validatePasswordStrength(String password) {
        boolean valid = password != null
                && password.length() >= 8
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw new ValidationException("密码至少 8 位，且必须同时包含字母和数字");
        }
    }

    private void recordLoginFailure(String email) {
        Duration ttl = Duration.ofSeconds(authProperties.getLogin().getFailLockSeconds());
        long failCount = authCacheService.incrementLoginFailCount(email, ttl);
        if (failCount >= authProperties.getLogin().getMaxFailCount()) {
            authCacheService.lockLogin(email, ttl);
        }
    }

    private void blacklistTokenIfNecessary(JwtClaims claims) {
        Duration remaining = jwtTokenService.remainingValidity(claims);
        if (!remaining.isNegative() && !remaining.isZero()) {
            authCacheService.blacklistToken(claims.jti(), remaining);
        }
    }

    private String generateVerificationCode() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(value);
    }
}
