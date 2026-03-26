package org.dromara.ai.model.service;

/**
 * 向量知识库数据检查接口（SPI，解耦 model 模块对 knowledge 模块的依赖）
 * 由 ruoyi-ai-knowledge 模块提供具体实现
 *
 * @author Mahone
 * @date 2026-03-27
 */
public interface IEmbeddingDataChecker {
    
    /**
     * 检查向量表中是否已有数据
     *
     * @return boolean true表示已有数据，false表示空
     */
    boolean hasData();
}
