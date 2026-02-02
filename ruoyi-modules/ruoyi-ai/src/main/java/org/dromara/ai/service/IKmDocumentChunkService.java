package org.dromara.ai.service;

import org.dromara.ai.domain.vo.KmDocumentChunkVo;

import java.util.List;

/**
 * 文档切片服务接口
 *
 * @author Mahone
 * @date 2026-02-02
 */
public interface IKmDocumentChunkService {

    /**
     * 查询文档下的切片列表
     *
     * @param documentId 文档ID
     * @return 切片列表
     */
    List<KmDocumentChunkVo> listByDocumentId(Long documentId);

    /**
     * 查询切片详情
     *
     * @param id 切片ID
     * @return 切片详情
     */
    KmDocumentChunkVo queryById(Long id);

    /**
     * 更新切片内容
     *
     * @param id      切片ID
     * @param content 新内容
     * @return 是否成功
     */
    Boolean updateChunk(Long id, String content);

    /**
     * 删除切片
     *
     * @param id 切片ID
     * @return 是否成功
     */
    Boolean deleteById(Long id);
}
