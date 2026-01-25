package org.dromara.ai.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.service.IKmAppTokenService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;

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
        // 开发环境下重定向到 9527，生产环境下重定向到相对路径或当前域名
        // 这里暂时硬编码 localhost:9527 以便测试，实际应从配置获取
        String frontendUrl = "http://localhost:9527/ai/chat?token=" + token;
        if (mode != null) {
            frontendUrl += "&mode=" + mode;
        }
        response.sendRedirect(frontendUrl);
    }

    /**
     * 生成嵌入脚本内容
     */
    private String generateEmbedScript(String baseUrl, String chatUrl, String token) {
        return """
                (function() {
                    // 防止重复加载
                    if (window.__KMATRIX_EMBED_LOADED__) return;
                    window.__KMATRIX_EMBED_LOADED__ = true;

                    // 配置
                    var config = {
                        baseUrl: '%s',
                        chatUrl: '%s',
                        token: '%s'
                    };

                    // 创建样式
                    var style = document.createElement('style');
                    style.textContent = `
                        #km-embed-btn {
                            position: fixed;
                            bottom: 24px;
                            right: 24px;
                            width: 56px;
                            height: 56px;
                            border-radius: 50%%;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            border: none;
                            cursor: pointer;
                            box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            z-index: 999998;
                            transition: transform 0.2s, box-shadow 0.2s;
                        }
                        #km-embed-btn:hover {
                            transform: scale(1.05);
                            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
                        }
                        #km-embed-btn svg {
                            width: 28px;
                            height: 28px;
                            fill: white;
                        }
                        #km-embed-container {
                            position: fixed;
                            bottom: 96px;
                            right: 24px;
                            width: 400px;
                            height: 600px;
                            max-height: calc(100vh - 120px);
                            border-radius: 16px;
                            overflow: hidden;
                            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
                            z-index: 999999;
                            display: none;
                            background: white;
                        }
                        #km-embed-container.active {
                            display: block;
                            animation: km-slide-up 0.3s ease-out;
                        }
                        #km-embed-container iframe {
                            width: 100%%;
                            height: 100%%;
                            border: none;
                        }
                        @keyframes km-slide-up {
                            from {
                                opacity: 0;
                                transform: translateY(20px);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }
                        @media (max-width: 480px) {
                            #km-embed-container {
                                width: calc(100vw - 32px);
                                right: 16px;
                                bottom: 88px;
                                height: calc(100vh - 120px);
                            }
                            #km-embed-btn {
                                right: 16px;
                                bottom: 16px;
                            }
                        }
                    `;
                    document.head.appendChild(style);

                    // 创建按钮
                    var btn = document.createElement('button');
                    btn.id = 'km-embed-btn';
                    btn.title = 'AI 助手';
                    btn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M12 3c5.5 0 10 3.58 10 8s-4.5 8-10 8c-1.24 0-2.43-.18-3.53-.5C5.55 21 2 21 2 21c2.33-2.33 2.7-3.9 2.75-4.5C3.05 15.07 2 13.13 2 11c0-4.42 4.5-8 10-8z"/></svg>';
                    document.body.appendChild(btn);

                    // 创建容器
                    var container = document.createElement('div');
                    container.id = 'km-embed-container';
                    container.innerHTML = '<iframe src="' + config.chatUrl + '?mode=float" allow="microphone"></iframe>';
                    document.body.appendChild(container);

                    // 切换显示
                    var isOpen = false;
                    btn.addEventListener('click', function() {
                        isOpen = !isOpen;
                        if (isOpen) {
                            container.classList.add('active');
                        } else {
                            container.classList.remove('active');
                        }
                    });

                    // 点击外部关闭
                    document.addEventListener('click', function(e) {
                        if (isOpen && !container.contains(e.target) && e.target !== btn && !btn.contains(e.target)) {
                            isOpen = false;
                            container.classList.remove('active');
                        }
                    });

                    console.log('KMatrix Embed loaded successfully');
                })();
                """
                .formatted(baseUrl, chatUrl, token);
    }
}
