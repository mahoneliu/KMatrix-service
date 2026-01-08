package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmNodeDefinitionBo;
import org.dromara.ai.domain.vo.KmNodeDefinitionVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

/**
 * 工作流节点定义服务接口
 *
 * @author Mahone
 * @date 2026-01-07
 */
public interface IKmNodeDefinitionService {

    /**
     * 获取所有节点类型定义
     *
     * @return 节点类型定义列表
     */
    List<KmNodeDefinitionVo> getNodeDefinitions();

    // ========== 节点定义管理 CRUD 方法 ==========

    /**
     * 分页查询节点定义列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    TableDataInfo<KmNodeDefinitionVo> queryPageList(
            KmNodeDefinitionBo kmNodeDefinitionBo, PageQuery pageQuery);

    /**
     * 根据ID查询节点定义详情
     *
     * @param nodeDefId 节点定义ID
     * @return 节点定义信息
     */
    KmNodeDefinitionVo getNodeDefinitionById(Long nodeDefId);

    /**
     * 新增节点定义
     *
     * @param bo 节点定义业务对象
     * @return 节点定义ID
     */
    Long addNodeDefinition(KmNodeDefinitionBo bo);

    /**
     * 复制节点定义
     *
     * @param nodeDefId   源节点定义ID
     * @param newNodeType 新节点类型
     * @return 新节点定义ID
     */
    Long copyNodeDefinition(Long nodeDefId, String newNodeType);

    /**
     * 更新节点定义
     *
     * @param bo 节点定义业务对象
     */
    void updateNodeDefinition(KmNodeDefinitionBo bo);

    /**
     * 删除节点定义
     *
     * @param nodeDefId 节点定义ID
     */
    void deleteNodeDefinition(Long nodeDefId);

    /**
     * 批量删除节点定义
     *
     * @param nodeDefIds 节点定义ID数组
     */
    void deleteNodeDefinitions(Long[] nodeDefIds);
}
