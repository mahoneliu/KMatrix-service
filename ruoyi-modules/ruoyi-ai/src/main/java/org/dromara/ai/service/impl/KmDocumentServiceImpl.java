package org.dromara.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmDocument;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.ai.mapper.KmDocumentMapper;
import org.dromara.ai.service.IKmDocumentService;
import org.dromara.ai.service.IKmEtlService;
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
    private final ISysOssService ossService;
    private final IKmEtlService etlService;

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

            // 4. 创建文档记录
            KmDocument document = new KmDocument();
            document.setDatasetId(datasetId);
            document.setOriginalFilename(file.getOriginalFilename());
            document.setOssId(ossVo.getOssId()); // 存储 OSS ID
            document.setFilePath(ossVo.getUrl()); // 存储访问 URL
            document.setFileType(FileUtil.extName(file.getOriginalFilename()));
            document.setFileSize(file.getSize());
            document.setStatus("PENDING");
            document.setHashCode(hash);

            documentMapper.insert(document);

            // 5. 异步触发 ETL 处理 (确保事务提交后再执行，避免异步线程查不到数据)
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
        doc.setStatus("PENDING");
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
        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setTitle(title);
        document.setContent(content);
        document.setOriginalFilename(title); // 使用 title 作为文件名
        document.setFileType("html");
        document.setStatus("PENDING");

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
        // 1. 创建文档记录
        KmDocument document = new KmDocument();
        document.setDatasetId(datasetId);
        document.setUrl(url);
        document.setOriginalFilename(url); // 使用 URL 作为文件名
        document.setFileType("url");
        document.setStatus("PENDING");

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
}
