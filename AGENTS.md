# 项目介绍

星云 ERP 是基于 SpringBoot 框架的中小企业完全开源的 ERP。

## Tech Stack

- Backend: Springboot 2.2.2.RELEASE / Java 8 / Maven / EasyExcel 2.2.10
- Database: MySql5.7 / MyBatis-plus 3.4.2
- Cache & MQ: Redis / Spring-session-data-redis 2.2.0.RELEASE / RabbitMQ
- Frontend: Vue 3.3.4 + ant-design-vue 4.0.7 + Vite + vxe-table 4.4.5 + vue-vben-admin 2.10（`frontend/`）

## Commands

- 构建：`mvn clean compile -DskipTests`
- 测试：`mvn test`
- 前端启动：`cd frontend && pnpm run dev`
- 前端检查：`cd frontend && pnpm run lint`

## Architecture

- 多模块 Maven 项目，按功能分模块。
- 后端遵循 `Controller -> Service -> Repository` 分层。
- 基础设施能力放在 `common/`，包括限流、AI 调用、异步任务、配置、异常、统一响应。
- 前端代码放在 `frontend/`。
- 详细项目结构见 `docs/architecture.md`。

## Must Follow

- Controller 只做参数校验和响应包装，不写业务逻辑。
- Service 承担业务编排，`@Transactional` 只放 Service 层。
- DAO 只负责数据访问，不写业务逻辑。
- 对外响应统一使用 `Result<T>`。
- 业务异常必须使用 `BusinessException(ErrorCode.XXX, "描述信息")`。
- Entity 映射使用 MapStruct，禁止手写重复转换逻辑。
- LLM、S3、外部 HTTP 调用不得放在数据库事务内。
- 统一通过 `LlmProviderRegistry` 获取 `ChatClient`。
- 结构化输出统一使用 `StructuredOutputInvoker` 做重试包装。
- Redis Stream 生产/消费使用 `AbstractStreamProducer` / `AbstractStreamConsumer` 模板。
- 限流使用 `@RateLimit`，不要手写散落的 Redis 限流逻辑。
- 数据库向量搜索使用 PostgreSQL + pgvector，维度为 1024，距离类型为 COSINE。

## Never Do

- 不要 `throw new RuntimeException(...)`，必须用 `BusinessException`。
- 不要直接返回 Entity 给前端。
- 不要把 `@Value` 散落在 Service 中，配置集中到 `@ConfigurationProperties`。
- 不要内联全限定类名，使用 import。
- 不要事务内调用 LLM、S3 或外部 HTTP。
- 不要同类内部调用 `@Transactional` 方法。
- 不要 `catch (Exception e) {}` 静默忽略。
- 不要循环调用 DB，优先批量操作。
- 不要硬编码密钥。
- 不要使用 `Executors.newXxxThreadPool()`，使用显式 `ThreadPoolExecutor`。

## More Rules

- 错误码规范：`.claude/rules/error-handling.md`
- 限流规范：`.claude/rules/rate-limit.md`
- Redis Stream 规范：`.claude/rules/redis-stream.md`
- AI 服务调用规范：`.claude/rules/ai-service.md`
- 数据库规范：`.claude/rules/database.md`
- 前端规范：`.claude/rules/frontend.md`