package org.dromara.ai.mapper;

import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.vo.KmDatasetVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 数据集Mapper接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface KmDatasetMapper extends BaseMapperPlus<KmDataset, KmDatasetVo> {

    /**
     * 查询数据集列表（包含统计信息）
     *
     * @param lqw 查询条件
     * @return 数据集列表
     */
    @org.apache.ibatis.annotations.Select("SELECT d.*, " +
            "(SELECT COUNT(1) FROM km_document doc WHERE doc.dataset_id = d.id AND doc.del_flag = '0') AS document_count "
            +
            "FROM km_dataset d ${ew.customSqlSegment}")
    java.util.List<KmDatasetVo> selectVoListWithStats(
            @org.apache.ibatis.annotations.Param(com.baomidou.mybatisplus.core.toolkit.Constants.WRAPPER) com.baomidou.mybatisplus.core.conditions.Wrapper<KmDataset> queryWrapper);
}
