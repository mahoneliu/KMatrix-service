package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
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
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.service.IKmFileService;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 语音识别节点
 * 将传入的音频附件转换成文本形式
 */
@Slf4j
@Component("AUDIO_ASR")
@RequiredArgsConstructor
public class AsrNode extends AbstractWorkflowNode {

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;
    private final ObjectProvider<ISysOssService> sysOssServiceProvider;
    private final ObjectProvider<IKmFileService> kmFileServiceProvider;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行AUDIO_ASR节点");
        NodeOutput output = new NodeOutput();
        
        Long modelId = context.getConfigAsLong("modelId");
        if (modelId == null) {
            throw new RuntimeException("AsrNode 缺少 modelId 配置");
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
                    wf.setTempFileId(map.get("tempFileId") instanceof Number ? ((Number) map.get("tempFileId")).longValue() : null);
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
                        Long idVal = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
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
            throw new RuntimeException("未提供要识别的音频文件 (缺少 files 或 ossIds 参数)");
        }

        KmWorkflowFile fileToProcess = targetFiles.get(0); // 只取第一个处理
        String fileIdRef = fileToProcess.getTempFileId() != null 
                ? fileToProcess.getTempFileId().toString() 
                : (fileToProcess.getOssId() != null ? fileToProcess.getOssId().toString() : null);
        String url = resolveOssUrlOrBase64(fileIdRef, fileToProcess.getUrl());
        if (StrUtil.isBlank(url)) {
            throw new RuntimeException("无法获取有效的音频资源 URL");
        }
        if (url.startsWith("/")) {
            throw new RuntimeException("系统未能成功把音频文件转为 Base64，无法将不支持的内部系统相对路径发给大模型进行处理。被拒绝的路径：" + url);
        }

        // 构建大模型消息
        List<ChatMessage> messages = new ArrayList<>();
        String systemPrompt = "你是一个专业、严谨的自动语音识别引擎(ASR)。请必须完全精确地提取传入音频文件中的文字。严禁输出任何多余的解释、问候语、格式标记或Markdown语法，只输出唯一的识别纯文本结果。";
        messages.add(new SystemMessage(systemPrompt));

        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from("请根据系统设定，提取这段音频。"));
        contents.add(AudioContent.from(url));
        messages.add(UserMessage.from(contents));

        // 调用大模型
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), 0.1, 8192);
        log.info("AUDIO_ASR节点 - 开始调用多模态音频模型进行识别");
        Response<AiMessage> response = chatModel.generate(messages);
        
        String transcribedText = response.content().text();
        log.info("AUDIO_ASR节点执行成功, 识别文本长度: {}", transcribedText.length());

        output.addOutput("transcription", transcribedText);
        output.addOutput("ossId", fileToProcess.getOssId());
        
        if (response.tokenUsage() != null) {
            Map<String, Object> tokenUsageMap = Map.of(
                "inputTokenCount", response.tokenUsage().inputTokenCount(),
                "outputTokenCount", response.tokenUsage().outputTokenCount(),
                "totalTokenCount", response.tokenUsage().totalTokenCount()
            );
            context.setTokenUsage(tokenUsageMap);
            output.addOutput("tokenUsage", tokenUsageMap);
        }

        return output;
    }

    private String resolveOssUrlOrBase64(String ossIdStr, String fallbackUrl) {
        if (StrUtil.isNotBlank(ossIdStr) && !"undefined".equals(ossIdStr)) {
            try {
                Long fileId = Long.parseLong(ossIdStr);
                
                // 开启最强力的全局忽略：忽略数据权限和多租户，确保异步线程(无租户Context)也能读到数据
                InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).tenantLine(true).build());
                try {
                    // 1. 优先尝试作为聊天产生的 KmTempFile 获取
                    IKmFileService fileService = kmFileServiceProvider.getIfAvailable();
                    if (fileService != null) {
                        KmTempFile tempFile = fileService.getTempFile(fileId);
                        if (tempFile != null) {
                            String absolutePath = "未知路径";
                            try {
                                // 尝试探测绝对路径用于日志诊断
                                try {
                                    java.lang.reflect.Method getAbs = fileService.getClass().getMethod("getAbsolutePath", String.class);
                                    absolutePath = (String) getAbs.invoke(fileService, tempFile.getFilePath());
                                } catch (Exception ignore) {}

                                log.info("尝试读取本地文件流进行音频 Base64 转换, ID: {}, 物理路径: {}", fileId, absolutePath);
                                
                                try (java.io.InputStream is = fileService.getFileStream(tempFile.getStoreType(), tempFile.getOssId(), tempFile.getFilePath())) {
                                    byte[] data = is.readAllBytes();
                                    if (data.length == 0) {
                                        throw new RuntimeException("读取到的音频流数据为空");
                                    }
                                    String base64 = Base64.getEncoder().encodeToString(data);
                                    String mimeType = "audio/mpeg";
                                    if (StrUtil.isNotBlank(tempFile.getFileExtension())) {
                                        String inferred = FileUtils.getMimeType("dummy." + tempFile.getFileExtension());
                                        if (StrUtil.isNotBlank(inferred)) {
                                            mimeType = inferred;
                                        }
                                    }
                                    return "data:" + mimeType + ";base64," + base64;
                                }
                            } catch (Exception ex) {
                                log.error("通过临时文件(ID:{})转码音频Base64内容失败: {}", fileId, ex.getMessage(), ex);
                            }
                        }
                    }
                    
                    // 2. 如果不是临时文件，或者读取流失败，尝试作为普通 SysOssVo 获取
                    ISysOssService ossService = sysOssServiceProvider.getIfAvailable();
                    if (ossService != null) {
                        SysOssVo ossVo = ossService.getById(fileId);
                        if (ossVo != null) {
                            try {
                                OssClient storage = OssFactory.instance(ossVo.getService());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                storage.download(ossVo.getFileName(), baos, null);
                                byte[] bytes = baos.toByteArray();
                                String base64 = Base64.getEncoder().encodeToString(bytes);
                                String mimeType = "audio/mpeg";
                                if (StrUtil.isNotBlank(ossVo.getFileSuffix())) {
                                    String inferred = FileUtils.getMimeType("dummy." + ossVo.getFileSuffix());
                                    if (StrUtil.isNotBlank(inferred)) {
                                        mimeType = inferred;
                                    }
                                }
                                return "data:" + mimeType + ";base64," + base64;
                            } catch (Exception ex) {
                                log.error("通过通用OSS管理转码Base64音频失败: {}, 将回退使用URL", fileId, ex);
                                return ossVo.getUrl();
                            }
                        }
                    }
                } finally {
                    // 必须清理，防止污染当前线程后续操作
                    InterceptorIgnoreHelper.clearIgnoreStrategy();
                }
            } catch (Exception e) {
                log.error("解析ossId转音频Base64失败: {}, 错误: {}", ossIdStr, e.getMessage(), e);
                throw new RuntimeException("多模态音频数据解析失败: " + e.getMessage());
            }
        }
        
        if (StrUtil.isNotBlank(fallbackUrl)) {
            // 如果是在转换 Base64 场景下，且使用了相对路径，说明转换失败了
            if (fallbackUrl.startsWith("/")) {
                log.error("核心逻辑错误: 无法根据音频 ID 找到文件流，且降级到了无效的相对路径: {}", fallbackUrl);
                throw new RuntimeException("音频资源转换失败，无法从 ID 解析到 Base64 内容，请检查本地存储是否完整。路径: " + fallbackUrl);
            }
            return fallbackUrl;
        }
        return null;
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
