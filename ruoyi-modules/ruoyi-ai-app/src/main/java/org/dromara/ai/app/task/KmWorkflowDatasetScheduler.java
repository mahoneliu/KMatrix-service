package org.dromara.ai.app.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.service.etl.DatasetProcessType;
import org.dromara.ai.knowledge.util.StatusMetaUtils;
import org.dromara.ai.workflow.domain.bo.WorkflowExecutionReq;
import org.dromara.ai.workflow.workflow.WorkflowExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 工作流数据集调度器
 * 定时轮询 WORKFLOW_FILE 类型的数据集，按并发上限触发工作流异步处理文档
 *
 * <p>状态流转：
 * <pre>
 *   0（待处理）→ 调度器选中 → 1（处理中）→ 工作流成功 → 2（已完成）
 *                                          → 工作流失败 → 3（失败）
 * </pre>
 *
 * @author Mahone
 * @date 2026-04-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmWorkflowDatasetScheduler {

    private final KmDatasetMapper datasetMapper;
    private final KmDocumentMapper documentMapper;
    private final KmAppMapper appMapper;
    private final WorkflowExecutor workflowExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 每 5 秒执行一次轮询
     */
    @Scheduled(fixedDelay = 5000)
    public void schedule() {
        try {
            processWorkflowDatasets();
        } catch (Exception e) {
            log.error("Workflow dataset scheduler execution failed", e);
        }
    }

    private void processWorkflowDatasets() {
        // 1. 查询所有 WORKFLOW_FILE 类型的数据集
        List<KmDataset> datasets = datasetMapper.selectList(
                new LambdaQueryWrapper<KmDataset>()
                        .eq(KmDataset::getProcessType, DatasetProcessType.WORKFLOW_FILE));

        if (datasets == null || datasets.isEmpty()) {
            return;
        }

        for (KmDataset dataset : datasets) {
            try {
                processDataset(dataset);
            } catch (Exception e) {
                log.error("Failed to process dataset {}: {}", dataset.getId(), e.getMessage(), e);
            }
        }
    }

    private void processDataset(KmDataset dataset) {
        // 解析数据集的工作流配置
        Map<String, Object> config = dataset.getConfig();
        if (config == null) {
            log.warn("Dataset {} missing config, skipping", dataset.getId());
            return;
        }

        Object appIdObj = config.get("appId");
        if (appIdObj == null) {
            log.warn("Dataset {} config missing appId, skipping", dataset.getId());
            return;
        }
        Long appId = ((Number) appIdObj).longValue();

        int maxConcurrency = config.containsKey("maxConcurrency")
                ? ((Number) config.get("maxConcurrency")).intValue()
                : 1;
        boolean enableExecutionDetail = config.containsKey("enableExecutionDetail")
                && Boolean.TRUE.equals(config.get("enableExecutionDetail"));

        // 2. 统计当前处于「处理中（1）」的文档数量
        long processingCount = documentMapper.selectCount(
                new LambdaQueryWrapper<KmDocument>()
                        .eq(KmDocument::getDatasetId, dataset.getId())
                        .eq(KmDocument::getEmbeddingStatus, 1));

        int freeSlots = maxConcurrency - (int) processingCount;
        if (freeSlots <= 0) {
            log.debug("Dataset {} concurrency limit reached (maxConcurrency={}, processing={})", dataset.getId(), maxConcurrency, processingCount);
            return;
        }

        // 3. 捞取待处理（0）的文档
        List<KmDocument> pendingDocs = documentMapper.selectList(
                new LambdaQueryWrapper<KmDocument>()
                        .eq(KmDocument::getDatasetId, dataset.getId())
                        .eq(KmDocument::getEmbeddingStatus, 0)
                        .last("LIMIT " + freeSlots));

        if (pendingDocs == null || pendingDocs.isEmpty()) {
            return;
        }

        // 4. 加载关联的 App
        KmApp app = appMapper.selectById(appId);
        if (app == null) {
            log.error("Dataset {} bound appId={} not found", dataset.getId(), appId);
            return;
        }
        if (app.getDslData() == null) {
            log.error("Dataset {} bound App {} has no DSL configured", dataset.getId(), appId);
            return;
        }

        for (KmDocument doc : pendingDocs) {
            // 5. CAS 更新：将 embeddingStatus 从 0 → 1，防止重复消费
            // 注意：LambdaUpdateWrapper.set() 不会应用 @TableField(typeHandler)，
            // statusMeta 需要序列化为 JSON 字符串后用 setSql 写入，避免 PostgreSQL hstore 报错
            Map<String, Object> newMeta = StatusMetaUtils.updateStateTime(
                    doc.getStatusMeta(), StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_STARTED);
            String metaJson;
            try {
                metaJson = objectMapper.writeValueAsString(newMeta);
            } catch (Exception e) {
                log.error("Failed to serialize statusMeta, skipping document {}", doc.getId(), e);
                continue;
            }

            int updated = documentMapper.update(null,
                    new LambdaUpdateWrapper<KmDocument>()
                            .set(KmDocument::getEmbeddingStatus, 1)
                            .set(KmDocument::getErrorMsg, null)
                            .setSql("status_meta = '" + metaJson.replace("'", "''") + "'::jsonb")
                            .eq(KmDocument::getId, doc.getId())
                            .eq(KmDocument::getEmbeddingStatus, 0));

            if (updated == 0) {
                log.debug("Document {} already being processed by another thread, skipping", doc.getId());
                continue;
            }

            // 6. 构建工作流执行请求，注入 documentId 作为全局变量
            final KmDocument targetDoc = doc;
            final KmApp targetApp = app;
            WorkflowExecutionReq req = WorkflowExecutionReq.builder()
                    .appId(appId)
                    .dslData(targetApp.getDslData())
                    .sessionId(-1L)
                    .userId(-1L)
                    .message(String.valueOf(targetDoc.getId()))
                    .documentId(targetDoc.getId())
                    .enableExecutionDetail(enableExecutionDetail ? "1" : "0")
                    .showExecutionInfo(false)
                    .build();

            // 7. 异步触发工作流（emitter 传 null，引擎已有防护）
            triggerWorkflowAsync(req, targetDoc, dataset.getId());
        }
    }

    /**
     * 异步触发工作流并处理结果/失败
     */
    private void triggerWorkflowAsync(WorkflowExecutionReq req, KmDocument doc, Long datasetId) {
        Thread thread = new Thread(() -> {
            try {
                log.info("Triggering workflow for document: docId={}, datasetId={}", doc.getId(), datasetId);
                workflowExecutor.executeWorkflow(req, null);
                // 注意：文档最终状态由 DatasetStorageNode 内部更新为 2（已完成）
                log.info("Workflow completed for document: docId={}", doc.getId());
            } catch (Exception e) {
                log.error("Failed to process document via workflow: docId={}, error={}", doc.getId(), e.getMessage(), e);
                // 更新文档状态为失败（3），statusMeta 序列化为 JSON 避免 hstore 报错
                Map<String, Object> failMeta = StatusMetaUtils.updateStateTime(
                        doc.getStatusMeta(), StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_FAILED);
                String failMetaJson;
                try {
                    failMetaJson = objectMapper.writeValueAsString(failMeta);
                } catch (Exception je) {
                    failMetaJson = "{}";
                }
                documentMapper.update(null,
                        new LambdaUpdateWrapper<KmDocument>()
                                .set(KmDocument::getEmbeddingStatus, 3)
                                .set(KmDocument::getErrorMsg, e.getMessage())
                                .setSql("status_meta = '" + failMetaJson.replace("'", "''") + "'::jsonb")
                                .eq(KmDocument::getId, doc.getId()));
            }
        });
        thread.setName("wf-dataset-" + doc.getId());
        thread.start();
    }
}
