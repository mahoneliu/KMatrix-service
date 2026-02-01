package org.dromara.ai.service.etl;

import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;

/**
 * ETL 处理器接口
 * 不同的数据集类型（通用文件、QA对、在线文档、网页链接）使用不同的处理器
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
     * 处理文档
     *
     * @param document 待处理的文档
     * @param dataset  所属数据集
     * @param kbId     知识库ID
     */
    void process(KmDocument document, KmDataset dataset, Long kbId);

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
