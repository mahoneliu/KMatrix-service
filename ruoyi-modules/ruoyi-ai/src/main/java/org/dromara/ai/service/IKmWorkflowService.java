package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmWorkflowBo;
import org.dromara.ai.domain.vo.KmWorkflowVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 工作流定义Service接口
 *
 * @author Mahone
 * @date 2025-12-27
 */
public interface IKmWorkflowService {

    /**
     * 查询工作流定义
     */
    KmWorkflowVo queryById(Long flowId);

    /**
     * 查询工作流定义列表
     */
    TableDataInfo<KmWorkflowVo> queryPageList(KmWorkflowBo bo, PageQuery pageQuery);

    /**
     * 查询工作流定义列表
     */
    List<KmWorkflowVo> queryList(KmWorkflowBo bo);

    /**
     * 新增工作流定义
     */
    Boolean insertByBo(KmWorkflowBo bo);

    /**
     * 修改工作流定义
     */
    Boolean updateByBo(KmWorkflowBo bo);

    /**
     * 校验并批量删除工作流定义信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
