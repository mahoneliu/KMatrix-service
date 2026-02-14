package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmWorkflowTemplateBo;
import org.dromara.ai.domain.vo.KmWorkflowTemplateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 工作流模板Service接口
 *
 * @author Mahone
 * @date 2026-01-26
 */
public interface IKmWorkflowTemplateService {

    /**
     * 查询工作流模板
     */
    KmWorkflowTemplateVo queryById(Long templateId);

    /**
     * 查询工作流模板列表
     */
    TableDataInfo<KmWorkflowTemplateVo> queryPageList(KmWorkflowTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询工作流模板列表
     */
    List<KmWorkflowTemplateVo> queryList(KmWorkflowTemplateBo bo);

    /**
     * 新增工作流模板
     */
    Boolean insertByBo(KmWorkflowTemplateBo bo);

    /**
     * 修改工作流模板
     */
    Boolean updateByBo(KmWorkflowTemplateBo bo);

    /**
     * 校验并批量删除工作流模板信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 通过模板创建应用
     * 
     * @param templateId 模板ID
     * @param appName    应用名称
     * @return 新创建的应用ID
     */
    Long createAppFromTemplate(Long templateId, String appName);

    /**
     * 复制模板为自定义模板
     * 
     * @param templateId 源模板ID
     * @param newName    新模板名称
     * @return 新创建的模板ID
     */
    Long copyTemplate(Long templateId, String newName);
}
