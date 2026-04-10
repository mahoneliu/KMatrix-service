package org.dromara.ai.app.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.ai.app.domain.KmChatMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * 发送消息业务对象 km_chat_message
 *
 * @author Mahone
 * @date 2025-12-31
 */
@Data
@AutoMapper(target = KmChatMessage.class, reverseConvertGenerate = false)
public class KmChatSendBo {

    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    /**
     * 会话ID(可选,首次对话时为空)
     */
    private Long sessionId;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "{ai.val.chat.message_content_required}")
    private String message;

    /**
     * 是否流式返回
     */
    private Boolean stream = true;

    /**
     * 是否为调试模式
     * 调试模式下，后端会实时从数据库获取最新的草稿DSL进行测试
     * 调试数据不会保存到数据库
     */
    private Boolean debug = false;

    /**
     * 是否显示执行信息（正式对话模式下可选）
     * 即使在正式对话模式下，用户也可以打开此开关来查看执行详情
     * 与debug不同的是，showExecutionInfo模式下执行详情仍然入库
     */
    private Boolean showExecutionInfo = false;

    /**
     * 用户ID（匿名用户认证时从 Session Token 解析后设置）
     */
    private Long userId;

    /**
     * 用户类型 (anonymous_user/system_user/third_user)
     */
    private String userType;

    /**
     * 附件临时文件 ID 列表（多模态聊天时携带，对应 km_temp_file.id）
     * 前端先调用 /ai/admin/chat/attachment/upload 上传，再把返回的 id 填入此字段
     */
    private java.util.List<Long> tempFileIds;

    /**
     * 请求ID（前端生成的唯一标识，用于中断请求时追踪）
     */
    private String requestId;

    /**
     * 自定义参数，用于传递外部上下文信息
     * 例如：{"userId": 1234, "userName": "Alice", "customField": "value"}
     * 参数将通过 globalState 注入到工作流中
     */
    private Map<String, Object> customParams;

    /**
     * 初始化自定义参数为空的 HashMap
     * 避免空指针异常
     */
    public void initCustomParams() {
        if (this.customParams == null) {
            this.customParams = new HashMap<>();
        }
    }
}

