package org.dromara.ai.workflow.workflow.nodes.session;

import java.util.Map;

/**
 * 会话变量提供者接口
 * <p>
 * 定义在 workflow 模块，由 app 模块实现，通过 Spring 依赖注入解耦。
 * 用于在工作流节点中读写 km_chat_session 表的 session_variables 字段。
 *
 * @author Mahone
 * @date 2026-05-01
 */
public interface ISessionVariableProvider {

    /**
     * 加载指定会话的所有会话变量
     *
     * @param sessionId 会话 ID
     * @return 会话变量 Map（key=变量名, value=变量值），不存在时返回空 Map
     */
    Map<String, Object> loadSessionVariables(Long sessionId);

    /**
     * 保存（覆盖）指定会话的所有会话变量
     *
     * @param sessionId        会话 ID
     * @param sessionVariables 要保存的会话变量 Map
     */
    void saveSessionVariables(Long sessionId, Map<String, Object> sessionVariables);
}
