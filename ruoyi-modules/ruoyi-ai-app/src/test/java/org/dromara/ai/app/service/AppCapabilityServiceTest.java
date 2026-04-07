package org.dromara.ai.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Set;

import cn.hutool.json.JSONObject;
import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.app.service.impl.AppCapabilityServiceImpl;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppCapabilityServiceTest {

    @Mock
    private KmAppMapper appMapper;
    @Mock
    private KmModelMapper modelMapper;

    @InjectMocks
    private AppCapabilityServiceImpl capabilityService;

    @Test
    void testGetAppCapabilities_ModelAbilities() {
        Long appId = 1L;
        KmApp app = new KmApp();
        app.setModelId(10L);
        when(appMapper.selectById(appId)).thenReturn(app);

        KmModel model = new KmModel();
        model.setAbilities(Arrays.asList("vision", "audio"));
        when(modelMapper.selectById(10L)).thenReturn(model);

        Set<String> capabilities = capabilityService.getAppCapabilities(appId);

        assertTrue(capabilities.contains("vision"));
        assertTrue(capabilities.contains("audio"));
    }

    @Test
    void testGetAppCapabilities_GraphNodes() {
        Long appId = 1L;
        KmApp app = new KmApp();
        
        // Mock graph data with FILE_STORAGE and IMAGE_OCR nodes
        JSONObject graph = new JSONObject();
        JSONObject node1 = new JSONObject().set("type", "FILE_STORAGE");
        JSONObject node2 = new JSONObject().set("type", "IMAGE_OCR");
        graph.set("nodes", Arrays.asList(node1, node2));
        app.setGraphData(graph.toString());
        
        when(appMapper.selectById(appId)).thenReturn(app);

        Set<String> capabilities = capabilityService.getAppCapabilities(appId);

        assertTrue(capabilities.contains("file-storage"));
        assertTrue(capabilities.contains("vision"));
        assertTrue(capabilities.contains("image-ocr"));
    }
}
