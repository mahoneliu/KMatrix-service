package org.dromara.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmApp;
import org.dromara.ai.domain.KmAppToken;
import org.dromara.ai.domain.bo.KmAppTokenBo;
import org.dromara.ai.domain.vo.KmAppTokenVo;
import org.dromara.ai.mapper.KmAppMapper;
import org.dromara.ai.mapper.KmAppTokenMapper;
import org.dromara.ai.service.IKmAppTokenService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * App嵌入Token Service实现
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KmAppTokenServiceImpl implements IKmAppTokenService {

    private final KmAppTokenMapper baseMapper;
    private final KmAppMapper appMapper;

    @Override
    public KmAppTokenVo queryByToken(String token) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<KmAppToken>()
                .eq(KmAppToken::getToken, token)
                .eq(KmAppToken::getDelFlag, "0"));
    }

    @Override
    public List<KmAppTokenVo> queryByAppId(Long appId) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<KmAppToken>()
                .eq(KmAppToken::getAppId, appId)
                .eq(KmAppToken::getDelFlag, "0")
                .orderByDesc(KmAppToken::getCreateTime));
    }

    @Override
    public KmAppTokenVo queryById(Long tokenId) {
        return baseMapper.selectVoById(tokenId);
    }

    @Override
    public KmAppTokenVo generateToken(KmAppTokenBo bo) {
        KmAppToken token = MapstructUtils.convert(bo, KmAppToken.class);
        // 生成唯一Token
        token.setToken(IdUtil.fastSimpleUUID().substring(0, 32));
        token.setStatus("1"); // 默认启用
        token.setDelFlag("0");

        baseMapper.insert(token);
        return baseMapper.selectVoById(token.getTokenId());
    }

    @Override
    public Boolean updateToken(KmAppTokenBo bo) {
        KmAppToken token = MapstructUtils.convert(bo, KmAppToken.class);
        return baseMapper.updateById(token) > 0;
    }

    @Override
    public Boolean deleteToken(Long tokenId) {
        KmAppToken token = new KmAppToken();
        token.setTokenId(tokenId);
        token.setDelFlag("1");
        return baseMapper.updateById(token) > 0;
    }

    @Override
    public Long validateToken(String token, String origin) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        KmAppTokenVo tokenVo = queryByToken(token);
        if (tokenVo == null) {
            log.warn("App Token不存在: {}", token);
            return null;
        }

        // 检查状态
        if (!"1".equals(tokenVo.getStatus())) {
            log.warn("App Token已停用: {}", token);
            return null;
        }

        // 检查公开访问开关
        KmApp app = appMapper.selectById(tokenVo.getAppId());
        if (app == null) {
            log.warn("App Token关联的应用不存在: appId={}", tokenVo.getAppId());
            return null;
        }
        if (!"1".equals(app.getPublicAccess())) {
            log.warn("应用未开启公开访问，App Token认证被拒绝: appId={}", tokenVo.getAppId());
            return null;
        }

        // 检查过期时间
        if (tokenVo.getExpiresAt() != null && tokenVo.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("App Token已过期: {}", token);
            return null;
        }

        // 检查来源域名
        String allowedOrigins = tokenVo.getAllowedOrigins();
        if (StringUtils.isNotBlank(allowedOrigins) && !"*".equals(allowedOrigins)) {
            if (StringUtils.isBlank(origin)) {
                log.warn("App Token域名校验失败: 缺少Origin");
                return null;
            }
            String[] origins = allowedOrigins.split(",");
            boolean matched = false;
            for (String o : origins) {
                if (origin.contains(o.trim())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                log.warn("App Token域名校验失败: {} 不在白名单 {}", origin, allowedOrigins);
                return null;
            }
        }

        return tokenVo.getAppId();
    }

    @Override
    public KmAppTokenVo refreshToken(Long tokenId) {
        KmAppToken token = baseMapper.selectById(tokenId);
        if (token == null || "1".equals(token.getDelFlag())) {
            log.warn("Token不存在或已删除: {}", tokenId);
            return null;
        }
        // 重新生成Token值
        token.setToken(IdUtil.fastSimpleUUID().substring(0, 32));
        baseMapper.updateById(token);
        return baseMapper.selectVoById(tokenId);
    }

    @Override
    public KmAppTokenVo createDefaultToken(Long appId, String appName) {
        KmAppToken token = new KmAppToken();
        token.setAppId(appId);
        token.setToken(IdUtil.fastSimpleUUID().substring(0, 32));
        token.setTokenName(appName + "-默认Token");
        token.setAllowedOrigins("*");
        token.setExpiresAt(null); // 永久有效
        token.setStatus("1");
        token.setDelFlag("0");

        baseMapper.insert(token);
        return baseMapper.selectVoById(token.getTokenId());
    }
}
