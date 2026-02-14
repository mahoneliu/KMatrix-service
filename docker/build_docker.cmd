@echo off
setlocal

:: 获取当前脚本所在目录
set "SCRIPT_DIR=%~dp0"
:: 获取项目根目录 (假设脚本在 kmatrix-service/docker 下，根目录是上两级)
cd /d "%SCRIPT_DIR%..\.."
set "PROJECT_ROOT=%CD%"

echo Working Directory: %PROJECT_ROOT%
echo Building KMatrix Docker Image...

:: 执行 docker build
docker build -t kmatrix -f kmatrix-service/docker/Dockerfile .

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo To run the container:
    echo   call kmatrix-service\docker\run_docker.cmd
) else (
    echo.
    echo Build failed!
    exit /b 1
)

endlocal
