#!/bin/bash

# 镜像名称和版本标签
IMAGE_NAME="registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix_base"
IMAGE_TAG="latest"

# 进入 docker 目录 (假设脚本在 docker 目录下执行)
cd "$(dirname "$0")"

echo "开始构建基础镜像: ${IMAGE_NAME}:${IMAGE_TAG}"
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -f Dockerfile.base .

if [ $? -eq 0 ]; then
    echo "镜像构建成功！"
    echo "准备推送到阿里云镜像仓库..."
    
    # 如果需要登录，取消下方注释并填入账号密码
    # docker login --username=您的用户名 registry.cn-guangzhou.aliyuncs.com
    
    docker push ${IMAGE_NAME}:${IMAGE_TAG}
    
    if [ $? -eq 0 ]; then
        echo "镜像推送成功: ${IMAGE_NAME}:${IMAGE_TAG}"
    else
        echo "镜像推送失败，请检查网络或登录状态。"
        exit 1
    fi
else
    echo "镜像构建失败，请检查 Dockerfile.base 语法和构建过程。"
    exit 1
fi
