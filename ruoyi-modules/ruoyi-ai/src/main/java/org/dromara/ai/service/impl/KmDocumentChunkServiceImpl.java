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
    private final EmbeddingModel embeddingModel;

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
            // 更新向量
            float[] vector = embeddingModel.embed(content).content().vector();
            chunk.setEmbedding(vector);
            chunk.setEmbeddingString(Arrays.toString(vector));
            hasChanges = true;

            // 更新 km_embedding 表 (SourceType=CONTENT)
            embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                    .eq(KmEmbedding::getSourceId, id)
                    .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.CONTENT));

            KmEmbedding embedding = new KmEmbedding();
            embedding.setKbId(chunk.getKbId());
            embedding.setSourceId(id);
            embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
            embedding.setEmbeddingString(Arrays.toString(vector));
            embedding.setTextContent(content);
            embedding.setCreateTime(LocalDateTime.now());
            embeddingMapper.insertOne(embedding);
        }

        if (!hasChanges) {
            return true;
        }

        return baseMapper.updateById(chunk) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        // 1. 删除切片关联的向量 (SourceType=CONTENT)
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .eq(KmEmbedding::getSourceId, id)
                .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.CONTENT));

        // 2. 删除切片与问题的关联
        // 注意：这里暂不删除问题本身，因为问题可能被其他切片复用(虽然业务上通常是1:N).
        // 如果要删除问题，先查出问题ID，再判断这些问题是否只关联了当前切片。
        // 为简化，目前只断开关联。
        questionChunkMapMapper.delete(new LambdaQueryWrapper<KmQuestionChunkMap>()
                .eq(KmQuestionChunkMap::getChunkId, id));

        // 3. 删除切片
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public TableDataInfo<KmDocumentChunkVo> pageList(KmDocumentChunkBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmDocumentChunk> lqw = new LambdaQueryWrapper<>();
        // 必填条件：文档ID
        lqw.eq(bo.getDocumentId() != null, KmDocumentChunk::getDocumentId, bo.getDocumentId());
        // 可选筛选条件
        lqw.eq(bo.getEnabled() != null, KmDocumentChunk::getEnabled, bo.getEnabled());
        lqw.eq(bo.getEmbeddingStatus() != null, KmDocumentChunk::getEmbeddingStatus, bo.getEmbeddingStatus());
        lqw.eq(bo.getQuestionStatus() != null, KmDocumentChunk::getQuestionStatus, bo.getQuestionStatus());
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
        chunk.setEmbedding(vector);
        chunk.setEmbeddingString(Arrays.toString(vector));

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
        chunk.setId(id);
        chunk.setEnabled(enabled ? 1 : 0);
        return baseMapper.updateById(chunk) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnable(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        KmDocumentChunk chunk = new KmDocumentChunk();
        chunk.setEnabled(enabled ? 1 : 0);
        return baseMapper.update(chunk,
                new LambdaQueryWrapper<KmDocumentChunk>().in(KmDocumentChunk::getId, ids)) > 0;
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
