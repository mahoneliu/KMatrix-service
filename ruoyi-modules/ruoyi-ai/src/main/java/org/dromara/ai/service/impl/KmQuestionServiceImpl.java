package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.*;
import org.dromara.ai.domain.vo.KmQuestionVo;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 问题服务实现
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmQuestionServiceImpl implements IKmQuestionService {

    private final KmQuestionMapper baseMapper;
    private final KmQuestionChunkMapMapper chunkMapMapper;
    private final KmDocumentChunkMapper chunkMapper;
    private final KmDocumentMapper documentMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final EmbeddingModel embeddingModel;

    private final KmModelMapper modelMapper;
    private final KmModelProviderMapper providerMapper;
    private final ModelBuilder modelBuilder;

    @Override
    public List<KmQuestionVo> listByChunkId(Long chunkId) {
        // 先查关联表
        List<KmQuestionChunkMap> maps = chunkMapMapper.selectList(
                new LambdaQueryWrapper<KmQuestionChunkMap>().eq(KmQuestionChunkMap::getChunkId, chunkId));
        if (CollUtil.isEmpty(maps)) {
            return new ArrayList<>();
        }
        List<Long> qIds = maps.stream().map(KmQuestionChunkMap::getQuestionId).toList();
        if (CollUtil.isEmpty(qIds)) {
            return new ArrayList<>();
        }
        List<KmQuestion> questions = baseMapper
                .selectList(new LambdaQueryWrapper<KmQuestion>().in(KmQuestion::getId, qIds));
        return MapstructUtils.convert(questions, KmQuestionVo.class);
    }

    @Override
    public List<KmQuestionVo> listByDocumentId(Long documentId) {
        // 1. Get all chunks for the document
        List<KmDocumentChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<KmDocumentChunk>()
                        .select(KmDocumentChunk::getId)
                        .eq(KmDocumentChunk::getDocumentId, documentId));

        if (CollUtil.isEmpty(chunks)) {
            return new ArrayList<>();
        }

        List<Long> chunkIds = chunks.stream().map(KmDocumentChunk::getId).toList();

        // 2. Get all question IDs mapped to these chunks
        List<KmQuestionChunkMap> maps = chunkMapMapper.selectList(
                new LambdaQueryWrapper<KmQuestionChunkMap>()
                        .in(KmQuestionChunkMap::getChunkId, chunkIds));

        if (CollUtil.isEmpty(maps)) {
            return new ArrayList<>();
        }

        List<Long> questionIds = maps.stream().map(KmQuestionChunkMap::getQuestionId).distinct().toList();

        // 3. Get questions
        if (CollUtil.isEmpty(questionIds)) {
            return new ArrayList<>();
        }

        List<KmQuestion> questions = baseMapper.selectBatchIds(questionIds);
        return MapstructUtils.convert(questions, KmQuestionVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addQuestion(Long chunkId, String content) {
        return addQuestionInternal(chunkId, null, content, "MANUAL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        // 1. Delete Embedding
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .eq(KmEmbedding::getSourceId, id)
                .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.QUESTION));

        // 2. Delete Map
        chunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getQuestionId, id));

        // 3. Delete Question
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmQuestionVo> generateQuestions(Long chunkId, Long modelId) {
        KmDocumentChunk chunk = chunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new RuntimeException("Chunk not found");
        }

        // Get Model
        KmModel model;
        if (modelId != null) {
            model = modelMapper.selectById(modelId);
        } else {
            // Pick first enabled model
            List<KmModel> models = modelMapper.selectList(
                    new LambdaQueryWrapper<KmModel>()
                            .eq(KmModel::getStatus, "0")
                            .last("LIMIT 1"));
            if (CollUtil.isEmpty(models)) {
                throw new RuntimeException("No available LLM models");
            }
            model = models.get(0);
        }

        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("Model provider not found");
        }

        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey());

        // Prompt
        String prompt = """
                请根据以下参考文本，识别 3-5 个潜在的用户问题。
                仅输出问题，每行一个。不要对它们进行编号。

                参考文本：
                %s
                """.formatted(chunk.getContent());

        String response = chatModel.generate(prompt);
        List<String> lines = StrUtil.split(response, '\n');

        for (String line : lines) {
            String qText = cleanQuestion(line);
            if (StrUtil.isBlank(qText))
                continue;

            addQuestionInternal(chunkId, chunk.getKbId(), qText, "LLM");
        }
        return listByChunkId(chunkId);
    }

    /**
     * Internal method to add question
     * 
     * @param chunkId    chunk id
     * @param kbId       kb id (optional, if null will be fetched from chunk)
     * @param content    question content
     * @param sourceType source type
     */
    private Boolean addQuestionInternal(Long chunkId, Long kbId, String content, String sourceType) {
        if (kbId == null) {
            KmDocumentChunk chunk = chunkMapper.selectById(chunkId);
            if (chunk == null) {
                throw new RuntimeException("切片不存在: " + chunkId);
            }
            kbId = chunk.getKbId();

            // 如果切片中的 kb_id 为空,从父级文档获取
            if (kbId == null) {
                KmDocument document = documentMapper.selectById(chunk.getDocumentId());
                if (document == null) {
                    throw new RuntimeException("切片关联的文档不存在: " + chunk.getDocumentId());
                }
                kbId = document.getKbId();
                if (kbId == null) {
                    throw new RuntimeException("无法确定知识库ID,切片和文档都缺少 kb_id");
                }
            }
        }

        // 1. Create Question
        KmQuestion q = new KmQuestion();
        q.setId(IdUtil.getSnowflakeNextId());
        q.setKbId(kbId);
        q.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        q.setHitNum(0);
        q.setSourceType(sourceType);
        q.setCreateTime(new Date());
        baseMapper.insert(q);

        // 2. Map
        KmQuestionChunkMap map = new KmQuestionChunkMap();
        map.setQuestionId(q.getId());
        map.setChunkId(chunkId);
        chunkMapMapper.insert(map);

        // 3. Embedding
        try {
            float[] vector = embeddingModel.embed(content).content().vector();
            KmEmbedding embedding = new KmEmbedding();
            embedding.setKbId(kbId);
            embedding.setSourceId(q.getId());
            embedding.setSourceType(KmEmbedding.SourceType.QUESTION);
            embedding.setEmbedding(vector);
            embedding.setEmbeddingString(Arrays.toString(vector));
            embedding.setTextContent(content);
            embedding.setCreateTime(LocalDateTime.now());
            embeddingMapper.insertOne(embedding);
        } catch (Exception e) {
            log.error("Failed to embed question: {}", content, e);
            throw new RuntimeException("Question embedding failed", e);
        }
        return true;
    }

    private String cleanQuestion(String line) {
        // Remove 1. 2. - 1、 etc
        return line.replaceAll("^[0-9\\.\\-\\s\\、]+", "").trim();
    }
}
