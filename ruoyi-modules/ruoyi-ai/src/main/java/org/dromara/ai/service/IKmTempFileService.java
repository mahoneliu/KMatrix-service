package org.dromara.ai.service;

import org.dromara.ai.domain.vo.TempFileVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 临时文件服务接口
 *
 * @author Mahone
 * @date 2026-02-07
 */
public interface IKmTempFileService {

    /**
     * 保存临时文件
     *
     * @param datasetId 数据集ID
     * @param file      文件
     * @return 临时文件信息
     */
    TempFileVo saveTempFile(Long datasetId, MultipartFile file);

    /**
     * 根据ID获取临时文件路径
     *
     * @param id 临时文件ID
     * @return 文件路径
     */
    String getTempFilePath(Long id);

    /**
     * 清理过期临时文件
     */
    void cleanExpiredFiles();
}
