package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmQuestionVo;

import org.dromara.ai.domain.bo.KmQuestionBo;
import org.dromara.ai.domain.vo.KmQuestionVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;
import java.util.Map;

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
         * 分页查询问题列表
         */
        TableDataInfo<KmQuestionVo> pageList(KmQuestionBo bo, PageQuery pageQuery);

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
         * @param chunkId     切片ID
         * @param modelId     模型ID (可选，若为空则使用系统默认模型)
         * @param prompt      提示词模板 (可选，若为空则使用默认提示词)
         * @param temperature 温度参数 (可选，若为空则使用模型配置的默认值)
         * @param maxTokens   最大Token数 (可选，若为空则使用模型配置的默认值)
         * @return 生成的问题列表
         */
        List<KmQuestionVo> generateQuestions(Long chunkId, Long modelId, String prompt, Double temperature,
                        Integer maxTokens);

        Boolean linkQuestion(Long chunkId, Long questionId);

        /**
         * Unlink a question from a chunk
         *
         * @param chunkId    chunk id
         * @param questionId question id
         * @return true if successful
         */
        Boolean unlinkQuestion(Long chunkId, Long questionId);

        /**
         * 批量为切片生成问题
         *
         * @param chunkIds 切片ID列表
         * @param modelId  模型ID (可选)
         * @return 是否成功
         */
        Boolean batchGenerateQuestions(List<Long> chunkIds, Long modelId, String prompt, Double temperature,
                        Integer maxTokens);

        /**
         * 为文档生成问题 (异步)
         *
         * @param documentId  文档ID
         * @param modelId     模型ID
         * @param prompt      提示词
         * @param temperature 温度
         * @param maxTokens   最大token
         */
        void processGenerateQuestionsAsync(Long documentId, Long modelId, String prompt, Double temperature,
                        Integer maxTokens);

        /**
         * 查询知识库下的所有问题
         *
         * @param kbId 知识库ID
         * @return 问题列表（包含关联分段数量）
         */
        List<KmQuestionVo> listByKbId(Long kbId);

        /**
         * 更新问题内容
         *
         * @param id      问题ID
         * @param content 新的问题内容
         * @return 是否成功
         */
        Boolean updateQuestion(Long id, String content);

        /**
         * 批量删除问题
         *
         * @param ids 问题ID列表
         * @return 是否成功
         */
        Boolean batchDelete(List<Long> ids);

        /**
         * 批量添加问题到知识库（不关联特定分块）
         *
         * @param kbId     知识库ID
         * @param contents 问题内容列表
         * @return 是否成功
         */
        Boolean batchAddToKb(Long kbId, List<String> contents);

        /**
         * 获取问题关联的分段列表
         *
         * @param questionId 问题ID
         * @return 分段列表（包含文档标题）
         */
        List<Map<String, Object>> getLinkedChunks(Long questionId);

        /**
         * 批量关联问题到分段
         *
         * @param questionId 问题ID
         * @param chunkIds   分段ID列表
         * @return 是否成功
         */
        Boolean batchLinkToChunks(Long questionId, List<Long> chunkIds);
}
