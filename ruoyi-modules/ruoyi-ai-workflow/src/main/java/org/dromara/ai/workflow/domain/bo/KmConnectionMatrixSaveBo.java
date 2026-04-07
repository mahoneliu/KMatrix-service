package org.dromara.ai.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 矩阵视图批量保存业务对象
 * <p>
 * 采用"出边全量替换"策略：前端提交当前节点的所有出边勾选状态，
 * 后端删除该 sourceNodeType 的所有出边规则后重建。
 * 入边（targetNodeType=sourceType）由对应源节点自己管理，不在此处处理。
 *
 * @author Mahone
 */
@Data
public class KmConnectionMatrixSaveBo {

    /** 当前选中的源节点类型 */
    @NotBlank(message = "源节点类型不能为空")
    private String sourceNodeType;

    /** 可连出的目标节点列表（勾选的） */
    private List<String> outboundTargets;

    /**
     * 规则类型（由前端根据当前模式自动填充）：
     * 白名单模式 = "0"（允许），黑名单模式 = "1"（禁止）
     */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;
}
