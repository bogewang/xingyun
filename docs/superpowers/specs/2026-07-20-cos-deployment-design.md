# GitHub 托管 Runner 与 COS 部署设计

## 目标

生产部署工作流改用 GitHub 托管 Runner，监听 `deploy_cos` 分支。构建后的后端 JAR 与前端压缩包先上传腾讯云 COS，再由部署主机从 COS 拉取，取消 runner 到主机的 SCP 传输。

## 工作流

1. 工作流运行于 `ubuntu-latest`，构建后端 JAR 和 `frontend-dist.tar.gz`。
2. Runner 下载官方 COSCLI，并使用 GitHub Environment Secrets 的 COS 写入凭据上传两个产物。
3. 对象使用运行 ID 不可变路径：`xingyun/prod/<run-id>-<attempt>/xingyun-api.jar` 和 `xingyun/prod/<run-id>-<attempt>/frontend-dist.tar.gz`。部署主机从同一个运行 ID 目录下载两项产物到本地 `.new` 文件，避免并发工作流混用不同版本的 JAR 和前端包。
4. 工作流继续使用 SSH 连接部署主机，但仅运行远程准备、下载、替换与重启命令，不再执行 SCP。
5. Runner 和远程命令从腾讯云中国站 COSCLI 地址下载二进制，连接超时为 15 秒、最多尝试 3 次；下载后校验官方 SHA-256。远程命令下载到 `mktemp` 创建的唯一文件，再安装到部署用户可写的 `/www/wwwroot/xingyun/bin/coscli`，无需新增 sudo 权限。主机使用独立、仅有上述路径读取权限的 COS 凭据拉取产物到本地 `.new` 文件，再沿用现有的原子目录替换和服务重启流程。

## 密钥与权限

GitHub `production` Environment 新增：

- `COS_SECRET_ID`、`COS_SECRET_KEY`：COS 访问密钥。
- `COS_BUCKET`、`COS_REGION`：存储桶名称和地域。

建议为 Runner 和部署主机分别创建最小权限 CAM 子账号：Runner 仅可写 `xingyun/prod/`，部署主机仅可读该路径。工作流通过 SSH 将读取密钥临时传给远程 COSCLI 配置；远端使用 `mktemp --suffix=.yaml` 创建权限为 0600 的配置文件，并在命令结束时删除，日志不得输出密钥。

## 错误处理与验证


- COS 上传或下载任一步失败时，`set -e` 使部署立即失败，不重启服务。
- 下载到 `.new` 后才替换运行中的 JAR 和前端目录。
- 工作流 YAML 校验检查触发分支、`runs-on`、无 `scp`、COS 上传及远程 COS 下载命令。
