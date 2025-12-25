package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmModelProviderBo;
import org.dromara.ai.domain.KmModelProvider;
import java.util.List;

/**
 * 模型供应商Service接口
 *
 * @author Mahone
 * @date 2024-01-27
 */
public interface IKmModelProviderService {

    /**
     * 查询所有供应商
     */
    List<KmModelProvider> queryList(KmModelProviderBo bo);

    /**
     * 根据ID查询
     */
    KmModelProvider queryById(Long providerId);
}
