package club.muimi.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.default-admin")
public class DefaultAdminProperties {

    private boolean enabled = true;
    private String username;
    private String password;
    private String email;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isConfigured() {
        return hasText(username) && hasText(password) && hasText(email);
    }

    public void validateRequired() {
        requireConfigured(username, "DEFAULT_ADMIN_USERNAME");
        requireConfigured(password, "DEFAULT_ADMIN_PASSWORD");
        requireConfigured(email, "DEFAULT_ADMIN_EMAIL");
    }

    private void requireConfigured(String value, String envName) {
        if (!hasText(value)) {
            throw new IllegalStateException("默认管理员配置缺失，请设置环境变量 " + envName);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
