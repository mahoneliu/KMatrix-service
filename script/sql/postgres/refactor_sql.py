#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SQL 初始化脚本重构工具
将 kmatrix_complete.sql 重构为更清晰的结构
"""

import re
from pathlib import Path
from typing import List, Dict, Tuple

class SQLRefactor:
    def __init__(self, input_file: str):
        self.input_file = Path(input_file)
        self.tables = {}  # 存储表定义
        self.inserts = {}  # 存储插入语句
        
    def read_file(self) -> str:
        """读取 SQL 文件"""
        with open(self.input_file, 'r', encoding='utf-8') as f:
            return f.read()
    
    def extract_table_definitions(self, content: str) -> Dict[str, List[str]]:
        """提取所有表定义(CREATE TABLE + COMMENT)"""
        tables = {}
        
        # 匹配 CREATE TABLE 到下一个 CREATE TABLE 或 INSERT 之间的所有内容
        pattern = r'(-- -+\s*\n.*?create table.*?(?=(?:-- -+\s*\n.*?create table|INSERT INTO|-- 字符串自动转时间|$)))'
        matches = re.finditer(pattern, content, re.IGNORECASE | re.DOTALL)
        
        for match in matches:
            table_def = match.group(1).strip()
            
            # 跳过测试表
            if 'test_demo' in table_def.lower() or 'test_tree' in table_def.lower():
                continue
            
            # 提取表名
            table_match = re.search(r'create table\s+(?:if not exists\s+)?(\w+)', table_def, re.IGNORECASE)
            if table_match:
                table_name = table_match.group(1)
                
                # 分类表
                category = self._categorize_table(table_name)
                if category not in tables:
                    tables[category] = []
                tables[category].append(table_def)
        
        return tables
    
    def extract_insert_statements(self, content: str) -> Dict[str, List[str]]:
        """提取所有 INSERT 语句"""
        inserts = {}
        
        # 匹配所有 INSERT 语句
        pattern = r"((?:-- .+\n)?INSERT INTO \w+[^;]+;)"
        matches = re.finditer(pattern, content, re.IGNORECASE)
        
        for match in matches:
            insert_stmt = match.group(1).strip()
            
            # 跳过测试表
            if 'test_demo' in insert_stmt.lower() or 'test_tree' in insert_stmt.lower():
                continue
            
            # 提取表名
            table_match = re.search(r'INSERT INTO\s+(\w+)', insert_stmt, re.IGNORECASE)
            if table_match:
                table_name = table_match.group(1)
                
                # 分类
                category = self._categorize_table(table_name)
                if category not in inserts:
                    inserts[category] = []
                inserts[category].append(insert_stmt)
        
        return inserts
    
    def _categorize_table(self, table_name: str) -> str:
        """根据表名分类"""
        table_name_lower = table_name.lower()
        
        # 系统核心表
        if table_name_lower in ['sys_dept', 'sys_user', 'sys_role', 'sys_post']:
            return '1_系统核心表'
        
        # 菜单权限表
        if table_name_lower in ['sys_menu', 'sys_user_role', 'sys_role_menu', 'sys_role_dept', 'sys_user_post']:
            return '2_菜单权限表'
        
        # 字典配置表
        if table_name_lower in ['sys_dict_type', 'sys_dict_data', 'sys_config']:
            return '3_字典配置表'
        
        # 日志表
        if table_name_lower in ['sys_oper_log', 'sys_logininfor']:
            return '4_日志表'
        
        # OSS 表
        if table_name_lower in ['sys_oss', 'sys_oss_config']:
            return '5_OSS表'
        
        # 客户端和社交表
        if table_name_lower in ['sys_client', 'sys_social']:
            return '6_客户端社交表'
        
        # 通知公告表
        if table_name_lower in ['sys_notice']:
            return '7_通知公告表'
        
        # 代码生成表
        if table_name_lower in ['gen_table', 'gen_table_column']:
            return '8_代码生成表'
        
        # AI 知识库表
        if table_name_lower.startswith('km_knowledge') or table_name_lower.startswith('km_document') or \
           table_name_lower.startswith('km_qa') or table_name_lower.startswith('km_temp'):
            return '9_AI知识库表'
        
        # AI 工作流表
        if table_name_lower.startswith('km_workflow') or table_name_lower.startswith('km_node'):
            return '10_AI工作流表'
        
        # AI 对话表
        if table_name_lower.startswith('km_chat') or table_name_lower.startswith('km_app'):
            return '11_AI对话表'
        
        # 其他
        return '99_其他表'
    
    def generate_refactored_sql(self, tables: Dict, inserts: Dict) -> str:
        """生成重构后的 SQL"""
        lines = []
        
        # 文件头
        lines.append("-- " + "=" * 70)
        lines.append("-- KMatrix 数据库初始化脚本")
        lines.append("-- PostgreSQL 17+")
        lines.append("-- 自动生成时间: " + "2026-02-09")
        lines.append("-- " + "=" * 70)
        lines.append("")
        lines.append("")
        
        # 第一部分:表结构定义
        lines.append("-- " + "=" * 70)
        lines.append("-- 第一部分:表结构定义")
        lines.append("-- " + "=" * 70)
        lines.append("")
        
        for category in sorted(tables.keys()):
            lines.append("")
            lines.append("-- " + "-" * 70)
            lines.append(f"-- {category}")
            lines.append("-- " + "-" * 70)
            lines.append("")
            
            for table_def in tables[category]:
                lines.append(table_def)
                lines.append("")
        
        # 第二部分:初始化数据
        lines.append("")
        lines.append("-- " + "=" * 70)
        lines.append("-- 第二部分:初始化数据")
        lines.append("-- " + "=" * 70)
        lines.append("")
        
        for category in sorted(inserts.keys()):
            lines.append("")
            lines.append("-- " + "-" * 70)
            lines.append(f"-- {category}初始化数据")
            lines.append("-- " + "-" * 70)
            lines.append("")
            
            for insert_stmt in inserts[category]:
                lines.append(insert_stmt)
                lines.append("")
        
        # 添加时间转换函数
        lines.append("")
        lines.append("-- " + "-" * 70)
        lines.append("-- 辅助函数")
        lines.append("-- " + "-" * 70)
        lines.append("")
        lines.append("-- 字符串自动转时间 避免框架时间查询报错问题")
        lines.append("create or replace function cast_varchar_to_timestamp(varchar) returns timestamptz as $$")
        lines.append("select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');")
        lines.append("$$ language sql strict ;")
        lines.append("")
        lines.append("create cast (varchar as timestamptz) with function cast_varchar_to_timestamp as IMPLICIT;")
        lines.append("")
        
        return '\n'.join(lines)
    
    def refactor(self, output_file: str):
        """执行重构"""
        print(f"读取文件: {self.input_file}")
        content = self.read_file()
        
        print("提取表定义...")
        tables = self.extract_table_definitions(content)
        print(f"  找到 {sum(len(v) for v in tables.values())} 个表定义")
        
        print("提取 INSERT 语句...")
        inserts = self.extract_insert_statements(content)
        print(f"  找到 {sum(len(v) for v in inserts.values())} 条 INSERT 语句")
        
        print("生成重构后的 SQL...")
        refactored_sql = self.generate_refactored_sql(tables, inserts)
        
        print(f"写入文件: {output_file}")
        output_path = Path(output_file)
        with open(output_path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(refactored_sql)
        
        print("✅ 重构完成!")
        print(f"   原文件: {self.input_file}")
        print(f"   新文件: {output_path}")
        print(f"   表分类: {len(tables)} 个类别")
        print(f"   数据分类: {len(inserts)} 个类别")

if __name__ == '__main__':
    input_file = r'f:/code/KMatrix/kmatrix-service/script/sql/postgres/kmatrix_complete.sql'
    output_file = r'f:/code/KMatrix/kmatrix-service/script/sql/postgres/kmatrix_complete_new.sql'
    
    refactor = SQLRefactor(input_file)
    refactor.refactor(output_file)
