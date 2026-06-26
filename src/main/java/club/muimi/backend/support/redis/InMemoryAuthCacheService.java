package club.muimi.backend.support.redis;

import club.muimi.backend.common.enums.EmailCodeScene;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(prefix = "app.auth", name = "cache-type", havingValue = "memory")
public class InMemoryAuthCacheService implements AuthCacheService {

    private final Map<String, ExpiringValue<String>> stringValues = new ConcurrentHashMap<>();
    private final Map<String, ExpiringCounter> counters = new ConcurrentHashMap<>();

    @Override
    public void saveEmailCode(EmailCodeScene scene, String email, String code, Duration ttl) {
        stringValues.put(emailCodeKey(scene, email), ExpiringValue.of(code, ttl));
    }

    @Override
    public Optional<String> getEmailCode(EmailCodeScene scene, String email) {
        return getStringValue(emailCodeKey(scene, email));
    }

    @Override
    public void deleteEmailCode(EmailCodeScene scene, String email) {
        stringValues.remove(emailCodeKey(scene, email));
    }

    @Override
    public void markEmailCooldown(EmailCodeScene scene, String email, Duration ttl) {
        stringValues.put(emailCooldownKey(scene, email), ExpiringValue.of("1", ttl));
    }

    @Override
    public boolean hasEmailCooldown(EmailCodeScene scene, String email) {
        return getStringValue(emailCooldownKey(scene, email)).isPresent();
    }

    @Override
    public long incrementLoginFailCount(String email, Duration ttl) {
        ExpiringCounter counter = counters.compute(loginFailKey(email), (key, oldValue) -> {
            if (oldValue == null || oldValue.expired()) {
                return new ExpiringCounter(ttl);
            }
            oldValue.extend(ttl);
            return oldValue;
        });
        return counter.incrementAndGet();
    }

    @Override
    public void clearLoginFailCount(String email) {
        counters.remove(loginFailKey(email));
        stringValues.remove(loginLockKey(email));
    }

    @Override
    public void lockLogin(String email, Duration ttl) {
        stringValues.put(loginLockKey(email), ExpiringValue.of("1", ttl));
    }

    @Override
    public boolean isLoginLocked(String email) {
        return getStringValue(loginLockKey(email)).isPresent();
    }

    @Override
    public void blacklistToken(String jti, Duration ttl) {
        stringValues.put(jwtBlacklistKey(jti), ExpiringValue.of("1", ttl));
    }

    @Override
    public boolean isTokenBlacklisted(String jti) {
        return getStringValue(jwtBlacklistKey(jti)).isPresent();
    }

    private Optional<String> getStringValue(String key) {
        ExpiringValue<String> value = stringValues.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value.expired()) {
            stringValues.remove(key);
            return Optional.empty();
        }
        return Optional.of(value.value());
    }

    private String emailCodeKey(EmailCodeScene scene, String email) {
        return "auth:email-code:" + scene.name() + ":" + email;
    }

    private String emailCooldownKey(EmailCodeScene scene, String email) {
        return "auth:email-send-cooldown:" + scene.name() + ":" + email;
    }

    private String loginFailKey(String email) {
        return "auth:login:fail:" + email;
    }

    private String loginLockKey(String email) {
        return "auth:login:lock:" + email;
    }

    private String jwtBlacklistKey(String jti) {
        return "auth:jwt:blacklist:" + jti;
    }

    private record ExpiringValue<T>(T value, Instant expireAt) {
        private static <T> ExpiringValue<T> of(T value, Duration ttl) {
            return new ExpiringValue<>(value, Instant.now().plus(ttl));
        }

        private boolean expired() {
            return Instant.now().isAfter(expireAt);
        }
    }

    private static final class ExpiringCounter {
        private final AtomicLong value = new AtomicLong();
        private Instant expireAt;

        private ExpiringCounter(Duration ttl) {
            this.expireAt = Instant.now().plus(ttl);
        }

        private long incrementAndGet() {
            return value.incrementAndGet();
        }

        private boolean expired() {
            return Instant.now().isAfter(expireAt);
        }

        private void extend(Duration ttl) {
            this.expireAt = Instant.now().plus(ttl);
        }
    }
}
