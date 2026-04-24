package org.dromara.ai.execution.tool.service;

import org.dromara.ai.execution.domain.KmBuiltinTool;

import java.util.List;

/**
 * 内置工具服务接口
 *
 * @author KMatrix
 */
public interface BuiltinToolService {

    /**
     * 根据 ID 获取内置工具
     *
     * @param toolId 工具ID
     * @return 工具实体，不存在返回 null
     */
    KmBuiltinTool getById(Long toolId);

    /**
     * 获取所有启用的内置工具
     *
     * @return 启用工具列表
     */
    List<KmBuiltinTool> listActiveTools();

    /**
     * 根据名称获取内置工具
     *
     * @param toolName 工具名称
     * @return 工具实体，不存在返回 null
     */
    KmBuiltinTool getByName(String toolName);

    /**
     * 判断工具是否可用
     *
     * @param toolId 工具ID
     * @return true=可用
     */
    boolean isToolAvailable(Long toolId);
}
