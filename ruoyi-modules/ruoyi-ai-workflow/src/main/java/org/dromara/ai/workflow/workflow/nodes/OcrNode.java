package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.dromara.ai.model.mapper.KmModelProviderMapper;
import org.dromara.ai.model.util.ModelBuilder;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowNodeUtils;

/**
 * 图像文字识别节点
 * 将图片提取为文本文本
 */
@Slf4j
@Component("IMAGE_OCR")
@RequiredArgsConstructor
public class OcrNode extends AbstractWorkflowNode {

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;
    private final WorkflowNodeUtils workflowNodeUtils;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        try {
            return doExecute(context);
        } catch (Throwable t) {
            log.error("OcrNode 执行发生异常: {}", t.getMessage(), t);
            throw t;
        }
    }

    private NodeOutput doExecute(NodeContext context) throws Exception {
        log.info("执行IMAGE_OCR节点");
        NodeOutput output = new NodeOutput();

        Long modelId = context.getConfigAsLong("modelId");
        if (modelId == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.ocr.missing_model_id"));
        }

        // 加载模型
        KmModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new RuntimeException("模型不存在: " + modelId);
        }
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("模型供应商不存在: " + model.getProviderId());
        }

        // 处理 apiBase
        String apiBase = StrUtil.isNotBlank(model.getApiBase()) ? model.getApiBase() : provider.getDefaultEndpoint();
        model.setApiBase(apiBase);

        // 提取待识别的图片文件
        List<KmWorkflowFile> targetFiles = new ArrayList<>();

        // 1. files
        Object inputFiles = context.getInput("files");
        if (inputFiles instanceof List) {
            for (Object item : (List<?>) inputFiles) {
                if (item instanceof KmWorkflowFile) {
                    targetFiles.add((KmWorkflowFile) item);
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    KmWorkflowFile wf = new KmWorkflowFile();
                    wf.setUrl((String) map.get("url"));
                    wf.setType((String) map.get("type"));
                    wf.setOssId(map.get("ossId") instanceof Number ? ((Number) map.get("ossId")).longValue() : null);
                    wf.setTempFileId(
                            map.get("tempFileId") instanceof Number ? ((Number) map.get("tempFileId")).longValue()
                                    : null);
                    wf.setName((String) map.get("name"));
                    targetFiles.add(wf);
                }
            }
        } else if (inputFiles instanceof KmWorkflowFile) {
            targetFiles.add((KmWorkflowFile) inputFiles);
        }

        // 2. ossIds
        if (targetFiles.isEmpty()) {
            Object inputOssIds = context.getInput("ossIds");
            if (inputOssIds == null) {
                inputOssIds = context.getInput("ossId");
            }
            if (inputOssIds != null) {
                List<Object> idList = new ArrayList<>();
                if (inputOssIds instanceof List) {
                    idList.addAll((List<?>) inputOssIds);
                } else {
                    idList.add(inputOssIds);
                }
                for (Object idObj : idList) {
                    try {
                        Long idVal = idObj instanceof Number ? ((Number) idObj).longValue()
                                : Long.parseLong(idObj.toString());
                        KmWorkflowFile wf = new KmWorkflowFile();
                        wf.setOssId(idVal);
                        wf.setType("image");
                        targetFiles.add(wf);
                    } catch (Exception e) {
                        log.warn("OcrNode 解析 ossId 失败: {}", idObj);
                    }
                }
            }
        }

        // 3. 最后兜底兼容全局的初始上传文件
        if (targetFiles.isEmpty()) {
            Object globalFiles = context.getGlobalValue("files");
            if (globalFiles instanceof List) {
                @SuppressWarnings("unchecked")
                List<KmWorkflowFile> list = (List<KmWorkflowFile>) globalFiles;
                for (KmWorkflowFile f : list) {
                    if ("image".equals(f.getType())) {
                        targetFiles.add(f);
                        break;
                    }
                }
            }
        }

        if (targetFiles.isEmpty()) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.ocr.missing_file"));
        }

        KmWorkflowFile fileToProcess = targetFiles.get(0); // 只取第一个处理

        // 优先使用 tempFileId
        String fileIdRef = fileToProcess.getTempFileId() != null
                ? fileToProcess.getTempFileId().toString()
                : (fileToProcess.getOssId() != null ? fileToProcess.getOssId().toString() : null);

        String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, fileToProcess.getUrl(), "image/jpeg");
        log.info("最终发给大模型的图片URL前缀: {}", url != null && url.length() > 50 ? url.substring(0, 50) + "..." : url);
        if (StrUtil.isBlank(url)) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.ocr.invalid_url"));
        }
        if (url.startsWith("/")) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.ocr.local_path_denied", url));
        }

        // 构建大模型消息
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = MessageUtils.message("ai.workflow.node.ocr.system_prompt");
        messages.add(new SystemMessage(systemPrompt));

        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(MessageUtils.message("ai.workflow.node.ocr.user_prompt")));
        contents.add(ImageContent.from(url));
        messages.add(UserMessage.from(contents));

        // 调用大模型
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), 0.1, 8192);
        log.info("IMAGE_OCR节点 - 开始调用多模态模型进行OCR识别");
        Response<AiMessage> response = chatModel.generate(messages);

        String extractedText = response.content().text();
        log.info("IMAGE_OCR节点执行成功, 识别文本长度: {}", extractedText.length());

        output.addOutput("text", extractedText);
        output.addOutput("ossId", fileToProcess.getOssId());

        if (response.tokenUsage() != null) {
            Map<String, Object> tokenUsageMap = Map.of(
                    "inputTokenCount", response.tokenUsage().inputTokenCount(),
                    "outputTokenCount", response.tokenUsage().outputTokenCount(),
                    "totalTokenCount", response.tokenUsage().totalTokenCount());
            context.setTokenUsage(tokenUsageMap);
            output.addOutput("tokenUsage", tokenUsageMap);
        }

        return output;
    }



    @Override
    public String getNodeType() {
        return "IMAGE_OCR";
    }

    @Override
    public String getNodeName() {
        return "图像OCR";
    }
}
