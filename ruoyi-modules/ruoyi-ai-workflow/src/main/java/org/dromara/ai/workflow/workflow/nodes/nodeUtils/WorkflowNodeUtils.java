package org.dromara.ai.workflow.workflow.nodes.nodeUtils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.ai.storage.domain.KmTempFile;
import org.dromara.ai.storage.service.IKmFileService;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Base64;

/**
 * 工作流节点使用的工具类
 * 包含通用操作如提取冗余的 Base64 转换逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowNodeUtils {

    private final ObjectProvider<ISysOssService> sysOssServiceProvider;
    private final ObjectProvider<IKmFileService> kmFileServiceProvider;

    /**
     * 将 OSS ID 解析为 Base64 或 URL
     * 配合 IgnoreStrategy 绕过数据权限和多租户，兼容异步线程无上下文的问题
     *
     * @param ossIdStr OSS ID 字符串
     * @param fallbackUrl 转换失败或无权限时回退使用的 URL
     * @param defaultMimeType 缺省的 MimeType，例如 "image/jpeg" 或 "audio/mpeg"
     * @return 转换后的 Base64 data URI 或原始 URL
     */
    public String resolveOssUrlOrBase64(String ossIdStr, String fallbackUrl, String defaultMimeType) {
        if (StrUtil.isNotBlank(ossIdStr) && !"undefined".equals(ossIdStr)) {
            try {
                Long fileId = Long.parseLong(ossIdStr);
                
                // 开启最强力的全局忽略：忽略数据权限和多租户，确保异步线程(无租户Context)也能读到数据
                InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).tenantLine(true).build());
                try {
                    // 1. 优先尝试作为聊天产生的 KmTempFile 获取
                    IKmFileService fileService = kmFileServiceProvider.getIfAvailable();
                    if (fileService != null) {
                        KmTempFile tempFile = fileService.getTempFile(fileId);
                        if (tempFile != null) {
                            String absolutePath = "未知路径";
                            try {
                                // 尝试探测绝对路径用于日志诊断
                                try {
                                    Method getAbs = fileService.getClass().getMethod("getAbsolutePath", String.class);
                                    absolutePath = (String) getAbs.invoke(fileService, tempFile.getFilePath());
                                } catch (Exception ignore) {}

                                log.info("尝试读取本地文件流进行 Base64 转换, ID: {}, 物理路径: {}", fileId, absolutePath);
                                
                                try (InputStream is = fileService.getFileStream(tempFile.getStoreType(), tempFile.getOssId(), tempFile.getFilePath())) {
                                    byte[] data = is.readAllBytes();
                                    if (data.length == 0) {
                                        throw new RuntimeException("stream_empty");
                                    }
                                    String base64 = Base64.getEncoder().encodeToString(data);
                                    String mimeType = defaultMimeType;
                                    if (StrUtil.isNotBlank(tempFile.getFileExtension())) {
                                        String inferred = FileUtils.getMimeType("dummy." + tempFile.getFileExtension());
                                        if (StrUtil.isNotBlank(inferred)) {
                                            mimeType = inferred;
                                        }
                                    }
                                    return "data:" + mimeType + ";base64," + base64;
                                }
                            } catch (Exception ex) {
                                log.error("通过临时文件(ID:{})转码Base64内容失败: {}", fileId, ex.getMessage(), ex);
                                if ("stream_empty".equals(ex.getMessage())) {
                                    throw new RuntimeException(MessageUtils.message("ai.workflow.node.ocr.read_stream_empty"));
                                }
                            }
                        }
                    }
                    
                    // 2. 如果不是临时文件，或者读取流失败，尝试作为普通 SysOssVo 获取
                    ISysOssService ossService = sysOssServiceProvider.getIfAvailable();
                    if (ossService != null) {
                        SysOssVo ossVo = ossService.getById(fileId);
                        if (ossVo != null) {
                            try {
                                OssClient storage = OssFactory.instance(ossVo.getService());
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                storage.download(ossVo.getFileName(), baos, null);
                                byte[] bytes = baos.toByteArray();
                                String base64 = Base64.getEncoder().encodeToString(bytes);
                                String mimeType = defaultMimeType;
                                if (StrUtil.isNotBlank(ossVo.getFileSuffix())) {
                                    String inferred = FileUtils.getMimeType("dummy." + ossVo.getFileSuffix());
                                    if (StrUtil.isNotBlank(inferred)) {
                                        mimeType = inferred;
                                    }
                                }
                                return "data:" + mimeType + ";base64," + base64;
                            } catch (Exception ex) {
                                log.error("通过通用OSS管理转码Base64图片/音频失败: {}, 将回退使用URL", fileId, ex);
                                return ossVo.getUrl();
                            }
                        }
                    }
                } finally {
                    // 必须清理，防止污染当前线程后续操作
                    InterceptorIgnoreHelper.clearIgnoreStrategy();
                }
            } catch (Exception e) {
                log.error("解析ossId转Base64失败: {}, 错误: {}", ossIdStr, e.getMessage(), e);
                // 抛出国际化错误
                throw new RuntimeException(MessageUtils.message("ai.workflow.node.multimodal.resolve_failed", e.getMessage()));
            }
        }
        
        if (StrUtil.isNotBlank(fallbackUrl)) {
            // 如果是在转换 Base64 场景下，且使用了相对路径，说明转换失败了
            if (fallbackUrl.startsWith("/")) {
                log.error("核心逻辑错误: 无法根据 ID 找到文件流，且降级到了无效的相对路径: {}", fallbackUrl);
                throw new RuntimeException(MessageUtils.message("ai.workflow.node.multimodal.fallback_denied", fallbackUrl));
            }
            return fallbackUrl;
        }
        return null;
    }
}
