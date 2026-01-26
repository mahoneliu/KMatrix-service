package org.dromara.ai.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.service.IKmAppTokenService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 嵌入对话脚本Controller
 * 提供可嵌入第三方页面的 JavaScript 代码
 *
 * @author Mahone
 * @date 2026-01-26
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class EmbedController {

    private final IKmAppTokenService appTokenService;
    private final org.dromara.ai.service.IKmAppService appService;

    /**
     * 获取应用信息 (免登录，通过 Token)
     */
    @GetMapping("/app-info/{token}")
    public org.dromara.common.core.domain.R<org.dromara.ai.domain.vo.KmAppVo> getAppInfo(@PathVariable String token) {
        Long appId = appTokenService.validateToken(token, null);
        if (appId == null) {
            return org.dromara.common.core.domain.R.fail("无效的 Token");
        }
        return org.dromara.common.core.domain.R.ok(appService.queryById(appId));
    }

    /**
     * 获取嵌入脚本
     * 返回一段 JavaScript 代码，用于在第三方页面创建浮窗对话
     *
     * @param protocol 协议 (http/https)
     * @param host     主机地址
     * @param token    App Token
     */
    @GetMapping(value = "/embed", produces = "application/javascript; charset=utf-8")
    public void getEmbedScript(
            @RequestParam(defaultValue = "https") String protocol,
            @RequestParam String host,
            @RequestParam String token,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/javascript; charset=utf-8");
        response.setHeader("Cache-Control", "public, max-age=3600");

        // 验证 Token
        Long appId = appTokenService.validateToken(token, null);
        if (appId == null) {
            try (PrintWriter writer = response.getWriter()) {
                writer.write("console.error('KMatrix Embed: Invalid or expired token');");
            }
            return;
        }

        String baseUrl = protocol + "://" + host;
        String chatUrl = baseUrl + "/chat/" + token;

        // 生成嵌入脚本
        String script = generateEmbedScript(baseUrl, chatUrl, token);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(script);
        }
    }

    /**
     * 重定向到前端对话页面
     * 自动处理端口偏差（开发环境 9527, 生产环境与后端一致）
     */
    @GetMapping("/{token}")
    public void getChatPage(@PathVariable String token,
            @RequestParam(required = false) String mode,
            HttpServletResponse response) throws IOException {
        // 验证 Token 并获取 AppId
        Long appId = appTokenService.validateToken(token, null);
        if (appId == null) {
            response.sendError(401, "Invalid token");
            return;
        }
        // 重定向到 embed.html（独立入口，不触发主应用的认证流程）
        String frontendUrl = "http://localhost:9527/embed.html?appToken=" + token + "&appId=" + appId;
        if (mode != null) {
            frontendUrl += "&mode=" + mode;
        }
        response.sendRedirect(frontendUrl);
    }

    /**
     * 生成嵌入脚本内容
     * 从模板文件读取并注入参数
     */
    private String generateEmbedScript(String baseUrl, String chatUrl, String token) {
        try {
            // 读取模板文件
            Resource resource = new ClassPathResource("static/embed.template.js");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 替换占位符
            String script = template
                    .replace("{{BASE_URL}}", baseUrl)
                    .replace("{{CHAT_URL}}", chatUrl)
                    .replace("{{TOKEN}}", token);

            log.info("成功从模板生成嵌入脚本, baseUrl: {}, chatUrl: {}", baseUrl, chatUrl);
            return script;
        } catch (Exception e) {
            log.error("Failed to load embed template", e);
            // 降级：返回基本的错误脚本
            return "console.error('[KMatrix Embed] Failed to load embed script template: " + e.getMessage() + "');";
        }
    }
}
