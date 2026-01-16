package org.dromara.ai.service;

import org.dromara.ai.domain.bo.KmAppBo;
import org.dromara.ai.domain.vo.KmAppVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * AI应用Service接口
 *
 * @author Mahone
 * @date 2025-12-27
 */
public interface IKmAppService {

    /**
     * 查询AI应用
     */
    KmAppVo queryById(Long appId);

    /**
     * 查询AI应用列表
     */
    TableDataInfo<KmAppVo> queryPageList(KmAppBo bo, PageQuery pageQuery);

    /**
     * 查询AI应用列表
     */
    List<KmAppVo> queryList(KmAppBo bo);

    /**
     * 新增AI应用
     */
    String insertByBo(KmAppBo bo);

    /**
     * 修改AI应用
     */
    Boolean updateByBo(KmAppBo bo);

    /**
     * 校验并批量删除AI应用信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 发布应用
     * 
     * @param appId  应用ID
     * @param remark 发布备注
     * @return 是否成功
     */
    Boolean publishApp(Long appId, String remark);
}
