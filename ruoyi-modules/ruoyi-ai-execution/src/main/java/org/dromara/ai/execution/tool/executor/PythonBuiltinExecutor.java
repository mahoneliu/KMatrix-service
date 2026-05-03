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
public class PythonBuiltinExecutor implements ToolExecutor {

    private final String toolName;
    private final String pythonCode;
    private final String pythonCmd;

    /**
     * @param toolName 工具名称
     * @param pythonCode Python 代码
     * @param pythonCmd 已经解析好的 Python 可执行路径
     */
    public PythonBuiltinExecutor(String toolName, String pythonCode, String pythonCmd) {
        this.toolName = toolName;
        this.pythonCode = pythonCode;
        this.pythonCmd = pythonCmd;
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

            if (pythonCmd == null) {
                return "Error: 未在系统中找到可用的 Python 环境。请安装 Python (https://www.python.org/) 并确保已将其添加到系统环境变量 PATH 中，或确保配置有效。";
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
