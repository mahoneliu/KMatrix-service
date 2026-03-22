package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmBuiltinTool;
import org.dromara.ai.domain.bo.KmBuiltinToolBo;
import org.dromara.ai.domain.vo.KmBuiltinToolVo;
import org.dromara.ai.mapper.KmBuiltinToolMapper;
import org.dromara.ai.service.IKmBuiltinToolService;
import org.dromara.ai.workflow.nodes.tool.ToolJsonSchemaUtils;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内置 Python 工具 Service 业务层处理
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmBuiltinToolServiceImpl implements IKmBuiltinToolService {

    private final KmBuiltinToolMapper baseMapper;

    @Override
    public List<KmBuiltinToolVo> queryList(KmBuiltinToolBo bo) {
        LambdaQueryWrapper<KmBuiltinTool> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getToolName()), KmBuiltinTool::getToolName, bo.getToolName());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), KmBuiltinTool::getStatus, bo.getStatus());
        lqw.eq(KmBuiltinTool::getDelFlag, "0");
        lqw.orderByDesc(KmBuiltinTool::getCreateTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public KmBuiltinToolVo queryById(Long toolId) {
        return baseMapper.selectVoById(toolId);
    }

    @Override
    public Boolean insertByBo(KmBuiltinToolBo bo) {
        KmBuiltinTool add = MapstructUtils.convert(bo, KmBuiltinTool.class);
        // 如果用户没有提供 inputSchema，则根据 initParams 自动生成（JSON Schema for LLM）
        if (StrUtil.isBlank(bo.getInputSchema()) && StrUtil.isNotBlank(bo.getInitParams())) {
            add.setInputSchema(ToolJsonSchemaUtils.generateInputSchema(bo.getInitParams()));
            log.info("新增内置工具 [{}]，inputSchema 已根据 initParams 自动生成", bo.getToolName());
        }
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmBuiltinToolBo bo) {
        KmBuiltinTool update = MapstructUtils.convert(bo, KmBuiltinTool.class);
        // 如果用户没有提供 inputSchema，且 initParams 有内容，则尝试自动生成
        if (StrUtil.isBlank(bo.getInputSchema()) && StrUtil.isNotBlank(bo.getInitParams())) {
            update.setInputSchema(ToolJsonSchemaUtils.generateInputSchema(bo.getInitParams()));
            log.info("更新内置工具 [{}]，inputSchema 已根据 initParams 重新生成", bo.getToolName());
        }
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

}
