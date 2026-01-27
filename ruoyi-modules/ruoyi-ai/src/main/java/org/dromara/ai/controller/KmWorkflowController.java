package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.bo.KmChatSendBo;
import org.dromara.ai.domain.bo.KmWorkflowTemplateBo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.domain.vo.KmWorkflowTemplateVo;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.service.IKmWorkflowTemplateService;
import org.dromara.ai.workflow.WorkflowExecutor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.Map;

/**
 * 工作流管理Controller
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/workflow")
public class KmWorkflowController extends BaseController {

    private final IKmWorkflowTemplateService workflowTemplateService;
    private final WorkflowExecutor workflowExecutor;
    private final IKmAppService appService;

    /**
     * 查询工作流模板列表
     */
    @SaCheckPermission("ai:workflow:list")
    @GetMapping("/template/list")
    public TableDataInfo<KmWorkflowTemplateVo> list(KmWorkflowTemplateBo bo, PageQuery pageQuery) {
        return workflowTemplateService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取工作流模板详细信息
     */
    @SaCheckPermission("ai:workflow:query")
    @GetMapping("/template/{templateId}")
    public R<KmWorkflowTemplateVo> getInfo(@PathVariable Long templateId) {
        return R.ok(workflowTemplateService.queryById(templateId));
    }

    /**
     * 新增工作流模板
     */
    @SaCheckPermission("ai:workflow:add")
    @Log(title = "工作流模板", businessType = BusinessType.INSERT)
    @PostMapping("/template")
    public R<Void> add(@Validated @RequestBody KmWorkflowTemplateBo bo) {
        return toAjax(workflowTemplateService.insertByBo(bo));
    }

    /**
     * 修改工作流模板
     */
    @SaCheckPermission("ai:workflow:edit")
    @Log(title = "工作流模板", businessType = BusinessType.UPDATE)
    @PutMapping("/template")
    public R<Void> edit(@Validated @RequestBody KmWorkflowTemplateBo bo) {
        return toAjax(workflowTemplateService.updateByBo(bo));
    }

    /**
     * 删除工作流模板
     */
    @SaCheckPermission("ai:workflow:remove")
    @Log(title = "工作流模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/template/{templateIds}")
    public R<Void> remove(@PathVariable Long[] templateIds) {
        return toAjax(workflowTemplateService.deleteWithValidByIds(Arrays.asList(templateIds), true));
    }

    /**
     * 执行工作流 (调试模式)
     * 用于在通过应用ID执行前，直接使用 DSL JSON测试
     */
    @PostMapping(value = "/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        // 构建 SseEmitter
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        try {
            // 提取参数
            Long appId = params.get("appId") != null ? Long.valueOf(params.get("appId").toString()) : null;
            String message = (String) params.get("message");
            // Map<String, Object> inputs = (Map<String, Object>) params.get("inputs");
            String dslData = (String) params.get("dslData"); // 调试时传递 DSL

            if (appId == null && dslData == null) {
                throw new RuntimeException("AppID 或 DSL 数据不能为空");
            }

            // 构造 ChatSendBo
            KmChatSendBo chatSendBo = new KmChatSendBo();
            chatSendBo.setAppId(appId);
            chatSendBo.setMessage(message);
            // chatSendBo.setInputs(inputs); // ChatSendBo 需要支持 inputs，如果没有字段需添加或通过其他方式传

            // 获取 App 信息
            KmAppVo appVo;
            if (appId != null) {
                appVo = appService.queryById(appId);
                // 如果传入了新的 DSL (调试模式)，覆盖 App 中的 DSL
                if (dslData != null) {
                    appVo.setDslData(dslData);
                }
            } else {
                // 纯 DSL 模式构造虚拟 AppVo
                appVo = new KmAppVo();
                appVo.setAppId(-1L);
                appVo.setAppName("Debug Workflow");
                appVo.setDslData(dslData);
            }

            // 执行
            // 异步执行以支持 SSE
            new Thread(() -> {
                try {
                    workflowExecutor.executeWorkflowDebug(appVo, -1L, chatSendBo, emitter, StpUtil.getLoginIdAsLong());
                    emitter.complete();
                } catch (Exception e) {
                    log.error("工作流执行异常", e);
                    emitter.completeWithError(e);
                }
            }).start();

        } catch (Exception e) {
            log.error("初始化工作流执行失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
