package org.dromara.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.ChatSessionTokenInfo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.enums.ChatUserType;
import org.dromara.ai.service.IChatSessionTokenService;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.workflow.WorkflowExecutor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 工作流执行接入Controller (公共/嵌入式)
 * 仅支持通过 Token (Session Token) 授权的执行
 *
 * @author Mahone
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/workflow")
public class KmWorkflowController extends BaseController {

    private final IKmAppService appService;
    private final WorkflowExecutor workflowExecutor;
    private final IChatSessionTokenService chatSessionTokenService;

    /**
     * 执行工作流
     */
    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(@RequestBody Map<String, Object> params,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // 鉴权：仅支持 Token 接入
        ChatSessionTokenInfo tokenInfo = validateAndParseToken(authHeader);

        // 构建 SseEmitter
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            // 提取参数
            Long appId = params.get("appId") != null ? Long.valueOf(params.get("appId").toString()) : null;
            String message = (String) params.get("message");

            if (appId == null) {
                throw new RuntimeException("AppID 不能为空");
            }

            // 构造 ChatSendBo
            KmChatSendBo chatSendBo = new KmChatSendBo();
            chatSendBo.setAppId(appId);
            chatSendBo.setMessage(message);
            chatSendBo.setUserId(tokenInfo.getUserId());
            if (tokenInfo.getUserType() != null) {
                chatSendBo.setUserType(tokenInfo.getUserType().getCode());
            }

            // 获取 App 信息
            KmAppVo appVo = appService.queryById(appId);
            if (appVo == null) {
                throw new ServiceException("应用不存在");
            }

            // 执行
            new Thread(() -> {
                try {
                    workflowExecutor.executeWorkflowDebug(appVo, -1L, chatSendBo, emitter, tokenInfo.getUserId());
                    emitter.complete();
                } catch (Exception e) {
                    log.error("公共端工作流执行异常", e);
                    emitter.completeWithError(e);
                }
            }).start();

        } catch (Exception e) {
            log.error("初始化公共端工作流失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 验证并解析 Token
     */
    private ChatSessionTokenInfo validateAndParseToken(String authHeader) {
        String token = extractToken(authHeader);

        // 1. 如果提供了 Token，尝试解析
        if (StringUtils.isNotBlank(token)) {
            ChatSessionTokenInfo tokenInfo = chatSessionTokenService.validateToken(token);
            if (tokenInfo != null) {
                return tokenInfo;
            }
        }

        // 2. 回退检查：是否为系统登录用户
        if (LoginHelper.isLogin()) {
            ChatSessionTokenInfo info = new ChatSessionTokenInfo();
            info.setUserId(LoginHelper.getUserId());
            info.setUserType(ChatUserType.SYSTEM_USER);
            return info;
        }

        if (StringUtils.isBlank(token)) {
            throw new ServiceException("访问受限：未提供 Token");
        } else {
            throw new ServiceException("访问受限：无效的 Token");
        }
    }

    /**
     * 提取 Token
     */
    private String extractToken(String authHeader) {
        if (StringUtils.isBlank(authHeader)) {
            return null;
        }
        String token = authHeader.trim();
        if (token.equalsIgnoreCase("Bearer")) {
            return null;
        }
        if (StringUtils.startsWithIgnoreCase(token, "Bearer ")) {
            String value = token.substring(7).trim();
            return StringUtils.isNotBlank(value) ? value : null;
        }
        return token;
    }
}
