package org.dromara.ai.execution.registry.adapter;

import org.dromara.ai.execution.registry.adapter.dto.McpRegistryEntryDTO;
import org.dromara.ai.execution.registry.domain.KmMcpRegistrySource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 注册源适配器接口
 * <p>
 * 每个外部数据源（官方注册源、Smithery 等）实现一个适配器，
 * 负责将外部 API 响应统一映射为 {@link McpRegistryEntryDTO}。
 *
 * @author Mahone
 */
public interface RegistrySourceAdapter {

    /**
     * 返回此适配器支持的来源平台标识
     * <p>
     * 对应 {@link org.dromara.ai.execution.registry.constant.McpRegistryConstants} 中的平台常量，
     * 如 {@code "official"}、{@code "smithery"}。
     *
     * @return 平台标识字符串
     */
    String getPlatform();

    /**
     * 拉取指定注册源的全量条目列表
     * <p>
     * 实现类应处理分页迭代，将所有页的数据合并后返回。
     * 若请求失败，应包装为 {@link org.dromara.common.core.exception.ServiceException} 抛出。
     *
     * @param source 注册源配置（包含 apiBaseUrl、apiKey 等）
     * @return 全量条目 DTO 列表，不为 null
     */
    List<McpRegistryEntryDTO> fetchAll(KmMcpRegistrySource source);

    /**
     * 拉取指定时间戳之后更新的增量条目列表
     * <p>
     * 默认实现直接调用 {@link #fetchAll(KmMcpRegistrySource)}，
     * 支持增量拉取的平台可覆盖此方法以提升效率。
     *
     * @param source       注册源配置
     * @param updatedSince 增量起始时间（仅返回此时间之后更新的条目）
     * @return 增量条目 DTO 列表，不为 null
     */
    default List<McpRegistryEntryDTO> fetchSince(KmMcpRegistrySource source, LocalDateTime updatedSince) {
        return fetchAll(source);
    }

}
