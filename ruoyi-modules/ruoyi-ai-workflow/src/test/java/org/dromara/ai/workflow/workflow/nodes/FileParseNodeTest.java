package org.dromara.ai.workflow.workflow.nodes;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.dromara.ai.knowledge.domain.KmDocument;
import org.dromara.ai.knowledge.mapper.KmDocumentMapper;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.workflow.workflow.core.NodeContext;
import org.dromara.ai.workflow.workflow.core.NodeOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class FileParseNodeTest {

    @Mock
    private KmDocumentMapper documentMapper;

    @Mock
    private IKmFileService fileService;

    @InjectMocks
    private FileParseNode fileParseNode;

    private NodeContext context;

    @BeforeEach
    void setUp() {
        context = new NodeContext();
    }

    @Test
    void testExecute_Success() throws Exception {
        // Arrange
        Long documentId = 1L;
        context.setInput("documentId", documentId);

        KmDocument document = new KmDocument();
        document.setId(documentId);
        document.setStoreType(2); // 本地存储
        document.setOssId(100L);
        document.setFilePath("test/file.txt");

        when(documentMapper.selectById(documentId)).thenReturn(document);

        String fileContent = "This is a test document content.";
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
        when(fileService.getFileStream(eq(2), eq(100L), eq("test/file.txt"))).thenReturn(inputStream);

        // Act
        NodeOutput output = fileParseNode.execute(context);

        // Assert
        assertNotNull(output);
        String text = (String) output.getOutput("text");
        assertNotNull(text);
        assertTrue(text.contains("This is a test document content."));
    }

    @Test
    void testExecute_MissingDocumentId() {
        // Arrange
        // (no documentId set in context)

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileParseNode.execute(context);
        });
        assertTrue(exception.getMessage().contains("缺少必要输入参数: documentId"));
    }

    @Test
    void testExecute_DocumentNotExist() {
        // Arrange
        Long documentId = 999L;
        context.setInput("documentId", documentId);
        when(documentMapper.selectById(documentId)).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            fileParseNode.execute(context);
        });
        assertTrue(exception.getMessage().contains("文档不存在"));
    }

    @Test
    void testExecute_FileStreamNull() throws Exception {
        // Arrange
        Long documentId = 2L;
        context.setInput("documentId", documentId);

        KmDocument document = new KmDocument();
        document.setId(documentId);
        document.setStoreType(1);
        when(documentMapper.selectById(documentId)).thenReturn(document);
        
        when(fileService.getFileStream(any(), any(), any())).thenReturn(null);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fileParseNode.execute(context);
        });
        assertTrue(exception.getMessage().contains("无法获取文档文件流"));
    }
}
