package org.dromara.ai.workflow.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.ai.workflow.core.NodeContext;
import org.dromara.ai.workflow.core.NodeOutput;
import org.dromara.ai.workflow.core.WorkflowState;
import org.dromara.ai.workflow.nodes.condition.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.dromara.ai.workflow.nodes.LoopNode.*;

/**
 * LoopNode 单元测试
 * 覆盖条件判断、最大迭代数保护、路由键输出等核心逻辑。
 *
 * @author Mahone
 */
@Tag("local")
@ExtendWith(MockitoExtension.class)
@DisplayName("LoopNode 循环节点逻辑测试")
class LoopNodeTest {

    // ConditionEvaluator 是纯业务逻辑类，注入真实实例即可
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConditionEvaluator conditionEvaluator;

    private LoopNode loopNode;

    @BeforeEach
    void setUp() {
        loopNode = new LoopNode(conditionEvaluator, objectMapper);
    }

    // ========== 辅助方法 ==========

    /**
     * 构造一个简单的条件组：globalState.counter > compareValue
     */
    private ConditionGroup buildCounterCondition(String operator, Object compareValue) {
        VariableRef variable = new VariableRef();
        variable.setSourceType("global");
        variable.setSourceKey("counter");
        variable.setSourceParam("counter");

        ConditionRule rule = new ConditionRule();
        rule.setVariable(variable);
        rule.setOperator(operator);
        rule.setCompareValue(compareValue);
        rule.setCompareValueType("static");

        List<Object> conditions = new ArrayList<>();
        conditions.add(rule);

        ConditionGroup group = new ConditionGroup();
        group.setLogicalOperator("AND");
        group.setConditions(conditions);
        return group;
    }

    /**
     * 构造测试用 NodeContext
     */
    private NodeContext buildContext(String nodeId, int iterationInState, Map<String, Object> extraGlobalState, Map<String, Object> config) {
        NodeContext ctx = new NodeContext();
        ctx.setNodeId(nodeId);

        Map<String, Object> globalState = new HashMap<>();
        if (iterationInState > 0) {
            globalState.put("LOOP_ITERATIONS_" + nodeId, iterationInState);
        }
        if (extraGlobalState != null) {
            globalState.putAll(extraGlobalState);
        }
        ctx.setGlobalState(globalState);
        ctx.setNodeConfig(config != null ? config : new HashMap<>());
        ctx.setAllNodeOutputs(new HashMap<>());
        return ctx;
    }

    // ========== 测试用例 ==========

    @Test
    @DisplayName("条件满足时应路由到 continue")
    void shouldRouteContinue_whenConditionIsTrue() throws Exception {
        // 模拟 globalState 中 counter=5，条件为 counter > 3（即5>3=true，继续循环）
        Map<String, Object> globalState = new HashMap<>();
        globalState.put("counter", 5);

        Map<String, Object> config = new HashMap<>();
        config.put("condition", buildCounterCondition("gt", "3"));
        config.put("maxIterations", 10);

        NodeContext ctx = buildContext("loopNode1", 0, globalState, config);

        NodeOutput output = loopNode.execute(ctx);

        assertThat(output.getOutputs().get("routeKey")).isEqualTo(ROUTE_CONTINUE);
        assertThat(output.getOutputs().get(KEY_ITERATION_COUNT)).isEqualTo(1);
    }

    @Test
    @DisplayName("条件不满足时应路由到 exit")
    void shouldRouteExit_whenConditionIsFalse() throws Exception {
        // counter=2，条件为 counter > 3（即2>3=false，退出循环）
        Map<String, Object> globalState = new HashMap<>();
        globalState.put("counter", 2);

        Map<String, Object> config = new HashMap<>();
        config.put("condition", buildCounterCondition("gt", "3"));
        config.put("maxIterations", 10);

        NodeContext ctx = buildContext("loopNode1", 0, globalState, config);

        NodeOutput output = loopNode.execute(ctx);

        assertThat(output.getOutputs().get("routeKey")).isEqualTo(ROUTE_EXIT);
    }

    @Test
    @DisplayName("超过 maxIterations 应抛出 RuntimeException")
    void shouldThrowException_whenMaxIterationsExceeded() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxIterations", 3);
        // 没有条件，使 continueLoop = false 后  --  但迭代次数已超过 maxIterations

        // 模拟已迭代 3 次（达到上限）
        NodeContext ctx = buildContext("loopNode2", 3, null, config);

        assertThatThrownBy(() -> loopNode.execute(ctx))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("最大循环迭代次数");
    }

    @Test
    @DisplayName("无条件配置时应直接退出（路由到 exit）")
    void shouldRouteExit_whenNoConditionConfigured() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("maxIterations", 10);
        // 不添加 condition 配置

        NodeContext ctx = buildContext("loopNode3", 0, null, config);

        NodeOutput output = loopNode.execute(ctx);

        assertThat(output.getOutputs().get("routeKey")).isEqualTo(ROUTE_EXIT);
    }

    @Test
    @DisplayName("退出循环后应清除迭代计数器")
    void shouldClearIterationCount_whenLoopExits() throws Exception {
        // 条件不满足，路由到 exit，应清除迭代计数
        Map<String, Object> globalState = new HashMap<>();
        globalState.put("counter", 1);

        Map<String, Object> config = new HashMap<>();
        config.put("condition", buildCounterCondition("gt", "100")); // 1>100=false，退出
        config.put("maxIterations", 10);

        NodeContext ctx = buildContext("loopNode4", 2, globalState, config);

        loopNode.execute(ctx);

        // 迭代计数器应已从全局状态中移除
        assertThat(ctx.getGlobalState()).doesNotContainKey("LOOP_ITERATIONS_loopNode4");
    }

    @Test
    @DisplayName("未设置 maxIterations 时应使用默认值 50")
    void shouldUseDefaultMaxIterations_whenNotConfigured() throws Exception {
        // 模拟第 50 次迭代（恰好到上限）
        Map<String, Object> config = new HashMap<>();
        // 不设置 maxIterations，默认 50

        NodeContext ctx = buildContext("loopNode5", 50, null, config);

        // 第 50+1=51 次 > 50，应抛出异常
        assertThatThrownBy(() -> loopNode.execute(ctx))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("最大循环迭代次数");
    }

    @Test
    @DisplayName("iterationCount 应在每次执行后正确递增")
    void shouldIncrementIterationCount_onEachExecution() throws Exception {
        Map<String, Object> globalState = new HashMap<>();
        globalState.put("counter", 10);

        Map<String, Object> config = new HashMap<>();
        config.put("condition", buildCounterCondition("gt", "5")); // 10>5=true，继续循环
        config.put("maxIterations", 20);

        // 模拟已迭代 4 次
        NodeContext ctx = buildContext("loopNode6", 4, globalState, config);

        NodeOutput output = loopNode.execute(ctx);

        assertThat(output.getOutputs().get(KEY_ITERATION_COUNT)).isEqualTo(5);
        assertThat(ctx.getGlobalState().get("LOOP_ITERATIONS_loopNode6")).isEqualTo(5);
    }
}
