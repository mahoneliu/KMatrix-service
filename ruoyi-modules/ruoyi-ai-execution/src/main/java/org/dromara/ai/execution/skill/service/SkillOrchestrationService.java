package org.dromara.ai.execution.skill.service;

import org.dromara.ai.execution.core.IToolProvider;
import org.dromara.ai.execution.core.ToolBinding;
import org.dromara.ai.execution.domain.KmSkill;
import org.dromara.ai.execution.mapper.KmSkillMapper;
import org.dromara.ai.execution.skill.model.SkillDefinition;
import org.dromara.ai.execution.tool.util.ToolJsonSchemaUtils;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 技能编排服务
 * <p>
 * 负责解析技能定义、解析内部工具绑定、选择执行策略。
 *
 * @author KMatrix
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SkillOrchestrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KmSkillMapper skillMapper;
    
    private final ObjectProvider<IToolProvider> toolProviderProvider;

    /**
     * 将技能解析为外层 ToolBinding（包装为单个 LLM 可见函数）
     *
     * @param skillId 技能ID
     * @return 包裹后的 ToolBinding 列表
     */
    public List<ToolBinding> resolveSkillAsBinding(Long skillId) {
        List<ToolBinding> result = new ArrayList<>();

        KmSkill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            log.error("技能不存在 (DB查询失败): skillId={}", skillId);
            return result;
        }

        if (!"0".equals(skill.getStatus())) {
            log.warn("技能已停用: skillId={}, skillName={}", skillId, skill.getSkillName());
            return result;
        }

        String toolBindingsJson = skill.getToolBindings();
        if (StrUtil.isBlank(toolBindingsJson)) {
            log.warn("技能未绑定任何工具: skillId={}", skillId);
            return result;
        }

        try {
            List<Map<String, Object>> innerToolRefs = MAPPER.readValue(toolBindingsJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<ToolBinding> innerBindings = toolProviderProvider.getObject().resolveBindings(innerToolRefs);

            if (innerBindings.isEmpty()) {
                log.warn("技能内未能解析出任何可用工具: skillId={}", skillId);
                return result;
            }

            String specText = StrUtil.isNotBlank(skill.getSpec()) ? skill.getSpec()
                    : ("技能：" + skill.getSkillName());
            var skillSpec = ToolJsonSchemaUtils.buildToolSpecification(
                    skill.getSkillName(), specText, skill.getInputSchema());

            // SkillExecutor：顺序执行内部工具并汇总结果
            org.dromara.ai.execution.core.ToolExecutor skillExecutor = request -> {
                StringBuilder sb = new StringBuilder();
                for (ToolBinding inner : innerBindings) {
                    try {
                        String innerResult = inner.getExecutor().execute(request);
                        sb.append("[").append(inner.getToolName()).append("]: ")
                                .append(innerResult).append("\n");
                    } catch (Exception ex) {
                        sb.append("[").append(inner.getToolName()).append(" 执行失败]: ")
                                .append(ex.getMessage()).append("\n");
                        log.error("技能内工具执行失败: skillId={}, innerTool={}", skillId, inner.getToolName(), ex);
                    }
                }
                return sb.toString().trim();
            };

            result.add(ToolBinding.builder()
                    .toolName(skill.getSkillName())
                    .specification(skillSpec)
                    .executor(skillExecutor)
                    .type("skill")
                    .sourceId(skillId)
                    .skillWrapper(true)
                    .build());

            log.info("技能解析成功: skillName={}, 底层工具数={}", skill.getSkillName(), innerBindings.size());
        } catch (Exception e) {
            log.error("解析技能绑定失败: skillId={}", skillId, e);
        }

        return result;
    }

    /**
     * 获取技能内部的工具绑定集合（不经过 Skill 包裹）
     *
     * @param skillId 技能ID
     * @return 技能内部的工具绑定列表
     */
    public List<ToolBinding> getInnerBindings(Long skillId) {
        KmSkill skill = skillMapper.selectById(skillId);
        if (skill == null || !"0".equals(skill.getStatus()) || StrUtil.isBlank(skill.getToolBindings())) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> innerToolRefs = MAPPER.readValue(skill.getToolBindings(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return toolProviderProvider.getObject().resolveBindings(innerToolRefs);
        } catch (Exception e) {
            log.error("提取技能底层工具失败: skillId={}", skillId, e);
        }
        return new ArrayList<>();
    }

    /**
     * 解析技能定义模型
     *
     * @param skillId 技能ID
     * @return SkillDefinition，不存在返回 null
     */
    public SkillDefinition parseSkillDefinition(Long skillId) {
        KmSkill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            return null;
        }

        List<SkillDefinition.ToolRef> toolRefs = new ArrayList<>();
        if (StrUtil.isNotBlank(skill.getToolBindings())) {
            try {
                List<Map<String, Object>> refs = MAPPER.readValue(skill.getToolBindings(),
                        new TypeReference<List<Map<String, Object>>>() {
                        });
                for (Map<String, Object> ref : refs) {
                    toolRefs.add(SkillDefinition.ToolRef.builder()
                            .type((String) ref.get("type"))
                            .id(ref.get("id") instanceof Number ? ((Number) ref.get("id")).longValue() : Long.parseLong(ref.get("id").toString()))
                            .build());
                }
            } catch (Exception e) {
                log.error("解析技能工具引用失败: skillId={}", skillId, e);
            }
        }

        return SkillDefinition.builder()
                .skillId(skill.getSkillId())
                .skillName(skill.getSkillName())
                .spec(skill.getSpec())
                .inputSchema(skill.getInputSchema())
                .toolRefs(toolRefs)
                .executionMode(SkillDefinition.ExecutionMode.SEQUENTIAL)
                .build();
    }
}
