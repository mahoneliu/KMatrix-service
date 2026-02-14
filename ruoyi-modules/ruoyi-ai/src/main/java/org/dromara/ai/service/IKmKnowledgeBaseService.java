package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmKnowledgeBaseBo;
import org.dromara.ai.domain.vo.KmKnowledgeBaseVo;
import org.dromara.ai.domain.vo.KmStatisticsVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmKnowledgeBaseService {

    /**
     * 查询知识库
     */
    KmKnowledgeBaseVo queryById(Long id);

    /**
     * 查询知识库列表
     */
    TableDataInfo<KmKnowledgeBaseVo> queryPageList(KmKnowledgeBaseBo bo, PageQuery pageQuery);

    /**
     * 查询知识库列表
     */
    List<KmKnowledgeBaseVo> queryList(KmKnowledgeBaseBo bo);

    /**
     * 新增知识库
     */
    Long insertByBo(KmKnowledgeBaseBo bo);

    /**
     * 修改知识库
     */
    Boolean updateByBo(KmKnowledgeBaseBo bo);

    /**
     * 校验并批量删除知识库信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 获取知识库统计信息 (Global)
     */
    KmStatisticsVo getStatistics();

    /**
     * 获取知识库统计信息 (Specific KB)
     */
    KmStatisticsVo getStatistics(Long kbId);
}
