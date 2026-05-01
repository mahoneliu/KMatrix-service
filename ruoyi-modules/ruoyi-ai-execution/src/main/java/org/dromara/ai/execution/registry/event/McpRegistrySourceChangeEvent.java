package org.dromara.ai.execution.registry.event;

import lombok.Getter;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;
import org.springframework.context.ApplicationEvent;

/**
 * MCP 注册源变更事件
 *
 * @author Mahone
 */
@Getter
public class McpRegistrySourceChangeEvent extends ApplicationEvent {

    private final KmMcpRegistrySource source;
    private final ChangeType type;

    public McpRegistrySourceChangeEvent(Object source, KmMcpRegistrySource registrySource, ChangeType type) {
        super(source);
        this.source = registrySource;
        this.type = type;
    }

    public enum ChangeType {
        UPDATED,
        DELETED
    }
}
