package org.dromara.ai.workflow.workflow.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.util.StatusMetaUtils;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.storage.mapper.KmTempFileMapper;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Component;
import cn.hutool.core.util.StrUtil;
import org.dromara.common.core.utils.MessageUtils;

/**
 * 文件存储节点
 * 接收基础的图片、音频等信息引用（如 ossId）并流转到后续节点
 */
@Slf4j
@Component("FILE_STORAGE")
@RequiredArgsConstructor
public class FileStorageNode extends AbstractWorkflowNode {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final KmTempFileMapper tempFileMapper;
    private final ISysConfigService configService;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行文档存储节点 (FILE_STORAGE)");
        
        // 1. 获取输入参数，兼容 LinkedHashMap（JSON反序列化结果）和直接对象
        Object raw = context.getInput("file");
        if (raw == null) {
            throw new IllegalArgumentException(MessageUtils.message("ai.workflow.node.file_storage.missing_input"));
        }
        KmWorkflowFile file = OBJECT_MAPPER.convertValue(raw, KmWorkflowFile.class);

        // 2. 决定 datasetId (参数 > 配置 > 系统兜底)
        Long datasetId = resolveDatasetId(context);
        if (datasetId == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.file_storage.missing_dataset_id"));
        }

        // 3. 查询数据集元数据获取 kbId
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.dataset_not_found") + ": " + datasetId);
        }

        // 4. 解析真实文件路径：优先通过 tempFileId 查 KmTempFile 拿相对路径，避免存 URL 前缀
        String filePath;
        Integer storeType;
        Long ossId = file.getOssId();
        if (file.getTempFileId() != null) {
            KmTempFile tempFile = tempFileMapper.selectById(file.getTempFileId());
            if (tempFile == null) {
                throw new RuntimeException(MessageUtils.message("ai.msg.knowledge.temp_file_not_found") + ": tempFileId=" + file.getTempFileId());
            }
            filePath = tempFile.getFilePath();
            storeType = tempFile.getStoreType();
            if (ossId == null) ossId = tempFile.getOssId();
        } else if (ossId != null) {
            filePath = file.getUrl();
            storeType = 1;
        } else {
            // 兜底：直接用传入的 url（知识库管理页面上传场景，已是相对路径）
            filePath = file.getUrl();
            storeType = 2;
        }

        // 5. 处理文件
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId());
        document.setOriginalFilename(file.getName());
        document.setFileType(file.getExtension());
        document.setFileSize(file.getSize());
        document.setOssId(ossId);
        document.setFilePath(filePath);
        document.setStoreType(storeType);
        
        // 6. 初始化状态为 0 (待处理)
        document.setEmbeddingStatus(0);
        document.setEnabled(1);
        document.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));
        
        documentMapper.insert(document);
        log.info("文档收录成功: documentId={}, fileName={}, filePath={}", document.getId(), document.getOriginalFilename(), filePath);

        NodeOutput output = new NodeOutput();
        output.addOutput(WorkflowState.KEY_DOCUMENT_ID, document.getId());
        return output;
    }

    private Long resolveDatasetId(NodeContext context) {
        // 1. 上游参数传入
        Object val = context.getInput("datasetId");
        log.info("resolveDatasetId - input[datasetId]={}", val);
        if (val != null) return toLong(val);
        
        // 2. 节点配置
        val = context.getConfig("datasetId");
        log.info("resolveDatasetId - config[datasetId]={}, nodeConfig={}", val, context.getNodeConfig());
        if (val != null) return toLong(val);
        
        // 3. 系统兜底配置
        String defaultId = configService.selectConfigByKey("ai.workflow.default_dataset_id");
        log.info("resolveDatasetId - sysConfig[ai.workflow.default_dataset_id]={}", defaultId);
        if (StrUtil.isNotBlank(defaultId)) {
            return Long.parseLong(defaultId);
        }
        
        return null;
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.parseLong(obj.toString());
    }

    @Override
    public String getNodeType() {
        return "FILE_STORAGE";
    }

    @Override
    public String getNodeName() {
        return "文档存储";
    }
}
