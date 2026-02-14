package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.bo.ChunkResult;
import org.dromara.ai.domain.enums.FileStoreType;
import org.dromara.ai.service.ILocalFileService;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * QA 对 ETL 处理器
 * 处理 Excel、CSV 格式的问答对文件
 *
 * 处理逻辑:
 * 1. 解析文件，识别"问题"、"答案"列
 * 2. 每行数据视为一个 QA 对
 * 3. "答案"作为chunk的content返回
 * 4. "问题"存入metadata的questions字段（支持多个问题）
 * 
 * 职责简化：仅负责文件解析和分块，问题-答案关联和向量化由上层服务处理
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QaPairEtlHandler implements EtlHandler {

    private final ISysOssService ossService;
    private final ILocalFileService localFileService;

    @Override
    public String getProcessType() {
        return DatasetProcessType.QA_PAIR;
    }

    @Override
    public List<ChunkResult> process(KmDocument document, KmDataset dataset) {
        log.info("QaPairEtlHandler processing document: {}", document.getId());

        // 1. 读取 Excel/CSV 文件 (返回原始行数据，包括可能的表头)
        List<List<String>> rawRows = readExcelFileAsRows(document);
        if (CollUtil.isEmpty(rawRows)) {
            throw new RuntimeException("QA 文件内容为空");
        }

        // 2. 检测是否需要跳过表头
        int startIndex = isHeaderRow(rawRows.get(0)) ? 1 : 0;
        log.info("Header detection: startIndex={}, totalRows={}", startIndex, rawRows.size());

        if (rawRows.size() <= startIndex) {
            throw new RuntimeException("QA 文件没有有效数据行");
        }

        // 3. 处理每行数据 (第一列=问题，第二列=答案)
        List<ChunkResult> results = new ArrayList<>();

        for (int rowIndex = startIndex; rowIndex < rawRows.size(); rowIndex++) {
            List<String> row = rawRows.get(rowIndex);
            if (row.size() < 2) {
                log.warn("Row {} has less than 2 columns, skipping", rowIndex + 1);
                continue;
            }

            // 第一列 = 问题, 第二列 = 答案
            String questionText = row.get(0) != null ? row.get(0).trim() : "";
            String answer = row.get(1) != null ? row.get(1).trim() : "";

            if (StrUtil.isBlank(answer)) {
                log.warn("Row {} has empty answer, skipping", rowIndex + 1);
                continue;
            }

            // 处理问题 (支持换行符分隔多个问题)
            List<String> questions = new ArrayList<>();
            String title = null;
            if (StrUtil.isNotBlank(questionText)) {
                String[] questionLines = questionText.split("[\r\n]+");
                for (String q : questionLines) {
                    q = q.trim();
                    if (StrUtil.isNotBlank(q)) {
                        questions.add(q.length() > 500 ? q.substring(0, 500) : q);
                        if (title == null) {
                            // 第一个问题作为标题
                            title = q.length() > 255 ? q.substring(0, 255) : q;
                        }
                    }
                }
            }

            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("rowIndex", rowIndex);
            metadata.put("source", "QA_PAIR");
            metadata.put("questions", questions); // 问题列表存入metadata

            results.add(ChunkResult.builder()
                    .content(answer)
                    .title(title)
                    .metadata(metadata)
                    .build());
        }

        log.info("QaPairEtlHandler completed: documentId={}, chunks={}",
                document.getId(), results.size());

        return results;
    }

    /**
     * 读取 Excel/CSV 文件并返回原始行数据 (List<List<String>>)
     */
    private List<List<String>> readExcelFileAsRows(KmDocument document) {
        try {
            // 根据存储类型获取文件流
            InputStream is = getDocumentInputStream(document);

            try (is) {
                String fileType = document.getFileType();
                if ("csv".equalsIgnoreCase(fileType)) {
                    return readCsvAsRows(is);
                } else {
                    return readExcelAsRows(is);
                }
            }
        } catch (Exception e) {
            log.error("Failed to read QA file: {}", document.getId(), e);
            throw new RuntimeException("QA 文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 根据文档存储类型获取输入流
     */
    private InputStream getDocumentInputStream(KmDocument document) throws IOException {
        FileStoreType storeType = FileStoreType.fromValue(document.getStoreType());

        if (storeType.isOss()) {
            // 从 OSS 读取
            Long ossId = document.getOssId();
            if (ossId == null) {
                throw new RuntimeException("Document OSS ID is null");
            }

            SysOssVo ossVo = ossService.getById(ossId);
            if (ossVo == null) {
                throw new RuntimeException("OSS file not found: " + ossId);
            }

            OssClient storage = OssFactory.instance(ossVo.getService());
            return storage.getObjectContent(ossVo.getFileName());

        } else if (storeType.isLocal()) {
            // 从本地文件系统读取
            String filePath = document.getFilePath();
            if (filePath == null) {
                throw new RuntimeException("Document file path is null");
            }

            return localFileService.getFileStream(filePath);

        } else {
            throw new RuntimeException("Unsupported store type: " + storeType);
        }
    }

    /**
     * 读取 Excel 文件为原始行列表
     */
    private List<List<String>> readExcelAsRows(InputStream is) {
        ExcelReader reader = ExcelUtil.getReader(is);
        List<List<Object>> rawData = reader.read();
        List<List<String>> result = new ArrayList<>();
        for (List<Object> row : rawData) {
            List<String> stringRow = new ArrayList<>();
            for (Object cell : row) {
                stringRow.add(cell != null ? cell.toString() : "");
            }
            result.add(stringRow);
        }
        return result;
    }

    /**
     * 读取 CSV 文件为原始行列表
     */
    private List<List<String>> readCsvAsRows(InputStream is) {
        cn.hutool.core.text.csv.CsvReader reader = cn.hutool.core.text.csv.CsvUtil.getReader();
        cn.hutool.core.text.csv.CsvData data = reader
                .read(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
        List<cn.hutool.core.text.csv.CsvRow> rows = data.getRows();
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        List<List<String>> result = new ArrayList<>();
        for (cn.hutool.core.text.csv.CsvRow row : rows) {
            result.add(row.getRawList());
        }
        return result;
    }

    /**
     * 检测第一行是否为表头
     * 通过关键字匹配判断: 如果第一列或第二列包含常见表头关键字，则认为是表头
     */
    private boolean isHeaderRow(List<String> firstRow) {
        if (firstRow == null || firstRow.isEmpty()) {
            return false;
        }

        // 常见的表头关键字 (大小写不敏感)
        Set<String> headerKeywords = Set.of(
                "问题", "question", "q", "问", "query", "提问",
                "答案", "answer", "a", "答", "回答", "response",
                "标题", "title", "name", "名称");

        // 检查前两列是否包含表头关键字
        for (int i = 0; i < Math.min(2, firstRow.size()); i++) {
            String cell = firstRow.get(i);
            if (cell != null && headerKeywords.contains(cell.toLowerCase().trim())) {
                log.info("Header row detected: column {} contains keyword '{}'", i, cell);
                return true;
            }
        }

        return false;
    }
}
