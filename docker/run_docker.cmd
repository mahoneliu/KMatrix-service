@echo off
setlocal

:: 获取当前脚本所在目录
set "SCRIPT_DIR=%~dp0"
:: 获取项目根目录 (假设脚本在 kmatrix-service/docker 下，根目录是上两级)
cd /d "%SCRIPT_DIR%..\.."
set "PROJECT_ROOT=%CD%"

echo Working Directory: %PROJECT_ROOT%

echo Stopping existing container (if any)...
docker stop kmatrix-container >nul 2>&1
docker rm kmatrix-container >nul 2>&1

echo Starting new container...
docker run -d ^
    --name kmatrix-container ^
    -p 80:80 ^
    -p 8080:8090 ^
    -p 5432:5432 ^
    -v "%PROJECT_ROOT%\kmatrix-data:/kmatrix-data" ^
    kmatrix

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo KMatrix container started successfully!
    echo ========================================
    echo.
    echo Access at http://localhost
    echo.
) else (
    echo.
    echo Failed to start container!
    exit /b 1
)

endlocal
