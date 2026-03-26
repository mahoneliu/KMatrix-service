package org.dromara.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.dromara.ai", "org.dromara.common"})
@MapperScan("org.dromara.ai.mapper")
public class TestApplication {
}
