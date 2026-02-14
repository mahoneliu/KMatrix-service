#!/bin/bash
# shellcheck disable=SC2046
BASE_PATH=$(cd `dirname $0`;pwd)
echo "当前系统版本：";sudo cat /etc/redhat-release;
echo -e "\033[46;37;5m -------------- 开始安装docker所需环境 -------------- \033[0m";
# 安装docker环境
echo " ------------ 开始安装docker服务 ------------ ";
apt-get install -y docker-ce;
systemctl start docker;
systemctl enable docker;
docker version;
echo " ------------ docker服务安装完毕 ------------ ";
