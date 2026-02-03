package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmQuestionVo;

import java.util.List;

/**
 * 问题服务接口
 *
 * @author Mahone
 * @date 2026-02-02
 */
public interface IKmQuestionService {

    /**
     * 查询切片关联的问题列表
     *
     * @param chunkId 切片ID
     * @return 问题列表
     */
    List<KmQuestionVo> listByChunkId(Long chunkId);

    /**
     * 查询文档下的所有问题
     *
     * @param documentId 文档ID
     * @return 问题列表
     */
    List<KmQuestionVo> listByDocumentId(Long documentId);

    /**
     * 手动添加问题
     *
     * @param chunkId 切片ID
     * @param content 问题内容
     * @return 是否成功
     */
    Boolean addQuestion(Long chunkId, String content);

    /**
     * 删除问题
     *
     * @param id 问题ID
     * @return 是否成功
     */
    Boolean deleteById(Long id);

    /**
     * AI自动生成问题
     *
     * @param chunkId 切片ID
     * @param modelId 模型ID (可选，若为空则使用默认)
     * @return 生成的问题列表
     */
    List<KmQuestionVo> generateQuestions(Long chunkId, Long modelId);
}
