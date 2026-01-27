package org.dromara.ai.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmAppBo;
import org.dromara.ai.domain.vo.KmAppStatisticsVo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.ai.service.IKmAppService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * AI应用Controller
 *
 * @author Mahone
 * @date 2025-12-27
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/app")
public class KmAppController extends BaseController {

    private final IKmAppService appService;

    /**
     * 查询AI应用列表
     */
    @SaCheckPermission("ai:app:list")
    @GetMapping("/list")
    public TableDataInfo<KmAppVo> list(KmAppBo bo, PageQuery pageQuery) {
        return appService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出AI应用列表
     */
    @SaCheckPermission("ai:app:export")
    @Log(title = "AI应用", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KmAppBo bo, HttpServletResponse response) {
        // List<KmAppVo> list = appService.queryList(bo);
        // ExcelUtil.exportExcel(list, "AI应用", KmAppVo.class, response);
    }

    /**
     * 获取AI应用详细信息
     */
    @SaCheckPermission("ai:app:query")
    @GetMapping("/{appId}")
    public R<KmAppVo> getInfo(@PathVariable Long appId) {
        return R.ok(appService.queryById(appId));
    }

    /**
     * 新增AI应用
     */
    @SaCheckPermission("ai:app:add")
    @Log(title = "AI应用", businessType = BusinessType.INSERT)
    @PostMapping
    public R<String> add(@Validated @RequestBody KmAppBo bo) {
        String appId = appService.insertByBo(bo);
        return R.ok("操作成功", appId);
    }

    /**
     * 修改AI应用
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "AI应用", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody KmAppBo bo) {
        return toAjax(appService.updateByBo(bo));
    }

    /**
     * 删除AI应用
     */
    @SaCheckPermission("ai:app:remove")
    @Log(title = "AI应用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public R<Void> remove(@PathVariable Long[] appIds) {
        return toAjax(appService.deleteWithValidByIds(Arrays.asList(appIds), true));
    }

    /**
     * 发布AI应用
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "AI应用发布", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{appId}")
    public R<Void> publish(@PathVariable Long appId,
            @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        return toAjax(appService.publishApp(appId, remark));
    }

    /**
     * 更新公开访问开关
     */
    @SaCheckPermission("ai:app:edit")
    @Log(title = "更新公开访问", businessType = BusinessType.UPDATE)
    @PatchMapping("/{appId}/public-access")
    public R<Void> updatePublicAccess(@PathVariable Long appId,
            @RequestBody Map<String, String> body) {
        String publicAccess = body.get("publicAccess");
        return toAjax(appService.updatePublicAccess(appId, publicAccess));
    }

    /**
     * 获取应用统计数据
     */
    @GetMapping("/{appId}/statistics")
    public R<KmAppStatisticsVo> getStatistics(@PathVariable Long appId,
            @RequestParam(defaultValue = "7d") String period) {
        return R.ok(appService.getAppStatistics(appId, period));
    }
}
