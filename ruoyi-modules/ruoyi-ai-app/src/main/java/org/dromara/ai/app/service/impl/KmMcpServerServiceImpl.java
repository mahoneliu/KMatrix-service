package org.dromara.ai.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmMcpServer;
import org.dromara.ai.app.domain.bo.KmMcpServerBo;
import org.dromara.ai.app.domain.vo.KmMcpServerVo;
import org.dromara.ai.app.mapper.KmMcpServerMapper;
import org.dromara.ai.app.service.IKmMcpServerService;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Server 配置 Service 业务层处理
 *
 * @author Mahone
 * @date 2026-03-15
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmMcpServerServiceImpl implements IKmMcpServerService {

    private final KmMcpServerMapper baseMapper;

    @Override
    public List<KmMcpServerVo> queryList(KmMcpServerBo bo) {
        LambdaQueryWrapper<KmMcpServer> lqw = Wrappers.lambdaQuery();
        lqw.like(StrUtil.isNotBlank(bo.getServerName()), KmMcpServer::getServerName, bo.getServerName());
        lqw.eq(StrUtil.isNotBlank(bo.getTransportType()), KmMcpServer::getTransportType, bo.getTransportType());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), KmMcpServer::getStatus, bo.getStatus());
        lqw.eq(KmMcpServer::getDelFlag, "0");
        lqw.orderByDesc(KmMcpServer::getCreateTime);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public KmMcpServerVo queryById(Long serverId) {
        return baseMapper.selectVoById(serverId);
    }

    @Override
    public Boolean insertByBo(KmMcpServerBo bo) {
        // Normalize blank serverConfig to null to avoid invalid JSON error in PostgreSQL
        if (StrUtil.isBlank(bo.getServerConfig())) {
            bo.setServerConfig(null);
        }
        KmMcpServer add = MapstructUtils.convert(bo, KmMcpServer.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(KmMcpServerBo bo) {
        // Normalize blank serverConfig to null to avoid invalid JSON error in PostgreSQL
        if (StrUtil.isBlank(bo.getServerConfig())) {
            bo.setServerConfig(null);
        }
        KmMcpServer update = MapstructUtils.convert(bo, KmMcpServer.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

}
