package org.dromara.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.ChatSessionTokenInfo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.dromara.ai.domain.vo.KmNodeExecutionVo;
import org.dromara.ai.service.IChatSessionTokenService;
import org.dromara.ai.service.IKmAppTokenService;
import org.dromara.ai.service.IKmChatService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

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
    private final IKmAppTokenService appTokenService;
    private final IChatSessionTokenService chatSessionTokenService;

    /**
     * 流式对话 (SSE)
     * 支持正常对话和调试模式
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody KmChatSendBo bo, HttpServletResponse response,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        // 如果没有常规登录，检查是否提供了 Session Token 或 App Token
        if (!StpUtil.isLogin()) {
            ChatSessionTokenInfo tokenInfo = parseSessionToken(authHeader);
            if (tokenInfo != null) {
                // Session Token 有效，设置 appId 和 userId
                bo.setAppId(tokenInfo.getAppId());
                bo.setUserId(tokenInfo.getUserId());
            } else {
                // 尝试验证 App Token (向后兼容)
                String token = extractToken(authHeader);
                if (token != null) {
                    Long appId = appTokenService.validateToken(token, null);
                    if (appId != null) {
                        bo.setAppId(appId);
                    } else {
                        throw new ServiceException("无效的访问 Token");
                    }
                } else {
                    throw new ServiceException("未登录且未提供访问 Token");
                }
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
    @PostMapping("/send")
    public R<String> send(@Valid @RequestBody KmChatSendBo bo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        bo.setStream(false);
        // 设置用户ID (登录或匿名)
        bo.setUserId(getUserIdFromContext(authHeader));
        return R.ok(chatService.chat(bo));
    }

    @GetMapping("/history/{sessionId}")
    public R<List<KmChatMessageVo>> getHistory(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId;
        if (StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        } else {
            ChatSessionTokenInfo info = validateAndParseToken(authHeader);
            userId = info.getUserId();
        }
        return R.ok(chatService.getHistory(sessionId, userId));
    }

    @GetMapping("/sessions/{appId}")
    public R<List<KmChatSessionVo>> getSessionList(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId;
        if (StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        } else {
            // 尝试解析 Session Token 获取匿名用户 userId
            ChatSessionTokenInfo tokenInfo = validateAndParseToken(authHeader);
            userId = tokenInfo.getUserId();
            if (userId == null) {
                // 如果是 App Token，没有 userId，无法查询会话列表(除了免登录模式下的)
                // 但这里我们简单处理：必须要有一个 userId
                throw new ServiceException("需要有效的 Session Token");
            }
        }
        return R.ok(chatService.getSessionList(appId, userId));
    }

    /**
     * 从 Authorization 头提取 Token
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 解析 Session Token（不抛异常）
     */
    private ChatSessionTokenInfo parseSessionToken(String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            return null;
        }
        return chatSessionTokenService.validateToken(token);
    }

    /**
     * 验证并解析 Token（失败抛异常）
     */
    private ChatSessionTokenInfo validateAndParseToken(String authHeader) {
        String token = extractToken(authHeader);
        if (token == null) {
            throw new ServiceException("未提供访问 Token");
        }

        // 优先尝试 Session Token
        ChatSessionTokenInfo tokenInfo = chatSessionTokenService.validateToken(token);
        if (tokenInfo != null) {
            return tokenInfo;
        }

        // 降级尝试 App Token
        Long appId = appTokenService.validateToken(token, null);
        if (appId != null) {
            ChatSessionTokenInfo info = new ChatSessionTokenInfo();
            info.setAppId(appId);
            return info;
        }

        throw new ServiceException("访问受限：无效的 Token");
    }

    /**
     * 内部校验 Token（向后兼容方法）
     */
    private void validateToken(String authHeader) {
        validateAndParseToken(authHeader);
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/clear/{sessionId}")
    public R<Void> clearHistory(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromContext(authHeader);
        // 如果是登录用户，为了兼容旧的前端逻辑，这里不校验 checkPermission，而是依赖 service 层的 ownership 校验
        // 如果需要管理员强删功能，则需要另外的接口或者判断 role
        return toAjax(chatService.clearHistory(sessionId, userId));
    }

    /**
     * 清除应用下所有会话
     */
    @DeleteMapping("/clear-app/{appId}")
    public R<Void> clearAppHistory(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserIdFromContext(authHeader);
        return toAjax(chatService.clearAppHistory(appId, userId));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/session/{sessionId}/title")
    public R<Void> updateSessionTitle(@PathVariable Long sessionId, @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String title = body.get("title");
        if (StringUtils.isBlank(title)) {
            return R.fail("标题不能为空");
        }
        Long userId = getUserIdFromContext(authHeader);
        return toAjax(chatService.updateSessionTitle(sessionId, title, userId));
    }

    /**
     * 查询会话的执行详情
     * 调试会话（sessionId < 0）不支持查询，因为调试数据不保存到数据库
     */
    @GetMapping("/execution/session/{sessionId}")
    public R<List<KmNodeExecutionVo>> getExecutionDetails(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 调试会话（sessionId < 0）不支持查询
        if (sessionId < 0) {
            return R.fail("调试会话不保存执行记录");
        }
        Long userId = getUserIdFromContext(authHeader);
        return R.ok(chatService.getExecutionDetails(sessionId, userId));
    }

    /**
     * 获取当前操作用户ID (登录用户或匿名用户)
     */
    private Long getUserIdFromContext(String authHeader) {
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsLong();
        }
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        if (info.getUserId() == null) {
            throw new ServiceException("无权限：非法的用户标识");
        }
        return info.getUserId();
    }
}
