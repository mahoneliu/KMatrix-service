package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmMcpServerBo;
import org.dromara.ai.domain.vo.KmMcpServerVo;

import java.util.List;

/**
 * MCP Server 配置 Service 接口
 *
 * @author Mahone
 * @date 2026-03-15
 */
public interface IKmMcpServerService {

    /**
     * 查询列表
     */
    List<KmMcpServerVo> queryList(KmMcpServerBo bo);

    /**
     * 根据ID查询
     */
    KmMcpServerVo queryById(Long serverId);

    /**
     * 新增
     */
    Boolean insertByBo(KmMcpServerBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmMcpServerBo bo);

    /**
     * 删除（逻辑删除）
     */
    Boolean deleteByIds(List<Long> ids);

}
