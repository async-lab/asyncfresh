package club.muimi.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private final EmailCode emailCode = new EmailCode();
    private final Login login = new Login();
    @Setter
    private CacheType cacheType = CacheType.REDIS;

    @Setter
    @Getter
    public static class EmailCode {
        private long ttlSeconds = 300;
        private long sendCooldownSeconds = 60;
        private int maxVerifyFailCount = 5;
        private long verifyLockSeconds = 300;

    }

    @Setter
    @Getter
    public static class Login {
        private int maxFailCount = 5;
        private long failLockSeconds = 900;
        private boolean trustForwardHeaders = false;
        private List<String> trustedProxies = new ArrayList<>();

    }

    @PostConstruct
    public void validate() {
        if (cacheType == null) {
            throw new IllegalStateException("必须通过 app.auth.cache-type 或 AUTH_CACHE_TYPE 配置认证缓存实现类型");
        }
        if (cacheType != CacheType.REDIS) {
            throw new IllegalStateException("当前版本仅支持 Redis 作为认证缓存，请将 app.auth.cache-type 配置为 redis");
        }
        if (emailCode.getTtlSeconds() <= 0 || emailCode.getSendCooldownSeconds() <= 0) {
            throw new IllegalStateException("邮箱验证码有效期和发送冷却时间必须为正数");
        }
        if (emailCode.getMaxVerifyFailCount() <= 0 || emailCode.getVerifyLockSeconds() <= 0) {
            throw new IllegalStateException("邮箱验证码错误次数上限和锁定时长必须为正数");
        }
        if (login.getMaxFailCount() <= 0 || login.getFailLockSeconds() <= 0) {
            throw new IllegalStateException("登录失败次数上限和锁定时长必须为正数");
        }
        for (String trustedProxy : login.getTrustedProxies()) {
            if (trustedProxy == null || trustedProxy.isBlank()) {
                continue;
            }
            try {
                new IpAddressMatcher(trustedProxy.trim());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("app.auth.login.trusted-proxies 中存在非法代理地址或网段: " + trustedProxy, exception);
            }
        }
    }

    public enum CacheType {
        REDIS
    }
}
