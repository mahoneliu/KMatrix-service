package org.dromara.ai.workflow.domain.bo;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class WorkflowExecutionReq {
    private Long appId;
    private String dslData;
    private String enableExecutionDetail;
    private Boolean showExecutionInfo;
    private String message;
    private Long sessionId;
    private Long userId;
    private java.util.List<Long> tempFileIds;
}