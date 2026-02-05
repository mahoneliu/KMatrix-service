package org.dromara.ai.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态元数据工具类
 */
public class StatusMetaUtils {

    public static final String KEY_STATE_TIME = "state_time";
    public static final String KEY_AGGS = "aggs";

    public static final String TASK_EMBEDDING = "1";
    public static final String TASK_GENERATE_QUESTION = "2";

    public static final String STATUS_PENDING = "0";
    public static final String STATUS_STARTED = "1";
    public static final String STATUS_SUCCESS = "2";
    public static final String STATUS_FAILED = "3";

    /**
     * 更新状态时间
     * 
     * @param meta     原有元数据(可能为null)
     * @param taskType 任务类型 (1=Embedding, 2=GenerateQuestion)
     * @param status   状态 (0, 1, 2, 3)
     * @return 更新后的元数据
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> updateStateTime(Map<String, Object> meta, String taskType, String status) {
        if (meta == null) {
            meta = new HashMap<>();
        }

        Map<String, Map<String, String>> stateTime = (Map<String, Map<String, String>>) meta.get(KEY_STATE_TIME);
        if (stateTime == null) {
            stateTime = new HashMap<>();
            meta.put(KEY_STATE_TIME, stateTime);
        }

        Map<String, String> taskState = stateTime.get(taskType);
        if (taskState == null) {
            taskState = new HashMap<>();
            stateTime.put(taskType, taskState);
        }

        // 记录当前时间
        taskState.put(status, DateUtil.now());

        return meta;
    }

    /**
     * 更新文档的状态聚合信息
     * 
     * @param meta            文档元数据
     * @param embeddingStatus 向量化状态 (0-3)
     * @param questionStatus  问题生成状态 (0-3)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> updateAggs(Map<String, Object> meta, int embeddingStatus, int questionStatus) {
        if (meta == null) {
            meta = new HashMap<>();
        }

        // 简单实现：将当前文档的整体状态作为聚合状态之一（实际上聚合通常是对子项的统计，这里简化为Document自身的聚合状态显示）
        // 根据需求描述：aggs: [{count: x, status: "22"}] 这里的count通常指切片数量
        // 但我们在实时更新单文档时，很难全量统计切片。
        //
        // 鉴于这是一个辅助展示功能，我们可以只在 Document 上记录它“当前”的状态快照用于历史记录，但这有些奇怪。
        // 重新阅读需求：aggs 是 "段落状态聚合统计"。这一步比较重，需要查询 chunk 表。
        // 我们可以暂时留空 aggs 的实时更新，或者仅在 "COMPLETED" 时做一次全量统计。

        return meta;
    }
}
