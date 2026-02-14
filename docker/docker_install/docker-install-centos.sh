#!/bin/bash
# shellcheck disable=SC2046
BASE_PATH=$(cd `dirname $0`;pwd)
echo "当前系统版本：";sudo cat /etc/redhat-release;
echo -e "\033[46;37;5m -------------- 开始安装docker所需环境 -------------- \033[0m";
# 安装docker环境
echo " ------------ 开始安装docker服务 ------------ ";
yum install -y yum-utils device-mapper-persistent-data lvm2;
# 如果是腾讯的centos，要注释掉下面这一句，否则会报错
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo;
yum install -y docker-ce;
systemctl start docker;
systemctl enable docker;
docker version;
echo " ------------ docker服务安装完毕 ------------ ";
