package org.dromara.ai.storage.service;

import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;

import java.io.File;

/**
 * 底层文件存储服务
 * 统一封装 OSS 和本地存储，对上层业务透明
 *
 * @author Mahone
 * @date 2026-03-28
 */
public interface IKmFileService {

    /**
     * 上传文件（由配置决定存储位置）
     *
     * @param file 页面上传文件
     * @return 统一结果
     */
    KmFileResult upload(MultipartFile file);

    /**
     * 上传本地文件
     *
     * @param file 本地文件
     * @return 统一结果
     */
    KmFileResult upload(File file);

    /**
     * 保存为临时文件并记录到 km_temp_file
     *
     * @param datasetId 数据集ID (可选，聊天附件场景传 null)
     * @param file      页面上传文件
     * @return 临时文件记录
     */
    KmTempFile saveTempFile(Long datasetId, MultipartFile file);

    /**
     * 查询临时文件
     *
     * @param id 临时文件ID
     * @return 临时文件记录，不存在则返回 null
     */
    KmTempFile getTempFile(Long id);

    /**
     * 清理过期临时文件（物理删除 + DB 删除）
     */
    void cleanExpiredTempFiles();

    /**
     * 删除物理文件
     *
     * @param storeType 存储类型
     * @param ossId     OSS ID
     * @param filePath  文件路径
     */
    void deleteFile(Integer storeType, Long ossId, String filePath);

    /**
     * 下载文件
     *
     * @param storeType        存储类型
     * @param ossId            OSS ID
     * @param filePath         文件路径
     * @param originalFilename 原始文件名
     * @param response         响应对象
     */
    void download(Integer storeType, Long ossId, String filePath, String originalFilename, HttpServletResponse response);

    /**
     * 获取文件输入流
     *
     * @param storeType 存储类型
     * @param ossId     OSS ID
     * @param filePath  文件路径
     * @return 输入流
     */
    InputStream getFileStream(Integer storeType, Long ossId, String filePath);
}
