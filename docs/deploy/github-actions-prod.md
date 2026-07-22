# GitHub Actions 正式环境部署

## 目标

本文档用于从零配置正式服务器自动部署。按本文顺序执行，最后推送 `deploy` 分支即可触发 `.github/workflows/deploy-prod.yml`。

部署结果：

- 后端 jar：`/www/wwwroot/xingyun/xingyun-api.jar`
- 前端 dist：`/www/wwwroot/xingyun/dist`
- 后端服务：`xingyun-api-prod`
- 后端 profile：`prod`
- Nginx：宝塔路径 `/www/server/nginx/sbin/nginx`

## 1. 确认依赖仓库

后端依赖 `com.lframework:*:4.1.2-SNAPSHOT`。GitHub Actions 会先拉取并安装：

```text
https://github.com/bogewang/jugg.git
```

要求：

- 仓库可被 GitHub Actions 访问。
- 默认分支为 `main`。
- POM 版本为 `4.1.2-SNAPSHOT`。

## 2. 创建部署 SSH Key

在本机或服务器执行：

```bash
ssh-keygen -t ed25519 -C "github-actions-xingyun-prod" -f ./github-actions-xingyun-prod
```

一路回车，不设置密码。生成：

```text
github-actions-xingyun-prod      私钥，放入 GitHub Secret PROD_SSH_KEY
github-actions-xingyun-prod.pub  公钥，放入正式服务器 www 用户
```

## 3. 确认服务器 www 用户

在正式服务器执行：

```bash
id www
getent passwd www
```

如果 `www` 用户不存在，需要先按服务器环境创建；如果最后一列是 `/sbin/nologin` 或 `/usr/sbin/nologin`，需要允许 SSH 登录：

```bash
sudo usermod -s /bin/bash www
```

## 4. 写入 www 公钥

把 `github-actions-xingyun-prod.pub` 的内容追加到正式服务器：

```bash
sudo mkdir -p /home/www/.ssh
echo "这里粘贴 github-actions-xingyun-prod.pub 的内容" | sudo tee -a /home/www/.ssh/authorized_keys
sudo chmod 700 /home/www/.ssh
sudo chmod 600 /home/www/.ssh/authorized_keys
sudo chown -R www:www /home/www/.ssh
```

## 5. 创建部署目录并赋权

正式服务器执行：

```bash
sudo mkdir -p /www/wwwroot/xingyun/data/tmp
sudo mkdir -p /www/wwwroot/xingyun/data/upload
sudo mkdir -p /www/wwwroot/xingyun/data/security-upload
sudo chown -R www:www /www/wwwroot/xingyun
sudo chmod 775 /www/wwwroot/xingyun
sudo chmod -R 775 /www/wwwroot/xingyun/data
```

说明：

- GitHub Actions 使用 `www` 上传文件。
- systemd 使用宝塔 `www` 用户运行 Java 服务。
- workflow 只会修改部署产物和 `data` 目录权限，不会递归修改 `logs` 目录。

## 6. 配置 GitHub Secrets

进入 GitHub 仓库：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

新增：

```text
PROD_SSH_HOST=正式服务器 IP 或域名
PROD_SSH_PORT=SSH 端口，通常为 22
PROD_SSH_USER=www
PROD_SSH_KEY=github-actions-xingyun-prod 私钥完整内容
```

`PROD_SSH_KEY` 必须包含首尾行：

```text
-----BEGIN OPENSSH PRIVATE KEY-----
-----END OPENSSH PRIVATE KEY-----
```

## 7. 安装 systemd 服务

正式服务器执行：

```bash
sudo vi /etc/systemd/system/xingyun-api-prod.service
```

写入：

```ini
[Unit]
Description=Xingyun ERP API Prod
After=network.target

[Service]
Type=simple
User=www
WorkingDirectory=/www/wwwroot/xingyun
ExecStart=/www/server/java/jdk1.8.0_371/bin/java -Dspring.profiles.active=prod -jar -Xmx1024M -Xms256M /www/wwwroot/xingyun/xingyun-api-81.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

加载并设置开机启动：

```bash
sudo systemctl daemon-reload
sudo systemctl enable xingyun-api-prod
```

首次配置时不要急着 `start`。第一次 GitHub Actions 成功上传 jar 后，再启动或重启服务。

## 8. 配置 sudo 免密

先确认 `systemctl` 路径：

```bash
command -v systemctl
```

如果输出是 `/usr/bin/systemctl`，执行：

```bash
sudo visudo
```

末尾添加：

```text
www ALL=(root) NOPASSWD: /usr/bin/systemctl restart xingyun-api-prod, /www/server/nginx/sbin/nginx -s reload
```

如果 `command -v systemctl` 输出是 `/bin/systemctl`，则改为：

```text
www ALL=(root) NOPASSWD: /bin/systemctl restart xingyun-api-prod, /www/server/nginx/sbin/nginx -s reload
```

验证：

```bash
sudo -u www sudo -n /usr/bin/systemctl restart xingyun-api-prod
sudo -u www sudo -n /www/server/nginx/sbin/nginx -s reload
```

如果 `command -v systemctl` 输出是 `/bin/systemctl`，上面的验证命令也要同步改成 `/bin/systemctl`。如果此时 jar 还不存在，第一条可能提示应用启动失败，但不能出现 `password is required`。

## 9. 配置 Nginx

宝塔主配置通常为：

```text
/www/server/nginx/conf/nginx.conf
```

可以在 `http { ... }` 内新增或调整正式站点：

```nginx
server {
    listen 80;
    server_name example.com;

    root /www/wwwroot/xingyun/dist;
    index index.html index.htm;

    location ^~ /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host:$server_port;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header REMOTE-HOST $remote_addr;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location ^~ /dynamic/web/ {
        proxy_pass http://127.0.0.1:8080/dynamic/web/;
        proxy_set_header Host $host:$server_port;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header REMOTE-HOST $remote_addr;
        proxy_set_header Origin "";
    }

    location ^~ /dynamic-api/ {
        proxy_pass http://127.0.0.1:8080/dynamic-api/;
        proxy_set_header Host $host:$server_port;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header REMOTE-HOST $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate" always;
        add_header Pragma "no-cache" always;
        add_header Expires "0" always;
    }

    location = /version.json {
        add_header Cache-Control "no-cache, no-store, must-revalidate" always;
        add_header Pragma "no-cache" always;
        add_header Expires "0" always;
    }

    location = /_app.config.js {
        add_header Cache-Control "no-cache, no-store, must-revalidate" always;
        add_header Pragma "no-cache" always;
        add_header Expires "0" always;
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable" always;
    }
}
```

把 `example.com` 改为正式域名。检查并重载：

```bash
sudo /www/server/nginx/sbin/nginx -t
sudo /www/server/nginx/sbin/nginx -s reload
```

## 10. 推送触发部署

提交 workflow 和文档：

```bash
git add .github/workflows/deploy-prod.yml deploy/systemd/xingyun-api-prod.service docs/deploy/github-actions-prod.md
git commit -m "配置正式环境 GitHub Actions 自动部署"
git push origin deploy
```

推送后打开：

```text
GitHub 仓库 -> Actions -> Deploy Prod
```

确认以下步骤成功：

```text
Install jugg dependencies
Build backend
Build frontend
Package frontend
Upload artifacts
Restart prod services
```

正式环境上传方式与测试环境保持一致：使用普通 `scp` 上传后端 jar 和前端压缩包，随后在远程服务器解压前端并重启服务。

## 11. 首次部署后验证

正式服务器查看服务状态：

```bash
sudo systemctl status xingyun-api-prod --no-pager -l
```

查看实时日志：

```bash
sudo journalctl -u xingyun-api-prod -n 200 -f
```

确认端口监听：

```bash
sudo ss -lntp | grep ':8080'
```

确认文件已上传：

```bash
ls -lh /www/wwwroot/xingyun/xingyun-api.jar
ls -ld /www/wwwroot/xingyun/dist
```

访问正式域名，确认前端页面可打开，接口 `/api/` 能访问后端。

## 12. 常见问题

`Non-resolvable import POM: com.lframework:parent:4.1.2-SNAPSHOT`：

```text
jugg 仓库没有被拉取或安装失败。检查 Install jugg dependencies 步骤。
```

`ERR_PNPM_FROZEN_LOCKFILE_WITH_OUTDATED_LOCKFILE`：

```text
lockfileVersion 是 9.0，workflow 必须使用 pnpm 9。
```

`sudo: a password is required`：

```text
www 用户 sudoers 免密规则未生效，重新检查第 8 步。
```

`Unit xingyun-api-prod.service not found`：

```text
没有执行第 7 步，或执行后没有 systemctl daemon-reload。
```

`nginx.service is not active, cannot reload`：

```text
宝塔 Nginx 进程可能已运行但 systemd 状态 inactive。使用 /www/server/nginx/sbin/nginx -s reload。
```

服务反复重启：

```bash
sudo journalctl -u xingyun-api-prod --no-pager -n 120
```

重点检查：

- `-Dspring.profiles.active=prod` 是否正确。
- `/www/wwwroot/xingyun/xingyun-api.jar` 是否存在。
- Java 是否为 8。
- MySQL、Redis、RabbitMQ 的 prod 配置是否可连接。
- `www` 用户是否有目录读写权限。
