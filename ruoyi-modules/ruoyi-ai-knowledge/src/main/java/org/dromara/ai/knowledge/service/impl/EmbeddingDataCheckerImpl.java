package org.dromara.ai.knowledge.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.knowledge.mapper.KmEmbeddingMapper;
import org.dromara.ai.model.service.IEmbeddingDataChecker;
import org.springframework.stereotype.Service;

/**
 * 向量知识库数据检查接口的具体实现
 *
 * @author Mahone
 * @date 2026-03-27
 */
@RequiredArgsConstructor
@Service
public class EmbeddingDataCheckerImpl implements IEmbeddingDataChecker {
    
    private final KmEmbeddingMapper embeddingMapper;

    @Override
    public boolean hasData() {
        return embeddingMapper.selectCount(null) > 0;
    }
}
