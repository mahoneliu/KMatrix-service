package org.dromara.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.domain.KmQuestionChunkMap;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.KmEmbeddingMapper;
import org.dromara.ai.mapper.KmQuestionChunkMapMapper;
import org.dromara.ai.service.IKmDocumentChunkService;
import org.dromara.common.core.utils.MapstructUtils;
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
    public Boolean updateChunk(Long id, String content) {
        KmDocumentChunk chunk = baseMapper.selectById(id);
        if (chunk == null) {
            return false;
        }

        if (StrUtil.equals(chunk.getContent(), content)) {
            return true;
        }

        chunk.setContent(content);
        // 更新向量
        float[] vector = embeddingModel.embed(content).content().vector();
        chunk.setEmbedding(vector);
        chunk.setEmbeddingString(Arrays.toString(vector));

        int rows = baseMapper.updateById(chunk);

        // 更新 km_embedding 表 (SourceType=CONTENT)
        embeddingMapper.delete(new LambdaQueryWrapper<KmEmbedding>()
                .eq(KmEmbedding::getSourceId, id)
                .eq(KmEmbedding::getSourceType, KmEmbedding.SourceType.CONTENT));

        KmEmbedding embedding = new KmEmbedding();
        embedding.setKbId(chunk.getKbId());
        embedding.setSourceId(id);
        embedding.setSourceType(KmEmbedding.SourceType.CONTENT);
        embedding.setEmbedding(vector);
        embedding.setEmbeddingString(Arrays.toString(vector));
        embedding.setTextContent(content);
        embedding.setCreateTime(LocalDateTime.now());
        embeddingMapper.insert(embedding);

        return rows > 0;
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
}
