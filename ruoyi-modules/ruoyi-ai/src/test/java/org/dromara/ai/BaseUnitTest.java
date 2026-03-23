package org.dromara.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 业务层单元测试基类（使用 Mockito，不需要启动完整上下文）
 * 适用于 Service / Component 纯业务逻辑的轻量测试。
 *
 * @author Mahone
 */
@Tag("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseUnitTest {
    // 子类可以使用 @MockitoBean / @Mock / @InjectMocks 注入 Mockito Mock
}
