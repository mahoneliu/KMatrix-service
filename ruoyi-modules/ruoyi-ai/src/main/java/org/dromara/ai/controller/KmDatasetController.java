package org.dromara.ai.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.bo.KmDatasetBo;
import org.dromara.ai.domain.vo.KmDatasetVo;
import org.dromara.ai.service.IKmDatasetService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.core.annotation.DemoBlock;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据集管理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/dataset")
public class KmDatasetController extends BaseController {

    private final IKmDatasetService datasetService;

    /**
     * 查询数据集列表
     */
    @GetMapping("/list")
    public TableDataInfo<KmDatasetVo> list(KmDatasetBo bo, PageQuery pageQuery) {
        return datasetService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询知识库下的所有数据集
     */
    @GetMapping("/listByKb/{kbId}")
    public R<List<KmDatasetVo>> listByKb(@NotNull(message = "知识库ID不能为空") @PathVariable Long kbId) {
        return R.ok(datasetService.queryListByKbId(kbId));
    }

    /**
     * 获取数据集详细信息
     */
    @GetMapping("/{id}")
    public R<KmDatasetVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(datasetService.queryById(id));
    }

    /**
     * 新增数据集
     */
    @Log(title = "数据集", businessType = BusinessType.INSERT)
    @PostMapping()
    public R<Long> add(@Validated(AddGroup.class) @RequestBody KmDatasetBo bo) {
        return R.ok(datasetService.insertByBo(bo));
    }

    /**
     * 修改数据集
     */
    @Log(title = "数据集", businessType = BusinessType.UPDATE)
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody KmDatasetBo bo) {
        return toAjax(datasetService.updateByBo(bo));
    }

    /**
     * 删除数据集
     */
    @DemoBlock
    @Log(title = "数据集", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(datasetService.deleteWithValidByIds(List.of(ids), true));
    }
}
