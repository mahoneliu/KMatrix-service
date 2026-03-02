<p align="center"><img src="https://download.kykms.cn/logo_keyi.png" alt="kmatrix" width="100" /></p>
[中文](./README.md)
<h1 align="center">KMatrix - AI-Enhanced Enterprise Knowledge Base Platform</h1>

<p align="center">
    <strong>A next-generation AI knowledge base workflow platform built on RuoYi-Vue-Plus and LangChain4j</strong>
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

## 📖 Introduction

**KMatrix** is a newly designed version of **KYKMS (Keyi Knowledge Management System)**, focusing on deep integration of traditional document management with advanced AI technologies.

In the digital era, enterprises accumulate vast amounts of unstructured data but often struggle to utilize it effectively. KMatrix aims to solve this pain point by transforming static documents into dynamic knowledge services through **RAG (Retrieval-Augmented Generation)** technology and **visual workflow orchestration**.

KMatrix is not just a document repository; it is an **AI Agent Incubation Platform**. Users can easily build intelligent Q&A assistants, customer service bots, document analysis experts, or business auxiliary bots based on local knowledge bases using a drag-and-drop workflow designer. It also supports natural language queries for databases to address long-tail business needs.

Adhering to the philosophy of ease of use, KMatrix provides an **out-of-the-box** experience. With simple operations, anyone can build a knowledge base and create AI dialogue apps within one or a few days. At the same time, it offers high flexibility for customization to meet complex enterprise-level requirements.

-----------------------------------

## ✨ Core Highlights

- **🚀 Modern Tech Stack**: Backend based on **RuoYi-Vue-Plus (Spring Boot 3 + JDK 17)**, frontend based on **Soybean Admin (Vue 3 + Vite + Naive UI)**, keeping up with technical trends with excellent performance and development experience.
- **🧠 Powerful AI Engine**: Deeply integrated with **LangChain4j** and **LangGraph4j**, providing the strongest AI application development experience in the Java ecosystem.
- **⛓️ Visual Workflow**: Built-in workflow orchestration engine based on **Vue Flow**, supporting node drag-and-drop and connection configuration. Users can customize AI processing flows (e.g., Knowledge Retrieval -> LLM Reasoning -> Result Formatting).
- **📚 Enhanced RAG**: Supports efficient vector retrieval with **PostgreSQL + pgvector**, combined with **Elasticsearch** (planned) hybrid search for precise document Q&A. Supports parsing of various formats including PDF, Word, and Markdown.
- **🔌 Seamless Embedding**: Embed an intelligent Q&A assistant into third-party business systems by copying just one line of script.
- **🌍 Model Agnostic**: Supports integration with various LLMs, including local private models (DeepSeek R1 / Llama 3 / Qwen 2, etc.) and public cloud models from both domestic (Tongyi Qianwen / ByteDance Doubao / Zhipu AI / Kimi, etc.) and international providers (OpenAI / Gemini, etc.).
- **🧩 Modular Design**: Complete separation of frontend and backend.
  - **kmatrix-service**: Robust backend services with RBAC permission support.
  - **kmatrix-ui**: Monorepo architecture containing the management dashboard (`@km/admin`) and embedded chat window (`@km/chat`).
- **🎨 Ultimate UI Experience**: Uses Naive UI components for meticulously crafted interface interactions. Supports dark mode, theme customization, and provides a smooth, Dify-like orchestration experience.
- **🔒 Secure and Controllable**: Supports full on-premise deployment. Combined with Sa-Token authentication and granular permission control, it ensures the security of enterprise knowledge assets.
- For more detailed features, please refer to 👉🏻 [KMatrix spec](http://docs.kykms.cn/docs/kmatrix/spec).

-----------------------------------

## 🛠️ Technical Architecture

### Backend (kmatrix-service)

- **Core Framework**: Spring Boot 3.5.7
- **Language**: Java 17+
- **ORM Framework**: MyBatis Plus 3.5.14 + Dynamic Datasource
- **Database**: PostgreSQL (Recommended, requires pgvector plugin) / MySQL / Oracle
- **AI Framework**: LangChain4j, LangGraph4j
- **Authentication**: Sa-Token 1.44.0 (JWT)
- **Cache**: Redis 5+ (Redisson)
- **Utils**: Hutool, Lombok, Knife4j

### Frontend (kmatrix-ui)

- **Core Framework**: Vue 3.5.25
- **Build Tool**: Vite 7.2.6
- **Language**: TypeScript 5.9.3
- **UI Framework**: Naive UI 2.43.2 + TailwindCSS (UnoCSS)
- **Workflow**: Vue Flow 1.48.1
- **Boilerplate**: Soybean Admin
- **Package Management**: pnpm (Monorepo)

-----------------------------------

## 📂 Project Structure

KMatrix follows a frontend-backend separation architecture. The code is organized as follows:

```none
KMatrix/
├── kmatrix-service/          # Backend service (Maven multi-module)
│   ├── ruoyi-admin/          # Web service entry point
│   ├── ruoyi-ai/             # AI core module (LangChain, RAG, Workflow)
│   ├── ruoyi-common/         # Common module
│   └── ...
├── kmatrix-ui/               # Frontend project (pnpm workspace)
│   ├── apps/
│   │   ├── admin/            # Admin dashboard (Knowledge base management, App orchestration)
│   │   └── chat/             # Chat window (Embedded AI assistant)
│   ├── packages/             # Shared packages (hooks, utils, materials)
│   └── ...
└── docker/                   # Container deployment scripts
```

This repository contains the backend project. Link to the frontend project: [https://gitee.com/kyxxjs/kmatrix-ui](https://gitee.com/kyxxjs/kmatrix-ui)

-----------------------------------

## 🚀 Quick Start

### Deployment via Docker

- For a quick trial and exploration, it's highly recommended to use Docker for simple and fast deployment.
- One-click startup:

```bash
* Linux:
docker run -d --name kmatrix-standalone -p 80:80 -v ~/kmatrix-data:/kmatrix-data registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix:latest

* Windows:
docker run -d --name kmatrix-standalone -p 80:80 -v c:\kmatrix-data:/kmatrix-data registry.cn-guangzhou.aliyuncs.com/kyxxjs/kmatrix:latest
```

- After all containers show a `healthy` status, access KMatrix via your browser:

  - http://[Target-Server-IP]

- Default Login:
  - Username: admin
  - Default Password: admin123

### Deployment via Source Code

#### Prerequisites

- **JDK**: >= 17
- **Node.js**: >= 20.19.0
- **pnpm**: >= 9.x
- **Database**: PostgreSQL 15+ (requires `vector` plugin and `jieba` tokenizer)
- **Redis**: 6.x+

1. **Environment Setup**:
    - Install Redis.
    - Download the rerank model.
    - Install PostgreSQL and enable pgvector extension and jieba tokenizer.
    - Initialize the database by importing the provided SQL script `kmatrix_complete.sql`.
    Note: PG deployment scripts and rerank model download scripts are available in the `script` directory.

2. **Backend Startup (kmatrix-service)**:

    ```bash
    cd kmatrix-service
    # Copy application-sample.yml to application-dev.yml and modify database, Redis, and rerank model configurations.
    mvn clean install
    # Start the ruoyi-admin module
    java -jar ruoyi-admin/target/ruoyi-admin.jar
    ```

3. **Frontend Startup (kmatrix-ui)**:

    ```bash
    cd kmatrix-ui
    pnpm install
    # Start the admin dashboard
    pnpm dev:admin
    # Start the chat app
    pnpm dev:chat
    ```

-----------------------------------

## 🔗 Links and Contact

- **Online Demo**: [http://kmatrix.kykms.cn](http://kmatrix.kykms.cn) (Credentials: test/666666 or admin1/admin123)
- **Floating Window Demo**: Visit the [Keyi Official Website](http://www.kykms.cn), check the chat icon in the bottom-right corner.
- **Tech Documentation**: [Quick Start](http://docs.kykms.cn/docs/kmatrix/kmatrix-1h4rc8em9u0c4)
- **WeChat**: Scan to join the community or for business cooperation.

  ![WeChat](./docs/images/wechat.jpg)
- **Email**: <service@mail.kykms.cn>

-----------------------------------

## 🤝 Special Thanks

KMatrix stands on the shoulders of giants. Special thanks to these excellent open-source projects:

- **RuoYi-Vue-Plus**: [https://gitee.com/dromara/RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus)
- **Soybean Admin**: [https://github.com/soybeanjs/soybean-admin](https://github.com/soybeanjs/soybean-admin)
- **LangChain4j**: [https://github.com/langchain4j/langchain4j](https://github.com/langchain4j/langchain4j)
- **Vue Flow**: [https://github.com/bcakmakoglu/vue-flow](https://github.com/bcakmakoglu/vue-flow)

-----------------------------------

## 📄 License

This software is licensed under the **MIT** License. You are free to use, modify, and distribute it, provided you retain the original copyright notice.
