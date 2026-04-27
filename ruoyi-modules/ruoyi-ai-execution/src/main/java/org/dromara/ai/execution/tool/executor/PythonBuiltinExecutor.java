package org.dromara.ai.execution.tool.executor;

import org.dromara.ai.execution.core.ToolExecutor;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 内置 Python 脚本执行器
 * 通过临时文件传递 JSON 参数，文件路径作为 sys.argv[1] 传给脚本。
 * 脚本约定：import json, sys; args = json.load(open(sys.argv[1]))
 * 支持自动探测 Python 可执行命令（python / py / python3）。
 *
 * @author Mahone
 * @date 2026-03-20
 */
@Slf4j
@RequiredArgsConstructor
public class PythonBuiltinExecutor implements ToolExecutor {

    private final String toolName;
    private final String pythonCode;

    /** 缓存已探测到的可用 Python 绝对路径（进程级别） */
    private static volatile String resolvedPythonCmd = null;

    /**
     * 探测系统中可用的 Python 可执行文件绝对路径。
     * 策略：
     *  1. 优先使用 Windows Python Launcher（py -0p）从注册表读取已安装 Python 列表，取第一个绝对路径。
     *  2. 若 Launcher 不可用，降级扫描已知常见安装目录。
     *  3. 若均找不到，最后再尝试 PATH 里的 "python"/"python3" 命令（对 Java 子进程来说通常不可靠）。
     */
    private static String resolvePythonCommand() {
        if (resolvedPythonCmd != null) {
            return resolvedPythonCmd;
        }

        // 1. 尝试通过 Windows Launcher（py -0p）获取注册表中注册的 Python 绝对路径
        String launcherResult = tryResolvePyLauncher();
        if (launcherResult != null) {
            log.info("Python 可执行路径（通过 Windows Launcher）: {}", launcherResult);
            resolvedPythonCmd = launcherResult;
            return resolvedPythonCmd;
        }

        // 2. 扫描常见安装路径
        String home = System.getProperty("user.home");
        String[] knownPaths = {
            home + "\\AppData\\Local\\Programs\\Python\\Python314\\python.exe",
            home + "\\AppData\\Local\\Programs\\Python\\Python313\\python.exe",
            home + "\\AppData\\Local\\Programs\\Python\\Python312\\python.exe",
            home + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
            home + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
            "C:\\Program Files\\Python313\\python.exe",
            "C:\\Program Files\\Python312\\python.exe",
            "C:\\Program Files\\Python311\\python.exe",
            "C:\\Program Files\\Python310\\python.exe",
            "/usr/bin/python3",
            "/usr/local/bin/python3",
            "/usr/bin/python",
        };
        for (String path : knownPaths) {
            if (FileUtil.exist(path) && verifyPythonExecutable(path)) {
                log.info("Python 可执行路径（常见目录扫描）: {}", path);
                resolvedPythonCmd = path;
                return resolvedPythonCmd;
            }
        }

        // 3. 最后尝试 PATH 中的 python3 / python（Java 子进程可能找不到）
        for (String cmd : new String[]{"python3", "python"}) {
            if (verifyPythonExecutable(cmd)) {
                log.info("Python 可执行命令（PATH）: {}", cmd);
                resolvedPythonCmd = cmd;
                return resolvedPythonCmd;
            }
        }

        log.warn("未找到可用的 Python 环境。请安装 Python 并确保可被 JVM 子进程访问。");
        return null;
    }

    /**
     * 尝试通过 Windows Python Launcher（py -0p）解析可用的 Python 绝对路径。
     * 输出示例: "-V:3.14 *        C:\\Users\\...\\python.exe"
     */
    private static String tryResolvePyLauncher() {
        try {
            Process p = new ProcessBuilder("py", "-0p").redirectErrorStream(true).start();
            String output;
            try (InputStream is = p.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            boolean ok = p.waitFor(5, TimeUnit.SECONDS);
            if (!ok || p.exitValue() != 0 || StrUtil.isBlank(output)) {
                return null;
            }
            // 解析每一行，取第一个有效的绝对路径
            for (String line : output.split("\\r?\\n")) {
                line = line.trim();
                int exeIdx = line.toLowerCase().lastIndexOf(".exe");
                if (exeIdx < 0) continue;
                String afterExe = line.substring(0, exeIdx + 4);
                int start = afterExe.lastIndexOf("  ");
                String path = (start >= 0 ? afterExe.substring(start) : afterExe).trim();
                if (FileUtil.exist(path) && verifyPythonExecutable(path)) {
                    return path;
                }
            }
        } catch (Exception ignored) {
            // py.exe 不存在（非 Windows 或未安装 Launcher），跳过
        }
        return null;
    }

    /**
     * 验证给定的 Python 命令或路径是否可以正常运行（exit 0 且输出含"Python"）。
     */
    private static boolean verifyPythonExecutable(String cmd) {
        try {
            Process probe = new ProcessBuilder(cmd, "--version").redirectErrorStream(true).start();
            String output;
            try (InputStream is = probe.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            boolean ok = probe.waitFor(5, TimeUnit.SECONDS);
            return ok && probe.exitValue() == 0 && StrUtil.containsIgnoreCase(output, "Python");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String execute(String arguments) throws Exception {
        if (StrUtil.isBlank(pythonCode)) {
            return "Error: Python code is empty.";
        }

        File tempDir = FileUtil.file(System.getProperty("java.io.tmpdir"), "kmatrix_tools", IdUtil.fastSimpleUUID());
        FileUtil.mkdir(tempDir);

        File scriptFile = FileUtil.file(tempDir, "script.py");
        File argsFile = FileUtil.file(tempDir, "args.json");

        try {
            FileUtil.writeUtf8String(pythonCode, scriptFile);

            String argsJson = (arguments == null || arguments.isBlank()) ? "{}" : arguments;
            FileUtil.writeUtf8String(argsJson, argsFile);

            log.debug("Python内置工具 [{}] 运行目录: {}, 参数文件: {}", toolName, tempDir.getAbsolutePath(), argsFile.getAbsolutePath());

            String pythonCmd = resolvePythonCommand();
            if (pythonCmd == null) {
                return "Error: 未在系统中找到可用的 Python 环境。请安装 Python (https://www.python.org/) 并确保已将其添加到系统环境变量 PATH 中，或确保默认安装路径有效。";
            }

            File outputFile = FileUtil.file(tempDir, "output.log");

            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-u", scriptFile.getAbsolutePath(), argsFile.getAbsolutePath());
            pb.directory(tempDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(outputFile);

            Process process = pb.start();
            process.getOutputStream().close();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.warn("Python 工具执行超时: {}", toolName);
                String outTmp = FileUtil.exist(outputFile) ? FileUtil.readUtf8String(outputFile) : "";
                return "Error: Tool execution timed out after 30 seconds. Output so far: " + outTmp;
            }

            String output = FileUtil.exist(outputFile) ? FileUtil.readUtf8String(outputFile) : "";
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                log.error("Python 工具 [{}] 异常退出 ({}):\n{}", toolName, exitCode, output);
                return "Error: Exit code " + exitCode + ".\nOutput: " + output;
            }

            return output;
        } finally {
            try {
                FileUtil.del(tempDir);
            } catch (Exception e) {
                log.warn("清理 Python 工具临时目录失败: {}, 目录: {}", e.getMessage(), tempDir.getAbsolutePath());
            }
        }
    }
}
