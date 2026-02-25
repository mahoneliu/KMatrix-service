package org.dromara.ai.auth;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.dromara.common.core.domain.R;
import org.dromara.system.service.ISysConfigService;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Demo环境关键操作拦截
 * 
 * @author Mahone
 */
@Aspect
@Component
@Slf4j
public class DemoFilterAspect {

        @Autowired
        private ISysConfigService sysConfigService;

        // 拦截带有 @DemoBlock 注解的方法
        @Pointcut("@annotation(org.dromara.common.core.annotation.DemoBlock)")
        public void demoBlock() {
        }

        @Around("demoBlock()")
        public Object authBefore(ProceedingJoinPoint pjp) throws Throwable {
                String isDemoEnabled = sysConfigService.selectConfigByKey("sys.demo.enabled");
                if ("true".equals(isDemoEnabled)) {
                        if (!LoginHelper.isSuperAdmin()) {
                                // 如果是演示环境且不是超级管理员，则拦截
                                return R.fail("演示环境，不允许操作敏感数据！");
                        }
                }
                return pjp.proceed();
        }
}
