#!/bin/bash

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# 获取项目根目录 (假设脚本在 kmatrix-service/docker 下，根目录是上两级)
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 切换到项目根目录
cd "$PROJECT_ROOT"
echo "Working Directory: $PROJECT_ROOT"

# 停止并删除旧容器
echo "Stopping existing container (if any)..."
docker stop kmatrix-container >/dev/null 2>&1
docker rm kmatrix-container >/dev/null 2>&1

# 运行新容器
echo "Starting new container..."
docker run -d \
    --name kmatrix-container \
    -p 80:80 \
    -p 8080:8090 \
    -p 5432:5432 \
    -v "$PROJECT_ROOT/kmatrix-data:/kmatrix-data" \
    kmatrix

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "KMatrix container started successfully!"
    echo "========================================"
    echo ""
    echo "Access at http://localhost"
    echo ""
else
    echo ""
    echo "Failed to start container!"
    exit 1
fi
