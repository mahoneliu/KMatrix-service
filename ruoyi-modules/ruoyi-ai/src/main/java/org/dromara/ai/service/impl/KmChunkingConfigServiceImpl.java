package org.dromara.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.ai.config.KmChunkingProperties;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.service.IKmChunkingConfigService;
import org.springframework.stereotype.Service;

/**
 * 知识库分块配置服务实现
 * 聚合系统级（KmChunkingProperties）和数据集级（KmDataset 字段）子块配置。
 * 数据集字段不为 NULL 时优先使用，否则 fallback 到系统默认值。
 *
 * @author Mahone
 * @date 2026-02-27
 */
@Service
@RequiredArgsConstructor
public class KmChunkingConfigServiceImpl implements IKmChunkingConfigService {

    private final KmChunkingProperties chunkingProperties;

    @Override
    public int getChildChunkSize(KmDataset dataset) {
        if (dataset != null && dataset.getChildChunkSize() != null) {
            return dataset.getChildChunkSize();
        }
        return chunkingProperties.getChildChunkSize();
    }

    @Override
    public int getChildChunkOverlap(KmDataset dataset) {
        if (dataset != null && dataset.getChildChunkOverlap() != null) {
            return dataset.getChildChunkOverlap();
        }
        return chunkingProperties.getChildChunkOverlap();
    }
}
