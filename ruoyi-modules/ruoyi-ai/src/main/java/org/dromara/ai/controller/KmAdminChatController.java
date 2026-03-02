package org.dromara.ai.controller;

import org.dromara.common.core.utils.MessageUtils;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmChatMessageVo;
import org.dromara.ai.domain.vo.KmChatSessionVo;
import org.dromara.ai.domain.vo.KmNodeExecutionVo;
import org.dromara.ai.enums.ChatUserType;
import org.dromara.ai.service.IKmChatService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI对话管理端Controller
 * 专门用于管理后台内部测试与对话，受系统标准鉴权保护
 *
 * @author Mahone
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/admin/chat")
public class KmAdminChatController extends BaseController {

    private final IKmChatService chatService;

    /**
     * 流式对话 (SSE)
     */
    @SaCheckPermission("ai:app:edit")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody KmChatSendBo bo) {
        // 管理端始终使用当前登录用户ID
        bo.setUserId(LoginHelper.getUserId());
        bo.setUserType(ChatUserType.SYSTEM_USER.getCode());
        return chatService.streamChat(bo);
    }

    /**
     * 普通对话(非流式)
     */
    @SaCheckPermission("ai:app:edit")
    @PostMapping("/send")
    public R<String> send(@Valid @RequestBody KmChatSendBo bo) {
        bo.setStream(false);
        bo.setUserId(LoginHelper.getUserId());
        bo.setUserType(ChatUserType.SYSTEM_USER.getCode());
        return R.ok(chatService.chat(bo));
    }

    /**
     * 获取历史消息
     */
    @SaCheckPermission("ai:app:edit")
    @GetMapping("/history/{sessionId}")
    public R<List<KmChatMessageVo>> getHistory(@PathVariable Long sessionId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeExecutions) {
        return R.ok(chatService.getHistory(sessionId, LoginHelper.getUserId(), includeExecutions));
    }

    /**
     * 获取会话列表
     */
    @SaCheckPermission("ai:app:edit")
    @GetMapping("/sessions/{appId}")
    public R<List<KmChatSessionVo>> getSessionList(@PathVariable Long appId) {
        return R.ok(chatService.getSessionList(appId, LoginHelper.getUserId()));
    }

    /**
     * 清除会话历史
     */
    @SaCheckPermission("ai:app:edit")
    @DeleteMapping("/clear/{sessionId}")
    public R<Void> clearHistory(@PathVariable Long sessionId) {
        return toAjax(chatService.clearHistory(sessionId, LoginHelper.getUserId()));
    }

    /**
     * 清除应用下所有会话
     */
    @SaCheckPermission("ai:app:edit")
    @DeleteMapping("/clear-app/{appId}")
    public R<Void> clearAppHistory(@PathVariable Long appId) {
        return toAjax(chatService.clearAppHistory(appId, LoginHelper.getUserId()));
    }

    /**
     * 更新会话标题
     */
    @SaCheckPermission("ai:app:edit")
    @PutMapping("/session/{sessionId}/title")
    public R<Void> updateSessionTitle(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (StringUtils.isBlank(title)) {
            return R.fail(MessageUtils.message("ai.val.common.title_required"));
        }
        return toAjax(chatService.updateSessionTitle(sessionId, title, LoginHelper.getUserId()));
    }

    /**
     * 查询会话的执行详情
     */
    @SaCheckPermission("ai:app:edit")
    @GetMapping("/execution/session/{sessionId}")
    public R<List<KmNodeExecutionVo>> getExecutionDetails(@PathVariable Long sessionId) {
        if (sessionId < 0) {
            return R.fail("调试会话不保存执行记录");
        }
        return R.ok(chatService.getExecutionDetails(sessionId, LoginHelper.getUserId()));
    }
}
