package org.dromara.ai.service.etl;

import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.bo.ChunkResult;

import java.util.List;

/**
 * ETL 处理器接口
 * 不同的数据集类型（通用文件、QA对、在线文档、网页链接）使用不同的处理器
 * 
 * 职责简化：仅负责文件解析和分块，不包含向量化逻辑
 * 向量化由 IKmEmbeddingService 统一处理
 *
 * @author Mahone
 * @date 2026-02-01
 */
public interface EtlHandler {

    /**
     * 获取处理器支持的处理类型
     * 
     * @return 处理类型标识 (GENERIC_FILE, QA_PAIR, ONLINE_DOC, WEB_LINK)
     */
    String getProcessType();

    /**
     * 处理文档，返回分块列表
     * 
     * 注意：此方法仅负责解析和分块，不进行向量化
     *
     * @param document 待处理的文档
     * @param dataset  所属数据集
     * @return 分块结果列表
     */
    List<ChunkResult> process(KmDocument document, KmDataset dataset);

    /**
     * 判断是否支持该处理类型
     * 
     * @param processType 处理类型
     * @return 是否支持
     */
    default boolean supports(String processType) {
        return getProcessType().equals(processType);
    }
}
