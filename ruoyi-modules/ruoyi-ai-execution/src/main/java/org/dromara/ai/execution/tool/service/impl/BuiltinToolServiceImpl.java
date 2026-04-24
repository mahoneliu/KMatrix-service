package org.dromara.ai.execution.tool.service.impl;

import org.dromara.ai.execution.domain.KmBuiltinTool;
import org.dromara.ai.execution.mapper.KmBuiltinToolMapper;
import org.dromara.ai.execution.tool.service.BuiltinToolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内置工具服务实现
 *
 * @author KMatrix
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BuiltinToolServiceImpl implements BuiltinToolService {

    private final KmBuiltinToolMapper builtinToolMapper;

    @Override
    public KmBuiltinTool getById(Long toolId) {
        return builtinToolMapper.selectById(toolId);
    }

    @Override
    public List<KmBuiltinTool> listActiveTools() {
        LambdaQueryWrapper<KmBuiltinTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmBuiltinTool::getStatus, "0");
        return builtinToolMapper.selectList(wrapper);
    }

    @Override
    public KmBuiltinTool getByName(String toolName) {
        LambdaQueryWrapper<KmBuiltinTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KmBuiltinTool::getToolName, toolName);
        wrapper.eq(KmBuiltinTool::getStatus, "0");
        wrapper.last("LIMIT 1");
        return builtinToolMapper.selectOne(wrapper);
    }

    @Override
    public boolean isToolAvailable(Long toolId) {
        KmBuiltinTool tool = builtinToolMapper.selectById(toolId);
        return tool != null && "0".equals(tool.getStatus());
    }
}
