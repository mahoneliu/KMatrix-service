package org.dromara.ai;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * 业务层单元测试基类（使用 Mockito，不需要启动完整上下文）
 * 适用于 Service / Component 纯业务逻辑的轻量测试。
 *
 * @author Mahone
 */
@Tag("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseUnitTest {

    @BeforeAll
    public void setupSpringContext() throws Exception {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        MessageSource mockMessageSource = Mockito.mock(MessageSource.class);
        
        // 默认返回 code 本身
        Mockito.when(mockMessageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        Mockito.when(mockContext.getBean(MessageSource.class)).thenReturn(mockMessageSource);
        
        // Mock Converter for MapstructUtils/BeanUtil
        Converter mockConverter = new Converter() {
            @Override
            public <T, V> V convert(T source, Class<V> desc) {
                try {
                    V target = desc.getDeclaredConstructor().newInstance();
                    cn.hutool.core.bean.BeanUtil.copyProperties(source, target);
                    return target;
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public <T, V> V convert(T source, V desc) {
                cn.hutool.core.bean.BeanUtil.copyProperties(source, desc);
                return desc;
            }
        };
        Mockito.when(mockContext.getBean(Converter.class)).thenReturn(mockConverter);

        // 通过反射初始化 Hutool SpringUtil 的静态 context
        try {
            Field field = SpringUtil.class.getDeclaredField("applicationContext");
            field.setAccessible(true);
            field.set(null, mockContext);
        } catch (Exception e) {
            // ignore
        }

        // 强行注入 MapstructUtils 的 CONVERTER (规避静态加载顺序问题)
        try {
            Class<?> mapstructUtilsClass = Class.forName("org.dromara.common.core.utils.MapstructUtils");
            Field converterField = mapstructUtilsClass.getDeclaredField("CONVERTER");
            converterField.setAccessible(true);
            // 静态字段需要设置 final 修饰符
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(converterField, converterField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            converterField.set(null, mockConverter);
        } catch (Exception e) {
            // ignore
        }

        // 强行注入 MessageUtils 的 MESSAGE_SOURCE
        try {
            Class<?> messageUtilsClass = Class.forName("org.dromara.common.core.utils.MessageUtils");
            Field sourceField = messageUtilsClass.getDeclaredField("MESSAGE_SOURCE");
            sourceField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(sourceField, sourceField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            sourceField.set(null, mockMessageSource);
        } catch (Exception e) {
            // ignore
        }
    }
}
