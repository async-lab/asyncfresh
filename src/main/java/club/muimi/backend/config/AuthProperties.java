package club.muimi.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private final EmailCode emailCode = new EmailCode();
    private final Login login = new Login();
    private CacheType cacheType = CacheType.REDIS;

    public EmailCode getEmailCode() {
        return emailCode;
    }

    public Login getLogin() {
        return login;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public void setCacheType(CacheType cacheType) {
        this.cacheType = cacheType;
    }

    public static class EmailCode {
        private long ttlSeconds;
        private long sendCooldownSeconds;

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        public long getSendCooldownSeconds() {
            return sendCooldownSeconds;
        }

        public void setSendCooldownSeconds(long sendCooldownSeconds) {
            this.sendCooldownSeconds = sendCooldownSeconds;
        }
    }

    public static class Login {
        private int maxFailCount;
        private long failLockSeconds;

        public int getMaxFailCount() {
            return maxFailCount;
        }

        public void setMaxFailCount(int maxFailCount) {
            this.maxFailCount = maxFailCount;
        }

        public long getFailLockSeconds() {
            return failLockSeconds;
        }

        public void setFailLockSeconds(long failLockSeconds) {
            this.failLockSeconds = failLockSeconds;
        }
    }

    public enum CacheType {
        REDIS,
        MEMORY
    }
}
