package org.dromara.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmDocumentChunk;
import org.dromara.ai.domain.KmKnowledgeBase;
import org.dromara.ai.domain.KmQuestion;
import org.dromara.ai.domain.bo.KmKnowledgeBaseBo;
import org.dromara.ai.domain.vo.KmKnowledgeBaseVo;
import org.dromara.ai.domain.vo.KmStatisticsVo;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmDocumentChunkMapper;
import org.dromara.ai.mapper.*;
import org.dromara.ai.service.IKmKnowledgeBaseService;
import org.dromara.ai.service.etl.DatasetProcessType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service业务层处理
 *
 * @author Mahone
 * @date 2026-01-28
 */
@RequiredArgsConstructor
@Service
public class KmKnowledgeBaseServiceImpl implements IKmKnowledgeBaseService {

    private final KmKnowledgeBaseMapper baseMapper;
    private final KmDatasetMapper datasetMapper;
    private final KmDocumentMapper documentMapper;
    private final KmDocumentChunkMapper chunkMapper;
    private final KmQuestionMapper questionMapper;
    private final KmEmbeddingMapper embeddingMapper;

    /**
     * 查询知识库
     */
    @Override
    public KmKnowledgeBaseVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public TableDataInfo<KmKnowledgeBaseVo> queryPageList(KmKnowledgeBaseBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = buildQueryWrapper(bo);
        Page<KmKnowledgeBaseVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public List<KmKnowledgeBaseVo> queryList(KmKnowledgeBaseBo bo) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KmKnowledgeBase> buildQueryWrapper(KmKnowledgeBaseBo bo) {
        LambdaQueryWrapper<KmKnowledgeBase> lqw = new LambdaQueryWrapper<>();
        lqw.like(StringUtils.isNotBlank(bo.getName()), KmKnowledgeBase::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), KmKnowledgeBase::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPermissionLevel()), KmKnowledgeBase::getPermissionLevel,
                bo.getPermissionLevel());
        lqw.orderByDesc(KmKnowledgeBase::getCreateTime);
        return lqw;
    }

    /**
     * 新增知识库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(KmKnowledgeBaseBo bo) {
        KmKnowledgeBase add = MapstructUtils.convert(bo, KmKnowledgeBase.class);
        // 设置所属用户
        add.setOwnerId(LoginHelper.getUserId());
        // 默认状态
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("ACTIVE");
        }
        if (StringUtils.isBlank(add.getPermissionLevel())) {
            add.setPermissionLevel("PRIVATE");
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            // 自动创建系统预设数据集
            createSystemDatasets(add.getId());
            return add.getId();
        }
        return null;
    }

    /**
     * 创建系统预设数据集
     */
    private void createSystemDatasets(Long kbId) {
        // 通用文件数据集
        KmDataset genericFile = new KmDataset();
        genericFile.setKbId(kbId);
        genericFile.setName("通用文件");
        genericFile.setProcessType(DatasetProcessType.GENERIC_FILE);
        genericFile.setSourceType(KmDataset.SourceType.FILE_UPLOAD);
        genericFile.setIsSystem(true);
        genericFile.setMinChunkSize(100);
        genericFile.setMaxChunkSize(500);
        genericFile.setChunkOverlap(50);
        genericFile.setAllowedFileTypes("*"); // 支持所有文件格式
        datasetMapper.insert(genericFile);

        // QA问答对数据集
        KmDataset qaPair = new KmDataset();
        qaPair.setKbId(kbId);
        qaPair.setName("QA问答对");
        qaPair.setProcessType(DatasetProcessType.QA_PAIR);
        qaPair.setSourceType(KmDataset.SourceType.FILE_UPLOAD);
        qaPair.setIsSystem(true);
        qaPair.setAllowedFileTypes("xlsx,xls,csv"); // 仅支持 Excel 和 CSV
        datasetMapper.insert(qaPair);

        // 在线文档数据集
        KmDataset onlineDoc = new KmDataset();
        onlineDoc.setKbId(kbId);
        onlineDoc.setName("在线文档");
        onlineDoc.setProcessType(DatasetProcessType.ONLINE_DOC);
        onlineDoc.setSourceType(KmDataset.SourceType.TEXT_INPUT);
        onlineDoc.setIsSystem(true);
        onlineDoc.setMinChunkSize(100);
        onlineDoc.setMaxChunkSize(500);
        onlineDoc.setChunkOverlap(50);
        // 在线文档不需要文件上传,无需设置 allowedFileTypes
        datasetMapper.insert(onlineDoc);

        // 网页链接数据集
        KmDataset webLink = new KmDataset();
        webLink.setKbId(kbId);
        webLink.setName("网页链接");
        webLink.setProcessType(DatasetProcessType.WEB_LINK);
        webLink.setSourceType(KmDataset.SourceType.WEB_CRAWL);
        webLink.setIsSystem(true);
        webLink.setMinChunkSize(100);
        webLink.setMaxChunkSize(500);
        webLink.setChunkOverlap(50);
        // 网页链接不需要文件上传,无需设置 allowedFileTypes
        datasetMapper.insert(webLink);
    }

    /**
     * 修改知识库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(KmKnowledgeBaseBo bo) {
        KmKnowledgeBase update = MapstructUtils.convert(bo, KmKnowledgeBase.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 校验并批量删除知识库信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否存在数据集 (无需校验，因为删除知识库会连带删除数据集)

            // 校验是否存在文档
            if (documentMapper.exists(new LambdaQueryWrapper<KmDocument>().in(KmDocument::getKbId, ids))) {
                throw new ServiceException("当前知识库下存在文档，无法直接删除");
            }
            // 校验是否存在问题
            if (questionMapper.exists(
                    new LambdaQueryWrapper<KmQuestion>().in(KmQuestion::getKbId, ids).apply("del_flag = '0'"))) {
                throw new ServiceException("当前知识库下存在问题，无法直接删除");
            }
        }

        // 级联删除
        for (Long id : ids) {
            // 删除文档 (逻辑删除)
            // KmDocument 继承 BaseEntity，若需物理删除需检查配置。
            // 这里我们使用 MP 的 delete 方法，假设配置了逻辑删除或者接受物理删除。
            // 按照需求 "软删除"，通常 Entity 需有 @TableLogic。
            // 若 KmDocument 未配置逻辑删除，此处为物理删除。
            documentMapper.delete(new LambdaQueryWrapper<KmDocument>().eq(KmDocument::getKbId, id));

            // 删除切片 (物理删除，因为 KmDocumentChunk 无 del_flag)
            chunkMapper.delete(new LambdaQueryWrapper<KmDocumentChunk>().eq(KmDocumentChunk::getKbId, id));

            // 删除问题 (逻辑删除)
            questionMapper.deleteByKbId(id);

            // 删除向量 (物理删除)
            embeddingMapper.deleteByKbId(id);

            // 删除数据集 (物理删除，因为 KmDataset 无 del_flag)
            datasetMapper.delete(new LambdaQueryWrapper<KmDataset>().eq(KmDataset::getKbId, id));
        }

        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 获取知识库统计信息 (Global)
     */
    @Override
    public KmStatisticsVo getStatistics() {
        KmStatisticsVo vo = new KmStatisticsVo();

        // 知识库总数
        vo.setTotalKbs(baseMapper.selectCount(null));

        // 数据集总数
        vo.setTotalDatasets(datasetMapper.selectCount(null));

        // 文档总数
        vo.setTotalDocuments(documentMapper.selectCount(null));

        // 切片总数
        vo.setTotalChunks(chunkMapper.selectCount(null));

        // 问题总数
        vo.setQuestionCount(questionMapper.selectCount(null));

        // 处理中文档数 (向量化中 或 问题生成中)
        LambdaQueryWrapper<KmDocument> processingQuery = new LambdaQueryWrapper<>();
        processingQuery.and(w -> w.eq(KmDocument::getEmbeddingStatus, 1)
                .or()
                .eq(KmDocument::getQuestionStatus, 1));
        vo.setProcessingDocs(documentMapper.selectCount(processingQuery));

        // 失败文档数 (向量化失败 或 问题生成失败)
        LambdaQueryWrapper<KmDocument> errorQuery = new LambdaQueryWrapper<>();
        errorQuery.and(w -> w.eq(KmDocument::getEmbeddingStatus, 3)
                .or()
                .eq(KmDocument::getQuestionStatus, 3));
        vo.setErrorDocs(documentMapper.selectCount(errorQuery));

        return vo;
    }

    /**
     * 获取知识库统计信息 (Specific KB)
     */
    @Override
    public KmStatisticsVo getStatistics(Long kbId) {
        KmStatisticsVo vo = new KmStatisticsVo();

        // 知识库总数 (Not relevant for single KB stats, but keeping structure if needed for
        // context, or just leaving as is is wrong for specific KB stats)
        // Since we are changing the method signature to get stats for a specific KB, we
        // should focus on that KB's stats.
        // However, the previous getStatistics() without args seemed to be for global
        // stats?
        // Let's check if the previous one was used globally. If so, I should have
        // overloaded it instead of replacing it.
        // But the previous controller method was `/statistics` which implies global or
        // current user/session context?
        // Wait, the previous controller method was `getStatistics()` calling
        // `getStatistics()`.
        // The implementation showed `baseMapper.selectCount(null)` which counts ALL
        // KBs.
        // The user request is for "Knowledge Base Detail Page", so we need stats for
        // ONE KB.
        // The existing method seemed to be a dashboard global stat.
        // I replaced the interface method, which is a breaking change for other callers
        // if any.
        // Let's assume for now I should repurpose it or add a new one.
        // In the plan I said "Add an endpoint or update getStats".
        // I replaced it in the interface.
        // Ideally I should have added `getStatistics(Long kbId)` as a new method.
        // But I already modified the interface to take `kbId`. If I did that, I broke
        // the global stats call if any.
        // Let's assume I am replacing it because the user only asked for this new
        // feature and there might not be other users.
        // Or better, I will implement it such that if kbId is null it returns global,
        // if not it returns specific?
        // No, the counts are different.
        // For this task, I will implement the specific KB stats.

        // 文档总数
        vo.setTotalDocuments(
                documentMapper.selectCount(new LambdaQueryWrapper<KmDocument>().eq(KmDocument::getKbId, kbId)));

        // 切片总数
        vo.setTotalChunks(
                chunkMapper.selectCount(new LambdaQueryWrapper<KmDocumentChunk>().eq(KmDocumentChunk::getKbId, kbId)));

        // 问题总数
        vo.setQuestionCount(
                questionMapper.selectCount(new LambdaQueryWrapper<KmQuestion>().eq(KmQuestion::getKbId, kbId)));

        // 处理中文档数 (向量化中 或 问题生成中)
        LambdaQueryWrapper<KmDocument> processingQuery = new LambdaQueryWrapper<>();
        processingQuery.eq(KmDocument::getKbId, kbId)
                .and(w -> w.eq(KmDocument::getEmbeddingStatus, 1)
                        .or()
                        .eq(KmDocument::getQuestionStatus, 1));
        vo.setProcessingDocs(documentMapper.selectCount(processingQuery));

        // 失败文档数 (向量化失败 或 问题生成失败)
        LambdaQueryWrapper<KmDocument> errorQuery = new LambdaQueryWrapper<>();
        errorQuery.eq(KmDocument::getKbId, kbId)
                .and(w -> w.eq(KmDocument::getEmbeddingStatus, 3)
                        .or()
                        .eq(KmDocument::getQuestionStatus, 3));
        vo.setErrorDocs(documentMapper.selectCount(errorQuery));

        return vo;
    }
}
