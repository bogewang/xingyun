# COS 部署工作流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生产构建改由 GitHub 托管 Runner 执行，并通过腾讯云 COS 向部署主机交付产物。

**Architecture:** 保留单个 `deploy-prod.yml` job。GitHub 托管 Runner 构建产物、下载官方 COSCLI 并上传到固定 COS 对象；SSH 远程脚本按需安装 COSCLI，创建一次性配置文件并下载至本地临时文件后再替换运行版本。

**Tech Stack:** GitHub Actions、Bash、官方 Tencent Cloud COSCLI、SSH。

## Global Constraints

- 只修改 `.github/workflows/deploy-prod.yml`。
- 生产触发分支必须为 `deploy_cos`，runner 必须为 `ubuntu-latest`。
- 禁止使用 `scp`；SSH 仅用于在部署主机执行命令。
- COS 路径固定为 `xingyun/prod/xingyun-api.jar` 和 `xingyun/prod/frontend-dist.tar.gz`。
- 密钥仅来自 GitHub `production` Environment Secrets，日志不可打印密钥。
- 部署主机没有预装 COSCLI，远程命令必须自动安装官方 Linux AMD64 版本。

---

### Task 1: 迁移生产工作流的构建与 COS 上传

**Files:**
- Modify: `.github/workflows/deploy-prod.yml`
- Test: `.github/workflows/deploy-prod.yml`（静态结构检查）

**Interfaces:**
- Consumes: `COS_SECRET_ID`、`COS_SECRET_KEY`、`COS_BUCKET`、`COS_REGION` GitHub Environment Secrets。
- Produces: `cos://${COS_BUCKET}/xingyun/prod/xingyun-api.jar` 与 `cos://${COS_BUCKET}/xingyun/prod/frontend-dist.tar.gz`。

- [ ] **Step 1: 写出失败的工作流结构检查**

```powershell
$workflow = Get-Content .github/workflows/deploy-prod.yml -Raw
-not (@(
  ($workflow -match 'deploy_cos')
  ($workflow -match 'runs-on: ubuntu-latest')
  ($workflow -notmatch '(?m)^\s*scp\s')
  ($workflow -match 'cp backend/xingyun-api/target/xingyun-api.jar cos://deploy/xingyun/prod/xingyun-api.jar')
  $workflow -match 'cp frontend-dist.tar.gz cos://deploy/xingyun/prod/frontend-dist.tar.gz'
) -contains $false)
```

- [ ] **Step 2: 运行检查并确认失败**

Run: 上述 PowerShell 命令。

Expected: `False`，因为现有生产工作流监听 `deploy`、使用 self-hosted runner 与 `scp`。

- [ ] **Step 3: 最小化修改工作流构建与上传段**

将工作流触发和 runner 改为：

```yaml
on:
  push:
    branches:
      - deploy_cos

jobs:
  deploy-prod:
    runs-on: ubuntu-latest
```

在前端打包后插入以下上传步骤，并移除 `Upload artifacts` 中的 SCP 上传命令；保留远程目录创建：

```yaml
      - name: Install COSCLI
        run: |
          curl --fail --location --retry 3 \
            --output "$RUNNER_TEMP/coscli" \
            https://cosbrowser.cloud.tencent.com/software/coscli/coscli-linux-amd64
          chmod 755 "$RUNNER_TEMP/coscli"

      - name: Upload artifacts to COS
        env:
          COS_SECRET_ID: ${{ secrets.COS_SECRET_ID }}
          COS_SECRET_KEY: ${{ secrets.COS_SECRET_KEY }}
          COS_BUCKET: ${{ secrets.COS_BUCKET }}
          COS_REGION: ${{ secrets.COS_REGION }}
        run: |
          config_file="$RUNNER_TEMP/coscli.yaml"
          cat > "$config_file" <<EOF
          cos:
            base:
              secretid: $COS_SECRET_ID
              secretkey: $COS_SECRET_KEY
              sessiontoken: ""
              protocol: https
            buckets:
            - name: $COS_BUCKET
              alias: deploy
              region: $COS_REGION
              endpoint: cos.$COS_REGION.myqcloud.com
              ofs: false
          EOF
          "$RUNNER_TEMP/coscli" -c "$config_file" cp backend/xingyun-api/target/xingyun-api.jar cos://deploy/xingyun/prod/xingyun-api.jar
          "$RUNNER_TEMP/coscli" -c "$config_file" cp frontend-dist.tar.gz cos://deploy/xingyun/prod/frontend-dist.tar.gz
```

- [ ] **Step 4: 运行结构检查并确认通过**

Run: Step 1 的 PowerShell 命令。

Expected: `True`。

- [ ] **Step 5: 提交**

```bash
git add .github/workflows/deploy-101.yml
git commit -m "ci: upload production artifacts to COS"
```

### Task 2: 让部署主机从 COS 获取并原子部署产物

**Files:**
- Modify: `.github/workflows/deploy-prod.yml`
- Test: `.github/workflows/deploy-prod.yml`（静态结构检查）

**Interfaces:**
- Consumes: Task 1 上传的 `cos://deploy/xingyun/prod/*` 对象和 `COS_*` Secrets。
- Produces: `/www/wwwroot/xingyun/xingyun-api-81.jar`、`/www/wwwroot/xingyun/dist/` 与已重启的 `xingyun-api-prod`。

- [ ] **Step 1: 写出失败的远程下载检查**

```powershell
$workflow = Get-Content .github/workflows/deploy-prod.yml -Raw
-not (@(
  ($workflow -match 'coscli-linux-amd64')
  ($workflow -match 'cp cos://deploy/xingyun/prod/xingyun-api.jar xingyun-api.jar.new')
  ($workflow -match 'cp cos://deploy/xingyun/prod/frontend-dist.tar.gz frontend-dist.tar.gz.new')
  ($workflow -match 'mv frontend-dist.tar.gz.new frontend-dist.tar.gz')
  $workflow -match 'mv xingyun-api.jar.new xingyun-api-81.jar'
) -contains $false)
```

- [ ] **Step 2: 运行检查并确认失败**

Run: 上述 PowerShell 命令。

Expected: `False`，因为现有远程脚本只解压 SCP 上传的文件。

- [ ] **Step 3: 最小化修改 SSH 部署脚本**

为 `Restart prod services` 步骤添加 `COS_*` 环境变量。先在 runner 创建 COSCLI YAML 配置并 Base64 编码；SSH 远程命令仅接收 Base64 字符串、以 `umask 077` 写入 `/tmp/xingyun-cos.yaml`，再通过标准输入执行单引号 here-document 脚本。这样不依赖 SSH 服务端 `AcceptEnv`，也不会将密钥拼入远程 shell 源码。在远程脚本中用以下逻辑替代原有产物读取部分：

```bash
if ! command -v coscli >/dev/null 2>&1; then
  curl --fail --location --retry 3 \
    https://cosbrowser.cloud.tencent.com/software/coscli/coscli-linux-amd64 \
    --output /tmp/coscli
  chmod 755 /tmp/coscli
  sudo mv /tmp/coscli /usr/local/bin/coscli
fi

config_file=/tmp/xingyun-cos.yaml
trap 'rm -f "$config_file"' EXIT
coscli -c "$config_file" cp cos://deploy/xingyun/prod/xingyun-api.jar xingyun-api.jar.new
coscli -c "$config_file" cp cos://deploy/xingyun/prod/frontend-dist.tar.gz frontend-dist.tar.gz.new
mv frontend-dist.tar.gz.new frontend-dist.tar.gz
```

远程 SSH 命令的结构如下；`$cos_config_base64` 仅在 GitHub runner 中从环境变量产生，内容是 Base64 字符，不记录到日志：

```bash
ssh -p "${{ secrets.PROD_SSH_PORT }}" -i ~/.ssh/deploy_key \
  "${{ secrets.PROD_SSH_USER }}@${{ secrets.PROD_SSH_HOST }}" \
  "umask 077; printf '%s' '$cos_config_base64' | base64 --decode > /tmp/xingyun-cos.yaml; exec bash -s" <<'REMOTE'
REMOTE
```

在上述 SSH 命令前增加 `cos_config_base64=$(base64 --wrap=0 "$RUNNER_TEMP/coscli.yaml")`；`REMOTE` 与其间的空行要替换为本任务 Step 3 已给出的完整远程安装、下载、替换和重启脚本。完成 SSH 后执行 `rm -f "$RUNNER_TEMP/coscli.yaml"`。保留现有 `dist.new` 解压、目录替换、JAR 替换、权限调整、systemd 重启与 nginx reload。实施时所有 COS 命令必须使用 `coscli -c "$config_file"`，配置临时文件由 `trap` 清理。

- [ ] **Step 4: 运行远程下载检查并确认通过**

Run: Step 1 的 PowerShell 命令。

Expected: `True`。

- [ ] **Step 5: 检查 YAML 和差异**

Run:

```powershell
git diff --check
git diff -- .github/workflows/deploy-prod.yml
```

Expected: 无空白错误；触发分支为 `deploy_cos`、runner 为 `ubuntu-latest`、无 `scp`，COS 上传和远程下载均使用 `coscli cp`。

- [ ] **Step 6: 提交**

```bash
git add .github/workflows/deploy-101.yml
git commit -m "ci: deploy production artifacts from COS"
```
