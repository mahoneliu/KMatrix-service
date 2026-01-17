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

    /**
     * 流式对话 (SSE)
     * 支持正常对话和调试模式
     */
    @SaCheckPermission("ai:chat:send")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody KmChatSendBo bo, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        // 调试模式需要额外权限校验
        if (Boolean.TRUE.equals(bo.getDebug())) {
            StpUtil.checkPermission("ai:app:edit");
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

    /**
     * 获取会话历史消息
     */
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/history/{sessionId}")
    public R<List<KmChatMessageVo>> getHistory(@PathVariable Long sessionId) {
        return R.ok(chatService.getHistory(sessionId));
    }

    /**
     * 获取应用下的会话列表
     */
    @SaCheckPermission("ai:chat:history")
    @GetMapping("/sessions/{appId}")
    public R<List<KmChatSessionVo>> getSessionList(@PathVariable Long appId) {
        return R.ok(chatService.getSessionList(appId));
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
