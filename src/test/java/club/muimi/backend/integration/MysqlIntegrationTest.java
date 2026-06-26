package club.muimi.backend.integration;

import club.muimi.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/fresh?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&sessionVariables=default_storage_engine=InnoDB",
        "spring.datasource.username=epoch",
        "spring.datasource.password=123456",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.auth.cache-type=memory"
})
class MysqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldLoadContextAndAccessMysql() {
        assertThat(userRepository).isNotNull();
        assertThatCode(userRepository::count).doesNotThrowAnyException();
    }
}
