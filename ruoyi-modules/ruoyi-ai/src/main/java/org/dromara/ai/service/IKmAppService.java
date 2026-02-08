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

    /**
     * 获取应用的最新发布版本快照
     * 
     * @param appId 应用ID
     * @return 最新发布版本的快照,如果没有发布版本则返回null
     */
    org.dromara.ai.domain.vo.config.AppSnapshot getLatestPublishedSnapshot(Long appId);

    /**
     * 更新公开访问开关
     *
     * @param appId        应用ID
     * @param publicAccess 公开访问开关 (0关闭 1开启)
     * @return 是否成功
     */
    Boolean updatePublicAccess(Long appId, String publicAccess);

    /**
     * 获取应用统计数据
     *
     * @param appId  应用ID
     * @param period 统计周期 (7d, 30d, 90d)
     * @return 统计数据
     */
    org.dromara.ai.domain.vo.KmAppStatisticsVo getAppStatistics(Long appId, String period);

    /**
     * 获取应用发布历史
     *
     * @param appId 应用ID
     * @return 发布历史列表
     */
    List<org.dromara.ai.domain.vo.KmAppVersionVo> getPublishHistory(Long appId);
}
