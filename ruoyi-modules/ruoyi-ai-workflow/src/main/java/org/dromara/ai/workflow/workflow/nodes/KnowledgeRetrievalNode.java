package org.dromara.ai.workflow.workflow.nodes;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.knowledge.domain.bo.KmRetrievalBo;
import org.dromara.ai.knowledge.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.knowledge.service.IKmRetrievalService;
import org.dromara.ai.workflow.workflow.core.AbstractWorkflowNode;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.dromara.ai.workflow.workflow.nodes.nodeUtils.SseHelper;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识检索节点
 * 从知识库中检索相关文档片段，用于 RAG 对话
 *
 * @author Mahone
 * @date 2026-01-29
 */
@Slf4j
@RequiredArgsConstructor
@Component("KNOWLEDGE_RETRIEVAL")
public class KnowledgeRetrievalNode extends AbstractWorkflowNode {

    private final IKmRetrievalService retrievalService;

    @Override
    public NodeOutput execute(NodeContext context) throws Exception {
        log.info("执行KNOWLEDGE_RETRIEVAL节点");

        NodeOutput output = new NodeOutput();
        SseEmitter emitter = context.getSseEmitter();
        // Boolean streamOutput = context.getConfigAsBoolean("streamOutput", false);
        Boolean streamOutput = true;

        // 1. 获取查询文本
        String query = (String) context.getInput("query");
        if (StrUtil.isBlank(query)) {
            throw new RuntimeException("查询文本不能为空 (query)");
        }

        // 2. 获取配置参数
        List<Long> kbIds = getConfigAsList(context, "kbIds");
        List<Long> datasetIds = getConfigAsList(context, "datasetIds");
        Integer topK = context.getConfigAsInteger("topK", 5);
        Double threshold = context.getConfigAsDouble("threshold", 0.5);
        String mode = context.getConfigAsString("mode");
        if (StrUtil.isBlank(mode)) {
            mode = "VECTOR";
        }
        Boolean enableRerank = context.getConfigAsBoolean("enableRerank", false);
        String emptyResponse = context.getConfigAsString("emptyResponse");

        // 3. 发送检索开始事件
        SseHelper.sendThinking(emitter, streamOutput, "🔍 正在检索知识库...\n");

        // 4. 构建检索请求
        KmRetrievalBo bo = new KmRetrievalBo();
        bo.setQuery(query);
        bo.setKbIds(kbIds);
        bo.setDatasetIds(datasetIds);
        bo.setTopK(topK);
        bo.setThreshold(threshold);
        bo.setMode(mode);
        bo.setEnableRerank(enableRerank);

        // 5. 执行检索
        List<KmRetrievalResultVo> results = retrievalService.search(bo);

        log.info("知识检索完成, 共检索到 {} 条结果", results.size());

        // 6. 处理空结果降级
        String contextText;
        boolean hasResults = CollUtil.isNotEmpty(results);

        if (!hasResults && StrUtil.isNotBlank(emptyResponse)) {
            // 配置了空结果回复，使用预设文本
            contextText = emptyResponse;
            SseHelper.sendThinking(emitter, streamOutput, "⚠️ 未找到相关内容，使用预设回复\n");
            log.info("未检索到结果，使用预设的空结果回复");
        } else if (!hasResults) {
            // 未配置空结果回复，给 LLM 明确的"无结果"指令
            contextText = "在知识库中未找到与用户问题相关的内容。请如实告知用户未能找到相关信息，不要编造答案。";
            SseHelper.sendThinking(emitter, streamOutput, "⚠️ 未找到相关内容\n");
        } else {
            // 正常构建上下文
            contextText = buildContextText(results);
            SseHelper.sendThinking(emitter, streamOutput,
                    "✅ 检索到 " + results.size() + " 条相关内容\n");
        }

        // 7. 设置输出
        output.addOutput("retrievedDocs", results);
        output.addOutput("context", contextText);
        output.addOutput("docCount", results.size());
        output.addOutput("hasResults", hasResults);

        // 8. 设置全局变量供后续节点使用
        context.setGlobalValue("retrievedContext", contextText);
        context.setGlobalValue("retrievedDocs", results);

        log.info("KNOWLEDGE_RETRIEVAL节点执行完成");
        return output;
    }

    /**
     * 构建上下文文本
     * 将检索结果拼接成 LLM 可用的上下文格式，使用 [1] 标记便于引用
     */
    private String buildContextText(List<KmRetrievalResultVo> results) {
        if (CollUtil.isEmpty(results)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是从知识库中检索到的相关内容，请在回答时使用 [序号] 格式引用来源：\n\n");

        for (int i = 0; i < results.size(); i++) {
            KmRetrievalResultVo result = results.get(i);
            sb.append("[").append(i + 1).append("] ");
            if (StrUtil.isNotBlank(result.getDocumentName())) {
                sb.append("《").append(result.getDocumentName()).append("》");
            }
            sb.append("\n");
            sb.append(result.getContent());
            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 获取配置中的 List<Long> 参数
     */
    private List<Long> getConfigAsList(NodeContext context, String key) {
        Object value = context.getConfig(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(item -> {
                        if (item instanceof Number) {
                            return ((Number) item).longValue();
                        }
                        return Long.parseLong(item.toString());
                    })
                    .collect(Collectors.toList());
        }
        // 支持逗号分隔的字符串格式
        if (value instanceof String) {
            String str = (String) value;
            if (StrUtil.isBlank(str)) {
                return new ArrayList<>();
            }
            List<Long> result = new ArrayList<>();
            for (String s : str.split(",")) {
                result.add(Long.parseLong(s.trim()));
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public String getNodeType() {
        return "KNOWLEDGE_RETRIEVAL";
    }

    @Override
    public String getNodeName() {
        return "知识检索";
    }
}
