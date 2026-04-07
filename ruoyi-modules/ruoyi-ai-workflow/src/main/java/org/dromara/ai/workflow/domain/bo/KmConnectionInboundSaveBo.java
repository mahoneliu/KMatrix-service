package org.dromara.ai.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 矩阵视图入边保存 BO
 * <p>
 * 保存"哪些节点可以连入 targetNodeType"，
 * 本质是对每个 inboundSource 维护 inboundSource -> targetNodeType 这条规则。
 *
 * @author Mahone
 */
@Data
public class KmConnectionInboundSaveBo {

    /** 目标节点类型（当前选中节点） */
    @NotBlank(message = "目标节点类型不能为空")
    private String targetNodeType;

    /** 勾选的入边来源节点列表 */
    private List<String> inboundSources;

    /** 规则类型：白名单=0，黑名单=1 */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;
}
