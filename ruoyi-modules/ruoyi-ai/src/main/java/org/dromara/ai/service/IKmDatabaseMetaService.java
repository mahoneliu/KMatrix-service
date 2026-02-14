package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmDatabaseMetaBo;
import org.dromara.ai.domain.vo.KmDatabaseMetaVo;

import java.util.List;

/**
 * 数据库元数据Service接口
 *
 * @author Mahone
 * @date 2026-01-20
 */
public interface IKmDatabaseMetaService {

    /**
     * 查询列表
     */
    List<KmDatabaseMetaVo> queryList(KmDatabaseMetaBo bo);

    /**
     * 根据ID查询
     */
    KmDatabaseMetaVo queryById(Long metaId);

    /**
     * 根据数据源ID查询所有元数据
     */
    List<KmDatabaseMetaVo> queryByDataSourceId(Long dataSourceId);

    /**
     * 新增
     */
    Boolean insertByBo(KmDatabaseMetaBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmDatabaseMetaBo bo);

    /**
     * 删除
     */
    Boolean deleteByIds(List<Long> ids);

    /**
     * 解析DDL语句并保存元数据
     *
     * @param dataSourceId 数据源ID
     * @param ddlContent   DDL内容
     * @return 解析后的元数据列表
     */
    List<KmDatabaseMetaVo> parseDdlAndSave(Long dataSourceId, String ddlContent);

    /**
     * 从数据库同步元数据
     *
     * @param dataSourceId 数据源ID
     * @param tableNames   要同步的表名列表(为空则同步所有)
     * @return 同步后的元数据列表
     */
    List<KmDatabaseMetaVo> syncFromDatabase(Long dataSourceId, List<String> tableNames);

}
