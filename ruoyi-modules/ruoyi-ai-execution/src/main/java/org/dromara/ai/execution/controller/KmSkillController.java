package org.dromara.ai.execution.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.execution.domain.bo.KmSkillBo;
import org.dromara.ai.execution.domain.vo.KmSkillVo;
import org.dromara.ai.execution.service.IKmSkillService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 技能管理控制器
 *
 * @author KMatrix
 * @date 2026-03-21
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/skill")
public class KmSkillController extends BaseController {

    private final IKmSkillService skillService;

    /**
     * 查询技能管理列表
     */
    @SaCheckPermission("ai:skill:list")
    @GetMapping("/list")
    public TableDataInfo<KmSkillVo> list(KmSkillBo bo, PageQuery pageQuery) {
        return skillService.queryPageList(bo, pageQuery);
    }
    
    /**
     * 查询所有技能管理列表（不分页）
     */
    @SaCheckPermission("ai:skill:list")
    @GetMapping("/listAll")
    public R<List<KmSkillVo>> listAll(KmSkillBo bo) {
        return R.ok(skillService.queryList(bo));
    }

    /**
     * 获取所有启用的技能列表（公共接口，无需鉴权）
     * 供 Chat 前端直接调用，实现 app 模块与 execution 模块解耦
     */
    @GetMapping("/active")
    public R<List<KmSkillVo>> activeList() {
        return R.ok(skillService.queryActiveList());
    }

    /**
     * 获取技能管理详细信息
     */
    @SaCheckPermission("ai:skill:query")
    @GetMapping("/{skillId}")
    public R<KmSkillVo> getInfo(@PathVariable Long skillId) {
        return R.ok(skillService.queryById(skillId));
    }

    /**
     * 新增技能管理
     */
    @SaCheckPermission("ai:skill:add")
    @Log(title = "技能管理", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody KmSkillBo bo) {
        return toAjax(skillService.insertByBo(bo));
    }

    /**
     * 修改技能管理
     */
    @SaCheckPermission("ai:skill:edit")
    @Log(title = "技能管理", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmSkillBo bo) {
        return toAjax(skillService.updateByBo(bo));
    }

    /**
     * 删除技能管理
     */
    @SaCheckPermission("ai:skill:remove")
    @Log(title = "技能管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{skillIds}")
    public R<Void> remove(@PathVariable List<Long> skillIds) {
        return toAjax(skillService.deleteWithValidByIds(skillIds, true));
    }
}
