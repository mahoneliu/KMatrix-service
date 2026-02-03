package org.dromara.ai.service.etl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.*;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.mapper.KmQuestionChunkMapMapper;
import org.dromara.ai.mapper.KmQuestionMapper;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.*;

/**
 * QA 对 ETL 处理器
 * 处理 Excel、CSV 格式的问答对文件
 *
 * 处理逻辑:
 * 1. 解析文件，识别"问题"、"答案"、"标题"列
 * 2. 每行数据视为一个 QA 对
 * 3. "答案"存入 km_document_chunk 的 content
 * 4. "问题"列支持换行符分隔多个相似问题，存入 km_question
 * 5. 问题与答案通过 km_question_chunk_map 关联
 * 6. 问题和答案都生成向量，存入 km_embedding
 *
 * @author Mahone
 * @date 2026-02-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QaPairEtlHandler implements EtlHandler {

    private final KmDocumentChunkMapper chunkMapper;
    private final KmQuestionMapper questionMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final ISysOssService ossService;
    private final EmbeddingModel embeddingModel;

    @Override
    public String getProcessType() {
        return DatasetProcessType.QA_PAIR;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(KmDocument document, KmDataset dataset, Long kbId) {
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
        List<KmDocumentChunk> chunks = new ArrayList<>();
        List<KmQuestion> questions = new ArrayList<>();
        List<KmQuestionChunkMap> questionChunkMaps = new ArrayList<>();
        List<KmEmbedding> embeddings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Date nowDate = new Date();

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

            // 创建 Chunk (Answer)
            Long chunkId = IdUtil.getSnowflakeNextId();
            KmDocumentChunk chunk = new KmDocumentChunk();
            chunk.setId(chunkId);
            chunk.setDocumentId(document.getId());
            chunk.setKbId(kbId);
            chunk.setContent(answer);
            // QA 模式下将问题设为标题 (截取前255字符)
            if (StrUtil.isNotBlank(questionText)) {
                chunk.setTitle(questionText.length() > 255 ? questionText.substring(0, 255) : questionText);
            }
            chunk.setCreateTime(now);

            // 元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("rowIndex", rowIndex);
            metadata.put("source", "QA_PAIR");
            chunk.setMetadata(metadata);

            // 生成答案向量
            float[] answerVector = embeddingModel.embed(answer).content().vector();
            chunk.setEmbedding(answerVector);
            chunk.setEmbeddingString(Arrays.toString(answerVector));

            chunks.add(chunk);

            // 创建 Embedding (Content)
            KmEmbedding contentEmbedding = new KmEmbedding();
            contentEmbedding.setId(IdUtil.getSnowflakeNextId());
            contentEmbedding.setKbId(kbId);
            contentEmbedding.setSourceId(chunkId);
            contentEmbedding.setSourceType(KmEmbedding.SourceType.CONTENT);
            contentEmbedding.setEmbedding(answerVector);
            contentEmbedding.setEmbeddingString(Arrays.toString(answerVector));
            contentEmbedding.setTextContent(answer);
            contentEmbedding.setCreateTime(now);
            embeddings.add(contentEmbedding);

            // 处理问题 (支持换行符分隔多个问题)
            if (StrUtil.isNotBlank(questionText)) {
                String[] questionLines = questionText.split("[\r\n]+");
                for (String q : questionLines) {
                    q = q.trim();
                    if (StrUtil.isBlank(q))
                        continue;

                    Long questionId = IdUtil.getSnowflakeNextId();
                    KmQuestion question = new KmQuestion();
                    question.setId(questionId);
                    question.setKbId(kbId);
                    question.setContent(q.length() > 500 ? q.substring(0, 500) : q);
                    question.setHitNum(0);
                    question.setSourceType("IMPORT");
                    // BaseEntity uses java.util.Date
                    question.setCreateTime(nowDate);
                    questions.add(question);

                    // 创建关联
                    KmQuestionChunkMap map = new KmQuestionChunkMap();
                    map.setId(IdUtil.getSnowflakeNextId());
                    map.setQuestionId(questionId);
                    map.setChunkId(chunkId);
                    questionChunkMaps.add(map);

                    // 生成问题向量
                    float[] questionVector = embeddingModel.embed(q).content().vector();
                    KmEmbedding questionEmbedding = new KmEmbedding();
                    questionEmbedding.setId(IdUtil.getSnowflakeNextId());
                    questionEmbedding.setKbId(kbId);
                    questionEmbedding.setSourceId(questionId);
                    questionEmbedding.setSourceType(KmEmbedding.SourceType.QUESTION);
                    questionEmbedding.setEmbedding(questionVector);
                    questionEmbedding.setEmbeddingString(Arrays.toString(questionVector));
                    questionEmbedding.setTextContent(q);
                    questionEmbedding.setCreateTime(now);
                    embeddings.add(questionEmbedding);
                }
            }
        }

        // 4. 批量保存
        if (CollUtil.isNotEmpty(chunks)) {
            chunkMapper.insertBatch(chunks);
        }
        if (CollUtil.isNotEmpty(questions)) {
            // 使用 MyBatis-Plus 批量插入
            for (KmQuestion question : questions) {
                questionMapper.insert(question);
            }
        }
        if (CollUtil.isNotEmpty(questionChunkMaps)) {
            for (KmQuestionChunkMap map : questionChunkMaps) {
                questionChunkMapMapper.insert(map);
            }
        }
        if (CollUtil.isNotEmpty(embeddings)) {
            embeddingMapper.insertBatch(embeddings);
        }

        log.info("QaPairEtlHandler completed: documentId={}, chunks={}, questions={}, embeddings={}",
                document.getId(), chunks.size(), questions.size(), embeddings.size());
    }

    /**
     * 读取 Excel/CSV 文件并返回原始行数据 (List<List<String>>)
     */
    private List<List<String>> readExcelFileAsRows(KmDocument document) {
        try {
            Long ossId = document.getOssId();
            if (ossId == null) {
                throw new RuntimeException("Document OSS ID is null");
            }

            SysOssVo ossVo = ossService.getById(ossId);
            if (ossVo == null) {
                throw new RuntimeException("OSS file not found: " + ossId);
            }

            OssClient storage = OssFactory.instance(ossVo.getService());
            try (InputStream is = storage.getObjectContent(ossVo.getFileName())) {
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
