# 生产环境自建 GitHub Actions Runner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为生产部署提供独立 Ubuntu 22.04 自建 Runner，并仅将生产工作流切换到该 Runner。

**Architecture:** 独立 Runner 以 `github-runner` 低权限用户和 systemd 服务常驻运行。Runner 使用固定公网出口地址向 GitHub 领取任务，并通过 SSH 连接生产服务器；生产服务器仅允许该出口地址访问其 SSH 端口。测试部署工作流保持 GitHub 托管 Runner。

**Tech Stack:** Ubuntu 22.04、GitHub Actions Runner、systemd、Temurin JDK 8、Maven、Node.js 18、pnpm 9、OpenSSH。

## Global Constraints

- 只修改生产环境 `.github/workflows/deploy-prod.yml`；测试 `.github/workflows/deploy-test.yml` 必须继续使用 `ubuntu-latest`。
- Runner 使用固定公网出口 IP，生产服务器 SSH 安全组与主机防火墙仅允许该地址及已有必要运维地址。
- Runner 不开放来自公网或 GitHub 的入站端口；仅允许必要出站 DNS、HTTPS 和生产 SSH 连接。
- 部署私钥继续存放在 GitHub Environment `production` 的 `PROD_SSH_KEY` Secret 中，不持久化到 Runner 主机。
- Runner 标签必须包含 `self-hosted`、`linux`、`x64`、`deploy`。

---

### Task 1: 固化 Runner 网络边界

**Files:**

- Modify: 生产云安全组与生产服务器防火墙规则（运维配置，不纳入仓库）

**Interfaces:**

- Consumes: Runner 固定公网出口 IP、生产服务器 SSH 端口 `PROD_SSH_PORT`。
- Produces: 仅允许 Runner 到生产 SSH 的网络路径。

- [ ] **Step 1: 记录固定出口地址与生产 SSH 端口**

在 Runner 主机执行：

```bash
curl -4 --fail --silent https://api.ipify.org; echo
```

预期：输出一个固定 IPv4 地址。将该地址记为 `RUNNER_EGRESS_IP`，并从 GitHub `production` Environment 的 `PROD_SSH_PORT` 读取生产 SSH 端口。

- [ ] **Step 2: 配置云安全组入站规则**

在生产服务器所属云平台安全组中新增或确认以下规则：

```text
协议: TCP
端口: PROD_SSH_PORT
来源: RUNNER_EGRESS_IP/32
动作: 允许
```

预期：除已有必需运维来源外，没有 `0.0.0.0/0` 指向该 SSH 端口的允许规则。

- [ ] **Step 3: 配置生产服务器主机防火墙**

在生产服务器以具备 sudo 权限的运维账号执行：

```bash
sudo ufw allow from RUNNER_EGRESS_IP to any port PROD_SSH_PORT proto tcp
sudo ufw status numbered
```

预期：状态中出现仅来自 `RUNNER_EGRESS_IP` 的 SSH 放行规则。若服务器未使用 UFW，则在其现有防火墙体系中创建等价规则，不混用防火墙工具。

- [ ] **Step 4: 从 Runner 验证 SSH TCP 连通性**

在 Runner 执行：

```bash
nc -zvw 5 PROD_SSH_HOST PROD_SSH_PORT
```

预期：输出 `succeeded`。超时表示安全组、主机防火墙或路由仍未放通。

- [ ] **Step 5: 记录变更**

在变更记录中记下安全组规则 ID、生产主机防火墙规则以及 `RUNNER_EGRESS_IP`；不得记录私钥内容。

### Task 2: 初始化 Ubuntu 22.04 Runner 主机

**Files:**

- Create: `/opt/actions-runner`（Runner 安装目录）
- Create: `/etc/systemd/system/actions.runner.<owner>-<repo>.<runner-name>.service`（由 Runner 安装程序生成）

**Interfaces:**

- Consumes: GitHub 仓库的 Runner 注册页面提供的下载链接、仓库 URL 与一次性注册令牌。
- Produces: 标签为 `deploy` 且服务状态为 `active` 的仓库级 Linux Runner。

- [ ] **Step 1: 安装系统依赖和构建工具**

以有 sudo 权限的管理员执行：

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git gnupg maven netcat-openbsd openssh-client tar unzip
curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo 'deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb jammy main' | sudo tee /etc/apt/sources.list.d/adoptium.list > /dev/null
curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/nodesource.gpg
echo 'deb [signed-by=/usr/share/keyrings/nodesource.gpg] https://deb.nodesource.com/node_18.x nodistro main' | sudo tee /etc/apt/sources.list.d/nodesource.list > /dev/null
sudo apt-get update
sudo apt-get install -y temurin-8-jdk nodejs
sudo corepack enable
sudo corepack prepare pnpm@9 --activate
```

预期：`java -version` 显示 1.8/8，`mvn -version` 可运行，`node --version` 显示 v18，`pnpm --version` 显示 9.x。

- [ ] **Step 2: 创建专用 Runner 用户与目录**

```bash
sudo useradd --create-home --shell /bin/bash github-runner
sudo install -d -o github-runner -g github-runner /opt/actions-runner
```

预期：`id github-runner` 成功，且 `/opt/actions-runner` 归 `github-runner` 所有。

- [ ] **Step 3: 下载并解压 GitHub Runner**

在 GitHub 仓库 `Settings -> Actions -> Runners -> New self-hosted runner -> Linux` 页面获取当前下载命令；以 `github-runner` 用户在 `/opt/actions-runner` 执行页面提供的下载和解压命令。

预期：目录中存在 `config.sh`、`run.sh` 与 `bin/Runner.Listener`。

- [ ] **Step 4: 注册仓库级 Runner**

仍在上述 GitHub 页面生成一次性注册令牌。以 `github-runner` 用户执行：

```bash
cd /opt/actions-runner
./config.sh --unattended --url "$REPOSITORY_URL" --token "$REGISTRATION_TOKEN" --name "$RUNNER_NAME" --labels deploy --work _work
```

其中 `REPOSITORY_URL` 为目标 GitHub 仓库 URL，`REGISTRATION_TOKEN` 为页面生成的一次性令牌，`RUNNER_NAME` 采用 `prod-deploy-01` 这类可识别名称。

预期：仓库 Actions Runner 页面显示在线 Runner，标签包含 `self-hosted`、`linux`、`x64`、`deploy`。

- [ ] **Step 5: 安装并启动 systemd 服务**

```bash
cd /opt/actions-runner
sudo ./svc.sh install github-runner
sudo ./svc.sh start
sudo ./svc.sh status
```

预期：服务显示 `active (running)`，重启主机后 Runner 自动恢复在线。

- [ ] **Step 6: 验证工具版本和出站访问**

```bash
java -version
mvn -version
node --version
pnpm --version
git --version
curl -I --fail https://github.com
curl -I --fail https://repo.maven.apache.org/maven2/
curl -I --fail https://registry.npmjs.org/
```

预期：所有命令成功，且 Runner 页面状态为 `Idle`。

### Task 3: 仅切换生产工作流至自建 Runner

**Files:**

- Modify: `.github/workflows/deploy-prod.yml:15`
- Verify unchanged: `.github/workflows/deploy-test.yml:15`

**Interfaces:**

- Consumes: 已注册且带 `deploy` 标签的自建 Runner。
- Produces: 仅生产 Job 被该 Runner 调度，测试 Job 保持 GitHub 托管调度。

- [ ] **Step 1: 写入工作流调度断言**

在仓库根目录执行：

```bash
grep -F 'runs-on: ubuntu-latest' .github/workflows/deploy-101.yml
grep -F 'runs-on: ubuntu-latest' .github/workflows/deploy-test-ssh.yml
```

预期：两条命令在修改前均匹配，证明修改目标与测试工作流基线正确。

- [ ] **Step 2: 修改生产 Job 的 `runs-on`**

将 `.github/workflows/deploy-prod.yml` 中：

```yaml
runs-on: ubuntu-latest
```

替换为：

```yaml
runs-on: [self-hosted, linux, x64, deploy]
```

- [ ] **Step 3: 执行工作流静态验证**

在仓库根目录执行：

```bash
grep -F 'runs-on: [self-hosted, linux, x64, deploy]' .github/workflows/deploy-101.yml
grep -F 'runs-on: ubuntu-latest' .github/workflows/deploy-test-ssh.yml
git diff --check -- .github/workflows/deploy-101.yml .github/workflows/deploy-test-ssh.yml
```

预期：三条命令均成功；测试工作流没有改动。

- [ ] **Step 4: 提交工作流变更**

```bash
git add .github/workflows/deploy-101.yml
git commit -m "ci: run production deploy on self-hosted runner"
```

预期：提交只包含生产工作流的 Runner 选择变更。

### Task 4: 执行生产部署验收

**Files:**

- Verify: GitHub Actions 生产工作流运行记录
- Verify: 生产应用服务状态

**Interfaces:**

- Consumes: Task 1 网络规则、Task 2 在线 Runner、Task 3 工作流修改以及现有 `production` Environment Secrets。
- Produces: 生产分支部署在自建 Runner 上完成的可审计记录。

- [ ] **Step 1: 手工触发生产工作流**

在 GitHub Actions 页面选择 `Deploy Prod`，点击 `Run workflow` 并选择 `deploy` 分支。

预期：运行详情的 Job 显示由名称为 `prod-deploy-01`（或注册时指定名称）的 self-hosted Runner 执行。

- [ ] **Step 2: 验证构建和 SSH 步骤**

检查 Job 日志中的 `Build backend`、`Build frontend`、`Upload artifacts` 和 `Restart prod services`。

预期：全部成功；不输出 `PROD_SSH_KEY` 或其内容。

- [ ] **Step 3: 验证生产服务**

在生产服务器执行：

```bash
sudo systemctl is-active xingyun-api-prod
```

预期：输出 `active`。随后访问既有生产健康检查或业务入口，确认前端静态资源和后端接口均可用。

- [ ] **Step 4: 验证测试工作流未受影响**

触发 `Deploy Test` 或检查下一次 `develop` 推送对应运行记录。

预期：Job 仍显示 GitHub-hosted `ubuntu-latest`，且未使用 `prod-deploy-01` Runner。

