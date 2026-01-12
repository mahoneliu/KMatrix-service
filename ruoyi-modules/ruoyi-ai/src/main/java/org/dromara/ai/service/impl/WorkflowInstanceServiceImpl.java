package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmNodeExecution;
import org.dromara.ai.domain.KmWorkflowInstance;
import org.dromara.ai.domain.enums.NodeExecutionStatus;
import org.dromara.ai.domain.enums.WorkflowInstanceStatus;
import org.dromara.ai.mapper.KmNodeExecutionMapper;
import org.dromara.ai.mapper.KmWorkflowInstanceMapper;
import org.dromara.ai.service.IWorkflowInstanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.Map;

/**
 * 工作流实例服务实现
 *
 * @author Mahone
 * @date 2026-01-02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WorkflowInstanceServiceImpl implements IWorkflowInstanceService {

    private final KmWorkflowInstanceMapper instanceMapper;
    private final KmNodeExecutionMapper executionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInstance(Long appId, Long sessionId, String workflowConfig) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setAppId(appId);
        instance.setSessionId(sessionId);
        instance.setWorkflowConfig(workflowConfig);
        instance.setStatus(WorkflowInstanceStatus.RUNNING);
        instance.setStartTime(new Date());

        instanceMapper.insert(instance);
        log.info("创建工作流实例: instanceId={}, appId={}", instance.getInstanceId(), appId);
        return instance.getInstanceId();
    }

    @Override
    public void updateInstanceStatus(Long instanceId, WorkflowInstanceStatus status) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setInstanceId(instanceId);
        instance.setStatus(status);
        instanceMapper.updateById(instance);
        log.debug("更新实例状态: instanceId={}, status={}", instanceId, status);
    }

    @Override
    public void updateCurrentNode(Long instanceId, String currentNode) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setInstanceId(instanceId);
        instance.setCurrentNode(currentNode);
        instanceMapper.updateById(instance);
        log.debug("更新当前节点: instanceId={}, currentNode={}", instanceId, currentNode);
    }

    @Override
    public void updateGlobalState(Long instanceId, Map<String, Object> globalState) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setInstanceId(instanceId);
        instance.setGlobalState(globalState);
        instanceMapper.updateById(instance);
        log.debug("更新全局状态: instanceId={}", instanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNodeExecution(Long instanceId, String nodeId, String nodeType, Map<String, Object> inputParams) {
        KmNodeExecution execution = new KmNodeExecution();
        execution.setInstanceId(instanceId);
        execution.setNodeId(nodeId);
        execution.setNodeType(nodeType);
        execution.setStatus(NodeExecutionStatus.RUNNING);
        execution.setInputParams(inputParams);
        execution.setStartTime(new Date());
        execution.setRetryCount(0);

        executionMapper.insert(execution);
        log.debug("创建节点执行记录: executionId={}, nodeId={}", execution.getExecutionId(), nodeId);
        return execution.getExecutionId();
    }

    @Override
    public void updateNodeExecution(Long executionId, NodeExecutionStatus status, Map<String, Object> outputParams,
            String nodeName, Long durationMs) {
        KmNodeExecution execution = new KmNodeExecution();
        execution.setExecutionId(executionId);
        execution.setStatus(status);
        execution.setOutputParams(outputParams);
        execution.setNodeName(nodeName);
        execution.setDurationMs(durationMs);
        execution.setEndTime(new Date());

        executionMapper.updateById(execution);
        log.debug("更新节点执行记录: executionId={}, status={}", executionId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeInstance(Long instanceId) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setInstanceId(instanceId);
        instance.setStatus(WorkflowInstanceStatus.COMPLETED);
        instance.setEndTime(new Date());
        instanceMapper.updateById(instance);
        log.info("工作流实例完成: instanceId={}", instanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failInstance(Long instanceId, String errorMessage) {
        KmWorkflowInstance instance = new KmWorkflowInstance();
        instance.setInstanceId(instanceId);
        instance.setStatus(WorkflowInstanceStatus.FAILED);
        instance.setErrorMessage(errorMessage);
        instance.setEndTime(new Date());
        instanceMapper.updateById(instance);
        log.error("工作流实例失败: instanceId={}, error={}", instanceId, errorMessage);
    }

    @Override
    public void pauseWorkflow(Long instanceId) {
        updateInstanceStatus(instanceId, WorkflowInstanceStatus.PAUSED);
        log.info("暂停工作流: instanceId={}", instanceId);
    }

    @Override
    public void resumeWorkflow(Long instanceId, SseEmitter emitter) {
        // TODO: 实现恢复逻辑
        updateInstanceStatus(instanceId, WorkflowInstanceStatus.RUNNING);
        log.info("恢复工作流: instanceId={}", instanceId);
    }

    @Override
    public void retryFailedNode(Long instanceId, String nodeId) {
        // 查找失败的节点执行记录
        LambdaQueryWrapper<KmNodeExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmNodeExecution::getInstanceId, instanceId);
        wrapper.eq(KmNodeExecution::getNodeId, nodeId);
        wrapper.eq(KmNodeExecution::getStatus, NodeExecutionStatus.FAILED);
        wrapper.orderByDesc(KmNodeExecution::getStartTime);
        wrapper.last("LIMIT 1");

        KmNodeExecution execution = executionMapper.selectOne(wrapper);
        if (execution != null) {
            // 增加重试次数
            execution.setRetryCount(execution.getRetryCount() + 1);
            execution.setStatus(NodeExecutionStatus.PENDING);
            execution.setErrorMessage(null);
            executionMapper.updateById(execution);
            log.info("重试失败节点: instanceId={}, nodeId={}, retryCount={}",
                    instanceId, nodeId, execution.getRetryCount());
        }
    }

    @Override
    public KmWorkflowInstance getInstanceDetail(Long instanceId) {
        return instanceMapper.selectById(instanceId);
    }
}
