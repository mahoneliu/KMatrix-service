package org.dromara.ai.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmChatSession;
import org.dromara.ai.app.mapper.KmChatSessionMapper;
import org.dromara.ai.workflow.workflow.nodes.session.ISessionVariableProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 会话变量提供者实现
 * <p>
 * 实现 workflow 模块定义的 {@link ISessionVariableProvider} 接口，
 * 通过 KmChatSessionMapper 读写 km_chat_session.session_variables 字段。
 *
 * @author Mahone
 * @date 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionVariableProviderImpl implements ISessionVariableProvider {

    private final KmChatSessionMapper chatSessionMapper;

    /**
     * 加载指定会话的所有会话变量
     */
    @Override
    public Map<String, Object> loadSessionVariables(Long sessionId) {
        if (sessionId == null) {
            return new HashMap<>();
        }
        try {
            KmChatSession session = chatSessionMapper.selectOne(
                    new LambdaQueryWrapper<KmChatSession>()
                            .eq(KmChatSession::getSessionId, sessionId)
                            .select(KmChatSession::getSessionVariables)
            );
            if (session == null || session.getSessionVariables() == null) {
                return new HashMap<>();
            }
            return new HashMap<>(session.getSessionVariables());
        } catch (Exception e) {
            log.error("加载会话变量失败，sessionId={}", sessionId, e);
            return new HashMap<>();
        }
    }

    /**
     * 保存（覆盖）指定会话的所有会话变量
     */
    @Override
    public void saveSessionVariables(Long sessionId, Map<String, Object> sessionVariables) {
        if (sessionId == null) {
            log.warn("saveSessionVariables：sessionId 为空，跳过保存");
            return;
        }
        try {
            Map<String, Object> toSave = sessionVariables != null ? sessionVariables : new HashMap<>();
            chatSessionMapper.update(null,
                    new LambdaUpdateWrapper<KmChatSession>()
                            .eq(KmChatSession::getSessionId, sessionId)
                            .set(KmChatSession::getSessionVariables, toSave)
            );
            log.debug("会话变量已保存，sessionId={}, 变量数={}", sessionId, toSave.size());
        } catch (Exception e) {
            log.error("保存会话变量失败，sessionId={}", sessionId, e);
        }
    }
}
