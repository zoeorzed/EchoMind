# EchoMind Java

EchoMind Java 是一个企业级智能客服系统，基于 Java 21、Spring Boot 3.5、Spring AI 1.1 和 LangChain4j 实现。当前版本覆盖智能客服主链路：对话请求、Redis 工作记忆、知识库检索、多 Agent 路由、LLM 模型调用、回答校验、评测、监控、Swagger 文档和 Docker 部署。

## 技术栈

| 类型 | 技术 |
|------|------|
| 语言 | Java 21 |
| Web 框架 | Spring Boot 3.5 |
| AI 框架 | Spring AI 1.1 |
| LLM Provider | Anthropic、DeepSeek |
| 文档处理 | LangChain4j DocumentSplitter |
| 记忆缓存 | Spring Data Redis |
| RAG | BM25 + 本地 hash vector + LLM rerank |
| 持久化 | Redis 工作记忆 + JSON 知识库、长期记忆、用户画像 |
| 监控 | Spring Boot Actuator、Micrometer、Prometheus |
| API 文档 | Springdoc OpenAPI、Swagger UI |
| 部署 | Docker、Docker Compose、Nginx、Prometheus |
| 构建 | Maven Wrapper |

## 核心链路

```text
POST /chat
  -> MemoryManager 读取 Redis 工作记忆、会话摘要、长期记忆、用户画像
  -> KnowledgeToolManager 做查询改写、并行召回、LLM rerank
  -> AgentOrchestrator 做意图识别和 Agent 路由
  -> General / Technical / Billing Agent 生成回复
  -> AnswerVerifier 校验回复是否可信、是否需要转人工
  -> 写入 Redis，并异步更新用户画像
```

主要实现：

- `src/main/java/com/echomind/api/EchoMindController.java`
- `src/main/java/com/echomind/memory/MemoryManager.java`
- `src/main/java/com/echomind/tool/KnowledgeToolManager.java`
- `src/main/java/com/echomind/agent/AgentOrchestrator.java`
- `src/main/java/com/echomind/agent/AnswerVerifier.java`

## 项目结构

```text
EchoMindJava/
├── src/main/java/com/echomind/
│   ├── api/          # HTTP 接口和 DTO
│   ├── agent/        # Agent 编排、General / Technical / Billing Agent、回答校验
│   ├── config/       # 应用、异步、OpenAPI、调度和配置属性
│   ├── evaluation/   # 端到端评测、LLM Judge
│   ├── intent/       # 意图识别和结果模型
│   ├── knowledge/    # 知识库服务、文档和检索结果
│   ├── llm/          # LLM 网关和 Spring AI 实现
│   ├── memory/       # Redis 工作记忆和长期记忆管理
│   ├── monitor/      # 性能监控和告警
│   ├── skill/        # 客服技能加载
│   └── tool/         # 知识工具管理、熔断和统计
├── src/main/resources/
│   └── application.yml
├── skills/
│   ├── general_customer_service/
│   ├── technical_support/
│   └── billing_support/
├── config/
│   ├── nginx/
│   │   └── nginx.conf
│   └── prometheus.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── mvnw
└── mvnw.cmd
```

## 主要能力

### 多 Agent 路由

系统内置三类客服 Agent：

- `GeneralAgent`
- `TechnicalAgent`
- `BillingAgent`

`AgentOrchestrator` 使用意图识别、历史性能和降级策略完成路由；复合问题支持并行处理。

### LLM 模型切换

通过 Spring profile 选择 Anthropic 或 DeepSeek：

```env
SPRING_PROFILES_ACTIVE=deepseek
DEEPSEEK_API_KEY=your_key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
```

Anthropic 配置：

```env
SPRING_PROFILES_ACTIVE=anthropic
ANTHROPIC_API_KEY=your_key
ANTHROPIC_BASE_URL=https://api.anthropic.com
ANTHROPIC_MODEL=claude-3-5-sonnet-20241022
```

缺少真实 API Key 时，系统会使用 `echomind-local-placeholder` 避免 Spring AI 自动配置阶段直接失败。若开启：

```env
LLM_FALLBACK_ENABLED=true
```

真实调用失败时会返回本地降级回复。

### 记忆和知识库

- Redis 保存工作记忆。
- `data/java/memory-store.json` 保存长期记忆和用户画像。
- `data/java/knowledge-store.json` 保存知识库片段。
- `data/eval/baseline.json` 保存评测基线。

Docker 环境中，应用数据保存到 `/app/data/java`，由 `app-data` volume 持久化。`data/` 目录属于运行时数据，不应提交到 Git 仓库。

### Hybrid RAG

知识库检索链路：

```text
文档导入
  -> LangChain4j recursive splitter
  -> JSON 持久化
  -> 本地 documents 索引

查询
  -> LLM 查询改写
  -> 多子查询并行召回
  -> BM25 关键词得分
  -> 本地 hash vector 语义得分
  -> 加权融合
  -> LLM rerank
  -> fallback 到融合分排序
```

知识工具还支持缓存、超时、熔断、降级和统计。

### 回答校验

`AnswerVerifier` 会校验回复是否可信，并在响应中返回：

- `verified`
- `grounded`
- `escalated`

### 评测和监控

系统提供：

- Intent Accuracy
- Macro-F1
- per-class Precision / Recall / F1
- LLM-as-Judge 四维评分
- baseline 保存和回归检测
- `/monitor` 监控摘要
- `/metrics` Prometheus 指标
- `/actuator/prometheus` Actuator 指标
- Webhook 告警
- Agent 路由惩罚反馈

主要实现：

- `src/main/java/com/echomind/evaluation/EndToEndEvaluator.java`
- `src/main/java/com/echomind/evaluation/LLMJudge.java`
- `src/main/java/com/echomind/monitor/PerformanceMonitor.java`

### Swagger / OpenAPI

- Swagger UI：`http://localhost:8080/docs`
- Nginx 代理：`http://localhost:8081/docs`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

`/docs` 页面会从 jsdelivr CDN 加载 Swagger UI 静态资源。如果部署环境无法访问外网，需要将 Swagger UI 静态资源放到项目本地。

## 主要接口

默认端口：`8080`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/chat` | 主对话接口 |
| POST | `/search` | 知识库检索 |
| POST | `/knowledge/add` | 批量添加知识文档 |
| POST | `/knowledge/upload` | 上传 `.txt` / `.md` / `.json` 文件 |
| GET | `/knowledge/stats` | 知识库统计 |
| GET | `/monitor` | 监控摘要 |
| GET | `/metrics` | Prometheus 指标 |
| GET | `/actuator/prometheus` | Spring Actuator Prometheus 指标 |
| POST | `/eval/run` | 运行评测 |
| GET | `/docs` | Swagger UI |
| GET | `/v3/api-docs` | OpenAPI JSON |

响应字段使用 Jackson `SNAKE_CASE`：

```json
{
  "conversation_id": "...",
  "response": "...",
  "intent": "...",
  "agent_type": "...",
  "escalated": false,
  "latency_ms": 123,
  "knowledge_used": true,
  "verified": true,
  "grounded": true
}
```

推荐请求字段：

```json
{
  "message": "我想申请退款",
  "user_id": "u1001",
  "conversation_id": "optional-conversation-id"
}
```

## 环境准备

- JDK 21 或更高版本
- Docker Desktop 或 Docker Engine
- Anthropic 或 DeepSeek API Key

## 本地运行

### macOS / Linux

启动依赖：

```bash
docker compose up -d redis chromadb
```

DeepSeek 启动：

```bash
export SPRING_PROFILES_ACTIVE=deepseek
export DEEPSEEK_API_KEY=your_key
./mvnw spring-boot:run
```

Anthropic 启动：

```bash
export SPRING_PROFILES_ACTIVE=anthropic
export ANTHROPIC_API_KEY=your_key
./mvnw spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/health
```

Swagger：

```text
http://localhost:8080/docs
```

### Windows PowerShell

启动依赖：

```powershell
docker compose up -d redis chromadb
```

DeepSeek 启动：

```powershell
$env:SPRING_PROFILES_ACTIVE="deepseek"
$env:DEEPSEEK_API_KEY="your_key"
.\mvnw.cmd spring-boot:run
```

Anthropic 启动：

```powershell
$env:SPRING_PROFILES_ACTIVE="anthropic"
$env:ANTHROPIC_API_KEY="your_key"
.\mvnw.cmd spring-boot:run
```

健康检查：

```powershell
curl http://localhost:8080/health
```

Swagger：

```text
http://localhost:8080/docs
```

## Docker 部署

复制配置：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，选择模型 profile 并填写对应 API Key。本地开发默认 Redis 密码为 `echomind123`，生产环境请替换为独立密码。

启动：

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f echomind-java
```

停止：

```bash
docker compose down
```

Compose 服务和端口：

| 服务 | 容器名 | 地址 |
|------|--------|------|
| Java App | `echomind-java-app` | `http://localhost:8080` |
| Nginx | `echomind-java-nginx` | `http://localhost:8081` |
| Prometheus | `echomind-java-prometheus` | `http://localhost:9091` |
| ChromaDB | `echomind-java-chromadb` | `http://localhost:8002` |
| Redis | `echomind-java-redis` | `localhost:6380` |

常用验证：

```bash
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8080/metrics
```

Swagger：

```text
http://localhost:8080/docs
http://localhost:8081/docs
```

## API 示例

### 对话

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想申请退款，订单号是 #12345",
    "user_id": "u1001"
  }'
```

也可以打开 Swagger UI，通过页面直接调用：

```text
http://localhost:8080/docs
```

### 检索

```bash
curl -X POST "http://localhost:8080/search?query=退款多久能到账&topK=3"
```

### 添加知识库

```bash
curl -X POST http://localhost:8080/knowledge/add \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [
      {
        "title": "退款补充政策",
        "content": "大促期间退款审核时间可能延长到 3-5 个工作日。"
      }
    ]
  }'
```

### 上传知识库文件

```bash
curl -X POST http://localhost:8080/knowledge/upload \
  -F "file=@docs.md"
```

### 运行评测

```bash
curl -X POST http://localhost:8080/eval/run \
  -H "Content-Type: application/json" \
  -d '{}'
```