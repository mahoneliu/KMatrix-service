package org.dromara.ai.storage.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.api.enums.FileStoreType;
import org.dromara.ai.storage.config.KmAiStorageProperties;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.domain.dto.KmFileResult;
import org.dromara.ai.storage.domain.vo.LocalFileVo;
import org.dromara.ai.storage.mapper.KmTempFileMapper;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.ai.storage.service.ILocalFileService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.oss.core.OssClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 底层文件存储服务实现
 * 根据配置自动路由到 OSS 或本地存储
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmFileServiceImpl implements IKmFileService {

    private final ISysOssService ossService;
    private final ILocalFileService localFileService;
    private final KmAiStorageProperties storageProperties;
    private final KmTempFileMapper tempFileMapper;

    @Override
    public KmFileResult upload(MultipartFile file) {
        try {
            String hash = DigestUtil.sha256Hex(file.getInputStream());
            FileStoreType storeType = FileStoreType.fromValue(storageProperties.getType());

            KmFileResult.KmFileResultBuilder builder = KmFileResult.builder()
                    .originalFilename(file.getOriginalFilename())
                    .fileExtension(FileUtil.extName(file.getOriginalFilename()))
                    .fileSize(file.getSize())
                    .hashCode(hash)
                    .storeType(storeType.getValue());

            if (storeType.isOss()) {
                SysOssVo ossVo = ossService.upload(file);
                builder.ossId(ossVo.getOssId()).filePath(ossVo.getUrl()).url(ossVo.getUrl());
            } else {
                LocalFileVo localFileVo = localFileService.upload(file);
                builder.filePath(localFileVo.getFilePath()).url(localFileVo.getUrl());
            }

            return builder.build();
        } catch (IOException e) {
            log.error("Failed to upload multipart file", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public KmFileResult upload(File file) {
        String hash = DigestUtil.sha256Hex(file);
        FileStoreType storeType = FileStoreType.fromValue(storageProperties.getType());

        KmFileResult.KmFileResultBuilder builder = KmFileResult.builder()
                .originalFilename(file.getName())
                .fileExtension(FileUtil.extName(file.getName()))
                .fileSize(file.length())
                .hashCode(hash)
                .storeType(storeType.getValue());

        if (storeType.isOss()) {
            SysOssVo ossVo = ossService.upload(file);
            builder.ossId(ossVo.getOssId()).filePath(ossVo.getUrl()).url(ossVo.getUrl());
        } else {
            LocalFileVo localFileVo = localFileService.upload(file);
            builder.filePath(localFileVo.getFilePath()).url(localFileVo.getUrl());
        }

        return builder.build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KmTempFile saveTempFile(Long datasetId, MultipartFile file) {
        KmFileResult result = upload(file);

        KmTempFile tempFile = new KmTempFile();
        tempFile.setDatasetId(datasetId);
        tempFile.setOriginalFilename(result.getOriginalFilename());
        tempFile.setFileExtension(result.getFileExtension());
        tempFile.setFileSize(result.getFileSize());
        tempFile.setFilePath(result.getFilePath());
        tempFile.setUrl(result.getUrl());
        tempFile.setOssId(result.getOssId());
        tempFile.setStoreType(result.getStoreType());
        tempFile.setHashCode(result.getHashCode());
        tempFile.setCreateTime(new Date());
        tempFile.setExpireTime(Date.from(LocalDateTime.now().plusHours(24)
                .atZone(ZoneId.systemDefault()).toInstant()));

        tempFileMapper.insert(tempFile);
        return tempFile;
    }

    @Override
    public KmTempFile getTempFile(Long id) {
        return tempFileMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredTempFiles() {
        LambdaQueryWrapper<KmTempFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(KmTempFile::getExpireTime, new Date());
        List<KmTempFile> expiredFiles = tempFileMapper.selectList(wrapper);

        log.info("Cleaning {} expired temp files", expiredFiles.size());

        for (KmTempFile tempFile : expiredFiles) {
            try {
                deleteFile(tempFile.getStoreType(), tempFile.getOssId(), tempFile.getFilePath());
                tempFileMapper.deleteById(tempFile.getId());
            } catch (Exception e) {
                log.error("Failed to clean temp file: {}", tempFile.getId(), e);
            }
        }
    }

    @Override
    public void deleteFile(Integer storeType, Long ossId, String filePath) {
        if (storeType == null) {
            return;
        }
        FileStoreType type = FileStoreType.fromValue(storeType);
        try {
            if (type.isLocal()) {
                if (StringUtils.isNotBlank(filePath)) {
                    localFileService.delete(filePath);
                }
            } else if (type.isOss()) {
                if (ossId != null) {
                    ossService.deleteWithValidByIds(List.of(ossId), true);
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete physical file: {}", filePath, e);
        }
    }

    @Override
    public void download(Integer storeType, Long ossId, String filePath, String originalFilename, HttpServletResponse response) {
        if (storeType == null) {
            throw new RuntimeException("存储类型非法");
        }
        FileStoreType type = FileStoreType.fromValue(storeType);
        try {
            if (type.isOss()) {
                if (ossId != null) {
                    ossService.download(ossId, response);
                } else {
                    throw new RuntimeException("OSS文件ID丢失");
                }
            } else if (type.isLocal()) {
                if (StringUtils.isBlank(filePath)) {
                    throw new RuntimeException("文件路径丢失");
                }
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                String fileName = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                try (InputStream is = localFileService.getFileStream(filePath);
                     OutputStream os = response.getOutputStream()) {
                    if (is == null) {
                        throw new RuntimeException("文件流获取失败");
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                throw new RuntimeException("不支持的存储类型: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to download file: {}", filePath, e);
            throw new RuntimeException("下载失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getFileStream(Integer storeType, Long ossId, String filePath) {
        if (storeType == null) {
            throw new RuntimeException("存储类型非法");
        }
        FileStoreType type = FileStoreType.fromValue(storeType);
        try {
            if (type.isOss()) {
                if (ossId != null) {
                    SysOssVo ossVo = ossService.getById(ossId);
                    if (ossVo == null) {
                        throw new RuntimeException("OSS文件不存在: " + ossId);
                    }
                    OssClient storage = OssFactory.instance(ossVo.getService());
                    return storage.getObjectContent(ossVo.getFileName());
                } else {
                    throw new RuntimeException("OSS文件ID丢失");
                }
            } else if (type.isLocal()) {
                if (StringUtils.isBlank(filePath)) {
                    throw new RuntimeException("文件路径丢失");
                }
                return localFileService.getFileStream(filePath);
            } else {
                throw new RuntimeException("不支持的存储类型: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to get file stream: {}", filePath, e);
            throw new RuntimeException("获取文件流失败: " + e.getMessage());
        }
    }
}
