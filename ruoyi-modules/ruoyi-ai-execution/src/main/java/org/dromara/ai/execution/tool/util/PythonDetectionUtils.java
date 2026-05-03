package org.dromara.ai.execution.tool.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Python 环境探测工具类
 *
 * @author KMatrix
 */
@Slf4j
public class PythonDetectionUtils {

    private static volatile String cachedPythonPath = null;

    /**
     * 探测可用的 Python 路径
     *
     * @param configuredPath 用户配置的路径（可选）
     * @return 验证通过的 Python 绝对路径或命令，若未找到则返回 null
     */
    public static String detect(String configuredPath) {
        if (cachedPythonPath != null && verifyPython(cachedPythonPath)) {
            return cachedPythonPath;
        }

        // 1. 尝试配置路径
        if (StrUtil.isNotBlank(configuredPath)) {
            if (verifyPython(configuredPath)) {
                log.info("使用配置的 Python 路径: {}", configuredPath);
                cachedPythonPath = configuredPath;
                return configuredPath;
            } else {
                log.warn("配置的 Python 路径无效: {}", configuredPath);
            }
        }

        // 2. 尝试环境变量 PYTHON_HOME
        String pythonHome = System.getenv("PYTHON_HOME");
        if (StrUtil.isNotBlank(pythonHome)) {
            String path = FileUtil.file(pythonHome, isWindows() ? "python.exe" : "bin/python3").getAbsolutePath();
            if (verifyPython(path)) {
                log.info("通过 PYTHON_HOME 找到 Python: {}", path);
                cachedPythonPath = path;
                return path;
            }
        }

        // 3. 动态探测
        Set<String> candidates = new LinkedHashSet<>();
        if (isWindows()) {
            candidates.addAll(findWindowsCandidates());
        } else {
            candidates.addAll(findUnixCandidates());
        }

        // 4. 验证并返回第一个有效的
        for (String candidate : candidates) {
            if (verifyPython(candidate)) {
                log.info("探测到可用的 Python 环境: {}", candidate);
                cachedPythonPath = candidate;
                return candidate;
            }
        }

        log.error("未能在系统中探测到可用的 Python 环境。请确保已安装 Python 并添加到 PATH 或配置 kmatrix.ai.execution.python-path");
        return null;
    }

    /**
     * 验证 Python 环境是否可用
     */
    public static boolean verifyPython(String cmd) {
        if (StrUtil.isBlank(cmd)) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(cmd, "--version")
                    .redirectErrorStream(true)
                    .start();

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.readLine();
            }

            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0 && StrUtil.containsIgnoreCase(output, "Python");
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> findWindowsCandidates() {
        List<String> candidates = new ArrayList<>();

        // 尝试 where python
        candidates.addAll(executeCommand("where", "python"));

        // 尝试 py -0p (Launcher)
        candidates.addAll(resolvePyLauncher());

        candidates.add("python"); // 依赖 PATH

        return candidates;
    }

    private static List<String> findUnixCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.addAll(executeCommand("which", "python3"));
        candidates.addAll(executeCommand("which", "python"));
        candidates.add("python3");
        candidates.add("python");
        return candidates;
    }

    private static List<String> executeCommand(String... command) {
        List<String> results = new ArrayList<>();
        try {
            Process process = new ProcessBuilder(command).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StrUtil.isNotBlank(line)) {
                        results.add(line.trim());
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return results;
    }

    private static List<String> resolvePyLauncher() {
        List<String> results = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("py", "-0p").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // 解析示例: "-V:3.12 * C:\Path\To\python.exe"
                    if (line.contains(":\\")) {
                        int start = line.indexOf(":\\") - 1;
                        if (start >= 0) {
                            String path = line.substring(start).trim();
                            results.add(path);
                        }
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return results;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 重置缓存（主要用于测试）
     */
    public static void resetCache() {
        cachedPythonPath = null;
    }
}
