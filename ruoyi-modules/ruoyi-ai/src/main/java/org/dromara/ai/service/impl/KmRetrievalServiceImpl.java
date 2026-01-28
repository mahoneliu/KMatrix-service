package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.bo.KmRetrievalBo;
import org.dromara.ai.domain.vo.KmRetrievalResultVo;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.service.IKmRetrievalService;
import org.dromara.ai.service.IKmRerankService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库检索服务实现
 * 支持向量检索、关键词检索、混合检索
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
    private final IKmRerankService rerankService;

    // 使用与 ETL 相同的 Embedding 模型
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    // RRF 融合常数
    private static final int RRF_K = 60;

    @Override
    public List<KmRetrievalResultVo> search(KmRetrievalBo bo) {
        if (StrUtil.isBlank(bo.getQuery())) {
            return Collections.emptyList();
        }

        // 解析数据集ID
        List<Long> datasetIds = resolveDatasetIds(bo.getKbIds(), bo.getDatasetIds());

        int topK = bo.getTopK() != null ? bo.getTopK() : 5;
        double threshold = bo.getThreshold() != null ? bo.getThreshold() : 0.5;
        String mode = bo.getMode() != null ? bo.getMode() : "VECTOR";

        List<KmRetrievalResultVo> results;

        switch (mode.toUpperCase()) {
            case "KEYWORD":
                results = keywordSearch(bo.getQuery(), datasetIds, topK);
                break;
            case "HYBRID":
                results = hybridSearch(bo.getQuery(), datasetIds, topK, threshold);
                break;
            case "VECTOR":
            default:
                results = vectorSearch(bo.getQuery(), datasetIds, topK, threshold);
                break;
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
        List<Map<String, Object>> rows = chunkMapper.keywordSearch(query, datasetIds, topK);
        return convertToResultList(rows);
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
            List<KmDocument> docs = documentMapper.selectBatchIds(docIds);
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
}
