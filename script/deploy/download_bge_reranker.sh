#!/bin/bash
# BGE-Reranker ONNX 模型下载脚本
# 来源: https://huggingface.co/corto-ai/bge-reranker-large-onnx

set -e

MODEL_DIR="/opt/models/bge-reranker-large-onnx"

echo "==========================================="
echo " BGE-Reranker ONNX 模型下载"
echo "==========================================="
echo ""

# 检查 git-lfs
if ! command -v git-lfs &> /dev/null; then
    echo "[ERROR] git-lfs 未安装，请先安装:"
    echo "  Ubuntu/Debian: sudo apt install git-lfs"
    echo "  CentOS/RHEL:   sudo yum install git-lfs"
    echo "  macOS:         brew install git-lfs"
    exit 1
fi

# 创建目录
echo "[1/3] 创建模型目录: $MODEL_DIR"
sudo mkdir -p "$MODEL_DIR"
sudo chown $(whoami) "$MODEL_DIR"
cd "$MODEL_DIR"

# 克隆模型仓库
echo "[2/3] 下载模型文件 (约 2.3GB, 请耐心等待)..."
git lfs install
git clone https://huggingface.co/corto-ai/bge-reranker-large-onnx .

# 验证文件
echo "[3/3] 验证文件..."
if [ -f "model.onnx" ] && [ -f "model.onnx_data" ] && [ -f "sentencepiece.bpe.model" ]; then
    echo ""
    echo "✅ 下载完成!"
    echo ""
    echo "文件列表:"
    ls -lh model.onnx model.onnx_data sentencepiece.bpe.model
    echo ""
    echo "==========================================="
    echo " 下一步: 修改 application.yml"
    echo "==========================================="
    echo ""
    echo "ai:"
    echo "  reranker:"
    echo "    enabled: true"
    echo "    model-path: $MODEL_DIR/model.onnx"
    echo "    tokenizer-path: $MODEL_DIR/sentencepiece.bpe.model"
    echo ""
else
    echo "[ERROR] 部分文件缺失，请检查下载状态"
    exit 1
fi
