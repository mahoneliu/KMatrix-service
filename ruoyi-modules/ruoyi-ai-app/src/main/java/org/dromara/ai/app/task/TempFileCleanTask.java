package org.dromara.ai.app.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.storage.service.IKmFileService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 临时文件清理定时任务
 *
 * @author Mahone
 * @date 2026-02-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempFileCleanTask {

    private final IKmFileService fileService;

    /**
     * 每小时清理一次过期临时文件
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredFiles() {
        log.info("Starting temp file cleanup task");
        try {
            fileService.cleanExpiredTempFiles();
        } catch (Exception e) {
            log.error("Temp file cleanup failed", e);
        }
    }
}
