package org.dromara.ai.workflow.constant;

/**
 * 工作流节点类型常量
 * <p>
 * 对应各节点 {@code @Component} 注解值及 {@code getNodeType()} 返回值。
 * 前端节点类型标识与此保持一致。
 *
 * @author Mahone
 */
public interface NodeTypeConstants {

    /** 开始节点 */
    String START = "START";

    /** 结束节点 */
    String END = "END";

    /** LLM 对话节点 */
    String LLM_CHAT = "LLM_CHAT";

    /** 条件判断节点 */
    String CONDITION = "CONDITION";

    /** 循环节点 */
    String LOOP = "LOOP";

    /** 知识检索节点 */
    String KNOWLEDGE_RETRIEVAL = "KNOWLEDGE_RETRIEVAL";

    /** 固定回复节点 */
    String FIXED_RESPONSE = "FIXED_RESPONSE";

    /** 意图识别节点 */
    String INTENT_CLASSIFIER = "INTENT_CLASSIFIER";

    /** 参数提取器节点 */
    String PARAMETER_EXTRACTOR = "PARAMETER_EXTRACTOR";

    /** 变量聚合器节点 */
    String VARIABLE_AGGREGATOR = "VARIABLE_AGGREGATOR";

    /** 会话变量赋值节点 */
    String SESSION_VARIABLE_ASSIGN = "SESSION_VARIABLE_ASSIGN";

    /** SQL 生成节点 */
    String SQL_GENERATE = "SQL_GENERATE";

    /** SQL 执行节点 */
    String SQL_EXECUTE = "SQL_EXECUTE";

    /** 数据库查询节点（NL2SQL 一体化） */
    String DB_QUERY = "DB_QUERY";

    /** 工具节点 */
    String TOOL = "TOOL";

    /** 技能节点 */
    String SKILL = "SKILL";

    /** 文件解析节点 */
    String FILE_PARSE = "FILE_PARSE";

    /** 文档存储节点 */
    String FILE_STORAGE = "FILE_STORAGE";

    /** 数据集存储节点 */
    String DATASET_STORAGE = "DATASET_STORAGE";

    /** 语音识别节点 */
    String AUDIO_ASR = "AUDIO_ASR";

    /** 图像 OCR 节点 */
    String IMAGE_OCR = "IMAGE_OCR";

    /** MCP 资源读取节点 */
    String MCP_RESOURCE = "MCP_RESOURCE";
}
