### 后端模块布局

Maven 多模块项目（`backend/pom.xml` packaging=pom）。所有模块均扫描自 `com.lframework.xingyun`：

| 模块 | 用途 |
|---|---|
| `backend/xingyun-api` | **主应用入口**。包含 `XingYunApiApplication`、Spring 配置、Flyway 数据库迁移、Swagger 配置 |
| `backend/xingyun-core` | 核心组件、工具类、消息队列 |
| `backend/xingyun-basedata` | 基础数据：仓库、供应商、客户、商品分类、品牌、属性 |
| `backend/xingyun-sc` | 供应链：采购订单、销售订单、零售订单、库存管理 |
| `backend/xingyun-chart` | 图表与报表 |
| `backend/xingyun-settle` | 结算：供应商发票、预付款、对账、结算单 |
| `backend/xingyun-comp` | 对照/竞品模块 |

### 后端包约定（各模块通用）

每个模块遵循一致的分层结构。并非所有层在每个模块中都存在：

- `controller/` — REST API 端点
- `service/` — 服务接口（MyBatis-Plus `IService` 扩展）
- `impl/` — 服务实现
- `mappers/` — MyBatis-Plus `BaseMapper` 接口（XML 位于 `resources/mappers/**/*.xml`）
- `entity/` — 数据库实体（`@TableName` 注解的 POJO）
- `dto/` — 请求/查询 DTO
- `vo/` — 响应/视图对象
- `bo/` — 业务对象（中间表示）
- `enums/` — 枚举（通过 MyBatis-Plus `typeEnumsPackage` 注册）
- `excel/` — EasyExcel 导入/导出处理器
- `events/` — Spring 应用事件（发布方）
- `listeners/` — Spring 事件监听器（订阅方）
- `converter/` — 对象映射（entity ↔ DTO/VO）
- `components/` — 通用 Spring `@Component` Bean
### 底层框架（jugg）

项目依赖 `com.lframework:jugg` —— 自定义 starter 框架，版本为 `4.1.2-SNAPSHOT`。jugg 提供：
- Web 层：认证（Sa-Token）、请求过滤、防重复提交、分布式锁（基于 Redis 的 `@EnableLock`）
- 缓存：基于 Redis 的 `@Cacheable`，支持 TTL 区域
- WebSocket 支持
- 文件上传（公开 + 安全隔离）
- MyBatis-Plus 集成、代码生成
- 数据权限/租户隔离
- 验证码（kaptcha）
- Warm-Flow 工作流引擎集成

**jugg 关键约定**：`cacheName` 值不得包含 `{}`。`createById`/`updateById` 自动填充的默认用户通过 `jugg.default-setting.default-user-id` 配置。

### 多租户

多租户**始终启用**（不再支持关闭）。数据库迁移分为：

- `backend/xingyun-api/src/main/resources/db/migration/platform/` — 平台级 schema（租户间共享）
- `backend/xingyun-api/src/main/resources/db/migration/tenant/` — 租户级 schema

主库数据源名称为 **`master`**（在 `spring.datasource.dynamic.primary` 中配置）。

### 线程约定

创建子线程时，始终使用 `DefaultCallable` 或 `DefaultRunnable` 包装器（来自 jugg），而非原始的 `new Thread()` 或裸的 `Runnable`/`Callable` —— 这确保租户上下文和 MDC 追踪 ID 正确传播。

### 前端架构

使用 pnpm workspaces + Turborepo 的 monorepo 结构。源码树位于 `frontend/src/`（主应用，不在当前为空的 `apps/portal-view` 下）：

- `src/api/` — API 客户端模块，按领域划分（`base-data/`、`sc/`、`settle/`、`system/`、`sys/`、`chart/`、`development/`、`bpm/` 等）
- `src/views/` — 页面组件，相同的领域分组
- `src/router/` — Vue Router（hash 模式）。路由定义在 `routes/` 目录下
- `src/store/` — Pinia store，位于 `modules/` 目录下
- `src/components/` — 共享/可复用组件
- `src/hooks/` — 组合式函数
- `src/directives/` — 自定义 Vue 指令
- `src/locales/` — 国际化（vue-i18n）
- `src/settings/` — 项目配置常量（`projectSetting.ts`、`designSetting.ts`、`encryptionSetting.ts` 等）
- `src/design/` — 设计系统样式
- `src/logics/` — 应用初始化逻辑（如 `initAppConfig`）

UI 技术栈：**ant-design-vue 4.x** + **vxe-table 4.x**（主要表格组件）。图表使用 **ECharts 5.x**。

Vite 开发服务器将 `/api` 代理到 `http://localhost:8080`（去除 `/api` 前缀）。消息 WebSocket 连接到 `ws://localhost:8080/message/bus`。

### 后端关键依赖

- **Sa-Token** — 统一认证（token 名称：`X-Auth-Token`，存储在 Redis DB 1 中）
- **Knife4j** — Swagger API 文档（可在应用的 Swagger UI 路径访问）
- **Warm-Flow** — Dromara 工作流引擎（启用逻辑删除，值为 2）
- **Dynamic Datasource** — 多数据源，主库名为 `master`
- **Druid** — 连接池
- **RabbitMQ** — 消息队列（启用发布确认）
- **Magic-API** — 动态 API/脚本，路径为 `/dynamic-api`
- **EasyExcel 2.2.10** — Excel 导入/导出（两种模式：一次性导出和分段导出）
