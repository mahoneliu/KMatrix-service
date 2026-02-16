package org.dromara.ai.workflow.nodes.nodeUtils;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.domain.enums.SseEventType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 事件发送工具类
 * 
 * @author Mahone
 * @date 2026-01-25
 */
@Slf4j
public final class SseHelper {

    private SseHelper() {
        // 工具类禁止实例化
    }

    /**
     * 发送 THINKING 事件
     * 
     * @param emitter      SSE 发送器
     * @param streamOutput 是否启用流式输出
     * @param message      消息内容
     */
    public static void sendThinking(SseEmitter emitter, Boolean streamOutput, String message) {
        if (Boolean.TRUE.equals(streamOutput) && emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.THINKING.getEventName())
                        .data(message));
            } catch (IOException e) {
                log.error("发送thinking事件失败", e);
            }
        }
    }

    /**
     * 发送任意 SSE 事件
     * 
     * @param emitter   SSE 发送器
     * @param eventType 事件类型
     * @param data      数据内容
     */
    public static void sendEvent(SseEmitter emitter, SseEventType eventType, Object data) {
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType.getEventName())
                        .data(data));
            } catch (IOException e) {
                log.error("发送SSE事件失败: {}", eventType.getEventName(), e);
            }
        }
    }
}
