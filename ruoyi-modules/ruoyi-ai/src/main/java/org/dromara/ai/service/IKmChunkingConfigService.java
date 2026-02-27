package org.dromara.ai.service;

import org.dromara.ai.domain.KmDataset;

/**
 * 知识库分块配置服务接口
 * 聚合系统级和数据集级子块配置，统一提供有效子块参数
 *
 * @author Mahone
 * @date 2026-02-27
 */
public interface IKmChunkingConfigService {

    /**
     * 获取有效子块大小（字符数）
     * 优先使用数据集级配置，数据集字段为 NULL 时使用系统默认值
     *
     * @param dataset 数据集实体
     * @return 子块大小（字符数）
     */
    int getChildChunkSize(KmDataset dataset);

    /**
     * 获取有效子块重叠大小（字符数）
     * 优先使用数据集级配置，数据集字段为 NULL 时使用系统默认值
     *
     * @param dataset 数据集实体
     * @return 子块重叠大小（字符数）
     */
    int getChildChunkOverlap(KmDataset dataset);
}
