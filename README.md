<p align="center"><img src="https://download.kykms.cn/logo_keyi.png" alt="kmatrix" width="100" /></p>
<h1 align="center">KMatrix - AI 增强型企业知识库平台</h1>

<p align="center">
    <strong>基于 RuoYi-Vue-Plus 与 LangChain4j 构建的新一代 AI 知识库工作流平台</strong>
</p>

<p align="center">
    <a href="https://gitee.com/kyxxjs/kmatrix-service">
        <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" />
    </a>
    <a href="https://spring.io/projects/spring-boot">
        <img src="https://img.shields.io/badge/Spring%20Boot-3.5.7-green.svg" alt="Spring Boot" />
    </a>
    <a href="https://vuejs.org/">
        <img src="https://img.shields.io/badge/Vue-3.5.25-4FC08D.svg" alt="Vue 3" />
    </a>
    <a href="https://www.postgresql.org/">
        <img src="https://img.shields.io/badge/PostgreSQL-17+-336791.svg" alt="PostgreSQL" />
    </a>
</p>

-----------------------------------

## 📖 项目简介

**KMatrix** 是**科亿知识库 (KYKMS)** 的全新设计版本，专注于将传统的文档管理与先进的 AI 技术深度融合。

在数字化时代，企业积累了海量非结构化数据，但往往难以有效利用。KMatrix 致力于解决这一痛点，通过 **RAG (检索增强生成)** 技术和 **可视化工作流编排**，将静态文档转化为动态知识服务。

KMatrix 不仅仅是一个文档存储库，更是一个 **AI Agent 孵化平台**。用户可以通过拖拽式的工作流设计器，轻松构建基于本地知识库的智能问答助手、客服机器人、文档分析专家或业务辅助机器人，也可以通过自然语言查询数据库以解决长尾业务需求。

kmatrix秉承易用至上的理念，提供 **开箱即用** 的体验，简单操作即可上手，一天至数天即可完成知识库的构建和AI对话App的打造，同时也提供高度自定义的灵活性，满足企业级应用的复杂需求。

-----------------------------------

## ✨ 核心亮点

- **🚀 现代技术栈**：后端基于 **RuoYi-Vue-Plus (Spring Boot 3 + JDK 17)**，前端基于 **Soybean Admin (Vue 3 + Vite + Naive UI)**，紧跟技术潮流，性能卓越，开发体验极佳。
- **🧠 强大的 AI 引擎**：深度集成 **LangChain4j** 和 **LangGraph4j**，提供 Java 领域最强的 AI 应用开发体验。
- **⛓️ 可视化工作流 (Workflow)**：内置基于 **Vue Flow** 的工作流编排引擎，支持节点拖拽、连线配置。用户可自定义 AI 处理流程（如：知识检索 -> LLM 思考 -> 结果格式化）。
- **📚 增强型 RAG**：支持 **PostgreSQL + pgvector** 高效向量检索，结合 **Elasticsearch** (计划中) 混合检索，提供精准的文档问答能力。支持 PDF、Word、Markdown 等多种格式解析。
- **🔌 无缝嵌入**：拷贝一行脚本即可嵌入到第三方业务系统，让已有系统快速拥有智能问答能力。
- **🌍 模型中立**：支持对接各种大模型，包括本地私有大模型（DeepSeek R1 / Llama 3 / Qwen 2 等）、国内公共大模型（通义千问 / 字节豆包 / 智谱 AI / Kimi 等）和国外公共大模型（OpenAI / Gemini 等）。
- **🧩 模块化设计**：前后端完全分离。
  - **kmatrix-service**: 强大的后端服务，支持RBAC 权限。
  - **kmatrix-ui**: Monorepo 架构，包含管理端 (`@km/admin`) 和 嵌入式聊天窗口 (`@km/chat`)。
- **🎨 极致 UI 体验**：使用 Naive UI 组件库，精心打磨的界面交互，支持暗黑模式、主题定制，提供类 Dify 的流畅编排体验。
- **🔒 安全可控**：支持完全私有化部署，结合 Sa-Token 认证与精细化权限控制，确保企业知识资产安全。
- 更详细的功能介绍，请参考 👉🏻 [KMatrix spec](http://docs.kykms.cn/docs/kmatrix/spec)。

-----------------------------------

## 🛠️ 技术架构

### 后端 (kmatrix-service)

- **基础框架**: Spring Boot 3.5.7
- **编程语言**: Java 17+
- **ORM 框架**: MyBatis Plus 3.5.14 + Dynamic Datasource
- **数据库**: PostgreSQL (推荐, 需开启 pgvector 插件) / MySQL / Oracle
- **AI 框架**: LangChain4j, LangGraph4j
- **权限认证**: Sa-Token 1.44.0 (JWT)
- **缓存**: Redis 5+ (Redisson)
- **工具**: Hutool, Lombok, Knife4j

### 前端 (kmatrix-ui)

- **核心框架**: Vue 3.5.25
- **构建工具**: Vite 7.2.6
- **语言**: TypeScript 5.9.3
- **UI 框架**: Naive UI 2.43.2 + TailwindCSS (UnoCSS)
- **工作流**: Vue Flow 1.48.1
- **脚手架**: Soybean Admin
- **包管理**: pnpm (Monorepo)

-----------------------------------

## 📂 项目结构

KMatrix 采用前后端分离架构，代码组织如下：

```none
KMatrix/
├── kmatrix-service/          # 后端服务 (Maven 多模块)
│   ├── ruoyi-admin/          # Web 服务入口
│   ├── ruoyi-ai/             # AI 核心模块 (LangChain, RAG, Workflow)
│   ├── ruoyi-common/         # 通用模块
│   └── ...
├── kmatrix-ui/               # 前端工程 (pnpm workspace)
│   ├── apps/
│   │   ├── admin/            # 管理后台 (知识库管理, 应用编排)
│   │   └── chat/             # 对话窗口 (嵌入式 AI 助手)
│   ├── packages/             # 共享包 (hooks, utils, materials)
│   └── ...
└── docker/                   # 容器化部署脚本
```

当前代码为KMatrix的后端项目，前端项目链接：[https://gitee.com/kyxxjs/kmatrix-ui](https://gitee.com/kyxxjs/kmatrix-ui)

-----------------------------------

## 🚀 快速开始

### 通过docker部署

- 如果想体验Kmatrix，试用一下，强烈建议使用docker部署，简单快捷。
- 一键启动，即可体验Kmatrix：

```bash
* linux系统：
docker run -d --name kmatrix-standalone -p 80:80 -v ~/kmatrix-data:/kmatrix-data registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix:standalone

* windows系统：
docker run -d --name kmatrix-standalone -p 80:80 -v c:/kmatrix-data:/kmatrix-data registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix:standalone
```

- 待所有容器状态显示为healthy后，可通过浏览器访问 KMatrix：

  - http://目标服务器 IP 地址

- 默认登录信息
  - 用户名：admin
  - 默认密码：admin123

### 通过代码部署

#### 环境要求

- **JDK**: >= 17
- **Node.js**: >= 20.19.0
- **pnpm**: >= 9.x
- **Database**: PostgreSQL 15+ (需安装 `vector` 插件和`jieba`分词器)
- **Redis**: 5.x+

1. **数据库准备**:
    - 安装 PostgreSQL 并启用 vector 扩展和 jieba 分词器。
    - 下载rerank模型。
    - 初始化数据库，导入项目提供的 SQL 脚本`kmatrix_complete.sql`。  
    注：script目录下有PG部署脚本和rerank模型下载脚本可参考

2. **后端启动 (kmatrix-service)**:

    ```bash
    cd kmatrix-service
    # 修改 application-dev.yml 中的数据库和 Redis 配置 、rerank模型配置
    mvn clean install
    # 启动 ruoyi-admin 模块
    java -jar ruoyi-admin/target/ruoyi-admin.jar
    ```

3. **前端启动 (kmatrix-ui)**:

    ```bash
    cd kmatrix-ui
    pnpm install
    # 启动管理端
    pnpm dev:admin
    # 启动聊天端
    pnpm dev:chat
    ```

-----------------------------------

## 🔗 链接与交流

- **在线试用**: [http://kmatrix.kykms.cn](http://kmatrix.kykms.cn) 账密：test/666666
- **浮窗嵌入效果**: 访问[科亿官网](http://www.kykms.cn)，右下角的聊天入口图标。
- **技术文档**: [快速开始](http://docs.kykms.cn/docs/kmatrix/kmatrix-1h4rc8em9u0c4)
- **微信**: 加群或者商业合作洽谈，可扫码

  ![微信](./docs/images/wechat.jpg)
- **邮箱**: <service@mail.kykms.cn>

-----------------------------------

## 🤝 特别鸣谢

本项目站在巨人的肩膀上，特别感谢以下优秀开源项目：

- **RuoYi-Vue-Plus**: [https://gitee.com/dromara/RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)
- **Soybean Admin**: [https://github.com/soybeanjs/soybean-admin](https://github.com/soybeanjs/soybean-admin)
- **LangChain4j**: [https://github.com/langchain4j/langchain4j](https://github.com/langchain4j/langchain4j)
- **Vue Flow**: [https://github.com/bcakmakoglu/vue-flow](https://github.com/bcakmakoglu/vue-flow)

-----------------------------------

## 📄 版权声明

本软件开源授权许可为 **MIT**。您可以自由使用、修改和分发，但请保留原作者的版权声明。
