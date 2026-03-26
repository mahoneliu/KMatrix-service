package org.dromara.ai.knowledge;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class BaseContainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("ankane/pgvector:v0.5.1").asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.dynamic.datasource.master.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.master.username", postgres::getUsername);
        registry.add("spring.datasource.dynamic.datasource.master.password", postgres::getPassword);
        registry.add("spring.datasource.dynamic.datasource.master.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.dynamic.primary", () -> "master");
    }
}
