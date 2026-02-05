package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmDocumentBo;
import org.dromara.ai.domain.enums.EmbeddingOption;
import org.dromara.ai.domain.vo.KmDocumentVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
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

    /**
     * 创建在线文档
     *
     * @param datasetId 数据集ID
     * @param title     文档标题
     * @param content   文档内容 (富文本HTML)
     * @return 文档信息
     */
    KmDocumentVo createOnlineDocument(Long datasetId, String title, String content);

    /**
     * 创建网页链接文档
     *
     * @param datasetId 数据集ID
     * @param url       网页URL
     * @return 文档信息
     */
    KmDocumentVo createWebLinkDocument(Long datasetId, String url);

    /**
     * 分页查询文档列表
     *
     * @param query     查询参数
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<KmDocumentVo> pageList(KmDocumentBo bo, PageQuery pageQuery);

    /**
     * 启用/禁用文档
     *
     * @param id      文档ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    Boolean enableDocument(Long id, boolean enabled);

    /**
     * 批量启用/禁用文档
     *
     * @param ids     文档ID列表
     * @param enabled 是否启用
     * @return 是否成功
     */
    Boolean batchEnable(List<Long> ids, boolean enabled);

    /**
     * 批量删除文档
     *
     * @param ids 文档ID列表
     * @return 是否成功
     */
    Boolean batchDelete(List<Long> ids);

    /**
     * 更新文档名称
     *
     * @param id   文档ID
     * @param name 新名称
     * @return 是否成功
     */
    Boolean updateDocumentName(Long id, String name);

    /**
     * 批量向量化生成
     *
     * @param documentIds 文档ID列表
     * @param option      向量化选项
     * @return 是否成功
     */
    Boolean batchEmbedding(List<Long> documentIds, EmbeddingOption option);

    /**
     * 批量问题生成
     *
     * @param documentIds 文档ID列表
     * @param modelId     模型ID（可选）
     * @param prompt      提示词 (可选)
     * @param temperature 温度 (可选)
     * @param maxTokens   最大token (可选)
     * @return 是否成功
     */
    Boolean batchGenerateQuestions(List<Long> documentIds, Long modelId, String prompt, Double temperature,
            Integer maxTokens);

    /**
     * 单个文档向量化
     *
     * @param documentId 文档ID
     * @param option     向量化选项
     * @return 是否成功
     */
    Boolean embeddingDocument(Long documentId, EmbeddingOption option);
}
