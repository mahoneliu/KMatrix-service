/**
 * KMatrix Embed SDK Template
 * 此文件由后端读取并注入参数
 * 占位符: {{BASE_URL}}, {{CHAT_URL}}, {{TOKEN}}
 */

(function () {
    // 防止重复加载
    if (window.__KMATRIX_EMBED_LOADED__) return;
    window.__KMATRIX_EMBED_LOADED__ = true;

    // 配置
    var config = {
        baseUrl: '{{BASE_URL}}',
        chatUrl: '{{CHAT_URL}}',
        token: '{{TOKEN}}'
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
            border-radius: 50%;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
            animation: km-slide-up 1s cubic-bezier(0.16, 1, 0.3, 1);
        }
        #km-embed-container iframe {
            width: 100%;
            height: 100%;
            border: none;
        }
        @keyframes km-slide-up {
            from {
                opacity: 0;
                transform: translateY(100px);
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
    var iframe = document.createElement('iframe');
    iframe.src = config.chatUrl + '?mode=float';
    iframe.allow = 'microphone';
    container.appendChild(iframe);
    document.body.appendChild(container);

    // 切换显示
    var isOpen = false;
    btn.addEventListener('click', function () {
        isOpen = !isOpen;
        if (isOpen) {
            container.classList.add('active');
        } else {
            container.classList.remove('active');
        }
    });

    // 点击外部关闭 (已移除)
    // document.addEventListener('click', function (e) {
    //     if (isOpen && !container.contains(e.target) && e.target !== btn && !btn.contains(e.target)) {
    //         isOpen = false;
    //         container.classList.remove('active');
    //     }
    // });

    // 监听来自 iframe 的消息
    window.addEventListener('message', function (event) {
        try {
            console.log('[KMatrix Embed] Message received:', event.data);

            if (!event.data) return;

            var type = event.data.type;
            var data = event.data.data;

            if (!type) return;

            if (type === 'maximize-chat') {
                console.log('[KMatrix Embed] Processing maximize-chat, maximized:', data ? data.maximized : false);
                if (data && data.maximized) {
                    // 最大化 - 半屏宽度
                    console.log('[KMatrix Embed] Maximizing chat window');
                    btn.style.display = 'none';
                    container.style.width = '50vw';  // 半屏宽度
                    container.style.height = '100vh';
                    container.style.maxHeight = '100vh';
                    container.style.top = '0';
                    container.style.bottom = '0';
                    container.style.left = 'auto';
                    container.style.right = '0';     // 靠右对齐
                    container.style.borderRadius = '0';
                    console.log('[KMatrix Embed] Chat window maximized to half-screen');
                } else {
                    // 恢复
                    console.log('[KMatrix Embed] Restoring chat window');
                    btn.style.display = 'flex';
                    container.style.width = '400px';
                    container.style.height = '600px';
                    container.style.maxHeight = 'calc(100vh - 120px)';
                    container.style.bottom = '96px';
                    container.style.right = '24px';
                    container.style.top = 'auto';
                    container.style.left = 'auto';
                    container.style.borderRadius = '16px';
                    console.log('[KMatrix Embed] Chat window restored');
                }
            } else if (type === 'close-chat') {
                console.log('[KMatrix Embed] Processing close-chat');
                // 恢复样式并关闭
                btn.style.display = 'flex';
                container.style.width = '400px';
                container.style.height = '600px';
                container.style.maxHeight = 'calc(100vh - 120px)';
                container.style.bottom = '96px';
                container.style.right = '24px';
                container.style.borderRadius = '16px';
                isOpen = false;
                container.classList.remove('active');
            }
        } catch (error) {
            console.error('[KMatrix Embed] Error handling message:', error);
        }
    });

    console.log('KMatrix Embed loaded successfully');
})();
