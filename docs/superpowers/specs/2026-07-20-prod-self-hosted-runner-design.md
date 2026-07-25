# 生产环境自建 GitHub Actions Runner 设计

## 目标

将生产部署工作流切换到独立 Ubuntu 22.04 自建 Runner，保留测试部署工作流继续使用 GitHub 托管的 `ubuntu-latest`。

## 范围

- 修改 `.github/workflows/deploy-prod.yml` 的运行器选择。
- 准备一台具备固定公网出口 IP 的 Ubuntu 22.04 构建主机。
- 在生产部署服务器上仅向该固定公网 IP 开放 SSH 端口。
- 不修改 `.github/workflows/deploy-test.yml`，也不变更测试服务器的网络策略。

## Runner 主机

- 主机使用独立 Ubuntu 22.04，不与生产应用服务共用。
- 使用专用低权限 `github-runner` 用户运行 GitHub Actions Runner，并由 systemd 守护。
- 安装 Git、curl、unzip、OpenSSH 客户端、Temurin JDK 8、Maven、Node.js 18 与 pnpm 9。
- 主机无需接受来自 GitHub 的入站连接；仅需允许出站 HTTPS、DNS、依赖下载以及到生产服务器 SSH 端口的连接。
- Runner 标签为 `self-hosted`、`linux`、`x64`、`deploy`。

## 网络与访问控制

- Runner 的公网出口 IP 必须固定；若主机位于 NAT 后，固定 NAT 出口地址同样可用。
- 生产服务器的云安全组和主机防火墙仅允许 `Runner公网出口IP -> 生产SSH端口/TCP`。
- 禁止生产 SSH 对 `0.0.0.0/0` 开放；保留需要的运维来源 IP 白名单。
- 工作流继续通过 `PROD_SSH_HOST`、`PROD_SSH_PORT`、`PROD_SSH_USER`、`PROD_SSH_KEY` 密钥完成认证；不在 Runner 磁盘持久化部署私钥。

## 工作流变更

`deploy-prod.yml` 的生产 Job 使用：

```yaml
runs-on: [self-hosted, linux, x64, deploy]
```

其他构建、制品打包、SSH 上传与服务重启步骤保持不变。`deploy-test.yml` 继续使用 `ubuntu-latest`。

## 验收标准

1. GitHub 仓库的 Actions Runner 页面显示 Runner 为 `Idle`。
2. 从生产 Runner 成功拉取仓库、下载依赖并完成后端与前端构建。
3. Runner 能使用生产 SSH 密钥连接到 `PROD_SSH_HOST:PROD_SSH_PORT`。
4. 推送 `deploy` 分支或手工触发生产工作流时，Job 被 `deploy` 标签 Runner 接收并完成发布。
5. `develop` 分支触发的测试部署仍在 GitHub 托管 `ubuntu-latest` 上运行。
