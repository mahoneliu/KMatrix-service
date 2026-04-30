package org.dromara.ai.workflow.workflow.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.domain.bo.ChunkResult;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.service.IKmEmbeddingService;
import org.dromara.ai.knowledge.service.IKmEtlService;
import org.dromara.ai.knowledge.util.StatusMetaUtils;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 数据集存储节点
 * <p>
 * 节点类型标识: DATASET_STORAGE
 * <p>
 * 输入:
 * - text (String): 待分块、向量化的纯文本
 * - documentId (Long): 关联的文档 ID
 * <p>
 * 可选配置 (来自节点 config):
 * - chunkSize (Integer): 分块大小，未配置时从数据集获取，最终降级到默认值 500
 * - overlap (Integer): 重叠大小，未配置时从数据集获取，最终降级到默认值 50
 * <p>
 * 内部逻辑:
 * 1. 读取 text 和 documentId，查询 km_document / km_dataset 获取知识库 ID 及分块参数。
 * 2. 调用 IKmEtlService.splitText 对文本分块。
 * 3. 调用 IKmEmbeddingService.embedAndStoreChunks 完成向量化并写入 pgvector。
 * 4. 将 km_document.embeddingStatus 更新为 2（已完成）。
 * <p>
 * 输出:
 * - chunkCount (Integer): 最终写入的切片数量
 *
 * @author Mahone
 * @date 2026-04-02
 */
@Slf4j
@Component(NodeTypeConstants.DATASET_STORAGE)
@RequiredArgsConstructor
public class DatasetStorageNode extends AbstractWorkflowNode {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;

    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final IKmEtlService etlService;
    private final IKmEmbeddingService embeddingService;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 DATASET_STORAGE 节点");

        // 1. 获取必要输入
        String text = (String) context.getInput(NodeIOConstants.OUTPUT_TEXT);
        if (text == null) {
            throw new IllegalArgumentException("DATASET_STORAGE node requires input: text");
        }

        Long documentId = (Long) context.getInput(NodeIOConstants.OUTPUT_DOCUMENT_ID);
        log.info("DATASET_STORAGE resolved documentId: {}", documentId);
        if (documentId == null) {
            throw new IllegalArgumentException("DATASET_STORAGE node requires input: documentId");
        }

        // 2. 查询文档和数据集
        KmDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: documentId=" + documentId);
        }

        KmDataset dataset = datasetMapper.selectById(document.getDatasetId());
        Long kbId = document.getKbId();

        // 3. 确定分块参数（优先节点 config > 数据集配置 > 系统默认值）
        int chunkSize = resolveChunkSize(context, dataset);
        int overlap = resolveOverlap(context, dataset);

        log.info("开始分块存储: documentId={}, kbId={}, chunkSize={}, overlap={}, textLength={}",
                documentId, kbId, chunkSize, overlap, text.length());

        // 4. 分块
        List<String> chunks = etlService.splitText(text, chunkSize, overlap);
        if (chunks == null || chunks.isEmpty()) {
            log.warn("文本分块结果为空，跳过存储: documentId={}", documentId);
            updateDocumentStatus(document, 2, null);
            NodeOutput output = new NodeOutput();
            output.addOutput(NodeIOConstants.OUTPUT_CHUNK_COUNT, 0);
            return output;
        }

        // 5. 转换为 ChunkResult
        List<ChunkResult> chunkResults = IntStream.range(0, chunks.size())
                .mapToObj(i -> ChunkResult.of(chunks.get(i)))
                .collect(Collectors.toList());

        // 6. 向量化并写入 pgvector
        embeddingService.embedAndStoreChunks(documentId, kbId, chunkResults);

        // 7. 更新文档状态为已完成（2）
        updateDocumentStatus(document, 2, null);

        log.info("DATASET_STORAGE 完成: documentId={}, chunkCount={}", documentId, chunkResults.size());

        NodeOutput output = new NodeOutput();
        output.addOutput(NodeIOConstants.OUTPUT_CHUNK_COUNT, chunkResults.size());
        return output;
    }

    private void updateDocumentStatus(KmDocument document, int status, String errorMsg) {
        KmDocument update = new KmDocument();
        update.setId(document.getId());
        update.setEmbeddingStatus(status);
        update.setErrorMsg(errorMsg);
        update.setStatusMeta(StatusMetaUtils.updateStateTime(
                document.getStatusMeta(),
                StatusMetaUtils.TASK_EMBEDDING,
                status == 2 ? StatusMetaUtils.STATUS_SUCCESS : StatusMetaUtils.STATUS_FAILED));
        documentMapper.updateById(update);
    }

    private int resolveChunkSize(NodeContext context, KmDataset dataset) {
        // 优先读取节点配置
        Object nodeChunkSize = context.getConfig(NodeConfigConstants.CFG_FILE_CHUNK_SIZE);
        if (nodeChunkSize instanceof Number) {
            return ((Number) nodeChunkSize).intValue();
        }
        // 从数据集获取
        if (dataset != null && dataset.getMaxChunkSize() != null) {
            return dataset.getMaxChunkSize();
        }
        return DEFAULT_CHUNK_SIZE;
    }

    private int resolveOverlap(NodeContext context, KmDataset dataset) {
        // 优先读取节点配置
        Object nodeOverlap = context.getConfig(NodeConfigConstants.CFG_FILE_OVERLAP);
        if (nodeOverlap instanceof Number) {
            return ((Number) nodeOverlap).intValue();
        }
        // 从数据集获取
        if (dataset != null && dataset.getChunkOverlap() != null) {
            return dataset.getChunkOverlap();
        }
        return DEFAULT_OVERLAP;
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(obj.toString());
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.DATASET_STORAGE;
    }

    @Override
    public String getNodeName() {
        return "数据集存储";
    }
}
