package org.dromara.ai.app.service;

import java.util.Set;

/**
 * 应用能力检测与聚合服务
 * 
 * 获取多模态（vision, audio等）或附加节点能力
 */
public interface IAppCapabilityService {
    /**
     * 根据应用ID解析该应用的混合能力集合
     *
     * @param appId 应用ID
     * @return 最终聚合的能力标签集合，例如 ["vision", "audio", "image-ocr"]
     */
    Set<String> getAppCapabilities(Long appId);
}
