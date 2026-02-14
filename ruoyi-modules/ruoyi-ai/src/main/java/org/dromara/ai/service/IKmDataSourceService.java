package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmDataSourceBo;
import org.dromara.ai.domain.vo.KmDataSourceVo;

import java.util.List;

/**
 * 数据源配置Service接口
 *
 * @author Mahone
 * @date 2026-01-20
 */
public interface IKmDataSourceService {

    /**
     * 查询列表
     */
    List<KmDataSourceVo> queryList(KmDataSourceBo bo);

    /**
     * 根据ID查询
     */
    KmDataSourceVo queryById(Long dataSourceId);

    /**
     * 新增
     */
    Boolean insertByBo(KmDataSourceBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmDataSourceBo bo);

    /**
     * 删除
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 测试数据源连接
     */
    Boolean testConnection(Long dataSourceId);

    /**
     * 获取可用的dynamic-datasource数据源列表
     */
    List<String> getDynamicDataSourceKeys();

}
