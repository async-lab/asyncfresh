package club.muimi.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
        private long ttlSeconds;
        private long sendCooldownSeconds;

    }

    @Setter
    @Getter
    public static class Login {
        private int maxFailCount;
        private long failLockSeconds;

    }

    @PostConstruct
    public void validate() {
        if (cacheType == null) {
            throw new IllegalStateException("必须通过 app.auth.cache-type 或 AUTH_CACHE_TYPE 配置认证缓存实现类型");
        }
        if (cacheType != CacheType.REDIS) {
            throw new IllegalStateException("当前版本仅支持 Redis 作为认证缓存，请将 app.auth.cache-type 配置为 redis");
        }
    }

    public enum CacheType {
        REDIS
    }
}
