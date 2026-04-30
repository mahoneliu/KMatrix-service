package org.dromara.ai.workflow.constant;

/**
 * 工作流节点配置项 Key 常量
 * <p>
 * 统一管理所有节点 {@code context.getConfig()} / {@code context.getConfigAsXxx()} 中使用的
 * 配置项字符串 key，避免 hardcode。
 *
 * <h3>命名规范</h3>
 * <ul>
 *   <li>AI 配置面板（require_ai_config）：{@code CFG_AI_*}</li>
 *   <li>对话配置面板（require_dialog_config）：{@code CFG_DIALOG_*}</li>
 *   <li>知识检索配置：{@code CFG_KR_*}</li>
 *   <li>SQL/DB 配置：{@code CFG_SQL_*}</li>
 *   <li>工具/技能配置：{@code CFG_TOOL_*}</li>
 *   <li>文件/数据集配置：{@code CFG_FILE_*}</li>
 *   <li>循环/条件配置：{@code CFG_LOOP_*}</li>
 *   <li>变量聚合配置：{@code CFG_VA_*}</li>
 *   <li>会话变量配置：{@code CFG_SV_*}</li>
 *   <li>MCP 配置：{@code CFG_MCP_*}</li>
 *   <li>系统配置（sys_config）：{@code SYS_CFG_*}</li>
 * </ul>
 *
 * @author Mahone
 */
public interface NodeConfigConstants {

    // =========================================================================
    // AI 配置面板（require_ai_config）
    // =========================================================================

    /** 模型 ID */
    String CFG_AI_MODEL_ID = "modelId";

    /** 温度 */
    String CFG_AI_TEMPERATURE = "temperature";

    /** 最大 Token 数 */
    String CFG_AI_MAX_TOKENS = "maxTokens";

    /** 是否流式输出 */
    String CFG_AI_STREAM_OUTPUT = "streamOutput";

    // =========================================================================
    // 对话配置面板（require_dialog_config）
    // =========================================================================

    /** 系统提示词 */
    String CFG_DIALOG_SYSTEM_PROMPT = "systemPrompt";

    /** 用户提示词 */
    String CFG_DIALOG_USER_PROMPT = "userPrompt";

    /** 是否启用多模态 */
    String CFG_DIALOG_ENABLE_MULTIMODAL = "enableMultimodal";

    /** 是否启用历史对话 */
    String CFG_DIALOG_HISTORY_ENABLED = "historyEnabled";

    /** 历史消息条数上限 */
    String CFG_DIALOG_HISTORY_LIMIT = "historyLimit";

    /** 历史消息 Token 上限 */
    String CFG_DIALOG_HISTORY_MAX_TOKENS = "historyMaxTokens";

    // =========================================================================
    // 知识检索节点（KNOWLEDGE_RETRIEVAL）
    // =========================================================================

    /** 知识库 ID 列表 */
    String CFG_KR_KB_IDS = "kbIds";

    /** 数据集 ID 列表 */
    String CFG_KR_DATASET_IDS = "datasetIds";

    /** 返回 Top-K 数量 */
    String CFG_KR_TOP_K = "topK";

    /** 相似度阈值 */
    String CFG_KR_THRESHOLD = "threshold";

    /** 检索模式（VECTOR / FULLTEXT / HYBRID） */
    String CFG_KR_MODE = "mode";

    /** 是否启用重排序 */
    String CFG_KR_ENABLE_RERANK = "enableRerank";

    /** 空结果时的预设回复 */
    String CFG_KR_EMPTY_RESPONSE = "emptyResponse";

    /** 默认检索模式 */
    String CFG_KR_DEFAULT_MODE = "VECTOR";

    // =========================================================================
    // SQL / DB 节点（SQL_GENERATE / SQL_EXECUTE / DB_QUERY）
    // =========================================================================

    /** 数据源 ID */
    String CFG_SQL_DATA_SOURCE_ID = "dataSourceId";

    /** 最大返回行数 */
    String CFG_SQL_MAX_ROWS = "maxRows";

    /** 表白名单（逗号分隔） */
    String CFG_SQL_TABLE_WHITELIST = "tableWhitelist";

    /** 表黑名单（逗号分隔） */
    String CFG_SQL_TABLE_BLACKLIST = "tableBlacklist";

    // =========================================================================
    // 工具节点（TOOL）
    // =========================================================================

    /** 工具配置对象（包含 type 和 id） */
    String CFG_TOOL_TOOL = "tool";

    /** 工具类型：内置工具 */
    String CFG_TOOL_TYPE_BUILTIN = "builtin";

    /** 工具类型：MCP 工具 */
    String CFG_TOOL_TYPE_MCP = "mcp";

    /** 工具类型：技能 */
    String CFG_TOOL_TYPE_SKILL = "skill";

    // =========================================================================
    // 技能节点（SKILL）
    // =========================================================================

    /** 技能 ID */
    String CFG_SKILL_ID = "skillId";

    // =========================================================================
    // LLM 对话节点（LLM_CHAT）工具相关配置
    // =========================================================================

    /** 内置工具 ID 列表 */
    String CFG_LLM_BUILTIN_TOOL_IDS = "builtinToolIds";

    /** MCP Server ID 列表 */
    String CFG_LLM_MCP_SERVER_IDS = "mcpServerIds";

    /** 技能 ID 列表 */
    String CFG_LLM_SKILL_IDS = "skillIds";

    /** 旧版工具配置列表（兼容） */
    String CFG_LLM_TOOLS = "tools";

    /** 是否启用工具调用追踪 */
    String CFG_LLM_ENABLE_TOOL_TRACE = "enableToolTrace";

    // =========================================================================
    // 结束节点（END）
    // =========================================================================

    /** 自定义回复内容 */
    String CFG_END_CUSTOM_RESPONSE = "customResponse";

    // =========================================================================
    // 条件节点（CONDITION）
    // =========================================================================

    /** 分支列表 */
    String CFG_CONDITION_BRANCHES = "branches";

    /** 旧版条件列表（兼容） */
    String CFG_CONDITION_CONDITIONS = "conditions";

    // =========================================================================
    // 循环节点（LOOP）
    // =========================================================================

    /** 最大迭代次数 */
    String CFG_LOOP_MAX_ITERATIONS = "maxIterations";

    /** 循环条件配置 */
    String CFG_LOOP_CONDITION = "condition";

    // =========================================================================
    // 变量聚合器节点（VARIABLE_AGGREGATOR）
    // =========================================================================

    /** 是否启用分组聚合 */
    String CFG_VA_ENABLE_GROUPING = "enableGrouping";

    /** 变量引用列表（单组模式） */
    String CFG_VA_VARIABLES = "variables";

    /** 分组列表（分组模式） */
    String CFG_VA_GROUPS = "groups";

    /** 输出 key 名称 */
    String CFG_VA_OUTPUT_KEY = "outputKey";

    /** 默认输出 key */
    String CFG_VA_DEFAULT_OUTPUT_KEY = "output";

    // =========================================================================
    // 会话变量赋值节点（SESSION_VARIABLE_ASSIGN）
    // =========================================================================

    /** 赋值配置列表 */
    String CFG_SV_ASSIGNMENTS = "assignments";

    // =========================================================================
    // 参数提取器节点（PARAMETER_EXTRACTOR）
    // =========================================================================

    /** 参数定义列表 */
    String CFG_PE_PARAMETERS = "parameters";

    /** 提取指令 */
    String CFG_PE_EXTRACTION_INSTRUCTIONS = "extractionInstructions";

    // =========================================================================
    // 意图识别节点（INTENT_CLASSIFIER）
    // =========================================================================

    /** 意图列表 */
    String CFG_IC_INTENTS = "intents";

    // =========================================================================
    // 文件解析节点（FILE_PARSE）
    // =========================================================================

    /** 解析类型 */
    String CFG_FILE_PROCESS_TYPE = "processType";

    // =========================================================================
    // 文件/数据集存储节点（FILE_STORAGE / DATASET_STORAGE）
    // =========================================================================

    /** 数据集 ID */
    String CFG_FILE_DATASET_ID = "datasetId";

    /** 分块大小 */
    String CFG_FILE_CHUNK_SIZE = "chunkSize";

    /** 分块重叠大小 */
    String CFG_FILE_OVERLAP = "overlap";

    // =========================================================================
    // MCP 资源节点（MCP_RESOURCE）
    // =========================================================================

    /** MCP Server ID */
    String CFG_MCP_SERVER_ID = "serverId";

    /** 资源 URI */
    String CFG_MCP_URI = "uri";

    // =========================================================================
    // 系统配置（sys_config key）
    // =========================================================================

    /** 默认数据集 ID 的系统配置 key */
    String SYS_CFG_DEFAULT_DATASET_ID = "ai.workflow.default_dataset_id";
}
