package org.dromara.ai.registry.domain.bo;

import lombok.Data;

/**
 * 从注册源导入 MCP Server 请求对象
 *
 * @author Mahone
 */
@Data
public class McpImportBo {

    /**
     * 注册源条目 ID
     */
    private Long entryId;

    /**
     * 自定义服务名称（可覆盖条目默认名称）
     */
    private String serverName;

    /**
     * 是否覆盖已有配置（true=覆盖，false=不覆盖）
     */
    private Boolean overwrite = false;

}
