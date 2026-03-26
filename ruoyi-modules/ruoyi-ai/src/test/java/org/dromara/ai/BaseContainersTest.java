package org.dromara.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Paths;

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
         * 共享的 PostgreSQL 容器（带 pgvector 和 pgroonga）
         * 使用自定义 Dockerfile 构建，内置了两个必要的扩展
         */
        /**
         * 共享的 PostgreSQL 容器（带 pgvector 和 pgroonga）
         * 使用自定义 Dockerfile 构建，内置了两个必要的扩展
         */
        static final GenericContainer<?> POSTGRES = new GenericContainer<>(
                        new ImageFromDockerfile("kmatrix-test-db", false)
                                        .withDockerfile(Paths.get("src/test/resources/Dockerfile-test-db")))
                        .withExposedPorts(5432)
                        .withEnv("POSTGRES_DB", "kmatrix_test")
                        .withEnv("POSTGRES_USER", "test")
                        .withEnv("POSTGRES_PASSWORD", "test")
                        .waitingFor(Wait.forListeningPort())
                        .withReuse(true);

        static {
                POSTGRES.start();
        }

        /**
         * 动态注入数据库连接信息，覆盖 application.yml 中的配置
         */
        @DynamicPropertySource
        static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url",
                                () -> String.format("jdbc:postgresql://%s:%d/kmatrix_test?currentSchema=public",
                                                POSTGRES.getHost(), POSTGRES.getMappedPort(5432)));
                registry.add("spring.datasource.username", () -> "test");
                registry.add("spring.datasource.password", () -> "test");
                registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
                // 关闭 Redis/Redisson 等在测试环境中不必要的组件
                registry.add("spring.data.redis.host", () -> "localhost");
                registry.add("spring.data.redis.port", () -> "6379");
        }
}
