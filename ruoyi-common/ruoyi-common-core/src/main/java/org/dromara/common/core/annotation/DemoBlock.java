package org.dromara.common.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 演示环境拦截注解
 *
 * <p>
 * 在演示环境下，带有此注解的接口将被拦截，防止敏感数据被修改或删除。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DemoBlock {
}
