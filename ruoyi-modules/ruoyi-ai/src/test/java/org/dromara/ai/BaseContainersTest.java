package org.dromara.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 持久层集成测试基类（Testcontainers）
 *
 * 使用真实的 PostgreSQL 容器（带有 pgvector 扩展支持），
 * 通过 @DynamicPropertySource 动态注入数据库连接信息，
 * 确保 Flyway 完成数据库迁移后再运行测试。
 *
 * 注意：
 * 1. 本类需要本地安装并启动了 Docker 才能运行。
 * 2. 默认使用 ankane/pgvector 镜像，集成了 pgvector 扩展。
 * 3. 测试时共享同一个容器实例（@Testcontainers + static 字段），提高运行效率。
 *
 * @author Mahone
 */
@Tag("local")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseContainersTest {

    /**
     * 共享的 PostgreSQL 容器（带 pgvector）
     * 使用 ankane/pgvector 镜像，内置了 pgvector 扩展
     */
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("ankane/pgvector:v0.5.1")
            .withDatabaseName("kmatrix_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // 在同一 JVM 中复用容器，提高测试速度

    /**
     * 动态注入数据库连接信息，覆盖 application.yml 中的配置
     */
    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                POSTGRES.getJdbcUrl() + "&currentSchema=public");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // 关闭 Redis/Redisson 等在测试环境中不必要的组件
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }
}
