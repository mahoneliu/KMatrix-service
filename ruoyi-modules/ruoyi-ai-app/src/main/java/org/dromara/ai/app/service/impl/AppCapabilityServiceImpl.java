package org.dromara.ai.app.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.app.service.IAppCapabilityService;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class AppCapabilityServiceImpl implements IAppCapabilityService {

    private final KmAppMapper appMapper;
    private final KmModelMapper modelMapper;

    @Override
    public Set<String> getAppCapabilities(Long appId) {
        Set<String> capabilities = new HashSet<>();
        
        KmApp app = appMapper.selectById(appId);
        if (app == null) {
            return capabilities;
        }

        // 1. 提取底层模型原生能力
        if (app.getModelId() != null) {
            KmModel model = modelMapper.selectById(app.getModelId());
            if (model != null && model.getAbilities() != null) {
                capabilities.addAll(model.getAbilities());
            }
        }
        
        // 2. 提取应用图谱附件节点能力
        String graphDataStr = app.getGraphData();
        if (StrUtil.isNotBlank(graphDataStr)) {
            try {
                JSONObject graphData = JSONUtil.parseObj(graphDataStr);
                // 兼容 nodes (Vue Flow) 和 cells (Legacy)
                JSONArray nodes = graphData.getJSONArray("nodes");
                if (nodes == null) {
                    nodes = graphData.getJSONArray("cells");
                }
                
                if (nodes != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        JSONObject node = nodes.getJSONObject(i);
                        // 同时从 node.type 和 node.data.nodeType 中提取
                        String type = node.getStr("type");
                        JSONObject data = node.getJSONObject("data");
                        String nodeType = data != null ? data.getStr("nodeType") : null;
                        
                        // 统一判断节点类型 (IMAGE_OCR, FILE_STORAGE, AUDIO_ASR)
                        String finalType = StrUtil.isNotBlank(nodeType) ? nodeType : type;
                        if (StrUtil.isBlank(finalType)) {
                            continue;
                        }

                        if ("FILE_STORAGE".equals(finalType)) {
                            capabilities.add("file-storage");
                        } else if ("FILE_PARSE".equals(finalType)) {
                            capabilities.add("file-parse");
                        } else if ("AUDIO_ASR".equals(finalType)) {
                            capabilities.add("audio");
                            capabilities.add("audio-asr");
                        } else if ("IMAGE_OCR".equals(finalType)) {
                            capabilities.add("vision");
                            capabilities.add("image-ocr");
                        }
                    }
                }
                
                if (capabilities.isEmpty()) {
                    log.info("未从图中发现多模态节点能力, appId: {}, nodesCount: {}", appId, nodes != null ? nodes.size() : 0);
                }
            } catch (Exception e) {
                log.warn("解析图谱节点获取附加能力失败, appId: {}, graphData: {}", appId, graphDataStr, e);
            }
        }
        
        return capabilities;
    }
}
