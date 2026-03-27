package org.dromara.ai.workflow.workflow.nodes;

import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.storage.domain.dto.KmWorkflowFile;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.springframework.stereotype.Component;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 文件存储节点
 * 接收基础的图片、音频等信息引用（如 ossId）并流转到后续节点
 */
@Slf4j
@Component("FILE_STORAGE")
public class FileStorageNode extends AbstractWorkflowNode {

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行FILE_STORAGE节点");
        NodeOutput output = new NodeOutput();
        
        Object ossIdObj = ObjectUtil.defaultIfNull(context.getInput("ossId"), context.getConfig("ossId"));
        String url = ObjectUtil.defaultIfNull((String)context.getInput("url"), context.getConfigAsString("url"));
        String type = ObjectUtil.defaultIfNull((String)context.getInput("type"), context.getConfigAsString("type", "image"));
        String name = ObjectUtil.defaultIfNull((String)context.getInput("name"), context.getConfigAsString("name", "file"));

        Long ossId = null;
        if (ossIdObj != null) {
            if (ossIdObj instanceof Number) {
                ossId = ((Number) ossIdObj).longValue();
            } else {
                try {
                    ossId = Long.parseLong(ossIdObj.toString());
                } catch (Exception ignored) {}
            }
        }

        KmWorkflowFile workflowFile = KmWorkflowFile.builder()
                .ossId(ossId)
                .url(url)
                .type(type)
                .name(name)
                .build();

        // 统一输出为 ossIds 数组，匹配 SQL 定义
        output.addOutput("ossIds", java.util.List.of(ossId));
        output.addOutput("file", workflowFile);
        output.addOutput("ossId", ossId);
        output.addOutput("url", url);
        return output;
    }

    @Override
    public String getNodeType() {
        return "FILE_STORAGE";
    }

    @Override
    public String getNodeName() {
        return "文件存储";
    }
}
