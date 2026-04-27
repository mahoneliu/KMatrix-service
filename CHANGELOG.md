# 更新日志

## [v0.5.1-beta](https://github.com/dromara/KMatrix/compare/v0.5-beta...v0.5.1-beta) (2026-04-27)

### 🚀 新功能

- **工作流节点**：
  - 新增变量聚合器节点，支持多路变量汇聚
  - 新增会话变量赋值节点，支持跨轮对话状态持久化
  - 新增参数提取器工作流节点
  - 新增 MCP Resource 读取节点，重构工具执行机制
  - 集成 MCP Resource 节点，重构 LlmChatNode 支持全模态交互（原生 PDF/Video）
  - 增强工作流引擎功能与循环节点保护机制
- **AI 模型**：
  - 新增 Anthropic Claude 模型支持（模型构建与连接测试）
  - 实现 IChatMessageProvider 接口并增强 Qwen 模型集成
  - 增强向量模型管理，支持 SiliconFlow 接口
  - ModelBuilder 增加默认 apiBase 的兜底读取
- **AI 应用**：
  - 重构应用配置文件结构，实现 AI 文件统一管理与服务
  - 对话欢迎页 UI 配置（AppUiSetting, km_app.ui_setting）
  - 实现底层文件上传并重构 AI 模块
  - 实现多模态工作流支持并迁移模板创建应用逻辑
- **对话功能**：
  - 支持中止请求与会话恢复，新增多模态节点
  - 支持在对话界面使用 @ 符号唤出可用技能列表
  - 完善 AI 聊天反馈与应用统计优化
  - 新增用户级限流配置及完善 Token 追踪
- **知识库**：
  - 迁移全文检索从 jieba 到 PGroonga，实现对话限流
  - 重构分块管理，支持动态父子分块与级联删除
  - 支持父子文档分块（Parent-Child Chunking）
- **技能管理**：
  - 实现技能管理系统与工作流技能执行
  - 技能列表公共接口添加 @SaIgnore 跳过鉴权
- **MCP 工具**：
  - 新增 MCP Server 与内置 Python 工具支持
- **其他**：
  - 增加外嵌系统自定义参数传递
  - 节点定义刷新缓存，rate limit 优化（by IP）
  - 实现 AI 文件统一管理与服务

### ♻️ 重构

- 重构 AI 节点基类与对话配置属性
- 升级 LangChain4j 框架至 1.13.0，解决 ONNX 依赖启动报错问题
- 模块化 ruoyi-ai 后端，统一 chat i18n 结构
- 完善后端国际化支持与工作流节点优化
- 重构分块管理，支持动态父子分块与级联删除

### 🐞 Bug 修复

- 修复 onnx 依赖的 dll 版本问题
- 修复会话 ID 的传递方式并补充前端所需数据
- 修复接口参数默认值赋值；debug chat 模式支持参数注入
- 持久化 Nginx 代理配置并优化本地存储路径
- 修复 llmChat 代码规整，km_chat_session.is_resumable 默认值：0
- 移除冗余关键词检索方法和搜索向量字段
- tika OCR 关闭配置；flyway 匹配 pg17 版本；rerank 模型优化

### 🗄️ 数据库迁移

- V0.1.0：新增 Anthropic 模型提供商
- V0.1.1：新增 km_chat_message.raw_message_json 字段
- V0.1.2：新增 MCP Resource 节点定义
- V0.1.3：新增 km_workflow_scope 表
- V0.1.4：新增对话配置必填项
- V0.1.5：新增参数提取器节点定义
- V0.1.6：新增 chat_session 会话变量字段
- V0.1.7：新增变量聚合器节点定义
