package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.domain.KmModelProvider;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.workflow.workflow.core.AbstractAiWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.WorkflowNodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 语音识别节点
 * 将传入的音频附件转换成文本形式
 */
@Slf4j
@Component("AUDIO_ASR")
public class AsrNode extends AbstractAiWorkflowNode {

    @Autowired
    private WorkflowNodeUtils workflowNodeUtils;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行AUDIO_ASR节点");
        NodeOutput output = new NodeOutput();

        Long modelId = context.getConfigAsLong("modelId");
        if (modelId == null) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.asr.missing_model_id"));
        }

        // 加载模型（基类统一处理）
        Object[] modelAndProvider = loadModelAndProviderById(modelId);
        KmModel model = (KmModel) modelAndProvider[0];
        KmModelProvider provider = (KmModelProvider) modelAndProvider[1];

        // 提取待识别的音频文件
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
                        wf.setType("audio");
                        targetFiles.add(wf);
                    } catch (Exception e) {
                        log.warn("AsrNode 解析 ossId 失败: {}", idObj);
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
                    if ("audio".equals(f.getType())) {
                        targetFiles.add(f);
                        break;
                    }
                }
            }
        }

        if (targetFiles.isEmpty()) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.asr.missing_file"));
        }

        KmWorkflowFile fileToProcess = targetFiles.get(0); // 只取第一个处理
        String fileIdRef = fileToProcess.getTempFileId() != null
                ? fileToProcess.getTempFileId().toString()
                : (fileToProcess.getOssId() != null ? fileToProcess.getOssId().toString() : null);
        String url = workflowNodeUtils.resolveOssUrlOrBase64(fileIdRef, fileToProcess.getUrl(), "audio/mpeg");
        if (StrUtil.isBlank(url)) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.asr.invalid_url"));
        }
        if (url.startsWith("/")) {
            throw new RuntimeException(MessageUtils.message("ai.workflow.node.asr.local_path_denied", url));
        }

        // 构建大模型消息
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = MessageUtils.message("ai.workflow.node.asr.system_prompt");
        messages.add(new SystemMessage(systemPrompt));

        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(MessageUtils.message("ai.workflow.node.asr.user_prompt")));
        contents.add(AudioContent.from(url));
        messages.add(UserMessage.from(contents));

        // 调用大模型
        ChatModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), 0.1, 8192);
        log.info("AUDIO_ASR节点 - 开始调用多模态音频模型进行识别");
        ChatResponse response = chatModel.chat(messages);

        String transcribedText = response.aiMessage().text();
        log.info("AUDIO_ASR节点执行成功, 识别文本长度: {}", transcribedText.length());

        output.addOutput("transcription", transcribedText);
        output.addOutput("ossId", fileToProcess.getOssId());

        // token 统计（基类统一处理）
        Map<String, Object> tokenUsageMap = recordTokenUsage(response, context);
        if (tokenUsageMap != null) {
            output.addOutput("tokenUsage", tokenUsageMap);
        }

        return output;
    }

    @Override
    public String getNodeType() {
        return "AUDIO_ASR";
    }

    @Override
    public String getNodeName() {
        return "语音识别";
    }
}
