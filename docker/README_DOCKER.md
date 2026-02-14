# KMatrix Docker 部署说明

KMatrix 的 Docker 相关文件位于 `kmatrix-service/docker` 目录。

## 📦 两种打包方式对比

| 特性         | 标准版 (Standard) | 自包含版 (Standalone) |
| ------------ | ----------------- | --------------------- |
| **配置文件** | 外部挂载          | 打包在镜像内          |
| **适用场景** | 开发/测试环境     | 生产/演示环境         |
| **配置修改** | 直接修改本地文件  | 需要重新构建镜像      |
| **启动命令** | 较复杂(多个挂载)  | 简单(一键启动)        |

---

## 🚀 发布到私有仓库 (阿里云)

如果你想将 **自包含版** 发布到阿里云私有仓库 (`registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix`)，可以使用以下脚本。

### 1. 发布镜像

```powershell
.\kmatrix-service\docker\push_docker_standalone.ps1
```
*脚本会提示登录阿里云仓库，然后构建并推送镜像。*

### 2. 从仓库拉取运行

在目标服务器上运行：

```powershell
.\kmatrix-service\docker\run_docker_registry.ps1
```
或者直接使用 Docker 命令：

```powershell
docker run -d --name kmatrix -p 80:80 -p 8080:8090 -p 5432:5432 registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix:standalone
```

---

## 🔧 本地构建和运行

### 标准版 (推荐用于开发)

```powershell
# 构建镜像
.\kmatrix-service\docker\build_docker.ps1

# 运行容器
.\kmatrix-service\docker\run_docker.ps1
```

### 自包含版 (推荐用于生产)

```powershell
# 构建镜像
.\kmatrix-service\docker\build_docker_standalone.ps1

# 运行容器
.\kmatrix-service\docker\run_docker_standalone.ps1
```

---

## 📊 数据持久化

所有脚本默认会挂载项目根目录下的以下目录:

| 目录            | 用途            |
| --------------- | --------------- |
| `postgres-data` | PostgreSQL 数据 |
| `redis-data`    | Redis 数据      |
| `uploads`       | 用户上传的文件  |
| `models`        | AI 模型文件     |

**注意**: 请确保在项目根目录运行脚本，以保证数据目录生成在预期位置。
