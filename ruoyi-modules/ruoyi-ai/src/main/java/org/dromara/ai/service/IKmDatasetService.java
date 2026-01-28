package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmDatasetBo;
import org.dromara.ai.domain.vo.KmDatasetVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 数据集Service接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmDatasetService {

    /**
     * 查询数据集
     */
    KmDatasetVo queryById(Long id);

    /**
     * 查询数据集列表 (按知识库)
     */
    TableDataInfo<KmDatasetVo> queryPageList(KmDatasetBo bo, PageQuery pageQuery);

    /**
     * 查询数据集列表 (按知识库)
     */
    List<KmDatasetVo> queryListByKbId(Long kbId);

    /**
     * 新增数据集
     */
    Long insertByBo(KmDatasetBo bo);

    /**
     * 修改数据集
     */
    Boolean updateByBo(KmDatasetBo bo);

    /**
     * 校验并批量删除数据集信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
