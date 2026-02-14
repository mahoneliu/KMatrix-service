package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmAppTokenBo;
import org.dromara.ai.domain.vo.KmAppTokenVo;

import java.util.List;

/**
 * App嵌入Token Service接口
 *
 * @author Mahone
 * @date 2026-01-26
 */
public interface IKmAppTokenService {

    /**
     * 根据Token值查询
     */
    KmAppTokenVo queryByToken(String token);

    /**
     * 根据应用ID查询Token列表
     */
    List<KmAppTokenVo> queryByAppId(Long appId);

    /**
     * 根据ID查询
     */
    KmAppTokenVo queryById(Long tokenId);

    /**
     * 生成新Token
     */
    KmAppTokenVo generateToken(KmAppTokenBo bo);

    /**
     * 更新Token
     */
    Boolean updateToken(KmAppTokenBo bo);

    /**
     * 删除Token
     */
    Boolean deleteToken(Long tokenId);

    /**
     * 验证Token有效性
     *
     * @param token  Token值
     * @param origin 请求来源域名
     * @return 验证通过返回关联的appId，失败返回null
     */
    Long validateToken(String token, String origin);

    /**
     * 刷新Token（重新生成token值）
     *
     * @param tokenId Token ID
     * @return 新的Token信息
     */
    KmAppTokenVo refreshToken(Long tokenId);

    /**
     * 为应用创建默认Token
     *
     * @param appId   应用ID
     * @param appName 应用名称
     * @return Token信息
     */
    KmAppTokenVo createDefaultToken(Long appId, String appName);
}
