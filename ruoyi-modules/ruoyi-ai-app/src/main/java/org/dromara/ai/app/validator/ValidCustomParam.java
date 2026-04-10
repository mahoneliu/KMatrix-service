package org.dromara.ai.app.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义参数验证注解
 * <p>
 * 用于验证自定义参数的合法性，包括：
 * <ul>
 *   <li>参数名长度限制（2-50 字符）</li>
 *   <li>参数值大小限制（默认≤10KB）</li>
 *   <li>嵌套对象深度限制（默认≤3 层）</li>
 *   <li>敏感参数名检测（password, token, secret, key 等）</li>
 *   <li>白名单验证（从配置或数据库加载）</li>
 * </ul>
 *
 * @author KMatrix Team
 * @date 2026-04-09
 */
@Constraint(validatedBy = CustomParamValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidCustomParam {

    /**
     * 默认校验失败提示信息
     */
    String message() default "自定义参数验证失败：{message}";

    /**
     * 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 是否启用白名单验证
     * 默认从配置类读取，true 时只允许白名单中的参数
     */
    boolean whitelistEnabled() default true;

    /**
     * 是否启用敏感词检测
     * 默认从配置类读取
     */
    boolean sensitiveCheckEnabled() default true;

    /**
     * 是否启用嵌套深度检测
     * 默认从配置类读取
     */
    boolean depthCheckEnabled() default true;

    /**
     * 是否启用参数值大小检测
     * 默认从配置类读取
     */
    boolean sizeCheckEnabled() default true;
}
