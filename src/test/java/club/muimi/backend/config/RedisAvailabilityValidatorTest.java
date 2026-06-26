package club.muimi.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAvailabilityValidatorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private RedisConnectionFactory redisConnectionFactory;
    @Mock
    private RedisConnection redisConnection;

    @Test
    void shouldPassWhenRedisPingReturnsPong() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setCacheType(AuthProperties.CacheType.REDIS);
        RedisAvailabilityValidator validator = new RedisAvailabilityValidator(authProperties, stringRedisTemplate);
        when(stringRedisTemplate.getRequiredConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        assertThatCode(validator::validateRedisAvailability).doesNotThrowAnyException();
    }

    @Test
    void shouldFailFastWhenRedisUnavailable() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setCacheType(AuthProperties.CacheType.REDIS);
        RedisAvailabilityValidator validator = new RedisAvailabilityValidator(authProperties, stringRedisTemplate);
        when(stringRedisTemplate.getRequiredConnectionFactory()).thenReturn(redisConnectionFactory);
        when(redisConnectionFactory.getConnection()).thenThrow(new IllegalStateException("connect failed"));

        assertThatThrownBy(validator::validateRedisAvailability)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("认证缓存 Redis 不可用");
    }
}
