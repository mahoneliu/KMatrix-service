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

    public static final String KEY_USER_INPUT = "userInput";

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行START节点");

        NodeOutput output = new NodeOutput();

        // 获取用户输入 - 从 LangGraph 的 state 中获取
        String userInput = (String) context.getGlobalValue(KEY_USER_INPUT);

        // 1. 保存用户输入到全局状态
        List<String> historyContext = new ArrayList<>();
        historyContext.add(userInput);
        context.setGlobalValue(WorkflowState.KEY_HISTORY_CONTEXT, historyContext);

        // 2. 处理初始上传的文件
        List<Long> tempFileIds = (List<Long>) context.getGlobalValue(WorkflowState.KEY_TEMP_FILE_IDS);
        if (tempFileIds != null && !tempFileIds.isEmpty()) {
            List<KmWorkflowFile> workflowFiles = tempFileIds.stream()
                .map(id -> {
                    KmTempFile tempFile = kmFileService.getTempFile(id);
                    if (tempFile == null) return null;
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
                .collect(Collectors.toList());
            
            context.setGlobalValue(WorkflowState.KEY_FILES, workflowFiles);
            output.addOutput(WorkflowState.KEY_FILES, workflowFiles);
        }

        // 3.保存用户名到全局状态
        String username = LoginHelper.getUsername();
        context.setGlobalValue(WorkflowState.KEY_USER_NAME, username);

        // 3.保存到输出
        output.addOutput(KEY_USER_INPUT, userInput);

        log.info("START节点执行完成, userInput={}", userInput);
        return output;
    }

    private String determineMediaType(String ext) {
        if (StrUtil.isBlank(ext)) return "file";
        ext = ext.toLowerCase();
        if (List.of("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(ext)) return "image";
        if (List.of("mp3", "wav", "flac", "aac", "ogg", "m4a").contains(ext)) return "audio";
        if (List.of("mp4", "mov", "avi", "mkv", "webm").contains(ext)) return "video";
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
