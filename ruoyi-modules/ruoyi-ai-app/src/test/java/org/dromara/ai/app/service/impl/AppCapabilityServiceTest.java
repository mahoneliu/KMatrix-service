package org.dromara.ai.app.service.impl;

import org.dromara.ai.app.domain.KmApp;
import org.dromara.ai.app.mapper.KmAppMapper;
import org.dromara.ai.model.domain.KmModel;
import org.dromara.ai.model.mapper.KmModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AppCapabilityServiceTest {

    @Mock
    private KmAppMapper appMapper;

    @Mock
    private KmModelMapper modelMapper;

    @InjectMocks
    private AppCapabilityServiceImpl appCapabilityService;

    private KmApp app;
    private KmModel model;

    @BeforeEach
    void setUp() {
        app = new KmApp();
        app.setAppId(1L);
        app.setModelId(100L);

        model = new KmModel();
        model.setModelId(100L);
        model.setAbilities(Collections.singletonList("vision"));
    }

    @Test
    void testGetAppCapabilities_FromModel() {
        // Arrange
        when(appMapper.selectById(1L)).thenReturn(app);
        when(modelMapper.selectById(100L)).thenReturn(model);

        // Act
        Set<String> capabilities = appCapabilityService.getAppCapabilities(1L);

        // Assert
        assertTrue(capabilities.contains("vision"));
        assertEquals(1, capabilities.size());
    }

    @Test
    void testGetAppCapabilities_FromGraph() {
        // Arrange
        app.setGraphData("{\"nodes\":[{\"type\":\"AUDIO_ASR\",\"data\":{\"nodeType\":\"AUDIO_ASR\"}}]}");
        when(appMapper.selectById(1L)).thenReturn(app);
        when(modelMapper.selectById(100L)).thenReturn(model);

        // Act
        Set<String> capabilities = appCapabilityService.getAppCapabilities(1L);

        // Assert
        assertTrue(capabilities.contains("vision")); // From model
        assertTrue(capabilities.contains("audio"));  // From graph
        assertTrue(capabilities.contains("audio-asr"));
        assertEquals(3, capabilities.size());
    }

    @Test
    void testGetAppCapabilities_AppNotFound() {
        // Arrange
        when(appMapper.selectById(999L)).thenReturn(null);

        // Act
        Set<String> capabilities = appCapabilityService.getAppCapabilities(999L);

        // Assert
        assertTrue(capabilities.isEmpty());
    }
}
