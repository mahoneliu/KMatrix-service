package org.dromara.ai.workflow.constant;

import org.dromara.ai.workflow.workflow.core.WorkflowState;

/**
 * 工作流节点输入/输出参数 Key 常量
 * <p>
 * 统一管理所有节点 {@code context.getInput()}、{@code output.addOutput()} 及
 * {@code context.setGlobalValue()} 中使用的字符串 key，避免 hardcode。
 *
 * <h3>与 WorkflowState.KEY_* 的关系</h3>
 * <p>
 * {@link WorkflowState} 定义的是状态机层（LangGraph4j AgentState）的 schema key，
 * 本接口定义的是节点层（输入/输出/全局状态）的 key。
 * 两者在语义上有重叠的 key（如 userInput、documentId、finalResponse），
 * 本接口通过直接引用 {@link WorkflowState} 常量消除重复定义，
 * 确保同一个字符串值只有一处定义。
 *
 * <h3>命名规范</h3>
 * <ul>
 *   <li>输入参数：{@code INPUT_*}</li>
 *   <li>输出参数：{@code OUTPUT_*}</li>
 *   <li>全局状态 key：{@code GLOBAL_*}（与 WorkflowState 中的 KEY_* 对应）</li>
 * </ul>
 *
 * @author Mahone
 */
public interface NodeIOConstants {

    // =========================================================================
    // 通用输入 key
    // =========================================================================

    /**
     * 用户输入文本
     * <p>同 {@link WorkflowState#KEY_USER_INPUT}，引用以消除重复定义。
     */
    String INPUT_USER_INPUT = WorkflowState.KEY_USER_INPUT;

    /** 用户查询（SQL 相关节点） */
    String INPUT_USER_QUERY = "userQuery";

    /** 系统提示词 */
    String INPUT_SYSTEM_PROMPT = "systemPrompt";

    /** 用户提示词（对话配置面板） */
    String INPUT_USER_PROMPT = "userPrompt";

    /** 查询文本（知识检索节点） */
    String INPUT_QUERY = "query";

    /** 待提取参数的文本（参数提取器节点） */
    String INPUT_INPUT_TEXT = "inputText";

    /** 意图识别指令文本 */
    String INPUT_INSTRUCTION = "instruction";

    /** SQL 语句（SQL 执行节点） */
    String INPUT_SQL = "sql";

    /**
     * 文档 ID（输入）
     * <p>同 {@link WorkflowState#KEY_DOCUMENT_ID}，引用以消除重复定义。
     */
    String INPUT_DOCUMENT_ID = WorkflowState.KEY_DOCUMENT_ID;

    /** 数据集 ID */
    String INPUT_DATASET_ID = "datasetId";

    /** 文件对象（单个） */
    String INPUT_FILE = "file";

    /** 文件列表（多个） */
    String INPUT_FILES = "files";

    /** OSS ID（单个） */
    String INPUT_OSS_ID = "ossId";

    /** OSS ID 列表 */
    String INPUT_OSS_IDS = "ossIds";

    /** 临时文件 ID */
    String INPUT_TEMP_FILE_ID = "tempFileId";

    /** 检索到的文档列表（LLM 引用） */
    String INPUT_RETRIEVED_DOCS = "retrievedDocs";

    /** 聊天上下文（知识库检索结果文本） */
    String INPUT_CHAT_CONTEXT = "chatContext";

    /**
     * 最终回复内容（结束节点输入）
     * <p>同 {@link WorkflowState#KEY_FINAL_RESPONSE}，引用以消除重复定义。
     */
    String INPUT_FINAL_RESPONSE = WorkflowState.KEY_FINAL_RESPONSE;

    /** 固定回复内容 */
    String INPUT_CONTENT = "content";

    /** MCP 资源 URI */
    String INPUT_URI = "uri";

    // =========================================================================
    // 通用输出 key
    // =========================================================================

    /** LLM 回复文本 */
    String OUTPUT_RESPONSE = "response";

    /** 推理内容（thinking） */
    String OUTPUT_REASONING_CONTENT = "reasoningContent";

    /** Token 使用统计 */
    String OUTPUT_TOKEN_USAGE = "tokenUsage";

    /** SQL 生成阶段 Token 统计 */
    String OUTPUT_SQL_GEN_TOKEN_USAGE = "sqlGenTokenUsage";

    /** 答案生成阶段 Token 统计 */
    String OUTPUT_ANSWER_GEN_TOKEN_USAGE = "answerGenTokenUsage";

    /** 检索到的文档列表 */
    String OUTPUT_RETRIEVED_DOCS = "retrievedDocs";

    /** 检索上下文文本 */
    String OUTPUT_CONTEXT = "context";

    /** 检索文档数量 */
    String OUTPUT_DOC_COUNT = "docCount";

    /** 是否有检索结果 */
    String OUTPUT_HAS_RESULTS = "hasResults";

    /** 路由 key（条件/意图/循环节点） */
    String OUTPUT_ROUTE_KEY = "routeKey";

    /** 意图识别结果 */
    String OUTPUT_INTENT = "intent";

    /** 提取的 JSON 字符串（参数提取器） */
    String OUTPUT_EXTRACTED_JSON = "extractedJson";

    /** 生成的 SQL 语句 */
    String OUTPUT_GENERATED_SQL = "generatedSql";

    /** SQL 查询结果（List&lt;Map&gt;） */
    String OUTPUT_QUERY_RESULT = "queryResult";

    /** SQL 查询结果（JSON 字符串） */
    String OUTPUT_STR_RESULT = "strResult";

    /** 查询行数 */
    String OUTPUT_ROW_COUNT = "rowCount";

    /** 工具/技能执行结果（结构化） */
    String OUTPUT_RESULT = "result";

    /** 工具/技能执行结果（纯文本），文件解析后的纯文本复用此 key */
    String OUTPUT_TEXT = "text";

    /** 语音识别转写文本 */
    String OUTPUT_TRANSCRIPTION = "transcription";

    /** OSS ID 输出 */
    String OUTPUT_OSS_ID = "ossId";

    /**
     * 文档 ID（输出）
     * <p>同 {@link WorkflowState#KEY_DOCUMENT_ID}，引用以消除重复定义。
     */
    String OUTPUT_DOCUMENT_ID = WorkflowState.KEY_DOCUMENT_ID;

    /** 分块数量 */
    String OUTPUT_CHUNK_COUNT = "chunkCount";

    /**
     * 最终回复（结束节点输出）
     * <p>同 {@link WorkflowState#KEY_FINAL_RESPONSE}，引用以消除重复定义。
     */
    String OUTPUT_FINAL_RESPONSE = WorkflowState.KEY_FINAL_RESPONSE;

    /** 文件列表输出 */
    String OUTPUT_FILES = "files";

    /** 单文件输出 */
    String OUTPUT_FILE = "file";

    /** 会话变量 Map */
    String OUTPUT_SESSION_VARIABLES = "sessionVariables";

    /** MCP 资源内容（结构化） */
    String OUTPUT_CONTENT = "content";

    /** MCP 资源内容（纯文本） */
    String OUTPUT_TEXT_CONTENT = "textContent";

    /**
     * 用户输入文本（输出，StartNode 专用）
     * <p>同 {@link WorkflowState#KEY_USER_INPUT}，引用以消除重复定义。
     */
    String OUTPUT_USER_INPUT = WorkflowState.KEY_USER_INPUT;

    /**
     * 用户 ID（输出，StartNode 专用）
     * <p>同 {@link WorkflowState#KEY_USER_ID}，引用以消除重复定义。
     */
    String OUTPUT_USER_ID = WorkflowState.KEY_USER_ID;

    // =========================================================================
    // 全局状态 key（context.setGlobalValue / context.getGlobalValue）
    // 与 WorkflowState.KEY_* 重叠的项直接引用，避免重复定义。
    // =========================================================================

    /** 全局：AI 回复文本 */
    String GLOBAL_AI_RESPONSE = "aiResponse";

    /** 全局：检索上下文文本 */
    String GLOBAL_RETRIEVED_CONTEXT = "retrievedContext";

    /** 全局：检索文档列表 */
    String GLOBAL_RETRIEVED_DOCS = "retrievedDocs";

    /** 全局：文件列表 */
    String GLOBAL_FILES = "files";

    /** 全局：MCP 资源内容 */
    String GLOBAL_MCP_RESOURCE_CONTENT = "mcpResourceContent";

    /**
     * 全局：历史上下文列表
     * <p>同 {@link WorkflowState#KEY_HISTORY_CONTEXT}，引用以消除重复定义。
     */
    String GLOBAL_HISTORY_CONTEXT = WorkflowState.KEY_HISTORY_CONTEXT;

    /**
     * 全局：用户名
     * <p>同 {@link WorkflowState#KEY_USER_NAME}，引用以消除重复定义。
     */
    String GLOBAL_USER_NAME = WorkflowState.KEY_USER_NAME;
}
