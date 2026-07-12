# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

星云ERP (Xingyun ERP) — an open-source ERP system for SMEs covering procurement, sales, retail, inventory, stock counting, settlement, and multi-tenant management. Backend is Spring Boot 2.2.2 + MyBatis-Plus 3.4.2. Frontend is Vue 3 + TypeScript based on vue-vben-admin.

## Build & Run Commands

### Backend (Java 8, Maven)

```bash
# Build all modules from root
mvn clean package -DskipTests

# Build a specific module + its dependencies
mvn clean package -pl xingyun-api -am -DskipTests

# Run the API application (from xingyun-api module)
cd xingyun-api && mvn spring-boot:run

# Run with a specific profile (dev/test/prod)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The main application entry is `xingyun-api/src/main/java/com/lframework/xingyun/api/XingYunApiApplication.java`. It starts on port 8080.

### Frontend (Node 20 + pnpm 9.7.1)

```bash
cd front

# Install dependencies
pnpm install

# Development server (hot reload, proxies /api to localhost:8080)
pnpm dev

# Production build
pnpm build

# Lint all (ESLint + Stylelint + Prettier)
pnpm lint

# Type-check only (no emit)
pnpm type:check
```

## Architecture

### Backend Module Layout

Maven multi-module project (root `pom.xml` packaging=pom). All scan from `com.lframework.xingyun`:

| Module | Purpose |
|---|---|
| `xingyun-api` | **Main application entry**. Contains `XingYunApiApplication`, Spring config, Flyway DB migrations, Swagger config |
| `xingyun-core` | Core components, utilities, message queues |
| `xingyun-basedata` | Base data: warehouses, suppliers, customers, product categories, brands, attributes |
| `xingyun-sc` | Supply chain: purchase orders, sales orders, retail orders, inventory management |
| `xingyun-chart` | Charts and reports |
| `xingyun-settle` | Settlement: supplier invoices, prepayments, reconciliation, settlement statements |
| `xingyun-comp` | Comparison/competition module |
| `cloud/xingyun-cloud-api` | Cloud-deployed variant of the API |
| `cloud/xingyun-cloud-gateway` | Cloud gateway (for distributed deployment) |

### Backend Package Conventions (per module)

Each module follows a consistent layered structure. Not every layer is present in every module:

- `controller/` — REST API endpoints
- `service/` — Service interfaces (MyBatis-Plus `IService` extensions)
- `impl/` — Service implementations
- `mappers/` — MyBatis-Plus `BaseMapper` interfaces (XML in `resources/mappers/**/*.xml`)
- `entity/` — Database entities (`@TableName` annotated POJOs)
- `dto/` — Request/query DTOs
- `vo/` — Response/view objects
- `bo/` — Business objects (intermediate representations)
- `enums/` — Enumerations (registered via MyBatis-Plus `typeEnumsPackage`)
- `excel/` — EasyExcel import/export handlers
- `events/` — Spring application events (publisher side)
- `listeners/` — Spring event listeners (subscriber side)
- `converter/` — Object mappers (entity ↔ DTO/VO)
- `components/` — Generic Spring `@Component` beans

### Underlying Framework (jugg)

The project depends on `com.lframework:jugg` — a custom starter framework with its own version (`4.1.2-SNAPSHOT`). jugg provides:
- Web layer: auth (Sa-Token), request filtering, repeat-submit prevention, distributed locking (Redis-based `@EnableLock`)
- Caching: Redis-based `@Cacheable` with TTL regions
- WebSocket support
- File upload (public + security-isolated)
- MyBatis-Plus integration, code generation
- Data permission/tenant isolation
- Captcha (kaptcha)
- Warm-Flow workflow engine integration

**Key convention from jugg**: `cacheName` values must NOT contain `{}`. Default user for `createById`/`updateById` auto-fill is configured via `jugg.default-setting.default-user-id`.

### Multi-Tenancy

Multi-tenancy is **always enabled** (no longer toggleable). Database migrations are split:

- `xingyun-api/src/main/resources/db/migration/platform/` — Platform-level schema (shared across tenants)
- `xingyun-api/src/main/resources/db/migration/tenant/` — Tenant-specific schema

The datasource name for the primary DB is **`master`** (configured in `spring.datasource.dynamic.primary`).

### Threading Convention

When creating child threads, always use `DefaultCallable` or `DefaultRunnable` wrappers (from jugg) rather than raw `new Thread()` or bare `Runnable`/`Callable` — this ensures tenant context and MDC trace IDs propagate correctly.

### Frontend Architecture

Monorepo using pnpm workspaces with Turborepo. The source tree is at `front/src/` (the main app, not under `apps/portal-view` which is currently empty):

- `src/api/` — API client modules organized by domain (`base-data/`, `sc/`, `settle/`, `system/`, `sys/`, `chart/`, `development/`, `bpm/`, etc.)
- `src/views/` — Page components, same domain grouping
- `src/router/` — Vue Router (hash mode). Routes defined in `routes/`
- `src/store/` — Pinia stores under `modules/`
- `src/components/` — Shared/reusable components
- `src/hooks/` — Composable hooks
- `src/directives/` — Custom Vue directives
- `src/locales/` — i18n (vue-i18n)
- `src/settings/` — Project configuration constants (`projectSetting.ts`, `designSetting.ts`, `encryptionSetting.ts`, etc.)
- `src/design/` — Design-system styles
- `src/logics/` — App initialization logic (e.g., `initAppConfig`)

UI stack: **ant-design-vue 4.x** + **vxe-table 4.x** (primary table component). Charts use **ECharts 5.x**.

The Vite dev server proxies `/api` → `http://localhost:8080` (strips the `/api` prefix). WebSocket for messaging connects to `ws://localhost:8080/message/bus`.

### Backend Key Dependencies

- **Sa-Token** — unified auth (token name: `X-Auth-Token`, stored in Redis DB 1)
- **Knife4j** — Swagger API docs (available at the app's Swagger UI path)
- **Warm-Flow** — Dromara workflow engine (logical delete enabled, value=2)
- **Dynamic Datasource** — multiple datasources, primary named `master`
- **Druid** — connection pooling
- **RabbitMQ** — messaging (publisher confirms enabled)
- **Magic-API** — dynamic API/scripting at `/dynamic-api`
- **EasyExcel 2.2.10** — Excel import/export (two modes: one-shot and segmented)

### External Services Required

- MySQL 5.7 (database: `xingyun_platform`)
- Redis (cache + session + Sa-Token auth)
- RabbitMQ 3.12.4

### Development Environment Setup

1. Ensure MySQL, Redis, and RabbitMQ are running
2. Configure DB credentials in `xingyun-api/src/main/resources/application-dev.yml`
3. Run Flyway migrations: they execute automatically on app startup via Flyway
4. Start backend: `cd xingyun-api && mvn spring-boot:run`
5. Start frontend: `cd front && pnpm install && pnpm dev`
6. Access: frontend at Vite dev URL, Swagger API docs at the Knife4j UI
