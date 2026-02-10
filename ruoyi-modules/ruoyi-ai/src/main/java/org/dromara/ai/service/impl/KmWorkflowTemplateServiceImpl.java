package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmWorkflowTemplate;
import org.dromara.ai.domain.bo.KmAppBo;
import org.dromara.ai.domain.bo.KmWorkflowTemplateBo;
import org.dromara.ai.domain.vo.KmWorkflowTemplateVo;
import org.dromara.ai.domain.vo.config.AppWorkflowConfig;
import org.dromara.ai.mapper.KmWorkflowTemplateMapper;
import org.dromara.ai.service.IKmAppService;
import org.dromara.ai.service.IKmWorkflowTemplateService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 工作流模板Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmWorkflowTemplateServiceImpl implements IKmWorkflowTemplateService {

    private final KmWorkflowTemplateMapper baseMapper;
    @Lazy
    private final IKmAppService appService;

    /**
     * 查询工作流模板
     */
    @Override
    public KmWorkflowTemplateVo queryById(Long templateId) {
        return baseMapper.selectVoById(templateId);
    }

    /**
     * 查询工作流模板列表
     */
    @Override
    public TableDataInfo<KmWorkflowTemplateVo> queryPageList(KmWorkflowTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = buildQueryWrapper(bo);
        Page<KmWorkflowTemplateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询工作流模板列表
     */
    @Override
    public List<KmWorkflowTemplateVo> queryList(KmWorkflowTemplateBo bo) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmWorkflowTemplate> buildQueryWrapper(KmWorkflowTemplateBo bo) {
        LambdaQueryWrapper<KmWorkflowTemplate> lqw = new LambdaQueryWrapper<>();
        lqw.like(StringUtils.isNotBlank(bo.getTemplateName()), KmWorkflowTemplate::getTemplateName,
                bo.getTemplateName());
        lqw.eq(StringUtils.isNotBlank(bo.getTemplateCode()), KmWorkflowTemplate::getTemplateCode, bo.getTemplateCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), KmWorkflowTemplate::getCategory, bo.getCategory());
        lqw.eq(StringUtils.isNotBlank(bo.getScopeType()), KmWorkflowTemplate::getScopeType, bo.getScopeType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsEnabled()), KmWorkflowTemplate::getIsEnabled, bo.getIsEnabled());
        return lqw;
    }

    /**
     * 新增工作流模板
     */
    @Override
    public Boolean insertByBo(KmWorkflowTemplateBo bo) {
        KmWorkflowTemplate add = MapstructUtils.convert(bo, KmWorkflowTemplate.class);

        // 如果 workflow_config 为空,设置默认值
        if (StringUtils.isBlank(add.getWorkflowConfig())) {
            add.setWorkflowConfig("{}");
        }

        // 如果 scopeType 为空,默认设置为用户级别
        if (StringUtils.isBlank(add.getScopeType())) {
            add.setScopeType("1");
        }

        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setTemplateId(add.getTemplateId());
        }
        return flag;
    }

    /**
     * 修改工作流模板
     */
    @Override
    public Boolean updateByBo(KmWorkflowTemplateBo bo) {
        KmWorkflowTemplate update = MapstructUtils.convert(bo, KmWorkflowTemplate.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KmWorkflowTemplate entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除工作流模板信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 通过模板创建应用
     */
    @Override
    public Long createAppFromTemplate(Long templateId, String appName) {
        // 1. 查询模板
        KmWorkflowTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }

        // 2. 构建应用 Bo 对象
        KmAppBo appBo = new KmAppBo();
        appBo.setAppName(appName);
        appBo.setDescription("基于模板【" + template.getTemplateName() + "】创建");
        appBo.setIcon(template.getIcon());
        // 根据模板类型设置 app_type：
        // 系统模板(scopeType=0) -> 固定模板类型(app_type=1)
        // 自定义模板(scopeType=1) -> 自定义工作流类型(app_type=2)
        if ("0".equals(template.getScopeType())) {
            appBo.setAppType("1"); // 固定模板
        } else {
            appBo.setAppType("2"); // 自定义工作流
        }
        appBo.setStatus("0"); // 草稿状态
        appBo.setGraphData(template.getGraphData());
        appBo.setDslData(template.getDslData()); // 拷贝 DSL 数据

        // 2.1 记录模版来源
        appBo.setSourceTemplateId(templateId);
        appBo.setSourceTemplateScope(template.getScopeType()); // 0=系统, 1=自建

        // 3. 解析 workflowConfig JSON
        if (StringUtils.isNotBlank(template.getWorkflowConfig())) {
            try {
                AppWorkflowConfig workflowConfig = JsonUtils.parseObject(template.getWorkflowConfig(),
                        AppWorkflowConfig.class);
                appBo.setWorkflowConfig(workflowConfig);
            } catch (Exception e) {
                log.warn("解析模板 workflowConfig 失败: {}", e.getMessage());
            }
        }

        // 4. 调用 AppService 创建应用（复用逻辑，会自动创建 Token）
        String appIdStr = appService.insertByBo(appBo);

        // 5. 更新模板使用次数
        template.setUseCount(template.getUseCount() != null ? template.getUseCount() + 1 : 1);
        baseMapper.updateById(template);

        return Long.valueOf(appIdStr);
    }

    /**
     * 复制模板为自定义模板
     */
    @Override
    public Long copyTemplate(Long templateId, String newName) {
        // 1. 查询源模板
        KmWorkflowTemplate source = baseMapper.selectById(templateId);
        if (source == null) {
            throw new ServiceException("源模板不存在");
        }

        // 2. 创建新模板
        KmWorkflowTemplate newTemplate = new KmWorkflowTemplate();
        newTemplate.setTemplateName(newName);
        newTemplate.setTemplateCode(source.getTemplateCode() + "_copy_" + System.currentTimeMillis());
        newTemplate.setDescription("复制自：" + source.getTemplateName());
        newTemplate.setIcon(source.getIcon());
        newTemplate.setCategory(source.getCategory());
        newTemplate.setScopeType("1"); // 强制设置为自定义模板
        newTemplate.setWorkflowConfig(source.getWorkflowConfig());
        newTemplate.setGraphData(source.getGraphData());
        newTemplate.setDslData(source.getDslData()); // 拷贝 DSL 数据
        newTemplate.setVersion(1);
        newTemplate.setIsPublished("0");
        newTemplate.setIsEnabled("1");
        newTemplate.setUseCount(0);

        // 3. 插入新模板
        boolean flag = baseMapper.insert(newTemplate) > 0;
        if (!flag) {
            throw new ServiceException("复制模板失败");
        }

        return newTemplate.getTemplateId();
    }
}
