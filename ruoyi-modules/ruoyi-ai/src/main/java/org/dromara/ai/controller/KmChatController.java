package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.dromara.ai.service.IKmChatService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI对话Controller
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/chat")
public class KmChatController extends BaseController {

    private final IKmChatService chatService;
    private final org.dromara.ai.service.IKmAppTokenService appTokenService;

    /**
     * 流式对话 (SSE)
     * 支持正常对话和调试模式
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody KmChatSendBo bo, HttpServletResponse response,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        // 如果没有常规登录，检查是否提供了 App Token
        if (!StpUtil.isLogin()) {
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token != null) {
                // 验证 Token 并获取 AppId
                Long appId = appTokenService.validateToken(token, null);
                if (appId != null) {
                    // Token 有效，强制设置 appId 并标记为 Token 认证模式
                    bo.setAppId(appId);
                    // 这里我们暂时借用 bo 的一个属性或者通过内部逻辑处理，
                    // 核心是让 chatService 知道这是一个免登录请求
                } else {
                    throw new org.dromara.common.core.exception.ServiceException("无效的访问 Token");
                }
            } else {
                throw new org.dromara.common.core.exception.ServiceException("未登录且未提供访问 Token");
            }
        } else {
            // 调试模式需要额外权限校验
            if (Boolean.TRUE.equals(bo.getDebug())) {
                StpUtil.checkPermission("ai:app:edit");
            }
        }

        return chatService.streamChat(bo);
    }

    /**
     * 普通对话(非流式)
     */
    @SaCheckPermission("ai:chat:send")
    @Log(title = "AI对话", businessType = BusinessType.OTHER)
    @PostMapping("/send")
    public R<String> send(@Valid @RequestBody KmChatSendBo bo) {
        bo.setStream(false);
        return R.ok(chatService.chat(bo));
    }

    @GetMapping("/history/{sessionId}")
    public R<List<KmChatMessageVo>> getHistory(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StpUtil.isLogin()) {
            validateAppToken(authHeader);
        }
        return R.ok(chatService.getHistory(sessionId));
    }

    @GetMapping("/sessions/{appId}")
    public R<List<KmChatSessionVo>> getSessionList(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StpUtil.isLogin()) {
            validateAppToken(authHeader);
        }
        return R.ok(chatService.getSessionList(appId));
    }

    /**
     * 内部校验 Token
     */
    private void validateAppToken(String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || appTokenService.validateToken(token, null) == null) {
            throw new org.dromara.common.core.exception.ServiceException("访问受限：请登录或提供有效的 Token");
        }
    }

    /**
     * 清除会话历史
     */
    @SaCheckPermission("ai:chat:clear")
    @Log(title = "清除对话历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/clear/{sessionId}")
    public R<Void> clearHistory(@PathVariable Long sessionId) {
        return toAjax(chatService.clearHistory(sessionId));
    }

    /**
     * 清除应用下所有会话
     */
    @SaCheckPermission("ai:chat:clear")
    @Log(title = "清除应用对话", businessType = BusinessType.DELETE)
    @DeleteMapping("/clear-app/{appId}")
    public R<Void> clearAppHistory(@PathVariable Long appId) {
        return toAjax(chatService.clearAppHistory(appId));
    }

    /**
     * 更新会话标题
     */
    @SaCheckPermission("ai:chat:edit")
    @Log(title = "更新会话标题", businessType = BusinessType.UPDATE)
    @PutMapping("/session/{sessionId}/title")
    public R<Void> updateSessionTitle(@PathVariable Long sessionId, @RequestBody java.util.Map<String, String> body) {
        String title = body.get("title");
        if (org.dromara.common.core.utils.StringUtils.isBlank(title)) {
            return R.fail("标题不能为空");
        }
        return toAjax(chatService.updateSessionTitle(sessionId, title));
    }

    /**
     * 查询会话的执行详情
     * 调试会话（sessionId < 0）不支持查询，因为调试数据不保存到数据库
     */
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/execution/session/{sessionId}")
    public R<List<org.dromara.ai.domain.vo.KmNodeExecutionVo>> getExecutionDetails(@PathVariable Long sessionId) {
        // 调试会话（sessionId < 0）不支持查询
        if (sessionId < 0) {
            return R.fail("调试会话不保存执行记录");
        }

        return R.ok(chatService.getExecutionDetails(sessionId));
    }
}
