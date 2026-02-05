package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmModelBo;
import org.dromara.ai.domain.vo.KmModelVo;
import java.util.List;

/**
 * AI模型配置Service接口
 *
 * @author Mahone
 * @date 2024-01-27
 */
public interface IKmModelService {

    /**
     * 查询列表
     */
    List<KmModelVo> queryList(KmModelBo bo);

    /**
     * 根据ID查询
     */
    KmModelVo queryById(Long modelId);

    /**
     * 新增
     */
    Boolean insertByBo(KmModelBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmModelBo bo);

    /**
     * 删除
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 测试连接
     */
    String testConnection(KmModelBo bo);

    /**
     * 复制模型
     *
     * @param modelId 原模型ID
     * @return 新模型ID
     */
    Long copyModel(Long modelId);

    /**
     * 设置系统默认模型
     *
     * @param modelId 模型ID
     * @return 是否成功
     */
    Boolean setDefaultModel(Long modelId);
}
