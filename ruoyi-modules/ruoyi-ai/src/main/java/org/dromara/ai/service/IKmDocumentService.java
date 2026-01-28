package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmDocumentVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 *
 * @author Mahone
 * @date 2026-01-28
 */
public interface IKmDocumentService {

    /**
     * 上传文档到数据集
     *
     * @param datasetId 数据集ID
     * @param file      文件
     * @return 文档信息
     */
    KmDocumentVo uploadDocument(Long datasetId, MultipartFile file);

    /**
     * 批量上传文档到数据集
     *
     * @param datasetId 数据集ID
     * @param files     文件列表
     * @return 文档信息列表
     */
    List<KmDocumentVo> uploadDocuments(Long datasetId, MultipartFile[] files);

    /**
     * 查询数据集下的文档列表
     *
     * @param datasetId 数据集ID
     * @return 文档列表
     */
    List<KmDocumentVo> listByDatasetId(Long datasetId);

    /**
     * 查询文档详情
     *
     * @param id 文档ID
     * @return 文档信息
     */
    KmDocumentVo queryById(Long id);

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否成功
     */
    Boolean deleteById(Long id);

    /**
     * 重新处理文档 (触发ETL)
     *
     * @param id 文档ID
     * @return 是否成功
     */
    Boolean reprocessDocument(Long id);
}
