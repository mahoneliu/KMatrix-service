package org.dromara.ai.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.KmTempFile;
import org.dromara.ai.domain.vo.TempFileVo;
import org.dromara.ai.mapper.KmTempFileMapper;
import org.dromara.ai.service.IKmTempFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 临时文件服务实现
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KmTempFileServiceImpl implements IKmTempFileService {

    private final KmTempFileMapper tempFileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TempFileVo saveTempFile(Long datasetId, MultipartFile file) {
        try {
            // 1. 生成临时文件名
            String originalFilename = file.getOriginalFilename();
            String tempFileName = System.currentTimeMillis() + "_" + originalFilename;

            // 2. 获取系统临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + "/kmatrix/temp_uploads/";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 3. 保存临时文件
            // 3. 保存临时文件
            String tempPath = tempDir + tempFileName;
            File destFile = new File(tempPath);
            long fileSize = file.getSize(); // Important: Get size before transferTo
            file.transferTo(destFile);

            // 4. 创建临时文件记录
            KmTempFile tempFile = new KmTempFile();
            tempFile.setDatasetId(datasetId);
            tempFile.setOriginalFilename(originalFilename);
            tempFile.setFileExtension(FileUtil.extName(originalFilename));
            tempFile.setFileSize(fileSize);
            tempFile.setTempPath(tempPath);
            tempFile.setCreateTime(new Date());
            tempFile.setExpireTime(Date.from(LocalDateTime.now().plusHours(24)
                    .atZone(ZoneId.systemDefault()).toInstant())); // 24小时后过期

            tempFileMapper.insert(tempFile);

            // 5. 转换为VO
            TempFileVo vo = new TempFileVo();
            vo.setId(tempFile.getId());
            vo.setDatasetId(tempFile.getDatasetId());
            vo.setOriginalFilename(tempFile.getOriginalFilename());
            vo.setFileExtension(tempFile.getFileExtension());
            vo.setFileSize(tempFile.getFileSize());
            vo.setTempPath(tempFile.getTempPath());

            return vo;
        } catch (IOException e) {
            log.error("Failed to save temp file", e);
            throw new RuntimeException("临时文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public String getTempFilePath(Long id) {
        KmTempFile tempFile = tempFileMapper.selectById(id);
        return tempFile != null ? tempFile.getTempPath() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredFiles() {
        // 查询过期文件
        LambdaQueryWrapper<KmTempFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(KmTempFile::getExpireTime, new Date());
        List<KmTempFile> expiredFiles = tempFileMapper.selectList(wrapper);

        log.info("Found {} expired temp files to clean", expiredFiles.size());

        for (KmTempFile tempFile : expiredFiles) {
            try {
                // 删除物理文件
                File file = new File(tempFile.getTempPath());
                if (file.exists()) {
                    file.delete();
                    log.debug("Deleted temp file: {}", tempFile.getTempPath());
                }

                // 删除数据库记录
                tempFileMapper.deleteById(tempFile.getId());
            } catch (Exception e) {
                log.error("Failed to clean temp file: {}", tempFile.getId(), e);
            }
        }

        log.info("Cleaned {} expired temp files", expiredFiles.size());
    }
}
