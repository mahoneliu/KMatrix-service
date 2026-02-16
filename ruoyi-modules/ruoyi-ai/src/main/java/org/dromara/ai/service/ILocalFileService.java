package org.dromara.ai.service;

import org.dromara.ai.domain.vo.LocalFileVo;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.InputStream;

/**
 * 本地文件存储服务接口
 *
 * @author AI Assistant
 * @date 2026-02-06
 */
public interface ILocalFileService {

    /**
     * 上传文件到本地存储
     *
     * @param file 要上传的文件
     * @return 本地文件信息
     */
    LocalFileVo upload(MultipartFile file);

    /**
     * 上传文件到本地存储
     *
     * @param file 要上传的文件
     * @return 本地文件信息
     */
    LocalFileVo upload(File file);

    /**
     * 获取文件输入流
     *
     * @param filePath 文件相对路径
     * @return 文件输入流
     */
    InputStream getFileStream(String filePath);

    /**
     * 删除本地文件
     *
     * @param filePath 文件相对路径
     * @return 是否删除成功
     */
    boolean delete(String filePath);

    /**
     * 获取文件绝对路径
     *
     * @param filePath 文件相对路径
     * @return 文件绝对路径
     */
    String getAbsolutePath(String filePath);
}
