package org.dromara.ai.storage.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.ai.api.enums.FileStoreType;
import org.dromara.ai.storage.config.KmAiStorageProperties;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.dromara.ai.storage.domain.vo.LocalFileVo;
import org.dromara.ai.storage.mapper.KmTempFileMapper;
import org.dromara.ai.storage.service.ILocalFileService;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KmFileServiceImplTest {

    @Mock
    private ISysOssService ossService;

    @Mock
    private ILocalFileService localFileService;

    @Mock
    private KmAiStorageProperties storageProperties;

    @Mock
    private KmTempFileMapper tempFileMapper;

    @InjectMocks
    private KmFileServiceImpl kmFileService;

    private MockMultipartFile mockFile;
    private String fileHash;

    @BeforeEach
    void setUp() throws IOException {
        mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
        fileHash = DigestUtil.sha256Hex(mockFile.getInputStream());
    }

    @Test
    void testUpload_Oss() {
        // Arrange
        when(storageProperties.getType()).thenReturn(FileStoreType.OSS.getValue());
        SysOssVo ossVo = new SysOssVo();
        ossVo.setOssId(123L);
        ossVo.setUrl("http://oss.com/test.txt");
        when(ossService.upload(mockFile)).thenReturn(ossVo);

        // Act
        KmFileResult result = kmFileService.upload(mockFile);

        // Assert
        assertEquals("http://oss.com/test.txt", result.getUrl());
        assertEquals(123L, result.getOssId());
        assertEquals(fileHash, result.getHashCode());
        assertEquals("oss", result.getStoreType());
        verify(ossService).upload(mockFile);
        verifyNoInteractions(localFileService);
    }

    @Test
    void testUpload_Local() {
        // Arrange
        when(storageProperties.getType()).thenReturn(FileStoreType.LOCAL.getValue());
        LocalFileVo localVo = new LocalFileVo();
        localVo.setFilePath("C:/uploads/test.txt");
        localVo.setUrl("/api/ai/file/download/local?path=...");
        when(localFileService.upload(mockFile)).thenReturn(localVo);

        // Act
        KmFileResult result = kmFileService.upload(mockFile);

        // Assert
        assertEquals("C:/uploads/test.txt", result.getFilePath());
        assertEquals(fileHash, result.getHashCode());
        assertEquals("local", result.getStoreType());
        verify(localFileService).upload(mockFile);
        verifyNoInteractions(ossService);
    }

    @Test
    void testSaveTempFile() {
        // Arrange
        when(storageProperties.getType()).thenReturn(FileStoreType.LOCAL.getValue());
        LocalFileVo localVo = new LocalFileVo();
        localVo.setFilePath("temp/test.txt");
        when(localFileService.upload(mockFile)).thenReturn(localVo);

        // Act
        KmTempFile tempFile = kmFileService.saveTempFile(1L, mockFile);

        // Assert
        assertNotNull(tempFile);
        assertEquals(1L, tempFile.getDatasetId());
        assertEquals("test.txt", tempFile.getOriginalFilename());
        assertNotNull(tempFile.getExpireTime());
        verify(tempFileMapper).insert(any(KmTempFile.class));
    }

    @Test
    void testCleanExpiredTempFiles() {
        // Arrange
        KmTempFile expiredFile = new KmTempFile();
        expiredFile.setId(10L);
        expiredFile.setStoreType(FileStoreType.LOCAL.getValue());
        expiredFile.setFilePath("expired.txt");
        
        when(tempFileMapper.selectList(any())).thenReturn(Collections.singletonList(expiredFile));

        // Act
        kmFileService.cleanExpiredTempFiles();

        // Assert
        verify(localFileService).delete("expired.txt");
        verify(tempFileMapper).deleteById(10L);
    }
}
