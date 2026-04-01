package org.dromara.ai.app.controller;

import org.dromara.common.core.utils.MessageUtils;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.app.domain.bo.KmChatSendBo;
import org.dromara.ai.app.domain.vo.ChatSessionTokenInfo;
import org.dromara.ai.app.domain.vo.KmChatMessageVo;
import org.dromara.ai.app.domain.vo.KmChatSessionVo;
import org.dromara.ai.workflow.domain.vo.KmNodeExecutionVo;
import org.dromara.ai.api.enums.ChatUserType;
import org.dromara.ai.app.service.IChatSessionTokenService;
import org.dromara.ai.app.service.IKmAppTokenService;
import org.dromara.ai.app.service.IKmChatService;
import org.dromara.ai.app.service.IKmSkillService;
import org.dromara.ai.app.domain.bo.KmChatFeedbackBo;
import org.dromara.ai.app.domain.bo.KmSkillBo;
import org.dromara.ai.app.domain.vo.KmSkillVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.service.IKmFileService;
import java.util.HashMap;

import jakarta.validation.Valid;
import java.util.List;

/**
 * AI对话接入Controller (公共/嵌入式)
 * 仅用于通过 Session Token 或 App Token 接入的对话
 *
 * @author Mahone
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/chat")
public class KmChatController extends BaseController {

    private final IKmChatService chatService;
    private final IKmAppTokenService appTokenService;
    private final IChatSessionTokenService chatSessionTokenService;
    private final IKmSkillService skillService;
    private final IKmFileService kmFileService;

    /**
     * 流式对话 (SSE)
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody KmChatSendBo bo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo tokenInfo = validateAndParseToken(authHeader);

        // 设置鉴权后的属性
        if (tokenInfo.getUserType() != null) {
            bo.setUserType(tokenInfo.getUserType().getCode());
        } else {
            bo.setUserType(ChatUserType.ANONYMOUS_USER.getCode());
        }

        if (tokenInfo.getAppId() != null) {
            bo.setAppId(tokenInfo.getAppId());
        }
        bo.setUserId(tokenInfo.getUserId());

        return chatService.streamChat(bo);
    }

    /**
     * 普通对话(非流式)
     */
    @PostMapping("/send")
    public R<KmChatMessageVo> send(@Valid @RequestBody KmChatSendBo bo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        bo.setStream(false);
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);

        bo.setUserId(info.getUserId());
        if (info.getUserType() != null) {
            bo.setUserType(info.getUserType().getCode());
        } else {
            bo.setUserType(ChatUserType.ANONYMOUS_USER.getCode());
        }

        return R.ok(chatService.chat(bo));
    }

    /**
     * 获取历史记录
     */
    @GetMapping("/history/{sessionId}")
    public R<List<KmChatMessageVo>> getHistory(@PathVariable Long sessionId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeExecutions,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return R.ok(chatService.getHistory(sessionId, info.getUserId(), includeExecutions));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions/{appId}")
    public R<List<KmChatSessionVo>> getSessionList(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo tokenInfo = validateAndParseToken(authHeader);
        if (tokenInfo.getUserId() == null) {
            throw new ServiceException("需要有效的 Session Token");
        }
        return R.ok(chatService.getSessionList(appId, tokenInfo.getUserId()));
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/clear/{sessionId}")
    public R<Void> clearHistory(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return toAjax(chatService.clearHistory(sessionId, info.getUserId()));
    }

    /**
     * 获取可用技能列表
     */
    @GetMapping("/skills")
    public R<List<KmSkillVo>> getSkills(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        validateAndParseToken(authHeader); // just validate
        KmSkillBo bo = new KmSkillBo();
        bo.setStatus("0");
        return R.ok(skillService.queryList(bo));
    }

    /**
     * 清除应用下所有会话
     */
    @DeleteMapping("/clear-app/{appId}")
    public R<Void> clearAppHistory(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return toAjax(chatService.clearAppHistory(appId, info.getUserId()));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/session/{sessionId}/title")
    public R<Void> updateSessionTitle(@PathVariable Long sessionId,
            @RequestBody Map<String, String> params,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String title = params.get("title");
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        chatService.updateSessionTitle(sessionId, title, info.getUserId());
        return R.ok();
    }

    /**
     * 提交消息评价 (点赞/踩)
     */
    @PostMapping("/feedback")
    public R<Void> submitFeedback(@Valid @RequestBody KmChatFeedbackBo bo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return toAjax(chatService.submitFeedback(bo.getMessageId(), bo.getFeedbackStatus(), info.getUserId()));
    }

    /**
     * 验证并解析 Token
     */
    private ChatSessionTokenInfo validateAndParseToken(String authHeader) {
        String token = extractToken(authHeader);

        // 1. 如果提供了 Token，尝试解析
        if (StringUtils.isNotBlank(token)) {
            // 尝试 Session Token
            ChatSessionTokenInfo tokenInfo = chatSessionTokenService.validateToken(token);
            if (tokenInfo != null) {
                return tokenInfo;
            }

            // 尝试 App Token
            Long appId = appTokenService.validateToken(token, null);
            if (appId != null) {
                ChatSessionTokenInfo info = new ChatSessionTokenInfo();
                info.setAppId(appId);
                info.setUserType(ChatUserType.ANONYMOUS_USER);
                return info;
            }
        }

        // 2. 如果都没有，或者 Token 无效，尝试检查当前是否为系统登录用户 (Sa-Token 会话)
        // 这样管理端在预览/调试时，共用这些公共接口也能正常工作
        if (LoginHelper.isLogin()) {
            ChatSessionTokenInfo info = new ChatSessionTokenInfo();
            info.setUserId(LoginHelper.getUserId());
            info.setUserType(ChatUserType.SYSTEM_USER);
            return info;
        }

        if (StringUtils.isBlank(token)) {
            throw new ServiceException(MessageUtils.message("ai.msg.auth.token_missing"));
        } else {
            throw new ServiceException(MessageUtils.message("ai.msg.auth.access_denied_token"));
        }
    }

    /**
     * 查询会话的执行详情 (公共端)
     * 调试会话（sessionId < 0）不支持查询
     */
    @GetMapping("/execution/session/{sessionId}")
    public R<List<KmNodeExecutionVo>> getExecutionDetails(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (sessionId < 0) {
            return R.fail("调试会话不保存执行记录");
        }
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return R.ok(chatService.getExecutionDetails(sessionId, info.getUserId()));
    }

    /**
     * 从 Authorization 头提取 Token
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

    /**
     * 中断当前请求
     */
    @PostMapping("/abort")
    public R<Object> abortRequest(@Valid @RequestBody org.dromara.ai.app.domain.bo.AbortRequestBo bo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return R.ok(chatService.abortRequest(bo.getRequestId(), info.getUserId()));
    }

    /**
     * 获取可恢复的会话列表
     */
    @GetMapping("/resumable-sessions/{appId}")
    public R<List<KmChatSessionVo>> getResumableSessions(@PathVariable Long appId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return R.ok(chatService.getResumableSessions(appId, info.getUserId()));
    }

    /**
     * 恢复会话
     */
    @PostMapping("/resume-session/{sessionId}")
    public R<KmChatSessionVo> resumeSession(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return R.ok(chatService.resumeSession(sessionId, info.getUserId()));
    }

    /**
     * 清除会话中断状态
     */
    @PostMapping("/clear-abort-status/{sessionId}")
    public R<Void> clearAbortStatus(@PathVariable Long sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        ChatSessionTokenInfo info = validateAndParseToken(authHeader);
        return toAjax(chatService.clearAbortStatus(sessionId, info.getUserId()));
    }

    /**
     * 上传聊天附件（公共端）
     */
    @PostMapping("/attachment/upload")
    public R<Map<String, Object>> uploadAttachment(@RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (file == null || file.isEmpty()) {
            return R.fail("文件不能为空");
        }
        validateAndParseToken(authHeader); // 确保是合法用户或Token
        KmTempFile tempFile = kmFileService.saveTempFile(null, file);
        Map<String, Object> result = new HashMap<>();
        result.put("id", tempFile.getId());
        result.put("tempFileId", tempFile.getId());
        if (tempFile.getOssId() != null) {
            result.put("ossId", tempFile.getOssId());
        }
        result.put("originalFilename", tempFile.getOriginalFilename());
        result.put("fileExtension", tempFile.getFileExtension());
        result.put("fileSize", tempFile.getFileSize());
        result.put("url", tempFile.getUrl());
        result.put("fileUrl", tempFile.getUrl());
        return R.ok(result);
    }
}
