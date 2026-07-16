# .codex/hooks/format.ps1
$ErrorActionPreference = "Stop"

$ROOT = git rev-parse --show-toplevel
$FRONTEND = Join-Path $ROOT "frontend"
# 后端 Java 就是当前目录，不再单独设 BACKEND 变量

# --------------------------
# 1. 前端 Vue3 格式化
# --------------------------
if (Test-Path $FRONTEND) {
  Write-Host "Formatting Vue3 frontend..."
  Set-Location $FRONTEND
  if (-not (Test-Path "node_modules")) { npm install }
  npx eslint --fix .
  npx prettier --write .
  Set-Location $ROOT
}

# --------------------------
# 2. 后端 Java 格式化（当前目录）
# --------------------------
Write-Host "Formatting Java backend in current directory..."
Set-Location $ROOT
# Maven Spotless 格式化 Java
.\mvnw.cmd spotless:apply

Write-Host "✅ Auto-format completed."
