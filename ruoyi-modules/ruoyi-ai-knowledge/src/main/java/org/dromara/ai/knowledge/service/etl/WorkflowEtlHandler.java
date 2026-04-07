package org.dromara.ai.knowledge.service.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.domain.bo.ChunkResult;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.util.StatusMetaUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工作流文件 ETL 处理器
 * 处理 WORKFLOW_FILE 类型的数据集
 * <p>
 * 与普通 ETL 处理器不同，本处理器不执行文件解析和分块，
 * 仅将文档状态初始化为「待处理(0)」，
 * 真正的处理由 KmWorkflowDatasetScheduler 异步调度工作流完成。
 *
 * @author Mahone
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEtlHandler implements EtlHandler {

    private final KmDocumentMapper documentMapper;

    @Override
    public String getProcessType() {
        return DatasetProcessType.WORKFLOW_FILE;
    }

    /**
     * 工作流模式下不做同步处理：
     * 将文档状态重置为 0（待处理），让调度器异步触发工作流来处理。
     *
     * @return 空列表（告知 ETL 主流程跳过向量化）
     */
    @Override
    public List<ChunkResult> process(KmDocument document, KmDataset dataset) {
        log.info("WorkflowEtlHandler: Document {} entering workflow processing queue", document.getId());

        // 将 embeddingStatus 重置为 0（待处理），等待调度器异步触发工作流
        KmDocument update = new KmDocument();
        update.setId(document.getId());
        update.setEmbeddingStatus(0);
        update.setStatusMeta(StatusMetaUtils.updateStateTime(
                document.getStatusMeta(),
                StatusMetaUtils.TASK_EMBEDDING,
                StatusMetaUtils.STATUS_PENDING));
        documentMapper.updateById(update);

        // 返回 null，告知 ETL 主流程该文档已由工作流接管，跳过向量化
        return null;
    }
}
