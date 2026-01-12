package org.dromara.ai.service;

import org.dromara.ai.domain.KmWorkflowInstance;
import org.dromara.ai.domain.enums.NodeExecutionStatus;
import org.dromara.ai.domain.enums.WorkflowInstanceStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 工作流实例服务接口
 *
 * @author Mahone
 * @date 2026-01-02
 */
public interface IWorkflowInstanceService {

    /**
     * 创建工作流实例
     *
     * @param appId          应用ID
     * @param sessionId      会话ID
     * @param workflowConfig 工作流配置
     * @return 实例ID
     */
    Long createInstance(Long appId, Long sessionId, String workflowConfig);

    /**
     * 更新实例状态
     *
     * @param instanceId 实例ID
     * @param status     状态
     */
    void updateInstanceStatus(Long instanceId, WorkflowInstanceStatus status);

    /**
     * 更新当前执行节点
     *
     * @param instanceId  实例ID
     * @param currentNode 当前节点ID
     */
    void updateCurrentNode(Long instanceId, String currentNode);

    /**
     * 更新全局状态
     *
     * @param instanceId  实例ID
     * @param globalState 全局状态
     */
    void updateGlobalState(Long instanceId, Map<String, Object> globalState);

    /**
     * 创建节点执行记录
     *
     * @param instanceId  实例ID
     * @param nodeId      节点ID
     * @param nodeType    节点类型
     * @param inputParams 输入参数
     * @return 执行ID
     */
    Long createNodeExecution(Long instanceId, String nodeId, String nodeType, Map<String, Object> inputParams);

    /**
     * 更新节点执行状态
     *
     * @param executionId  执行ID
     * @param status       状态
     * @param outputParams 输出参数
     * @param nodeName     节点名称
     * @param durationMs   执行耗时(毫秒)
     */
    void updateNodeExecution(Long executionId, NodeExecutionStatus status, Map<String, Object> outputParams,
            String nodeName, Long durationMs);

    /**
     * 标记实例完成
     *
     * @param instanceId 实例ID
     */
    void completeInstance(Long instanceId);

    /**
     * 标记实例失败
     *
     * @param instanceId   实例ID
     * @param errorMessage 错误信息
     */
    void failInstance(Long instanceId, String errorMessage);

    /**
     * 暂停工作流
     *
     * @param instanceId 实例ID
     */
    void pauseWorkflow(Long instanceId);

    /**
     * 恢复工作流
     *
     * @param instanceId 实例ID
     * @param emitter    SSE推送器
     */
    void resumeWorkflow(Long instanceId, SseEmitter emitter);

    /**
     * 重试失败节点
     *
     * @param instanceId 实例ID
     * @param nodeId     节点ID
     */
    void retryFailedNode(Long instanceId, String nodeId);

    /**
     * 获取实例详情
     *
     * @param instanceId 实例ID
     * @return 实例对象
     */
    KmWorkflowInstance getInstanceDetail(Long instanceId);
}
