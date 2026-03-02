package org.dromara.ai.controller;

import org.dromara.common.core.utils.MessageUtils;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.workflow.WorkflowExecutor;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 工作流管理端Controller
 *
 * @author Mahone
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/admin/workflow")
public class KmAdminWorkflowController extends BaseController {

    private final WorkflowExecutor workflowExecutor;
    private final IKmAppService appService;

    /**
     * 执行工作流 (调试模式)
     */
    @SaCheckPermission("ai:app:edit")
    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(@RequestBody Map<String, Object> params) {
        // 构建 SseEmitter
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            // 提取参数
            Long appId = params.get("appId") != null ? Long.valueOf(params.get("appId").toString()) : null;
            String message = (String) params.get("message");
            String dslData = (String) params.get("dslData");

            if (appId == null && dslData == null) {
                throw new RuntimeException(MessageUtils.message("ai.msg.app.id_or_dsl_required"));
            }

            // 构造 ChatSendBo
            KmChatSendBo chatSendBo = new KmChatSendBo();
            chatSendBo.setAppId(appId);
            chatSendBo.setMessage(message);

            // 获取 App 信息
            KmAppVo appVo;
            if (appId != null) {
                appVo = appService.queryById(appId);
                if (dslData != null) {
                    appVo.setDslData(dslData);
                }
            } else {
                appVo = new KmAppVo();
                appVo.setAppId(-1L);
                appVo.setAppName("Admin Debug Workflow");
                appVo.setDslData(dslData);
            }

            // 获取当前登录用户ID
            Long userId = LoginHelper.getUserId();

            // 异步执行
            new Thread(() -> {
                try {
                    workflowExecutor.executeWorkflowDebug(appVo, -1L, chatSendBo, emitter, userId);
                    emitter.complete();
                } catch (Exception e) {
                    log.error("管理端工作流执行异常", e);
                    emitter.completeWithError(e);
                }
            }).start();

        } catch (Exception e) {
            log.error("初始化管理端工作流测试失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
