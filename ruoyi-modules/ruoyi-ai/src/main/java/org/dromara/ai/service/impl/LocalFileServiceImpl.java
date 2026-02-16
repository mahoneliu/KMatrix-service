package org.dromara.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.config.KmAiProperties;
import org.dromara.ai.domain.vo.LocalFileVo;
import org.dromara.ai.service.ILocalFileService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件存储服务实现
 *
 * @author AI Assistant
 * @date 2026-02-06
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LocalFileServiceImpl implements ILocalFileService {

    private final KmAiProperties aiProperties;

    @Override
    public LocalFileVo upload(MultipartFile file) {
        try {
            // 1. 获取配置的本地存储路径
            String basePath = aiProperties.getFileStore().getLocalPath();
            if (StringUtils.isBlank(basePath)) {
                throw new ServiceException("本地文件存储路径未配置");
            }

            // 2. 创建日期子目录 (格式: yyyy/MM/dd)
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uploadDir = basePath + File.separator + dateDir;

            // 3. 确保目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new ServiceException("创建上传目录失败: " + uploadDir);
                }
            }

            // 4. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String suffix = StringUtils.substring(originalFilename,
                    originalFilename.lastIndexOf("."), originalFilename.length());
            String fileName = IdUtil.fastSimpleUUID() + suffix;
            long fileSize = file.getSize(); // 提前获取文件大小

            // 5. 保存文件
            String filePath = uploadDir + File.separator + fileName;
            File destFile = new File(filePath);
            file.transferTo(destFile);

            // 6. 构建返回对象
            LocalFileVo vo = new LocalFileVo();
            vo.setFileName(fileName);
            vo.setOriginalName(originalFilename);
            vo.setFileSuffix(suffix);
            vo.setFileSize(fileSize);

            // 相对路径 (相对于 basePath)
            String relativePath = dateDir + "/" + fileName;
            vo.setFilePath(relativePath);
            vo.setAbsolutePath(filePath);
            vo.setUrl(relativePath); // 前端显示用

            log.info("文件上传成功: {}, 大小: {} bytes", relativePath, fileSize);
            return vo;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public LocalFileVo upload(File file) {
        try {
            // 1. 获取配置的本地存储路径
            String basePath = aiProperties.getFileStore().getLocalPath();
            if (StringUtils.isBlank(basePath)) {
                throw new ServiceException("本地文件存储路径未配置");
            }

            // 2. 创建日期子目录 (格式: yyyy/MM/dd)
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String uploadDir = basePath + File.separator + dateDir;

            // 3. 确保目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new ServiceException("创建上传目录失败: " + uploadDir);
                }
            }

            // 4. 生成唯一文件名
            String originalFilename = file.getName();
            // 尝试去除timestamp前缀以获取原始文件名 (如果有)
            if (originalFilename.contains("_")) {
                originalFilename = originalFilename.substring(originalFilename.indexOf("_") + 1);
            }

            String suffix = StringUtils.substring(originalFilename,
                    originalFilename.lastIndexOf("."), originalFilename.length());
            String fileName = IdUtil.fastSimpleUUID() + suffix;
            long fileSize = file.length();

            // 5. 保存文件 (Copy)
            String filePath = uploadDir + File.separator + fileName;
            FileUtil.copy(file, new File(filePath), true);

            // 6. 构建返回对象
            LocalFileVo vo = new LocalFileVo();
            vo.setFileName(fileName);
            vo.setOriginalName(originalFilename);
            vo.setFileSuffix(suffix);
            vo.setFileSize(fileSize);

            // 相对路径 (相对于 basePath)
            String relativePath = dateDir + "/" + fileName;
            vo.setFilePath(relativePath);
            vo.setAbsolutePath(filePath);
            vo.setUrl(relativePath); // 前端显示用

            log.info("本地文件上传成功: {}, 大小: {} bytes", relativePath, fileSize);
            return vo;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getFileStream(String filePath) {
        try {
            // 验证路径安全性
            String absolutePath = getAbsolutePath(filePath);
            validatePath(absolutePath);

            File file = new File(absolutePath);
            if (!file.exists()) {
                throw new ServiceException("文件不存在: " + filePath);
            }

            return new FileInputStream(file);

        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw new ServiceException("读取文件失败: " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            // 验证路径安全性
            String absolutePath = getAbsolutePath(filePath);
            validatePath(absolutePath);

            File file = new File(absolutePath);
            if (!file.exists()) {
                log.warn("文件不存在,无需删除: {}", filePath);
                return true;
            }

            boolean deleted = file.delete();
            if (deleted) {
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件删除失败: {}", filePath);
            }
            return deleted;

        } catch (Exception e) {
            log.error("删除文件失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public String getAbsolutePath(String filePath) {
        String basePath = aiProperties.getFileStore().getLocalPath();
        if (StringUtils.isBlank(basePath)) {
            throw new ServiceException("本地文件存储路径未配置");
        }

        // 规范化路径分隔符
        filePath = filePath.replace("\\", "/");

        // 拼接绝对路径
        return basePath + File.separator + filePath;
    }

    /**
     * 验证路径安全性,防止路径遍历攻击
     *
     * @param absolutePath 绝对路径
     */
    private void validatePath(String absolutePath) {
        try {
            String basePath = aiProperties.getFileStore().getLocalPath();
            Path basePathNormalized = Paths.get(basePath).toRealPath();
            Path filePathNormalized = Paths.get(absolutePath).toRealPath();

            // 检查文件路径是否在基础路径下
            if (!filePathNormalized.startsWith(basePathNormalized)) {
                throw new ServiceException("非法的文件路径访问");
            }
        } catch (IOException e) {
            throw new ServiceException("路径验证失败: " + e.getMessage());
        }
    }
}
