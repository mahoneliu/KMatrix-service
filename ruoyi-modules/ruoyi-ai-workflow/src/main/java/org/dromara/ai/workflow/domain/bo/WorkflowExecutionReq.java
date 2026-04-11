package org.dromara.ai.workflow.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.dromara.ai.api.domain.vo.config.AppParametersConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流执行请求
 *
 * @author Dromara
 * @date 2024
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExecutionReq {
    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * DSL 数据
     */
    private String dslData;

    /**
     * 启用执行详情
     */
    private String enableExecutionDetail;

    /**
     * 显示执行信息
     */
    private Boolean showExecutionInfo;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 临时文件 ID 列表
     */
    private java.util.List<Long> tempFileIds;

    private Long documentId;

    /**
     * appInfo 的api参数，用于传递外部上下文信息
     * 例如：{"userRole": "admin", "customField": "value"}
     * 参数将通过 appInfo.apiParameters 注入到工作流中
     */
    private Map<String, Object> apiParameters;

    /**
     * 应用参数配置定义，用于匹配和注入不同类别的参数
     */
    private AppParametersConfig parametersConfig;

    /**
     * 初始化自定义参数为空的 HashMap
     * 避免空指针异常
     */
    public void initApiParameters() {
        if (this.apiParameters == null) {
            this.apiParameters = new HashMap<>();
        }
    }
}
