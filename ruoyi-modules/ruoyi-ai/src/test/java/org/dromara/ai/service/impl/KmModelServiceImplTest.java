package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.domain.KmModel;
import org.dromara.ai.domain.bo.KmModelBo;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.mapper.KmModelMapper;
import org.dromara.ai.mapper.KmModelProviderMapper;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI模型配置Service单元测试
 */
public class KmModelServiceImplTest extends BaseUnitTest {

    @Mock
    private KmModelMapper baseMapper;
    @Mock
    private KmModelProviderMapper providerMapper;
    @Mock
    private KmEmbeddingMapper embeddingMapper;
    @Mock
    private ModelBuilder modelBuilder;

    private KmModelServiceImpl modelService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        modelService = new KmModelServiceImpl(baseMapper, providerMapper, embeddingMapper, modelBuilder);
    }

    @Test
    void insertByBo_EmbeddingDefaultExists_ThrowsException() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            mockedMessageUtils.when(() -> MessageUtils.message(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
            mockedMessageUtils.when(() -> MessageUtils.message(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            KmModelBo bo = new KmModelBo();
            bo.setModelType("2");
            bo.setIsDefault(1);

            // 模拟向量表有数据
            when(embeddingMapper.selectCount(isNull())).thenReturn(1L);
            // 模拟数据库中已存在一个默认向量模型
            when(baseMapper.selectCount(any())).thenReturn(1L);

            ServiceException exception = assertThrows(ServiceException.class, () -> {
                modelService.insertByBo(bo);
            });

            assertEquals("ai.msg.embedding.once_only", exception.getMessage());
        }
    }

    @Test
    void insertByBo_EmbeddingTableEmpty_AllowsNewDefault() {
        KmModelBo bo = new KmModelBo();
        bo.setModelType("2");
        bo.setIsDefault(1);

        // 模拟向量表无数据
        when(embeddingMapper.selectCount(isNull())).thenReturn(0L);
        when(baseMapper.insert(any(KmModel.class))).thenReturn(1);

        assertTrue(modelService.insertByBo(bo));
    }

    @Test
    void updateByBo_EmbeddingDefaultExists_ThrowsException() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            mockedMessageUtils.when(() -> MessageUtils.message(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
            mockedMessageUtils.when(() -> MessageUtils.message(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            Long modelId = 2L;
            KmModelBo bo = new KmModelBo();
            bo.setModelId(modelId);
            bo.setModelType("2");
            bo.setIsDefault(1);

            // 模拟当前模型不是默认，但数据库已有另一个默认
            KmModel existing = new KmModel();
            existing.setModelId(modelId);
            existing.setModelType("2");
            existing.setIsDefault(0);

            when(baseMapper.selectById(modelId)).thenReturn(existing);
            // 模拟向量表有数据
            when(embeddingMapper.selectCount(isNull())).thenReturn(1L);
            // 模拟 selectCount 发现数据库里有另一个默认（排除自己后仍有1个）
            when(baseMapper.selectCount(any())).thenReturn(1L);

            ServiceException exception = assertThrows(ServiceException.class, () -> {
                modelService.updateByBo(bo);
            });

            assertEquals("ai.msg.embedding.once_only", exception.getMessage());
        }
    }

    @Test
    void setDefaultModel_EmbeddingDefaultExists_ThrowsException() {
        try (MockedStatic<MessageUtils> mockedMessageUtils = mockStatic(MessageUtils.class)) {
            mockedMessageUtils.when(() -> MessageUtils.message(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
            mockedMessageUtils.when(() -> MessageUtils.message(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            Long modelId = 3L;
            KmModel model = new KmModel();
            model.setModelId(modelId);
            model.setModelType("2");

            when(baseMapper.selectById(modelId)).thenReturn(model);
            // 模拟向量表有数据
            when(embeddingMapper.selectCount(isNull())).thenReturn(1L);
            // 模拟 selectCount 发现数据库里有另一个默认
            when(baseMapper.selectCount(any())).thenReturn(1L);

            ServiceException exception = assertThrows(ServiceException.class, () -> {
                modelService.setDefaultModel(modelId);
            });

            assertEquals("ai.msg.embedding.once_only", exception.getMessage());
        }
    }
}
