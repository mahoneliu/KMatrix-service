package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.domain.KmQuestionChunkMap;
import org.dromara.ai.domain.bo.KmDocumentChunkBo;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.mapper.KmQuestionChunkMapMapper;
import org.dromara.ai.service.IKmDocumentChunkService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 文档切片服务实现
 *
 * @author Mahone
 * @date 2026-02-02
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmDocumentChunkServiceImpl implements IKmDocumentChunkService {

    private final KmDocumentChunkMapper baseMapper;
    private final KmDocumentMapper documentMapper;
    private final KmEmbeddingMapper embeddingMapper;
    private final KmQuestionChunkMapMapper questionChunkMapMapper;
    private final org.dromara.ai.service.IKmQuestionService questionService;
    private final EmbeddingModel embeddingModel;
    private final org.dromara.ai.service.IKmChunkingConfigService chunkingConfigService;
    private final org.dromara.ai.mapper.KmDatasetMapper datasetMapper;

    @Override
    public List<KmDocumentChunkVo> listByDocumentId(Long documentId) {
        return MapstructUtils.convert(baseMapper.selectList(
                new LambdaQueryWrapper<KmDocumentChunk>()
                        .eq(KmDocumentChunk::getDocumentId, documentId)
                        .orderByAsc(KmDocumentChunk::getId)),
                KmDocumentChunkVo.class);
    }

    @Override
    public KmDocumentChunkVo queryById(Long id) {
        return MapstructUtils.convert(baseMapper.selectById(id), KmDocumentChunkVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateChunk(Long id, String title, String content) {
        KmDocumentChunk chunk = baseMapper.selectById(id);
        if (chunk == null) {
            return false;
        }

        boolean hasChanges = false;

        // 更新标题
        if (title != null && !StrUtil.equals(chunk.getTitle(), title)) {
            chunk.setTitle(title);
            hasChanges = true;
        }

        // 更新内容
        if (content != null && !StrUtil.equals(chunk.getContent(), content)) {
            chunk.setContent(content);
            hasChanges = true;

            if (KmDocumentChunk.ChunkType.PARENT == chunk.getChunkType()) {
                // 父分块: 删除旧的所有CHILD分块及其向量，并重新分词生成新的CHILD分块
                // 1. 获取所有的旧CHILD分块
                List<KmDocumentChunk> oldChildren = baseMapper.selectList(new LambdaQueryWrapper<KmDocumentChunk>()
                        .eq(KmDocumentChunk::getParentId, id));

                if (!oldChildren.isEmpty()) {
                    List<Long> oldChildIds = oldChildren.stream().map(KmDocumentChunk::getId).toList();
                    // 删除子分块的向量和关联
                    embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                            .in(KmEmbedding::getSourceId, oldChildIds)
                            .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.CHILD_CONTENT));

                    // 物理删除旧子分块
                    baseMapper.deleteByIds(oldChildIds);
                }

                // 2. 将修改后的新文本进行滑动切分并生成新的CHILD分块
                // 动态拉取所处知识库/数据集的分块参数
                int chunkSize = 500;
                int overlap = 50;
                if (chunk.getKbId() != null) {
                    org.dromara.ai.domain.KmDataset dataset = datasetMapper
                            .selectOne(new LambdaQueryWrapper<org.dromara.ai.domain.KmDataset>()
                                    .eq(org.dromara.ai.domain.KmDataset::getKbId, chunk.getKbId())
                                    .last("limit 1"));
                    if (dataset != null) {
                        chunkSize = chunkingConfigService.getChildChunkSize(dataset);
                        overlap = chunkingConfigService.getChildChunkOverlap(dataset);
                    }
                }

                // 这里调用langchain4j的工具类，使用动态获取的参数分割
                var splitter = dev.langchain4j.data.document.splitter.DocumentSplitters.recursive(chunkSize, overlap);
                dev.langchain4j.data.document.Document doc = dev.langchain4j.data.document.Document.from(content);
                List<dev.langchain4j.data.segment.TextSegment> segments = splitter.split(doc);

                LocalDateTime now = LocalDateTime.now();
                List<KmDocumentChunk> newChildren = new java.util.ArrayList<>();
                List<KmEmbedding> childEmbeddings = new java.util.ArrayList<>();

                for (int i = 0; i < segments.size(); i++) {
                    String childText = segments.get(i).text();
                    float[] childVector = embeddingModel.embed(childText).content().vector();
                    Long childId = cn.hutool.core.util.IdUtil.getSnowflakeNextId();

                    KmDocumentChunk childEntity = new KmDocumentChunk();
                    childEntity.setId(childId);
                    childEntity.setDocumentId(chunk.getDocumentId());
                    childEntity.setKbId(chunk.getKbId());
                    childEntity.setContent(childText);
                    childEntity.setCreateTime(now);
                    childEntity.setChunkType(KmDocumentChunk.ChunkType.CHILD);
                    childEntity.setParentId(id);
                    childEntity.setTitle(chunk.getTitle());
                    childEntity.setEmbeddingStatus(2); // 已生成

                    java.util.Map<String, Object> childMeta = new java.util.HashMap<>();
                    childMeta.put("childIndex", i);
                    childEntity.setMetadata(childMeta);
                    childEntity.setStatusMeta(org.dromara.ai.util.StatusMetaUtils.updateStateTime(null,
                            org.dromara.ai.util.StatusMetaUtils.TASK_EMBEDDING,
                            org.dromara.ai.util.StatusMetaUtils.STATUS_SUCCESS));

                    newChildren.add(childEntity);

                    KmEmbedding childEmbedding = new KmEmbedding();
                    childEmbedding.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextId());
                    childEmbedding.setKbId(chunk.getKbId());
                    childEmbedding.setSourceId(childId);
                    childEmbedding.setSourceType(KmEmbedding.SourceType.CHILD_CONTENT);
                    childEmbedding.setEmbedding(childVector);
                    childEmbedding.setEmbeddingString(Arrays.toString(childVector));
                    childEmbedding.setTextContent(childText);
                    childEmbedding.setCreateTime(now);

                    childEmbeddings.add(childEmbedding);
                }

                if (!newChildren.isEmpty()) {
                    baseMapper.insertBatch(newChildren);
                    embeddingMapper.insertBatch(childEmbeddings);
                }

            } else {
                // 非父分块 (CHILD 或 STANDALONE 或默认的): 重新生成当前块的向量 (维持原逻辑)
                float[] vector = embeddingModel.embed(content).content().vector();
                int sourceType = KmDocumentChunk.ChunkType.CHILD == chunk.getChunkType()
                        ? KmEmbedding.SourceType.CHILD_CONTENT
                        : KmEmbedding.SourceType.CONTENT;

                // 更精确的删除之前的向量
                embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                        .eq(KmEmbedding::getSourceId, id)
                        .eq(KmEmbedding::getSourceType, sourceType));

                KmEmbedding embedding = new KmEmbedding();
                embedding.setKbId(chunk.getKbId());
                embedding.setSourceId(id);
                embedding.setSourceType(sourceType);
                embedding.setEmbeddingString(Arrays.toString(vector));
                embedding.setTextContent(content);
                embedding.setCreateTime(LocalDateTime.now());
                embeddingMapper.insertOne(embedding);
            }
        }

        if (!hasChanges) {
            return true;
        }

        return baseMapper.updateById(chunk) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        // 查找可能存在的子分块
        List<KmDocumentChunk> children = baseMapper.selectList(new LambdaQueryWrapper<KmDocumentChunk>()
                .eq(KmDocumentChunk::getParentId, id));

        List<Long> idsToDelete = new java.util.ArrayList<>();
        idsToDelete.add(id);
        children.forEach(c -> idsToDelete.add(c.getId()));

        // 1. 删除所有关联切片的向量 (SourceType in CONTENT, CHILD_CONTENT)
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .in(KmEmbedding::getSourceId, idsToDelete)
                .in(KmEmbedding::getSourceType, KmEmbedding.SourceType.CONTENT, KmEmbedding.SourceType.CHILD_CONTENT));

        // 2. 删切片前，找出并清理“孤儿”问题（仅仅关联了这批被删切片的问题）
        List<KmQuestionChunkMap> oldMaps = questionChunkMapMapper
                .selectList(new LambdaQueryWrapper<KmQuestionChunkMap>()
                        .in(KmQuestionChunkMap::getChunkId, idsToDelete));
        if (cn.hutool.core.collection.CollUtil.isNotEmpty(oldMaps)) {
            List<Long> qIds = oldMaps.stream().map(KmQuestionChunkMap::getQuestionId).distinct().toList();
            // 找出这些问题还关联了哪些别的（没被删的）分块
            List<KmQuestionChunkMap> otherMaps = questionChunkMapMapper
                    .selectList(new LambdaQueryWrapper<KmQuestionChunkMap>()
                            .in(KmQuestionChunkMap::getQuestionId, qIds)
                            .notIn(KmQuestionChunkMap::getChunkId, idsToDelete));
            List<Long> safeQIds = otherMaps.stream().map(KmQuestionChunkMap::getQuestionId).distinct().toList();
            List<Long> orphanQIds = qIds.stream().filter(orphanId -> !safeQIds.contains(orphanId)).toList();

            // 彻底删除这些仅关联到当前切片的问题，服务层自带清理对应的 embedding
            if (cn.hutool.core.collection.CollUtil.isNotEmpty(orphanQIds)) {
                questionService.batchDelete(orphanQIds);
            }
        }

        // 删除切片的关联映射
        questionChunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .in(KmQuestionChunkMap::getChunkId, idsToDelete));

        // 3. 级联删除子切片和当前切片
        return baseMapper.deleteByIds(idsToDelete) > 0;
    }

    @Override
    public TableDataInfo<KmDocumentChunkVo> pageList(KmDocumentChunkBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmDocumentChunk> lqw = new LambdaQueryWrapper<>();
        // 必填条件：文档ID
        lqw.eq(bo.getDocumentId() != null, KmDocumentChunk::getDocumentId, bo.getDocumentId());
        // 隐去底层子分块
        lqw.ne(KmDocumentChunk::getChunkType, KmDocumentChunk.ChunkType.CHILD);
        // 可选筛选条件
        lqw.eq(bo.getEnabled() != null, KmDocumentChunk::getEnabled, bo.getEnabled());
        lqw.eq(bo.getEmbeddingStatus() != null, KmDocumentChunk::getEmbeddingStatus, bo.getEmbeddingStatus());
        lqw.eq(bo.getQuestionStatus() != null, KmDocumentChunk::getQuestionStatus, bo.getQuestionStatus());
        // 标题搜索
        lqw.like(StrUtil.isNotBlank(bo.getTitle()), KmDocumentChunk::getTitle, bo.getTitle());
        // 内容搜索
        lqw.like(StrUtil.isNotBlank(bo.getContent()), KmDocumentChunk::getContent, bo.getContent());
        // 关键词搜索
        lqw.like(StrUtil.isNotBlank(bo.getKeyword()), KmDocumentChunk::getContent, bo.getKeyword());
        // 默认排序
        lqw.orderByAsc(KmDocumentChunk::getId);

        Page<KmDocumentChunk> entityPage = baseMapper.selectPage(pageQuery.build(), lqw);
        List<KmDocumentChunkVo> voList = MapstructUtils.convert(entityPage.getRecords(), KmDocumentChunkVo.class);
        Page<KmDocumentChunkVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(),
                entityPage.getTotal());
        voPage.setRecords(voList);
        return TableDataInfo.build(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentChunkVo addChunk(KmDocumentChunkBo bo) {
        KmDocumentChunk chunk = new KmDocumentChunk();
        chunk.setDocumentId(bo.getDocumentId());
        chunk.setKbId(bo.getKbId());
        chunk.setTitle(bo.getTitle());
        chunk.setContent(bo.getContent());
        chunk.setEnabled(1);
        chunk.setEmbeddingStatus(0);
        chunk.setQuestionStatus(0);
        chunk.setCreateTime(LocalDateTime.now());

        // 生成向量
        float[] vector = embeddingModel.embed(bo.getContent()).content().vector();

        if (chunk.getKbId() == null && chunk.getDocumentId() != null) {
            KmDocument document = documentMapper.selectById(chunk.getDocumentId());
            if (document != null) {
                chunk.setKbId(document.getKbId());
                bo.setKbId(document.getKbId());
            }
        }

        baseMapper.insert(chunk);

        // 插入 km_embedding 表
        KmEmbedding embedding = new KmEmbedding();
        embedding.setKbId(bo.getKbId());
        embedding.setSourceId(chunk.getId());
        embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
        embedding.setEmbeddingString(Arrays.toString(vector));
        embedding.setTextContent(bo.getContent());
        embedding.setCreateTime(LocalDateTime.now());
        embeddingMapper.insertOne(embedding);

        // 更新 embeddingStatus
        chunk.setEmbeddingStatus(2);
        baseMapper.updateById(chunk);

        return MapstructUtils.convert(chunk, KmDocumentChunkVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableChunk(Long id, boolean enabled) {
        KmDocumentChunk chunk = new KmDocumentChunk();
        chunk.setEnabled(enabled ? 1 : 0);
        // 级联更新自己及所有的子分块
        return baseMapper.update(chunk, new LambdaQueryWrapper<KmDocumentChunk>()
                .eq(KmDocumentChunk::getId, id)
                .or()
                .eq(KmDocumentChunk::getParentId, id)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnable(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        KmDocumentChunk chunk = new KmDocumentChunk();
        chunk.setEnabled(enabled ? 1 : 0);
        // 级联更新自己及所有的子分块
        return baseMapper.update(chunk,
                new LambdaQueryWrapper<KmDocumentChunk>()
                        .in(KmDocumentChunk::getId, ids)
                        .or()
                        .in(KmDocumentChunk::getParentId, ids)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            deleteById(id);
        }
        return true;
    }
}
