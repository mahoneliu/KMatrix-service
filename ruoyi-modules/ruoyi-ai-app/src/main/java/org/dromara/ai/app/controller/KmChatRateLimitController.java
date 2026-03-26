package org.dromara.ai.app.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.ai.app.domain.vo.ChatRateLimitConfigVo;
import org.dromara.common.core.annotation.DemoBlock;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysConfigVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysConfigService;
import org.dromara.system.service.ISysUserService;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天限流配置管理 Controller
 * 独立管理对话全局与用户级限流
 *
 * @author KMatrix
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/admin/rate-limit")
public class KmChatRateLimitController extends BaseController {

    private final ISysConfigService configService;
    private final ISysUserService userService;

    private static final String DEFAULT_RATE_LIMIT_KEY = "chat.rate.limit.default";

    /**
     * 获取系统默认限流配置
     */
    @SaCheckPermission("ai:rateLimit:query")
    @GetMapping("/system-default")
    public R<String> getSystemDefaultConfig() {
        String configValue = configService.selectConfigByKey(DEFAULT_RATE_LIMIT_KEY);
        return R.ok("操作成功", configValue);
    }

    /**
     * 更新系统默认限流配置
     */
    @DemoBlock
    @SaCheckPermission("ai:rateLimit:edit")
    @PutMapping("/system-default")
    public R<Void> updateSystemDefaultConfig(@RequestBody ChatRateLimitConfigVo configVo) {
        SysConfigBo queryBo = new SysConfigBo();
        queryBo.setConfigKey(DEFAULT_RATE_LIMIT_KEY);
        List<SysConfigVo> list = configService.selectConfigList(queryBo);
        if (list.isEmpty()) {
            return R.fail("系统默认限流配置项不存在");
        }

        SysConfigBo updateBo = new SysConfigBo();
        updateBo.setConfigId(list.get(0).getConfigId());
        updateBo.setConfigKey(DEFAULT_RATE_LIMIT_KEY);
        updateBo.setConfigValue(JsonUtils.toJsonString(configVo));

        configService.updateConfig(updateBo);
        return R.ok();
    }

    /**
     * 分页查询用户列表与其限流配置
     */
    @SaCheckPermission("ai:rateLimit:query")
    @GetMapping("/users")
    public TableDataInfo<SysUserVo> getUserList(SysUserBo user, PageQuery pageQuery) {
        return userService.selectPageUserList(user, pageQuery);
    }

    /**
     * 更新用户的限流配置
     */
    @DemoBlock
    @SaCheckPermission("ai:rateLimit:edit")
    @PutMapping("/user/{userId}")
    public R<Void> updateUserConfig(@PathVariable Long userId, @RequestBody ChatRateLimitConfigVo configVo) {
        SysUserBo userBo = new SysUserBo();
        userBo.setUserId(userId);
        if (configVo == null) {
            userBo.setRateLimitConfig(""); // 清空相当于使用默认配置
        } else {
            userBo.setRateLimitConfig(JsonUtils.toJsonString(configVo));
        }

        userService.updateUser(userBo);
        return R.ok();
    }

    /**
     * 清理用户的限流配置（恢复系统默认）
     */
    @DemoBlock
    @SaCheckPermission("ai:rateLimit:edit")
    @DeleteMapping("/user/{userId}")
    public R<Void> clearUserConfig(@PathVariable Long userId) {
        SysUserBo userBo = new SysUserBo();
        userBo.setUserId(userId);
        userBo.setRateLimitConfig("");
        userService.updateUser(userBo);
        return R.ok();
    }
}
