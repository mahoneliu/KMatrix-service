package org.dromara.ai.execution.registry.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.ai.execution.registry.domain.KmMcpRegistryEntry;
import org.dromara.ai.execution.registry.domain.vo.McpRegistryEntryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * MCP 注册源条目缓存 Mapper 接口
 *
 * @author Mahone
 */
public interface McpRegistryEntryMapper extends BaseMapperPlus<KmMcpRegistryEntry, McpRegistryEntryVo> {

    /**
     * 全文搜索注册源条目（支持关键词 + 来源平台筛选 + 标签筛选）
     *
     * @param keyword        搜索关键词（匹配名称、显示名称、描述）
     * @param sourcePlatform 来源平台筛选（official / smithery，为空则不筛选）
     * @param tags           标签筛选（JSON 数组字符串，为空则不筛选）
     * @param offset         分页偏移量
     * @param limit          每页条数
     * @return 匹配的条目列表
     */
    List<McpRegistryEntryVo> searchEntries(@Param("keyword") String keyword,
                                           @Param("sourcePlatform") String sourcePlatform,
                                           @Param("transportType") String transportType,
                                           @Param("tags") String tags,
                                           @Param("offset") long offset,
                                           @Param("limit") int limit);

    /**
     * 统计全文搜索结果总数
     *
     * @param keyword        搜索关键词
     * @param sourcePlatform 来源平台筛选
     * @param tags           标签筛选
     * @return 匹配的条目总数
     */
    long countEntries(@Param("keyword") String keyword,
                      @Param("sourcePlatform") String sourcePlatform,
                      @Param("transportType") String transportType,
                      @Param("tags") String tags);

    /**
     * 批量 UPSERT 注册源条目（PostgreSQL ON CONFLICT 语法）
     *
     * @param entries 条目列表
     * @return 影响行数
     */
    int batchUpsert(@Param("list") List<KmMcpRegistryEntry> entries);

    /**
     * 将本次同步未出现的条目标记为 offline（软删除）
     *
     * @param sourceId        注册源 ID
     * @param activeExternalIds 本次同步中仍活跃的外部 ID 列表
     * @return 影响行数
     */
    int markOfflineBySourceId(@Param("sourceId") Long sourceId,
                              @Param("activeExternalIds") List<String> activeExternalIds);

    /**
     * 按注册源 ID 删除所有条目（用于删除注册源时级联清除）
     *
     * @param sourceId 注册源 ID
     * @return 影响行数
     */
    int deleteBySourceId(@Param("sourceId") Long sourceId);

}
