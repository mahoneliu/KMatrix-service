# 安装 PG17、 pgvector/PGroonga扩展
以 Ubuntu 为基础，最快捷地安装带有 pgvector（向量检索）和PGroonga (全文检索) 扩展的 PostgreSQL，最推荐的方式是直接通过 APT 仓库安装编译好的二进制包，而不是从源码手动编译。

## 1. 首先添加 PostgreSQL 官方 APT 仓库，以确保能获取到 17 版本。

```bash
sudo apt update
sudo apt install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc
sudo sh -c 'echo \"deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main\" > /etc/apt/sources.list.d/pgdg.list'\n```

## 2. 安装 PG17 及 pgvector 扩展
```bash
sudo apt install -y postgresql-17 postgresql-17-pgvector
```

## 3. 安装PGroonga
### 3.1. 进入临时目录
```bash
sudo mkdir -p /ky/kmatrix/tmp && cd /ky/kmatrix/tmp
```
### 3.2. 下载组件
(此处省略具体 wget 链接，详见文档)
### 3.3. 安装
```bash
sudo dpkg -i *.deb
sudo apt install -f -y
```

## 4. 启用并连接postgres
默认使用 Peer Authentication，无需密码即可从本地终端以 postgres 用户身份登录。
