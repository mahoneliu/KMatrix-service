package org.dromara.ai.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.ai.domain.KmNodeDefinition;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 工作流节点定义Mapper接口
 *
 * @author Mahone
 * @date 2026-01-07
 */
public interface KmNodeDefinitionMapper extends BaseMapperPlus<KmNodeDefinition, KmNodeDefinitionVo> {

    /**
     * 分页查询节点定义列表
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件
     * @return 包含节点定义信息的分页结果
     */
    Page<KmNodeDefinitionVo> selectPageNodeDefinitionList(
            @Param("page") Page<KmNodeDefinition> page,
            @Param(Constants.WRAPPER) Wrapper<KmNodeDefinition> queryWrapper);

    /**
     * 根据条件查询节点定义列表
     *
     * @param queryWrapper 查询条件
     * @return 节点定义列表
     */
    List<KmNodeDefinitionVo> selectNodeDefinitionList(
            @Param(Constants.WRAPPER) Wrapper<KmNodeDefinition> queryWrapper);

    /**
     * 根据ID查询节点定义
     *
     * @param nodeDefId 节点定义ID
     * @return 节点定义VO
     */
    KmNodeDefinitionVo selectNodeDefinitionById(Long nodeDefId);
}
