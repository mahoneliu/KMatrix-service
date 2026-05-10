package org.dromara.ai.blog.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.blog.mapper.KmBlogArticleMapper;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 博客文章浏览量定时回写任务
 * <p>
 * 每 5 分钟将 Redis 中的浏览量增量（blog:view:{articleId}）批量回写到数据库。
 *
 * @author KMatrix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogViewCountFlushJob {

    private static final String VIEW_KEY_PREFIX = "blog:view:";

    private final KmBlogArticleMapper articleMapper;
    private final RedissonClient redissonClient;

    @Scheduled(fixedDelay = 300_000)
    public void execute() {
        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> matchedKeys = keys.getKeysByPattern(VIEW_KEY_PREFIX + "*");

            int flushed = 0;
            for (String key : matchedKeys) {
                try {
                    Long delta = redissonClient.<Long>getBucket(key).getAndDelete();
                    if (delta != null && delta > 0) {
                        Long articleId = Long.parseLong(key.substring(VIEW_KEY_PREFIX.length()));
                        articleMapper.incrementViewCount(articleId, delta);
                        flushed++;
                    }
                } catch (Exception e) {
                    log.warn("[BlogViewCountFlushJob] 处理 key={} 失败: {}", key, e.getMessage());
                }
            }

            if (flushed > 0) {
                log.info("[BlogViewCountFlushJob] 浏览量回写完成，共更新 {} 篇文章", flushed);
            }
        } catch (Exception e) {
            log.error("[BlogViewCountFlushJob] 执行失败", e);
        }
    }
}
