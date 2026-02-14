package org.dromara.ai.util;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 文件类型校验工具类
 *
 * @author Mahone
 * @date 2026-02-01
 */
public class FileTypeValidator {

    /**
     * 校验文件类型是否允许
     *
     * @param filename     文件名
     * @param allowedTypes 允许的文件类型 (逗号分隔,如: "pdf,docx,txt" 或 "*" 表示全部)
     * @throws ServiceException 如果文件类型不允许
     */
    public static void validate(String filename, String allowedTypes) {
        // 如果 allowedTypes 为空或为 "*",则允许所有类型
        if (StringUtils.isBlank(allowedTypes) || "*".equals(allowedTypes.trim())) {
            return;
        }

        // 提取文件扩展名
        String extension = getExtension(filename);
        if (StringUtils.isBlank(extension)) {
            throw new ServiceException("无法识别文件类型: " + filename);
        }

        // 解析允许的类型列表
        List<String> allowedList = Arrays.asList(allowedTypes.toLowerCase().split(","));

        // 校验扩展名是否在允许列表中
        if (!allowedList.contains(extension.toLowerCase())) {
            throw new ServiceException(
                    String.format("不支持的文件格式: %s, 仅支持: %s", extension, allowedTypes));
        }
    }

    /**
     * 获取文件扩展名 (不含点号)
     *
     * @param filename 文件名
     * @return 扩展名 (小写)
     */
    public static String getExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 检查文件类型是否允许 (不抛异常,返回布尔值)
     *
     * @param filename     文件名
     * @param allowedTypes 允许的文件类型
     * @return true 如果允许, false 否则
     */
    public static boolean isAllowed(String filename, String allowedTypes) {
        try {
            validate(filename, allowedTypes);
            return true;
        } catch (ServiceException e) {
            return false;
        }
    }
}
