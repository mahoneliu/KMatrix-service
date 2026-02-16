package org.dromara.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.*;
import org.dromara.ai.domain.bo.KmQuestionBo;
import org.dromara.ai.domain.vo.KmQuestionVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.ai.util.ModelBuilder;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import java.util.Map;
import org.dromara.ai.util.StatusMetaUtils;

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

    @Autowired
    @Lazy
    private IKmQuestionService self;

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
        // 单次JOIN查询获取文档关联的所有问题（优化：3次查询→1次查询）
        List<KmQuestion> questions = baseMapper.selectByDocumentId(documentId);
        return MapstructUtils.convert(questions, KmQuestionVo.class);
    }

    @Override
    public TableDataInfo<KmQuestionVo> pageList(KmQuestionBo bo, PageQuery pageQuery) {
        // 使用 MyBatis-Plus 分页查询,直接传递参数
        Page<KmQuestionVo> page = baseMapper.selectPageList(
                pageQuery.build(),
                bo.getKbId(),
                bo.getContent());

        // 使用 TableDataInfo.build 简化返回
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addQuestion(Long chunkId, String content) {
        return addQuestionInternal(chunkId, null, content, "MANUAL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        // 1. 查询问题的所有关联记录ID
        List<Long> mapIds = chunkMapMapper.selectIdsByQuestionIds(Arrays.asList(id));

        // 2. 删除对应的embedding记录（通过map.id）
        if (CollUtil.isNotEmpty(mapIds)) {
            embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                    .in(KmEmbedding::getSourceId, mapIds)
                    .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.QUESTION));
        }

        // 3. Delete Map
        chunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getQuestionId, id));

        // 4. Delete Question
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmQuestionVo> generateQuestions(Long chunkId, Long modelId, String prompt, Double temperature,
            Integer maxTokens) {
        KmDocumentChunk chunk = chunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new RuntimeException("Chunk not found");
        }

        // 1. 获取模型
        KmModel model;
        if (modelId != null) {
            model = modelMapper.selectById(modelId);
            if (model == null) {
                throw new RuntimeException("指定的模型不存在: " + modelId);
            }
        } else {
            // 获取系统默认模型
            List<KmModel> defaultModels = modelMapper.selectList(
                    new LambdaQueryWrapper<KmModel>()
                            .eq(KmModel::getModelType, "1") // 语言模型
                            .eq(KmModel::getStatus, "0") // 启用状态
                            .eq(KmModel::getIsDefault, 1) // 默认模型
                            .last("LIMIT 1"));

            if (CollUtil.isNotEmpty(defaultModels)) {
                model = defaultModels.get(0);
            } else {
                // 如果没有默认模型,选择第一个启用的语言模型
                List<KmModel> models = modelMapper.selectList(
                        new LambdaQueryWrapper<KmModel>()
                                .eq(KmModel::getModelType, "1")
                                .eq(KmModel::getStatus, "0")
                                .last("LIMIT 1"));
                if (CollUtil.isEmpty(models)) {
                    throw new RuntimeException("No available LLM models");
                }
                model = models.get(0);
            }
        }

        // 2. 从模型配置中读取默认参数(如果未传)
        if (temperature == null || maxTokens == null) {
            try {
                if (StrUtil.isNotBlank(model.getConfig())) {
                    JSONObject configObj = JSONUtil.parseObj(model.getConfig());
                    if (temperature == null && configObj.containsKey("temperature")) {
                        temperature = configObj.getDouble("temperature");
                    }
                    if (maxTokens == null && configObj.containsKey("maxTokens")) {
                        maxTokens = configObj.getInt("maxTokens");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse model config, using default values", e);
            }
        }

        // 设置默认值
        if (temperature == null) {
            temperature = 0.7;
        }
        if (maxTokens == null) {
            maxTokens = 2048;
        }

        // 3. 获取供应商信息
        KmModelProvider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null) {
            throw new RuntimeException("Model provider not found");
        }

        // 4. 构建聊天模型(使用传入的参数)
        ChatLanguageModel chatModel = modelBuilder.buildChatModel(model, provider.getProviderKey(), temperature,
                maxTokens);

        // 5. 构建提示词
        String finalPrompt;
        if (StrUtil.isNotBlank(prompt)) {
            // 使用传入的提示词,替换 {data} 占位符
            finalPrompt = prompt.replace("{data}", chunk.getContent());
        } else {
            // 使用默认提示词
            finalPrompt = """
                    请根据以下参考文本，识别 3-5 个潜在的用户问题。
                    仅输出问题，每行一个。不要对它们进行编号。

                    参考文本：
                    %s
                    """.formatted(chunk.getContent());
        }

        // 6. 调用模型生成问题
        String response = chatModel.generate(finalPrompt);

        // 7. 解析响应
        List<String> questions = parseQuestions(response);

        // 8. 保存问题
        for (String qText : questions) {
            if (StrUtil.isBlank(qText)) {
                continue;
            }
            addQuestionInternal(chunkId, chunk.getKbId(), qText, "LLM");
        }

        return listByChunkId(chunkId);
    }

    /**
     * 解析AI返回的问题列表
     * 支持两种格式:
     * 1. 每行一个问题
     * 2. <question>问题内容</question> 标签格式
     */
    private List<String> parseQuestions(String response) {
        List<String> questions = new ArrayList<>();

        // 先尝试解析 <question> 标签
        if (response.contains("<question>")) {
            String[] parts = response.split("<question>");
            for (String part : parts) {
                if (part.contains("</question>")) {
                    String question = part.substring(0, part.indexOf("</question>")).trim();
                    if (StrUtil.isNotBlank(question)) {
                        questions.add(question);
                    }
                }
            }
        }

        // 如果没有找到标签格式,按行分割
        if (questions.isEmpty()) {
            List<String> lines = StrUtil.split(response, '\n');
            for (String line : lines) {
                String qText = cleanQuestion(line);
                if (StrUtil.isNotBlank(qText)) {
                    questions.add(qText);
                }
            }
        }

        return questions;
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

        // 2. Map - 显式生成ID，用于embedding
        KmQuestionChunkMap map = new KmQuestionChunkMap();
        map.setId(IdUtil.getSnowflakeNextId());
        map.setQuestionId(q.getId());
        map.setChunkId(chunkId);
        chunkMapMapper.insert(map);

        // 3. Embedding - 使用关联记录ID作为source_id
        try {
            float[] vector = embeddingModel.embed(content).content().vector();
            KmEmbedding embedding = new KmEmbedding();
            embedding.setKbId(kbId);
            embedding.setSourceId(map.getId()); // 使用关联记录ID
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean linkQuestion(Long chunkId, Long questionId) {
        // Check if mapping already exists
        Long count = chunkMapMapper.selectCount(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getChunkId, chunkId)
                .eq(KmQuestionChunkMap::getQuestionId, questionId));

        if (count > 0) {
            return true;
        }

        // 获取问题信息
        KmQuestion question = baseMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在: " + questionId);
        }

        // 创建关联记录
        KmQuestionChunkMap map = new KmQuestionChunkMap();
        map.setId(IdUtil.getSnowflakeNextId());
        map.setQuestionId(questionId);
        map.setChunkId(chunkId);
        chunkMapMapper.insert(map);

        // 创建对应的embedding记录
        try {
            float[] vector = embeddingModel.embed(question.getContent()).content().vector();
            KmEmbedding embedding = new KmEmbedding();
            embedding.setKbId(question.getKbId());
            embedding.setSourceId(map.getId()); // 使用关联记录ID
            embedding.setSourceType(KmEmbedding.SourceType.QUESTION);
            embedding.setEmbedding(vector);
            embedding.setEmbeddingString(Arrays.toString(vector));
            embedding.setTextContent(question.getContent());
            embedding.setCreateTime(LocalDateTime.now());
            embeddingMapper.insertOne(embedding);
        } catch (Exception e) {
            log.error("Failed to create embedding for question link: questionId={}, chunkId={}", questionId, chunkId,
                    e);
            throw new RuntimeException("创建问题关联向量失败", e);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlinkQuestion(Long chunkId, Long questionId) {
        // 查询要删除的关联记录ID
        List<KmQuestionChunkMap> maps = chunkMapMapper.selectList(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getChunkId, chunkId)
                .eq(KmQuestionChunkMap::getQuestionId, questionId));

        if (CollUtil.isEmpty(maps)) {
            return true;
        }

        // 删除对应的embedding记录
        List<Long> mapIds = maps.stream().map(KmQuestionChunkMap::getId).toList();
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .in(KmEmbedding::getSourceId, mapIds)
                .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.QUESTION));

        // 删除关联记录
        return chunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getChunkId, chunkId)
                .eq(KmQuestionChunkMap::getQuestionId, questionId)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchGenerateQuestions(List<Long> chunkIds, Long modelId, String prompt, Double temperature,
            Integer maxTokens) {
        if (CollUtil.isEmpty(chunkIds)) {
            return true;
        }
        for (Long chunkId : chunkIds) {
            try {
                generateQuestions(chunkId, modelId, prompt, temperature, maxTokens);
            } catch (Exception e) {
                log.error("批量生成问题失败, chunkId={}", chunkId, e);
                // 继续处理下一个，不中断
            }
        }
        return true;
    }

    private String cleanQuestion(String line) {

        // Remove 1. 2. - 1、 etc
        return line.replaceAll("^[0-9\\.\\-\\s\\、]+", "").trim();
    }

    @Override
    @Async
    public void processGenerateQuestionsAsync(Long documentId, Long modelId, String prompt, Double temperature,
            Integer maxTokens) {
        // 更新问题生成状态为"生成中" already done by caller, but we can do handling logic here

        try {
            // 获取文档下的所有切片
            List<KmDocumentChunk> chunks = chunkMapper.selectList(
                    new LambdaQueryWrapper<KmDocumentChunk>()
                            .eq(KmDocumentChunk::getDocumentId, documentId));

            if (chunks != null && !chunks.isEmpty()) {
                // 为每个切片生成问题
                for (KmDocumentChunk chunk : chunks) {
                    try {
                        updateChunkQuestionStatus(chunk.getId(), 1, StatusMetaUtils.STATUS_STARTED);
                        // 使用 self 调用事务方法
                        self.generateQuestions(chunk.getId(), modelId, prompt, temperature, maxTokens);
                        updateChunkQuestionStatus(chunk.getId(), 2, StatusMetaUtils.STATUS_SUCCESS);
                    } catch (Exception e) {
                        log.error("Failed to generate questions for chunk: {}", chunk.getId(), e);
                        updateChunkQuestionStatus(chunk.getId(), 3, StatusMetaUtils.STATUS_FAILED);
                    }
                }
            }
            // 更新问题生成状态为"已生成"
            updateDocumentQuestionStatus(documentId, 2, StatusMetaUtils.STATUS_SUCCESS);
        } catch (Exception e) {
            log.error("Failed to generate questions for document: {}", documentId, e);
            // 更新问题生成状态为"失败"
            updateDocumentQuestionStatus(documentId, 3, StatusMetaUtils.STATUS_FAILED);
        }
    }

    private void updateDocumentQuestionStatus(Long documentId, Integer status, String metaStatus) {
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setQuestionStatus(status);

        // 更新 meta
        KmDocument exist = documentMapper.selectById(documentId);
        Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
        update.setStatusMeta(StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_GENERATE_QUESTION, metaStatus));

        documentMapper.updateById(update);
    }

    private void updateChunkQuestionStatus(Long chunkId, Integer status, String metaStatus) {
        KmDocumentChunk update = new KmDocumentChunk();
        update.setId(chunkId);
        update.setQuestionStatus(status);

        KmDocumentChunk exist = chunkMapper.selectById(chunkId);
        Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
        update.setStatusMeta(StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_GENERATE_QUESTION, metaStatus));

        chunkMapper.updateById(update);
    }

    @Override
    public List<KmQuestionVo> listByKbId(Long kbId) {
        List<KmQuestion> questions = baseMapper.selectList(
                new LambdaQueryWrapper<KmQuestion>()
                        .eq(KmQuestion::getKbId, kbId)
                        .orderByDesc(KmQuestion::getCreateTime));

        List<KmQuestionVo> result = MapstructUtils.convert(questions, KmQuestionVo.class);

        if (CollUtil.isEmpty(result)) {
            return new ArrayList<>();
        }

        // 批量查询所有问题的分段数量（优化：避免N+1查询）
        List<Long> questionIds = result.stream().map(KmQuestionVo::getId).toList();
        Map<Long, Map<String, Object>> countMap = chunkMapMapper.countByQuestionIds(questionIds);

        // 设置分段数量
        for (KmQuestionVo vo : result) {
            Map<String, Object> countData = countMap.get(vo.getId());
            Integer count = countData != null ? ((Number) countData.get("count")).intValue() : 0;
            vo.setChunkCount(count);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateQuestion(Long id, String content) {
        KmQuestion question = baseMapper.selectById(id);
        if (question == null) {
            throw new RuntimeException("问题不存在: " + id);
        }

        // 1. 更新问题内容
        KmQuestion update = new KmQuestion();
        update.setId(id);
        update.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        baseMapper.updateById(update);

        // 2. 查询该问题的所有关联记录ID
        List<Long> mapIds = chunkMapMapper.selectIdsByQuestionIds(Arrays.asList(id));

        // 3. 更新向量嵌入（为每个关联创建一个向量）
        if (CollUtil.isNotEmpty(mapIds)) {
            try {
                // 删除旧的嵌入（通过map.id）
                embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                        .in(KmEmbedding::getSourceId, mapIds)
                        .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.QUESTION));

                // 为每个关联创建新的嵌入
                float[] vector = embeddingModel.embed(content).content().vector();
                String vectorStr = Arrays.toString(vector);

                for (Long mapId : mapIds) {
                    KmEmbedding embedding = new KmEmbedding();
                    embedding.setKbId(question.getKbId());
                    embedding.setSourceId(mapId); // 使用关联记录ID
                    embedding.setSourceType(KmEmbedding.SourceType.QUESTION);
                    embedding.setEmbedding(vector);
                    embedding.setEmbeddingString(vectorStr);
                    embedding.setTextContent(content);
                    embedding.setCreateTime(LocalDateTime.now());
                    embeddingMapper.insertOne(embedding);
                }
            } catch (Exception e) {
                log.error("Failed to update question embedding: {}", content, e);
                throw new RuntimeException("问题向量更新失败", e);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return true;
        }

        for (Long id : ids) {
            try {
                deleteById(id);
            } catch (Exception e) {
                log.error("批量删除问题失败, id={}", id, e);
                // 继续处理下一个
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchAddToKb(Long kbId, List<String> contents) {
        if (CollUtil.isEmpty(contents)) {
            return true;
        }

        for (String content : contents) {
            if (StrUtil.isBlank(content)) {
                continue;
            }

            try {
                // 创建独立问题（不关联特定分块）
                KmQuestion q = new KmQuestion();
                q.setId(IdUtil.getSnowflakeNextId());
                q.setKbId(kbId);
                q.setContent(content.length() > 500 ? content.substring(0, 500) : content);
                q.setHitNum(0);
                q.setSourceType("MANUAL");
                q.setCreateTime(new Date());
                baseMapper.insert(q);

                // 不创建向量嵌入（没有关联分块的问题不向量化）
                log.debug("创建独立问题（无分块关联），不创建向量: questionId={}", q.getId());
            } catch (Exception e) {
                log.error("批量添加问题失败, content={}", content, e);
                // 继续处理下一个
            }
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> getLinkedChunks(Long questionId) {
        // 1. 查询问题关联的分块ID列表
        List<Long> chunkIds = chunkMapMapper.selectChunkIdsByQuestionId(questionId);

        if (CollUtil.isEmpty(chunkIds)) {
            return new ArrayList<>();
        }

        // 2. 一次性查询分块及文档信息（优化：通过 LEFT JOIN 避免N+1查询）
        List<Map<String, Object>> result = chunkMapper.selectChunksWithDocument(chunkIds);

        // 3. 规范化字段名
        for (Map<String, Object> item : result) {
            item.put("chunkId", item.get("id"));
            item.put("documentTitle", item.get("document_title"));
            item.put("documentId", item.get("document_id"));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchLinkToChunks(Long questionId, List<Long> chunkIds) {
        if (CollUtil.isEmpty(chunkIds)) {
            return true;
        }

        // 验证问题是否存在
        KmQuestion question = baseMapper.selectById(questionId);
        if (question == null) {
            throw new RuntimeException("问题不存在: " + questionId);
        }

        // 预先计算向量（避免重复计算）
        float[] vector = null;
        String vectorStr = null;

        int successCount = 0;
        for (Long chunkId : chunkIds) {
            try {
                // 检查分块是否存在
                KmDocumentChunk chunk = chunkMapper.selectById(chunkId);
                if (chunk == null) {
                    log.warn("分块不存在，跳过关联: chunkId={}", chunkId);
                    continue;
                }

                // 检查是否已关联
                Long count = chunkMapMapper.selectCount(new LambdaQueryWrapper<KmQuestionChunkMap>()
                        .eq(KmQuestionChunkMap::getChunkId, chunkId)
                        .eq(KmQuestionChunkMap::getQuestionId, questionId));

                if (count > 0) {
                    log.debug("问题已关联到分块，跳过: questionId={}, chunkId={}", questionId, chunkId);
                    continue;
                }

                // 创建关联
                KmQuestionChunkMap map = new KmQuestionChunkMap();
                map.setId(IdUtil.getSnowflakeNextId());
                map.setQuestionId(questionId);
                map.setChunkId(chunkId);
                chunkMapMapper.insert(map);

                // 创建对应的embedding记录
                if (vector == null) {
                    vector = embeddingModel.embed(question.getContent()).content().vector();
                    vectorStr = Arrays.toString(vector);
                }

                KmEmbedding embedding = new KmEmbedding();
                embedding.setKbId(question.getKbId());
                embedding.setSourceId(map.getId()); // 使用关联记录ID
                embedding.setSourceType(KmEmbedding.SourceType.QUESTION);
                embedding.setEmbedding(vector);
                embedding.setEmbeddingString(vectorStr);
                embedding.setTextContent(question.getContent());
                embedding.setCreateTime(LocalDateTime.now());
                embeddingMapper.insertOne(embedding);

                successCount++;
            } catch (Exception e) {
                log.error("关联问题到分块失败: questionId={}, chunkId={}", questionId, chunkId, e);
                // 继续处理下一个
            }
        }

        log.info("批量关联完成: questionId={}, 成功关联数={}/{}", questionId, successCount, chunkIds.size());
        return true;
    }
}
