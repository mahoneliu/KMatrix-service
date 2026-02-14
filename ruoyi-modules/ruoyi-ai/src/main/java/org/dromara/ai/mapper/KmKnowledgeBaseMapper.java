package org.dromara.ai.mapper;

import org.dromara.ai.domain.KmKnowledgeBase;
import org.dromara.ai.domain.vo.KmKnowledgeBaseVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 知识库Mapper接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface KmKnowledgeBaseMapper extends BaseMapperPlus<KmKnowledgeBase, KmKnowledgeBaseVo> {

    @org.apache.ibatis.annotations.Select("SELECT k.*, " +
            "(SELECT COUNT(1) FROM km_dataset d WHERE d.kb_id = k.id AND d.del_flag = '0') AS dataset_count, " +
            "(SELECT COUNT(1) FROM km_document doc WHERE doc.kb_id = k.id AND doc.del_flag = '0') AS document_count " +
            "FROM km_knowledge_base k ${ew.customSqlSegment}")
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<KmKnowledgeBaseVo> selectVoPageWithStats(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<KmKnowledgeBase> page,
            @org.apache.ibatis.annotations.Param(com.baomidou.mybatisplus.core.toolkit.Constants.WRAPPER) com.baomidou.mybatisplus.core.conditions.Wrapper<KmKnowledgeBase> queryWrapper);
}
