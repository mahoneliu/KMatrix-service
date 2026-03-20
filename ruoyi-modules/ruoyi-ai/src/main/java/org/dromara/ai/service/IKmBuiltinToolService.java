package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmBuiltinToolBo;
import org.dromara.ai.domain.vo.KmBuiltinToolVo;

import java.util.List;

/**
 * 内置 Python 工具 Service 接口
 *
 * @author Mahone
 * @date 2026-03-15
 */
public interface IKmBuiltinToolService {

    /**
     * 查询列表
     */
    List<KmBuiltinToolVo> queryList(KmBuiltinToolBo bo);

    /**
     * 根据ID查询
     */
    KmBuiltinToolVo queryById(Long toolId);

    /**
     * 新增
     */
    Boolean insertByBo(KmBuiltinToolBo bo);

    /**
     * 修改
     */
    Boolean updateByBo(KmBuiltinToolBo bo);

    /**
     * 删除（逻辑删除）
     */
    Boolean deleteByIds(List<Long> ids);

}
