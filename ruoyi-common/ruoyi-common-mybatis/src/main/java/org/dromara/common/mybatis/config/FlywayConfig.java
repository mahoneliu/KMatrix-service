package org.dromara.common.mybatis.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * Flyway 配置类，支持启动时自动 Repair
 *
 * @author Mahone
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.repair-on-migrate:false}")
    private boolean repairOnMigrate;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (repairOnMigrate) {
                log.info("Flyway repair-on-migrate is enabled. Performing repair...");
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
