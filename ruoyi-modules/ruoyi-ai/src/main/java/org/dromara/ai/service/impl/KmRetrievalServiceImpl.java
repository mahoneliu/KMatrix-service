package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmEmbedding;

import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmRetrievalService;
import org.dromara.ai.service.IKmRerankService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现
 * 支持向量检索、关键词检索、混合检索
 * 支持多源检索 (Content + Question)
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmRetrievalServiceImpl implements IKmRetrievalService {

    private final KmDocumentChunkMapper chunkMapper;
    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final KmQuestionMapper questionMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;
    private final IKmRerankService rerankService;
    private final EmbeddingModel embeddingModel;

    // RRF 融合常数
    private static final int RRF_K = 60;

    @Override
    public List<KmRetrievalResultVo> search(KmRetrievalBo bo) {
        if (StrUtil.isBlank(bo.getQuery())) {
            return Collections.emptyList();
        }

        // 获取知识库ID列表
        List<Long> kbIds = bo.getKbIds();
        if (CollUtil.isEmpty(kbIds) && CollUtil.isNotEmpty(bo.getDatasetIds())) {
            // 如果只有 datasetIds，转换为 kbIds
            kbIds = resolveKbIdsFromDatasets(bo.getDatasetIds());
        }

        int topK = bo.getTopK() != null ? bo.getTopK() : 5;
        double threshold = bo.getThreshold() != null ? bo.getThreshold() : 0.5;
        String mode = bo.getMode() != null ? bo.getMode() : "VECTOR";

        // 是否使用多源检索 (默认启用，可通过参数关闭)
        boolean useMultiSource = bo.getEnableMultiSource() == null || bo.getEnableMultiSource();

        List<KmRetrievalResultVo> results;

        if (useMultiSource) {
            // 使用多源检索 (搜索 km_embedding 表，包括 Content + Question + Title)
            switch (mode.toUpperCase()) {
                case "KEYWORD":
                    results = multiSourceKeywordSearch(bo.getQuery(), kbIds, topK);
                    break;
                case "HYBRID":
                    results = multiSourceHybridSearch(bo.getQuery(), kbIds, topK, threshold);
                    break;
                case "VECTOR":
                default:
                    results = multiSourceVectorSearch(bo.getQuery(), kbIds, topK, threshold);
                    break;
            }
        } else {
            // 降级到旧版检索 (直接查询 km_document_chunk 表)
            List<Long> datasetIds = resolveDatasetIds(bo.getKbIds(), bo.getDatasetIds());
            switch (mode.toUpperCase()) {
                case "KEYWORD":
                    if (Boolean.TRUE.equals(bo.getEnableHighlight())) {
                        results = keywordSearchWithHighlight(bo.getQuery(), datasetIds, topK);
                    } else {
                        results = keywordSearch(bo.getQuery(), datasetIds, topK);
                    }
                    break;
                case "HYBRID":
                    results = hybridSearch(bo.getQuery(), datasetIds, topK, threshold);
                    break;
                case "VECTOR":
                default:
                    results = vectorSearch(bo.getQuery(), datasetIds, topK, threshold);
                    break;
            }
        }

        // 如果启用 Rerank
        if (Boolean.TRUE.equals(bo.getEnableRerank()) && CollUtil.isNotEmpty(results)) {
            results = rerankService.rerank(bo.getQuery(), results, topK);
        }

        return results;
    }

    @Override
    public List<KmRetrievalResultVo> vectorSearch(String query, List<Long> datasetIds, int topK, double threshold) {
        // 生成查询向量
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();
        String vectorStr = floatArrayToString(queryEmbedding);

        // 执行向量搜索
        List<Map<String, Object>> rows = chunkMapper.vectorSearch(vectorStr, datasetIds, topK * 2);

        // 转换结果并过滤低分
        return convertAndFilter(rows, threshold);
    }

    @Override
    public List<KmRetrievalResultVo> keywordSearch(String query, List<Long> datasetIds, int topK) {
        if (StrUtil.isBlank(query)) {
            return Collections.emptyList();
        }
        // pg_jieba 会自动进行中英文分词
        List<Map<String, Object>> rows = chunkMapper.keywordSearch(query, datasetIds, topK);
        return convertToResultList(rows);
    }

    /**
     * 关键词检索 (带高亮)
     */
    public List<KmRetrievalResultVo> keywordSearchWithHighlight(String query, List<Long> datasetIds, int topK) {
        if (StrUtil.isBlank(query)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = chunkMapper.keywordSearchWithHighlight(query, datasetIds, topK);
        return convertToResultListWithHighlight(rows);
    }

    @Override
    public List<KmRetrievalResultVo> hybridSearch(String query, List<Long> datasetIds, int topK, double threshold) {
        // 1. 同时执行向量检索和关键词检索
        List<KmRetrievalResultVo> vectorResults = vectorSearch(query, datasetIds, topK * 2, 0); // 不过滤
        List<KmRetrievalResultVo> keywordResults = keywordSearch(query, datasetIds, topK * 2);

        // 2. RRF 融合
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, KmRetrievalResultVo> chunkMap = new HashMap<>();

        // 向量结果排名
        for (int i = 0; i < vectorResults.size(); i++) {
            KmRetrievalResultVo r = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);
            chunkMap.putIfAbsent(r.getChunkId(), r);
        }

        // 关键词结果排名
        for (int i = 0; i < keywordResults.size(); i++) {
            KmRetrievalResultVo r = keywordResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);
            chunkMap.putIfAbsent(r.getChunkId(), r);
        }

        // 3. 按 RRF 分数排序
        List<KmRetrievalResultVo> results = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    KmRetrievalResultVo vo = chunkMap.get(e.getKey());
                    vo.setScore(e.getValue()); // 使用 RRF 融合分数
                    return vo;
                })
                .collect(Collectors.toList());

        // 4. 过滤低分结果 (归一化后)
        double maxScore = results.isEmpty() ? 1 : results.get(0).getScore();
        return results.stream()
                .peek(r -> r.setScore(r.getScore() / maxScore)) // 归一化
                .filter(r -> r.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 多源向量检索 (搜索 km_embedding 表，包括 Content 和 Question)
     * 如果命中问题，则通过 km_question_chunk_map 关联到内容块
     *
     * @param query     查询文本
     * @param kbIds     知识库ID列表
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceVectorSearch(String query, List<Long> kbIds, int topK,
            double threshold) {
        // 生成查询向量
        float[] queryEmbedding = embeddingModel.embed(query).content().vector();
        String vectorStr = floatArrayToString(queryEmbedding);

        // 搜索 Content, Question 和 Title 类型的 embedding
        List<Integer> sourceTypes = Arrays.asList(
                KmEmbedding.SourceType.CONTENT,
                KmEmbedding.SourceType.QUESTION,
                KmEmbedding.SourceType.TITLE);

        List<Map<String, Object>> embeddingResults = embeddingMapper.vectorSearch(
                vectorStr, kbIds, sourceTypes, topK * 2, threshold);

        return processEmbeddingResults(embeddingResults, topK);
    }

    /**
     * 处理 Embedding 检索结果 (转换、关联 Chunk、去重、排序)
     */
    private List<KmRetrievalResultVo> processEmbeddingResults(List<Map<String, Object>> embeddingResults, int topK) {
        if (CollUtil.isEmpty(embeddingResults)) {
            return Collections.emptyList();
        }

        // 解析结果，将 Question 类型转换为关联的 Chunk
        Set<Long> chunkIds = new HashSet<>();
        Map<Long, Double> chunkScores = new HashMap<>();
        Map<Long, String> chunkHighlights = new HashMap<>();

        for (Map<String, Object> row : embeddingResults) {
            Long sourceId = ((Number) row.get("source_id")).longValue();
            Integer sourceType = ((Number) row.get("source_type")).intValue();
            Double score = ((Number) row.get("score")).doubleValue();
            String highlight = (String) row.get("highlight");

            if (sourceType == KmEmbedding.SourceType.CONTENT || sourceType == KmEmbedding.SourceType.TITLE) {
                // Content/Title 类型：sourceId 就是 chunkId
                chunkIds.add(sourceId);
                chunkScores.merge(sourceId, score, Math::max);
                // 优先保存 Content 的高亮
                if (sourceType == KmEmbedding.SourceType.CONTENT && highlight != null) {
                    chunkHighlights.put(sourceId, highlight);
                }
            } else if (sourceType == KmEmbedding.SourceType.QUESTION) {
                // Question 类型：需要通过 km_question_chunk_map 查找关联的 chunkId
                // TODO: 考虑批量查询优化
                List<Long> relatedChunkIds = questionChunkMapMapper.selectChunkIdsByQuestionId(sourceId);
                for (Long chunkId : relatedChunkIds) {
                    chunkIds.add(chunkId);
                    // 问题匹配可能比内容匹配更重要，这里取最高分
                    chunkScores.merge(chunkId, score, Math::max);
                }
                // 更新问题命中次数
                questionMapper.incrementHitNum(sourceId);
            }
        }

        if (chunkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取 Chunk 内容
        List<Map<String, Object>> chunkRows = chunkMapper.selectChunksByIds(new ArrayList<>(chunkIds));

        // 获取文档名称映射
        Set<Long> docIds = chunkRows.stream()
                .map(r -> ((Number) r.get("document_id")).longValue())
                .collect(Collectors.toSet());

        Map<Long, String> docNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<KmDocument> docs = documentMapper.selectByIds(docIds);
            docs.forEach(d -> docNameMap.put(d.getId(), d.getOriginalFilename()));
        }

        // 构建结果并按分数排序
        return chunkRows.stream()
                .map(row -> {
                    Long chunkId = ((Number) row.get("id")).longValue();
                    KmRetrievalResultVo vo = new KmRetrievalResultVo();
                    vo.setChunkId(chunkId);
                    vo.setDocumentId(((Number) row.get("document_id")).longValue());
                    vo.setContent((String) row.get("content"));
                    vo.setTitle((String) row.get("title"));
                    vo.setMetadata(row.get("metadata"));
                    vo.setScore(chunkScores.getOrDefault(chunkId, 0.0));
                    vo.setDocumentName(docNameMap.get(vo.getDocumentId()));
                    vo.setHighlight(chunkHighlights.get(chunkId));
                    return vo;
                })
                .sorted(Comparator.comparingDouble(KmRetrievalResultVo::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 多源关键词检索
     * 对于关键词检索，仍然使用 km_document_chunk 表的全文索引
     * 但结果会与多源向量检索的结果格式保持一致
     *
     * @param query 查询文本
     * @param kbIds 知识库ID列表
     * @param topK  返回数量
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceKeywordSearch(String query, List<Long> kbIds, int topK) {
        // 搜索 Content, Question 和 Title 类型的 embedding
        List<Integer> sourceTypes = Arrays.asList(
                KmEmbedding.SourceType.CONTENT,
                KmEmbedding.SourceType.QUESTION,
                KmEmbedding.SourceType.TITLE);

        List<Map<String, Object>> embeddingResults = embeddingMapper.keywordSearch(
                query, kbIds, sourceTypes, topK * 2);

        return processEmbeddingResults(embeddingResults, topK);
    }

    /**
     * 多源混合检索 (向量 + 关键词 + RRF 融合)
     *
     * @param query     查询文本
     * @param kbIds     知识库ID列表
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 检索结果
     */
    public List<KmRetrievalResultVo> multiSourceHybridSearch(String query, List<Long> kbIds, int topK,
            double threshold) {
        // 1. 多源向量检索
        List<KmRetrievalResultVo> vectorResults = multiSourceVectorSearch(query, kbIds, topK * 2, 0);
        // 2. 多源关键词检索
        List<KmRetrievalResultVo> keywordResults = multiSourceKeywordSearch(query, kbIds, topK * 2);

        // 3. RRF 融合
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, KmRetrievalResultVo> chunkMap = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            KmRetrievalResultVo r = vectorResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);
            chunkMap.putIfAbsent(r.getChunkId(), r);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            KmRetrievalResultVo r = keywordResults.get(i);
            double rrfScore = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), rrfScore, Double::sum);
            chunkMap.putIfAbsent(r.getChunkId(), r);
        }

        // 4. 按 RRF 分数排序
        List<KmRetrievalResultVo> results = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    KmRetrievalResultVo vo = chunkMap.get(e.getKey());
                    vo.setScore(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());

        // 5. 归一化并过滤
        double maxScore = results.isEmpty() ? 1 : results.get(0).getScore();
        return results.stream()
                .peek(r -> r.setScore(r.getScore() / maxScore))
                .filter(r -> r.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 从数据集ID列表反向解析知识库ID列表
     */
    private List<Long> resolveKbIdsFromDatasets(List<Long> datasetIds) {
        if (CollUtil.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }
        List<KmDataset> datasets = datasetMapper.selectByIds(datasetIds);
        return datasets.stream()
                .map(KmDataset::getKbId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 解析数据集ID (如果传入知识库ID，则查询其下所有数据集)
     */
    private List<Long> resolveDatasetIds(List<Long> kbIds, List<Long> datasetIds) {
        Set<Long> result = new HashSet<>();

        if (CollUtil.isNotEmpty(datasetIds)) {
            result.addAll(datasetIds);
        }

        if (CollUtil.isNotEmpty(kbIds)) {
            List<KmDataset> datasets = datasetMapper.selectList(
                    new LambdaQueryWrapper<KmDataset>().in(KmDataset::getKbId, kbIds));
            datasets.forEach(d -> result.add(d.getId()));
        }

        return new ArrayList<>(result);
    }

    /**
     * 将 float[] 转换为 PostgreSQL vector 字符串格式
     */
    private String floatArrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 转换查询结果为 VO 列表
     */
    private List<KmRetrievalResultVo> convertToResultList(List<Map<String, Object>> rows) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        // 获取文档名称映射
        Set<Long> docIds = rows.stream()
                .map(r -> ((Number) r.get("document_id")).longValue())
                .collect(Collectors.toSet());

        Map<Long, String> docNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<KmDocument> docs = documentMapper.selectByIds(docIds);
            docs.forEach(d -> docNameMap.put(d.getId(), d.getOriginalFilename()));
        }

        return rows.stream()
                .map(row -> {
                    KmRetrievalResultVo vo = new KmRetrievalResultVo();
                    vo.setChunkId(((Number) row.get("chunk_id")).longValue());
                    vo.setDocumentId(((Number) row.get("document_id")).longValue());
                    vo.setContent((String) row.get("content"));
                    vo.setTitle((String) row.get("title"));
                    vo.setMetadata(row.get("metadata"));
                    vo.setScore(row.get("score") != null ? ((Number) row.get("score")).doubleValue() : 0);
                    vo.setDocumentName(docNameMap.get(vo.getDocumentId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换并过滤低分结果
     */
    private List<KmRetrievalResultVo> convertAndFilter(List<Map<String, Object>> rows, double threshold) {
        return convertToResultList(rows).stream()
                .filter(r -> r.getScore() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 转换查询结果为 VO 列表 (含高亮)
     */
    private List<KmRetrievalResultVo> convertToResultListWithHighlight(List<Map<String, Object>> rows) {
        if (CollUtil.isEmpty(rows)) {
            return Collections.emptyList();
        }

        // 获取文档名称映射
        Set<Long> docIds = rows.stream()
                .map(r -> ((Number) r.get("document_id")).longValue())
                .collect(Collectors.toSet());

        Map<Long, String> docNameMap = new HashMap<>();
        if (!docIds.isEmpty()) {
            List<KmDocument> docs = documentMapper.selectByIds(docIds);
            docs.forEach(d -> docNameMap.put(d.getId(), d.getOriginalFilename()));
        }

        return rows.stream()
                .map(row -> {
                    KmRetrievalResultVo vo = new KmRetrievalResultVo();
                    vo.setChunkId(((Number) row.get("chunk_id")).longValue());
                    vo.setDocumentId(((Number) row.get("document_id")).longValue());
                    vo.setContent((String) row.get("content"));
                    vo.setMetadata(row.get("metadata"));
                    vo.setScore(row.get("score") != null ? ((Number) row.get("score")).doubleValue() : 0);
                    vo.setDocumentName(docNameMap.get(vo.getDocumentId()));
                    // 高亮字段
                    vo.setHighlight((String) row.get("highlight"));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
