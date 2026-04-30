package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.domain.bo.ChunkResult;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.knowledge.service.etl.DatasetProcessType;
import org.dromara.ai.knowledge.service.etl.EtlHandler;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.workflow.constant.NodeConfigConstants;
import org.dromara.ai.workflow.constant.NodeIOConstants;
import org.dromara.ai.workflow.constant.NodeTypeConstants;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件解析节点
 * <p>
 * 节点类型标识: FILE_PARSE
 * <p>
 * 输入:
 * - documentId (Long): 待解析的文档 ID
 * - file (object): 实时文件对象 (支持不入库解析)
 * <p>
 * 配置项:
 * - processType (String): 手动指定解析方式（GENERIC_FILE/QA_PAIR 等），若不指定则根据数据集自动识别
 * <p>
 * 内部逻辑:
 * 1. 根据 documentId 或 file 获取待解析源。
 * 2. 路由合适的 EtlHandler 进行业务解析（如 QA对、网页抓取等）。
 * 3. 兜底使用 Apache Tika 将文件解析为纯文本。
 * <p>
 * 输出:
 * - text (String): 解析后的纯文本内容
 *
 * @author Mahone
 * @date 2026-04-02
 */
@Slf4j
@Component(NodeTypeConstants.FILE_PARSE)
@RequiredArgsConstructor
public class FileParseNode extends AbstractWorkflowNode {

    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final IKmFileService fileService;
    private final List<EtlHandler> etlHandlers;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行 FILE_PARSE 节点");

        // 1. 获取输入参数 (优先获取 file，其次是 documentId)
        KmWorkflowFile workflowFile = (KmWorkflowFile) context.getInput(NodeIOConstants.INPUT_FILE);
        Long finalDocumentId = toLong(context.getInput(NodeIOConstants.INPUT_DOCUMENT_ID));

        KmDocument document = null;

        if (workflowFile != null) {
            log.info("从实时文件对象解析: name={}", workflowFile.getName());
        } else if (finalDocumentId != null) {
            document = documentMapper.selectById(finalDocumentId);
            if (document == null) {
                throw new IllegalArgumentException("未找到文档记录: documentId=" + finalDocumentId);
            }
            log.info("从数据库记录解析: documentId={}", finalDocumentId);
        } else {
            throw new IllegalArgumentException("FILE_PARSE 节点缺少输入参数: file 或 documentId");
        }

        // 2. 确定配置的解析类型 (优先级: 节点静态配置 > 数据集动态配置 > 兜底)
        String processType = context.getConfigAsString(NodeConfigConstants.CFG_FILE_PROCESS_TYPE);
        if (processType == null && document != null && document.getDatasetId() != null) {
            KmDataset dataset = datasetMapper.selectById(document.getDatasetId());
            if (dataset != null) {
                processType = dataset.getProcessType();
            }
        }

        // 3. 安全防护：如果是“工作流编排”或为空，降级为“通用文件解析”以防止死循环
        if (processType == null || DatasetProcessType.WORKFLOW_FILE.equals(processType)) {
            processType = DatasetProcessType.GENERIC_FILE;
        }

        log.info("识别到解析类型: {}", processType);

        String text = null;

        // 4. 如果是库内文档，尝试通过 Handler 处理 (支持复杂格式如 QA、网页等)
        if (document != null) {
            EtlHandler handler = findHandler(processType);
            if (handler != null) {
                log.info("使用 Handler 进行解析: {}", handler.getClass().getSimpleName());
                // 构造临时数据集对象供 Handler 使用配置项
                KmDataset dataset = new KmDataset();
                dataset.setProcessType(processType);

                List<ChunkResult> chunkResults = handler.process(document, dataset);
                if (CollUtil.isNotEmpty(chunkResults)) {
                    // 合并解析结果
                    text = chunkResults.stream()
                            .map(ChunkResult::getContent)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.joining("\n\n"));
                }
            }
        }

        // 5. 兜底使用 Tika 进行原始文件解析 (实时文件或 Handler 未处理情况)
        if (text == null) {
            log.info("使用 Tika 解析兜底");
            Integer storeType;
            Long ossId;
            String filePath;

            if (workflowFile != null) {
                storeType = workflowFile.getOssId() != null ? 1 : 2;
                ossId = workflowFile.getOssId();
                filePath = workflowFile.getUrl();
            } else {
                storeType = document.getStoreType();
                ossId = document.getOssId();
                filePath = document.getFilePath();
            }

            try (InputStream inputStream = fileService.getFileStream(storeType, ossId, filePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("无法获取文件流: path=" + filePath);
                }

                DocumentParser parser = new ApacheTikaDocumentParser();
                Document parsedDocument = parser.parse(inputStream);
                text = parsedDocument.text();
            }
        }

        if (text == null || text.isBlank()) {
            log.warn("文件解析结果为空");
            text = "";
        }

        log.info("文件解析完成, 文本长度: {}", text.length());

        NodeOutput output = new NodeOutput();
        output.addOutput(NodeIOConstants.OUTPUT_TEXT, text);
        if (finalDocumentId != null) {
            output.addOutput(NodeIOConstants.OUTPUT_DOCUMENT_ID, finalDocumentId);
        }
        return output;
    }

    private EtlHandler findHandler(String processType) {
        if (etlHandlers == null)
            return null;
        return etlHandlers.stream()
                .filter(h -> h.supports(processType))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getNodeType() {
        return NodeTypeConstants.FILE_PARSE;
    }

    @Override
    public String getNodeName() {
        return "文件解析";
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return Long.parseLong(obj.toString());
    }
}
