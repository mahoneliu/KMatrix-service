package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.ai.domain.KmQuestionChunkMap;
import org.dromara.ai.domain.bo.ChunkResult;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.mapper.KmQuestionChunkMapMapper;
import org.dromara.ai.mapper.KmQuestionMapper;
import org.dromara.ai.service.IKmEmbeddingService;
import org.dromara.ai.util.StatusMetaUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 向量化服务实现
 * 从EtlHandler中抽离，提供可复用的向量化功能
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KmEmbeddingServiceImpl implements IKmEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final KmDocumentChunkMapper chunkMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final KmQuestionMapper questionMapper;
    private final KmDocumentMapper documentMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void embedAndStoreChunks(Long documentId, Long kbId, List<ChunkResult> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            log.warn("No chunks to embed for document: {}", documentId);
            return;
        }

        log.info("Starting parent-child embedding for {} top-level chunks of document {}", chunks.size(), documentId);

        List<KmDocumentChunk> allChunkEntities = new ArrayList<>();
        List<KmEmbedding> embeddings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunkResult = chunks.get(i);
            String chunkText = chunkResult.getContent();

            if (chunkText == null || chunkText.isBlank()) {
                log.warn("Skipping empty chunk at index {}", i);
                continue;
            }

            int chunkType = chunkResult.getChunkType();
            Long chunkId = IdUtil.getSnowflakeNextId();
            KmDocumentChunk chunkEntity = new KmDocumentChunk();
            chunkEntity.setId(chunkId);
            chunkEntity.setDocumentId(documentId);
            chunkEntity.setKbId(kbId);
            chunkEntity.setTitle(chunkResult.getTitle());
            chunkEntity.setContent(chunkText);
            chunkEntity.setCreateTime(now);
            chunkEntity.setChunkType(chunkType);
            chunkEntity.setParentId(null);

            Map<String, Object> metadata = new HashMap<>();
            if (chunkResult.getMetadata() != null) {
                metadata.putAll(chunkResult.getMetadata());
            }
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            chunkEntity.setMetadata(metadata);

            boolean hasChildren = CollUtil.isNotEmpty(chunkResult.getChildren());

            if (hasChildren) {
                // PARENT 块：入库但不向量化
                chunkEntity.setEmbeddingStatus(2);
                chunkEntity.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                        StatusMetaUtils.STATUS_SUCCESS));
                allChunkEntities.add(chunkEntity);

                // 处理子块
                List<ChunkResult> childResults = chunkResult.getChildren();
                for (int j = 0; j < childResults.size(); j++) {
                    ChunkResult childResult = childResults.get(j);
                    String childText = childResult.getContent();
                    if (childText == null || childText.isBlank())
                        continue;

                    float[] childVector = embeddingModel.embed(childText).content().vector();
                    Long childId = IdUtil.getSnowflakeNextId();

                    KmDocumentChunk childEntity = new KmDocumentChunk();
                    childEntity.setId(childId);
                    childEntity.setDocumentId(documentId);
                    childEntity.setKbId(kbId);

                    childEntity.setContent(childText);
                    childEntity.setCreateTime(now);
                    childEntity.setChunkType(KmDocumentChunk.ChunkType.CHILD);
                    childEntity.setParentId(chunkId);
                    childEntity
                            .setTitle(childResult.getTitle() != null ? childResult.getTitle() : chunkEntity.getTitle());

                    Map<String, Object> childMeta = new HashMap<>();
                    if (childResult.getMetadata() != null)
                        childMeta.putAll(childResult.getMetadata());
                    childMeta.put("childIndex", j);
                    childEntity.setMetadata(childMeta);
                    childEntity.setEmbeddingStatus(2);
                    childEntity.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                            StatusMetaUtils.STATUS_SUCCESS));
                    allChunkEntities.add(childEntity);

                    KmEmbedding childEmbedding = new KmEmbedding();
                    childEmbedding.setId(IdUtil.getSnowflakeNextId());
                    childEmbedding.setKbId(kbId);
                    childEmbedding.setSourceId(childId);
                    childEmbedding.setSourceType(KmEmbedding.SourceType.CHILD_CONTENT);
                    childEmbedding.setEmbedding(childVector);
                    childEmbedding.setEmbeddingString(Arrays.toString(childVector));
                    childEmbedding.setTextContent(childText);
                    childEmbedding.setCreateTime(now);
                    embeddings.add(childEmbedding);
                }
            } else {
                // STANDALONE 块：直接向量化
                float[] vector = embeddingModel.embed(chunkText).content().vector();
                chunkEntity.setEmbeddingStatus(2);
                chunkEntity.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                        StatusMetaUtils.STATUS_SUCCESS));
                allChunkEntities.add(chunkEntity);

                KmEmbedding embedding = new KmEmbedding();
                embedding.setId(IdUtil.getSnowflakeNextId());
                embedding.setKbId(kbId);
                embedding.setSourceId(chunkId);
                embedding.setSourceType(KmEmbedding.SourceType.CHILD_CONTENT);
                embedding.setEmbedding(vector);
                embedding.setEmbeddingString(Arrays.toString(vector));
                embedding.setTextContent(chunkText);
                embedding.setCreateTime(now);
                embeddings.add(embedding);
            }
        }

        // 批量插入
        if (!allChunkEntities.isEmpty()) {
            chunkMapper.insertBatch(allChunkEntities);
            log.info("Stored {} chunk entities for document {}", allChunkEntities.size(), documentId);
        }
        if (!embeddings.isEmpty()) {
            embeddingMapper.insertBatch(embeddings);
            log.info("Embedded {} child/standalone chunks for document {}", embeddings.size(), documentId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void embedAndStoreQaChunks(Long documentId, Long kbId, List<ChunkResult> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            log.warn("No QA chunks to embed for document: {}", documentId);
            return;
        }

        log.info("Starting QA embedding for {} chunks of document {}", chunks.size(), documentId);

        List<KmDocumentChunk> chunkEntities = new ArrayList<>();
        List<KmQuestion> questions = new ArrayList<>();
        List<KmQuestionChunkMap> questionChunkMaps = new ArrayList<>();
        List<KmEmbedding> embeddings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Date nowDate = new Date();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult chunkResult = chunks.get(i);
            String answer = chunkResult.getContent();

            if (answer == null || answer.isBlank()) {
                log.warn("Skipping empty QA chunk at index {}", i);
                continue;
            }

            // 生成答案向量
            float[] answerVector = embeddingModel.embed(answer).content().vector();

            // 创建Chunk实体
            Long chunkId = IdUtil.getSnowflakeNextId();
            KmDocumentChunk chunk = new KmDocumentChunk();
            chunk.setId(chunkId);
            chunk.setDocumentId(documentId);
            KmDocument document = documentMapper.selectById(documentId);
            chunk.setKbId(kbId);
            chunk.setTitle(chunkResult.getTitle() != null ? chunkResult.getTitle()
                    : (document != null ? document.getOriginalFilename() : null));
            chunk.setContent(answer);
            chunk.setCreateTime(now);
            chunk.setChunkType(KmDocumentChunk.ChunkType.STANDALONE);
            chunk.setParentId(null);

            // 元数据（移除questions，避免存储冗余数据）
            Map<String, Object> metadata = new HashMap<>();
            if (chunkResult.getMetadata() != null) {
                chunkResult.getMetadata().forEach((k, v) -> {
                    if (!"questions".equals(k)) {
                        metadata.put(k, v);
                    }
                });
            }
            chunk.setMetadata(metadata);

            chunk.setEmbeddingStatus(2); // 2 = 已生成
            chunk.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_SUCCESS));

            chunkEntities.add(chunk);

            // 创建答案的Embedding
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

            // 处理问题列表（从metadata中获取）
            @SuppressWarnings("unchecked")
            List<String> questionList = chunkResult.getMetadata() != null
                    ? (List<String>) chunkResult.getMetadata().get("questions")
                    : null;

            if (CollUtil.isNotEmpty(questionList)) {
                for (String q : questionList) {
                    if (StrUtil.isBlank(q))
                        continue;

                    Long questionId = IdUtil.getSnowflakeNextId();
                    KmQuestion question = new KmQuestion();
                    question.setId(questionId);
                    question.setKbId(kbId);
                    question.setContent(q.length() > 500 ? q.substring(0, 500) : q);
                    question.setHitNum(0);
                    question.setSourceType("IMPORT");
                    question.setCreateTime(nowDate);
                    questions.add(question);

                    // 创建问题-答案关联
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
                    questionEmbedding.setSourceId(map.getId());
                    questionEmbedding.setSourceType(KmEmbedding.SourceType.QUESTION);
                    questionEmbedding.setEmbedding(questionVector);
                    questionEmbedding.setEmbeddingString(Arrays.toString(questionVector));
                    questionEmbedding.setTextContent(q);
                    questionEmbedding.setCreateTime(now);
                    embeddings.add(questionEmbedding);
                }
            }
        }

        // 批量插入
        if (CollUtil.isNotEmpty(chunkEntities)) {
            chunkMapper.insertBatch(chunkEntities);
        }
        if (CollUtil.isNotEmpty(questions)) {
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

        log.info("QA embedding completed: documentId={}, chunks={}, questions={}, embeddings={}",
                documentId, chunkEntities.size(), questions.size(), embeddings.size());
    }

    @Override
    public void embedTitleForDocument(Long documentId, Long kbId, String title) {
        if (title == null || title.isBlank()) {
            return;
        }

        try {
            // 生成标题向量
            float[] titleVector = embeddingModel.embed(title).content().vector();
            String vectorString = Arrays.toString(titleVector);

            // 构建embedding实体
            KmEmbedding titleEmbedding = new KmEmbedding();
            titleEmbedding.setId(IdUtil.getSnowflakeNextId());
            titleEmbedding.setKbId(kbId);
            titleEmbedding.setSourceId(documentId); // 注意：这里是documentId
            titleEmbedding.setSourceType(KmEmbedding.SourceType.TITLE);
            titleEmbedding.setEmbedding(titleVector);
            titleEmbedding.setEmbeddingString(vectorString);
            titleEmbedding.setTextContent(title);
            titleEmbedding.setCreateTime(LocalDateTime.now());

            embeddingMapper.insertOne(titleEmbedding);
            log.info("Title vectorized for document: {}, title: {}", documentId, title);
        } catch (Exception e) {
            log.error("Failed to vectorize title for document: {}", documentId, e);
            // 不抛出异常，避免影响整体流程
        }
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be null or blank");
        }
        return embeddingModel.embed(text).content().vector();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (CollUtil.isEmpty(texts)) {
            return new ArrayList<>();
        }

        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            if (text != null && !text.isBlank()) {
                result.add(embeddingModel.embed(text).content().vector());
            }
        }
        return result;
    }
}
