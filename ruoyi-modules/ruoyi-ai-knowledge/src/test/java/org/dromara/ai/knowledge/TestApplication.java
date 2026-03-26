package org.dromara.ai.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.dromara.common.security.config.SecurityConfig;
import org.redisson.api.RedissonClient;
import org.mockito.Mockito;
import org.dromara.common.core.service.*;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    SecurityConfig.class,
    RedisAutoConfiguration.class,
    org.dromara.common.translation.config.TranslationConfig.class
})
@ComponentScan(basePackages = {
    "org.dromara.ai.knowledge.mapper",
    "org.dromara.ai.model.mapper",
    "org.dromara.common.mybatis"
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        org.dromara.ai.model.controller.KmModelController.class,
        org.dromara.ai.model.service.impl.KmModelServiceImpl.class
    })
})
@MapperScan({"org.dromara.ai.knowledge.mapper", "org.dromara.ai.model.mapper"})
public class TestApplication {

    @Bean public RedissonClient redissonClient() { return Mockito.mock(RedissonClient.class); }
    @Bean public DeptService deptService() { return Mockito.mock(DeptService.class); }
    @Bean public UserService userService() { return Mockito.mock(UserService.class); }
    @Bean public DictService dictService() { return Mockito.mock(DictService.class); }
    @Bean public OssService ossService() { return Mockito.mock(OssService.class); }
    @Bean public RoleService roleService() { return Mockito.mock(RoleService.class); }
    @Bean public ConfigService configService() { return Mockito.mock(ConfigService.class); }
}
