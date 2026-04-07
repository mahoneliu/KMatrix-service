package org.dromara.ai.model.service;

import org.dromara.ai.model.domain.bo.KmModelBo;
import org.dromara.ai.model.domain.bo.KmModelChatSendBo;
import org.dromara.ai.model.domain.vo.KmModelVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

/**
 * AI模型配置Service接口
 *
 * @author Mahone
 * @date 2024-01-27
 */
public interface IKmModelService {

    /**
     * 查询列表
     */
    List<KmModelVo> queryList(KmModelBo bo);

    /**
     * 根据ID查询
     */
    KmModelVo queryById(Long modelId);

    /**
     * 新增
     */
    Boolean insertByBo(KmModelBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmModelBo bo);

    /**
     * 删除
     */
    Boolean deleteById(Long id);

    /**
     * 测试连接
     */
    String testConnection(KmModelBo bo);

    /**
     * 复制模型
     *
     * @param modelId 原模型ID
     * @return 新模型ID
     */
    Long copyModel(Long modelId);

    /**
     * 设置系统默认模型
     *
     * @param modelId 模型ID
     * @return 是否成功
     */
    Boolean setDefaultModel(Long modelId);

    /**
     * 检查指定类型是否存在默认兆底模型
     *
     * @param modelType 模型类型（1-语言模型 2-向量模型）
     * @return 是否存在
     */
    Boolean hasDefaultModel(String modelType);

    /**
     * 获取指定类型的系统默认模型
     *
     * @param modelType 模型类型
     * @return 默认模型对象，未找到返回 null
     */
    KmModelVo getDefaultModel(String modelType);

    /**
     * 测试模型对话 (流式)
     *
     * @param bo 发送消息对象
     * @return SseEmitter
     */
    SseEmitter streamTestChat(
            KmModelChatSendBo bo);
}
