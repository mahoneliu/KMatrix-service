package org.dromara.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDataset;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.KmEmbedding;
import org.dromara.ai.domain.bo.KmDocumentBo;
import org.dromara.ai.domain.enums.EmbeddingOption;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.ai.util.StatusMetaUtils;
import org.dromara.ai.mapper.KmDatasetMapper;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.service.IKmDocumentChunkService;
import org.dromara.ai.service.IKmDocumentService;
import org.dromara.ai.service.IKmEtlService;
import org.dromara.ai.service.IKmQuestionService;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.domain.vo.SysOssVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档服务实现
 *
 * @author Mahone
 * @date 2026-01-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmDocumentServiceImpl implements IKmDocumentService {

    private final KmDocumentMapper documentMapper;
    private final KmDatasetMapper datasetMapper;
    private final org.dromara.ai.mapper.KmEmbeddingMapper embeddingMapper;
    private final ISysOssService ossService;
    private final IKmEtlService etlService;
    private final IKmDocumentChunkService chunkService;
    private final IKmQuestionService questionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo uploadDocument(Long datasetId, MultipartFile file) {
        try {
            // 1. 计算文件哈希 (用于去重)
            String hash = DigestUtil.sha256Hex(file.getInputStream());

            // 2. 检查是否已存在相同哈希的文档
            KmDocument existingDoc = documentMapper.selectOne(
                    new LambdaQueryWrapper<KmDocument>()
                            .eq(KmDocument::getDatasetId, datasetId)
                            .eq(KmDocument::getHashCode, hash));
            if (existingDoc != null) {
                log.info("Document already exists with hash: {}", hash);
                return documentMapper.selectVoById(existingDoc.getId());
            }

            // 3. 上传文件到 OSS
            SysOssVo ossVo = ossService.upload(file);

            // 3.5 获取知识库ID
            KmDataset dataset = datasetMapper.selectById(datasetId);
            if (dataset == null) {
                throw new RuntimeException("数据集不存在");
            }

            // 4. 创建文档记录
            KmDocument document = new KmDocument();
            document.setDatasetId(datasetId);
            document.setKbId(dataset.getKbId()); // 设置知识库ID
            document.setOriginalFilename(file.getOriginalFilename());
            document.setOssId(ossVo.getOssId()); // 存储 OSS ID
            document.setFilePath(ossVo.getUrl()); // 存储访问 URL
            document.setFileType(FileUtil.extName(file.getOriginalFilename()));
            document.setFileSize(file.getSize());
            document.setFileSize(file.getSize());
            document.setEmbeddingStatus(1); // 1 = 生成中
            document.setStatusMeta(StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING,
                    StatusMetaUtils.STATUS_PENDING));
            document.setHashCode(hash);
            document.setHashCode(hash);

            documentMapper.insert(document);

            // 5. 异步触发 ETL 处理 (确保事务提交后再执行,避免异步线程查不到数据)
            Long docId = document.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    etlService.processDocumentAsync(docId);
                }
            });

            return documentMapper.selectVoById(document.getId());
        } catch (IOException e) {
            log.error("Failed to upload document", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KmDocumentVo> uploadDocuments(Long datasetId, MultipartFile[] files) {
        List<KmDocumentVo> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadDocument(datasetId, file));
        }
        return results;
    }

    @Override
    public List<KmDocumentVo> listByDatasetId(Long datasetId) {
        LambdaQueryWrapper<KmDocument> lqw = new LambdaQueryWrapper<>();
        lqw.eq(KmDocument::getDatasetId, datasetId);
        lqw.orderByDesc(KmDocument::getCreateTime);
        return documentMapper.selectVoList(lqw);
    }

    @Override
    public KmDocumentVo queryById(Long id) {
        return documentMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        // 删除向量切片
        etlService.deleteChunksByDocumentId(id);
        // 删除文档记录
        return documentMapper.deleteById(id) > 0;
    }

    @Override
    public Boolean reprocessDocument(Long id) {
        // 更新状态为 PENDING
        KmDocument doc = new KmDocument();
        doc.setId(id);
        doc.setEmbeddingStatus(1); // 1 = 生成中

        // 更新状态元数据
        KmDocument exist = documentMapper.selectById(id);
        Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
        doc.setStatusMeta(
                StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));
        doc.setErrorMsg(null);
        documentMapper.updateById(doc);

        // 删除旧的切片
        etlService.deleteChunksByDocumentId(id);

        // 重新触发 ETL
        etlService.processDocumentAsync(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo createOnlineDocument(Long datasetId, String title, String content) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException("数据集不存在");
        }

        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setTitle(title);
        document.setContent(content);
        document.setOriginalFilename(title); // 使用 title 作为文件名
        document.setFileType("html");
        document.setFileType("html");
        document.setEmbeddingStatus(1); // 1 = 生成中
        document.setStatusMeta(
                StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));

        documentMapper.insert(document);

        // 2. 异步触发 ETL 处理
        Long docId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processDocumentAsync(docId);
            }
        });

        return documentMapper.selectVoById(document.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmDocumentVo createWebLinkDocument(Long datasetId, String url) {
        // 0. 获取知识库ID
        KmDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new RuntimeException("数据集不存在");
        }

        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setKbId(dataset.getKbId()); // 设置知识库ID
        document.setUrl(url);
        document.setOriginalFilename(url); // 使用 URL 作为文件名
        document.setFileType("url");
        document.setFileType("url");
        document.setEmbeddingStatus(1); // 1 = 生成中
        document.setStatusMeta(
                StatusMetaUtils.updateStateTime(null, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_PENDING));

        documentMapper.insert(document);

        // 2. 异步触发 ETL 处理
        Long docId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processDocumentAsync(docId);
            }
        });

        return documentMapper.selectVoById(document.getId());
    }

    @Override
    public TableDataInfo<KmDocumentVo> pageList(KmDocumentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KmDocument> lqw = new LambdaQueryWrapper<>();
        // 必填条件: 数据集ID
        lqw.eq(bo.getDatasetId() != null, KmDocument::getDatasetId, bo.getDatasetId());
        // 可选筛选条件
        lqw.eq(bo.getEnabled() != null, KmDocument::getEnabled, bo.getEnabled());
        lqw.eq(bo.getEmbeddingStatus() != null, KmDocument::getEmbeddingStatus, bo.getEmbeddingStatus());
        lqw.eq(bo.getQuestionStatus() != null, KmDocument::getQuestionStatus, bo.getQuestionStatus());
        // 关键词搜索
        lqw.like(StringUtils.isNotBlank(bo.getKeyword()), KmDocument::getOriginalFilename, bo.getKeyword());

        Page<KmDocumentVo> page = documentMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableDocument(Long id, boolean enabled) {
        KmDocument document = new KmDocument();
        document.setId(id);
        document.setEnabled(enabled ? 1 : 0);
        return documentMapper.updateById(document) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEnable(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        KmDocument document = new KmDocument();
        document.setEnabled(enabled ? 1 : 0);
        return documentMapper.update(document,
                new LambdaQueryWrapper<KmDocument>().in(KmDocument::getId, ids)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 先删除关联的切片
        for (Long id : ids) {
            etlService.deleteChunksByDocumentId(id);
        }
        // 再批量删除文档
        return documentMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public Boolean updateDocumentName(Long id, String name) {
        if (id == null || StringUtils.isBlank(name)) {
            return false;
        }
        KmDocument document = new KmDocument();
        document.setId(id);
        document.setOriginalFilename(name);
        return documentMapper.updateById(document) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchEmbedding(List<Long> documentIds, EmbeddingOption option) {
        if (documentIds == null || documentIds.isEmpty()) {
            return false;
        }
        // 遍历每个文档,重新触发向量化流程
        for (Long documentId : documentIds) {
            embeddingDocument(documentId, option);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean embeddingDocument(Long documentId, EmbeddingOption option) {
        KmDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            return false;
        }
        // 更新向量化状态为"生成中"
        KmDocument update = new KmDocument();
        update.setId(documentId);
        update.setEmbeddingStatus(1); // 1 = 生成中
        update.setEmbeddingStatus(1); // 1 = 生成中

        // 更新状态元数据
        Map<String, Object> meta = doc.getStatusMeta();
        update.setStatusMeta(
                StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_EMBEDDING, StatusMetaUtils.STATUS_STARTED));

        documentMapper.updateById(update);

        // 异步执行向量化 (事务提交后执行)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                etlService.processEmbeddingAsync(documentId, option);
            }
        });
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchGenerateQuestions(List<Long> documentIds, Long modelId, String prompt, Double temperature,
            Integer maxTokens) {
        if (documentIds == null || documentIds.isEmpty()) {
            return false;
        }
        // 遍历每个文档，为其所有切片生成问题
        for (Long documentId : documentIds) {
            // 更新问题生成状态为"生成中"
            KmDocument update = new KmDocument();
            update.setId(documentId);
            update.setQuestionStatus(1); // 1 = 生成中
            update.setQuestionStatus(1); // 1 = 生成中

            // 更新状态元数据
            KmDocument exist = documentMapper.selectById(documentId);
            Map<String, Object> meta = exist != null ? exist.getStatusMeta() : null;
            update.setStatusMeta(StatusMetaUtils.updateStateTime(meta, StatusMetaUtils.TASK_GENERATE_QUESTION,
                    StatusMetaUtils.STATUS_STARTED));

            documentMapper.updateById(update);

            // 异步生成问题 (事务提交后执行)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    questionService.processGenerateQuestionsAsync(documentId, modelId, prompt, temperature, maxTokens);
                }
            });
        }
        return true;
    }
}
