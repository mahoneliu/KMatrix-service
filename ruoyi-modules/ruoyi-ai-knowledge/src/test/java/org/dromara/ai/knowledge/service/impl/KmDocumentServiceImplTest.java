package org.dromara.ai.knowledge.service.impl;

import org.dromara.ai.knowledge.domain.KmDataset;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDatasetMapper;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.knowledge.service.IKmEtlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KmDocumentServiceImplTest {

    @Mock
    private KmDocumentMapper documentMapper;

    @Mock
    private KmDatasetMapper datasetMapper;

    @Mock
    private IKmFileService kmFileService;

    @Mock
    private IKmEtlService etlService;

    @InjectMocks
    private KmDocumentServiceImpl kmDocumentService;

    private MockMultipartFile mockFile;
    private KmDataset dataset;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
        dataset = new KmDataset();
        dataset.setId(1L);
        dataset.setKbId(10L);
    }

    @Test
    void testUploadDocument_Success() throws IOException {
        // Arrange
        KmFileResult fileResult = KmFileResult.builder()
                .originalFilename("test.pdf")
                .hashCode("hash123")
                .filePath("/path/test.pdf")
                .fileSize(100L)
                .storeType(1)
                .build();

        when(kmFileService.upload(mockFile)).thenReturn(fileResult);
        when(documentMapper.selectOne(any())).thenReturn(null);
        when(datasetMapper.selectById(1L)).thenReturn(dataset);

        // Act
        kmDocumentService.uploadDocument(1L, mockFile);

        // Assert
        verify(kmFileService).upload(mockFile);
        verify(documentMapper).insert(any(KmDocument.class));
    }

    @Test
    void testUploadDocument_DuplicateHash() throws IOException {
        // Arrange
        KmFileResult fileResult = KmFileResult.builder()
                .hashCode("hash123")
                .build();
        KmDocument existingDoc = new KmDocument();
        existingDoc.setId(100L);

        when(kmFileService.upload(mockFile)).thenReturn(fileResult);
        when(documentMapper.selectOne(any())).thenReturn(existingDoc);

        // Act
        kmDocumentService.uploadDocument(1L, mockFile);

        // Assert
        verify(documentMapper, never()).insert(any(KmDocument.class));
    }
}
