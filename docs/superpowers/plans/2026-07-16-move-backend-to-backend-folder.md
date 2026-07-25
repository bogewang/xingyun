# Move Backend Code to backend Folder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将仓库根目录的 Maven 后端工程及云端后端模块统一移动到 `backend/`，并保持构建、启动及文档路径有效。

**Architecture:** `backend/pom.xml` 作为 Maven 聚合根，`xingyun-*` 作为业务模块，`backend/cloud/` 作为云端模块目录；根目录保留 `frontend/`、文档和工程级配置。移动整个 `cloud/` 目录可保持云模块相对父 POM 路径不变。

**Tech Stack:** Maven、Spring Boot 2.2.2、Java 8、PowerShell、Git。

## Global Constraints

- 后端构建必须从 `backend/` 目录执行。
- Maven 模块间依赖和 Java 包名保持不变。
- 不修改业务代码、依赖版本或运行时配置。
- 更新所有面向开发者的后端路径说明。

---

### Task 1: 迁移 Maven 后端工程目录

**Files:**
- Move: `pom.xml` to `backend/pom.xml`
- Move: `xingyun-api/` to `backend/xingyun-api/`
- Move: `xingyun-basedata/` to `backend/xingyun-basedata/`
- Move: `xingyun-chart/` to `backend/xingyun-chart/`
- Move: `xingyun-comp/` to `backend/xingyun-comp/`
- Move: `xingyun-core/` to `backend/xingyun-core/`
- Move: `xingyun-sc/` to `backend/xingyun-sc/`
- Move: `xingyun-settle/` to `backend/xingyun-settle/`
- Move: `cloud/` to `backend/cloud/`

**Interfaces:**
- Produces: `backend/pom.xml` and all original Maven modules at paths relative to it.

- [ ] **Step 1: Move the complete backend tree**

Run from `D:\dev\CODE\xingyun`:

```powershell
Move-Item -LiteralPath 'pom.xml' -Destination 'backend/pom.xml'
Move-Item -LiteralPath 'xingyun-api' -Destination 'backend/xingyun-api'
Move-Item -LiteralPath 'xingyun-basedata' -Destination 'backend/xingyun-basedata'
Move-Item -LiteralPath 'xingyun-chart' -Destination 'backend/xingyun-chart'
Move-Item -LiteralPath 'xingyun-comp' -Destination 'backend/xingyun-comp'
Move-Item -LiteralPath 'xingyun-core' -Destination 'backend/xingyun-core'
Move-Item -LiteralPath 'xingyun-sc' -Destination 'backend/xingyun-sc'
Move-Item -LiteralPath 'xingyun-settle' -Destination 'backend/xingyun-settle'
Move-Item -LiteralPath 'cloud' -Destination 'backend/cloud'
```

- [ ] **Step 2: Verify the moved Maven topology**

Run:

```powershell
Get-ChildItem backend -Directory
Test-Path backend/pom.xml
Test-Path backend/xingyun-api/pom.xml
Test-Path backend/cloud/xingyun-cloud-api/pom.xml
```

Expected: all three paths exist and the original root module paths no longer exist.

### Task 2: Update developer-facing paths

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/architecture.md`
- Modify: `.codex/hooks/format.ps1`

**Interfaces:**
- Consumes: the new `backend/` directory layout.
- Produces: commands and documentation that point to the moved modules.

- [ ] **Step 1: Update build and startup commands**

Use `backend` as the working directory for Maven commands and prefix module paths in architecture documentation with `backend/`.

- [ ] **Step 2: Update the formatting hook**

Set the hook's Maven working directory to `$ROOT/backend`, while leaving the frontend formatting working directory at `$ROOT/frontend`.

### Task 3: Verify build and stale references

**Files:**
- Verify: `backend/pom.xml`
- Verify: `backend/xingyun-api/pom.xml`
- Verify: `backend/cloud/xingyun-cloud-api/pom.xml`

- [ ] **Step 1: Search for stale root-level backend paths**

Run `rg` across tracked project configuration and documentation, excluding historical plan files, and confirm no active reference points to `xingyun-api/`, `cloud/`, or a root `pom.xml`.

- [ ] **Step 2: Compile the backend**

Run `mvn clean compile -DskipTests` from `backend/` and expect a successful build.

- [ ] **Step 3: Inspect the final Git diff**

Run `git status --short` and `git diff --stat` to confirm the change is a directory relocation plus path documentation updates only.
