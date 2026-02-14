package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmDocumentChunkBo;
import org.dromara.ai.domain.vo.KmDocumentChunkVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

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
     * 更新切片
     *
     * @param id      切片ID
     * @param title   新标题（可选）
     * @param content 新内容（可选）
     * @return 是否成功
     */
    Boolean updateChunk(Long id, String title, String content);

    /**
     * 删除切片
     *
     * @param id 切片ID
     * @return 是否成功
     */
    Boolean deleteById(Long id);

    /**
     * 分页查询切片列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    TableDataInfo<KmDocumentChunkVo> pageList(KmDocumentChunkBo bo, PageQuery pageQuery);

    /**
     * 手工添加切片
     *
     * @param bo 切片信息
     * @return 切片详情
     */
    KmDocumentChunkVo addChunk(KmDocumentChunkBo bo);

    /**
     * 启用/禁用切片
     *
     * @param id      切片ID
     * @param enabled 是否启用
     * @return 是否成功
     */
    Boolean enableChunk(Long id, boolean enabled);

    /**
     * 批量启用/禁用切片
     *
     * @param ids     切片ID列表
     * @param enabled 是否启用
     * @return 是否成功
     */
    Boolean batchEnable(List<Long> ids, boolean enabled);

    /**
     * 批量删除切片
     *
     * @param ids 切片ID列表
     * @return 是否成功
     */
    Boolean batchDelete(List<Long> ids);
}
