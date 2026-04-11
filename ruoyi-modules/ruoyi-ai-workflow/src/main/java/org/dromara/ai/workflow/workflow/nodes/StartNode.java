package org.dromara.ai.workflow.workflow.nodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.core.WorkflowState;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import cn.hutool.core.util.StrUtil;

/**
 * 开始节点
 * 接收用户输入并保存到全局状态
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Component("START")
public class StartNode extends AbstractWorkflowNode {

    private final IKmFileService kmFileService;

    // public static final String KEY_USER_INPUT = "userInput";

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行START节点");

        NodeOutput output = new NodeOutput();

        // 获取用户输入 - 从 LangGraph 的 state 中获取
        String userInput = (String) context.getGlobalValue(WorkflowState.KEY_USER_INPUT);

        // 1.1 从全局状态获取 documentId
        Long documentId = (Long) context.getGlobalValue(WorkflowState.KEY_DOCUMENT_ID);
        log.info("documentId: {}", documentId);

        // documentId 可能因 JSON 序列化/反序列化变成 Integer，需要安全转换
        // Object documentIdRaw = context.getGlobalValue(WorkflowState.KEY_DOCUMENT_ID);
        // Long documentId = null;
        // if (documentIdRaw instanceof Number) {
        // documentId = ((Number) documentIdRaw).longValue();
        // } else if (documentIdRaw instanceof String) {
        // try {
        // documentId = Long.parseLong((String) documentIdRaw);
        // } catch (Exception ignore) {
        // }
        // }
        // // 兜底：globalState 里没有时，尝试把 userInput 当 documentId 解析（调度器 message=documentId）
        // if (documentId == null && StrUtil.isNotBlank(userInput)) {
        // try {
        // documentId = Long.parseLong(userInput.trim());
        // log.info("START节点从 userInput 解析 documentId={}", documentId);
        // } catch (NumberFormatException ignore) {
        // }
        // }

        // 1. 保存用户输入到全局状态
        List<String> historyContext = new ArrayList<>();
        historyContext.add(userInput);
        context.setGlobalValue(WorkflowState.KEY_HISTORY_CONTEXT, historyContext);

        // ================== 解析多模态参数提取 ==================
        List<KmWorkflowFile> extractedFilesFromInput = new ArrayList<>();
        List<Long> extractedOssIds = new ArrayList<>();

        // 如果 userInput 是 JSON 数组格式（代表是从前端 Chat 窗口传上来的多模态数据）
        if (StrUtil.isNotBlank(userInput) && cn.hutool.json.JSONUtil.isTypeJSONArray(userInput)) {
            try {
                cn.hutool.json.JSONArray array = cn.hutool.json.JSONUtil.parseArray(userInput);
                for (int i = 0; i < array.size(); i++) {
                    cn.hutool.json.JSONObject obj = array.getJSONObject(i);
                    // 找到了多模态文件对象 (排除普通 text 类型的字符串片段)
                    if (obj != null && obj.containsKey("type") && !("text".equals(obj.getStr("type")))) {
                        KmWorkflowFile file = new KmWorkflowFile();
                        file.setType(obj.getStr("type"));
                        file.setUrl(obj.getStr("url"));
                        file.setName(obj.getStr("name"));

                        String ossIdStr = obj.getStr("ossId");
                        String tempFileIdStr = obj.getStr("tempFileId");

                        if (StrUtil.isNotBlank(tempFileIdStr) && !"undefined".equals(tempFileIdStr)) {
                            try {
                                file.setTempFileId(Long.parseLong(tempFileIdStr));
                            } catch (Exception ignore) {
                            }
                        }

                        if (StrUtil.isNotBlank(ossIdStr) && !"undefined".equals(ossIdStr)) {
                            try {
                                Long ossId = Long.parseLong(ossIdStr);
                                file.setOssId(ossId);
                                extractedOssIds.add(ossId);
                            } catch (Exception ignore) {
                            }
                        }
                        extractedFilesFromInput.add(file);
                    }
                }
            } catch (Exception e) {
                log.warn("START节点解析多模态 userInput 失败", e);
            }
        }
        // =======================================================

        // 2. 处理初始上传的文件
        List<KmWorkflowFile> workflowFiles = new ArrayList<>();

        List<Long> tempFileIds = (List<Long>) context.getGlobalValue(WorkflowState.KEY_TEMP_FILE_IDS);
        if (tempFileIds != null && !tempFileIds.isEmpty()) {
            workflowFiles.addAll(tempFileIds.stream()
                    .map(id -> {
                        KmTempFile tempFile = kmFileService.getTempFile(id);
                        if (tempFile == null)
                            return null;
                        return KmWorkflowFile.builder()
                                .type(determineMediaType(tempFile.getFileExtension()))
                                .name(tempFile.getOriginalFilename())
                                .extension(tempFile.getFileExtension())
                                .size(tempFile.getFileSize())
                                .tempFileId(id)
                                .url(tempFile.getFilePath()) // 初始使用本地路径
                                .build();
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        // 合并文件: 把从 chat 中解析出的文件也加入其中
        if (!extractedFilesFromInput.isEmpty()) {
            workflowFiles.addAll(extractedFilesFromInput);
        }

        // 把文件合并集合推入全局状态并导出为 files 和 file
        if (!workflowFiles.isEmpty()) {
            context.setGlobalValue("files", workflowFiles);
            output.addOutput("files", workflowFiles);
            // 如果只有一个文件，额外输出单文件对象
            if (workflowFiles.size() == 1) {
                output.addOutput("file", workflowFiles.get(0));
            }
        }

        // 3.保存用户名到全局状态
        String username = LoginHelper.getUsername();
        context.setGlobalValue(WorkflowState.KEY_USER_NAME, username);

        // 4.保存到输出
        output.addOutput(WorkflowState.KEY_USER_INPUT, userInput);
        if (documentId != null) {
            output.addOutput(WorkflowState.KEY_DOCUMENT_ID, documentId);
        }

        // 5.保存用户ID到输出
        Long userId = LoginHelper.getUserId();
        output.addOutput(WorkflowState.KEY_USER_ID, userId);

        log.info("START节点执行完成, userInput={}", userInput);
        return output;
    }

    private String determineMediaType(String ext) {
        if (StrUtil.isBlank(ext))
            return "file";
        ext = ext.toLowerCase();
        if (List.of("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(ext))
            return "image";
        if (List.of("mp3", "wav", "flac", "aac", "ogg", "m4a").contains(ext))
            return "audio";
        if (List.of("mp4", "mov", "avi", "mkv", "webm").contains(ext))
            return "video";
        return "file";
    }

    @Override
    public String getNodeType() {
        return "START";
    }

    @Override
    public String getNodeName() {
        return "开始";
    }
}
