# .codex/hooks/format.ps1
$ErrorActionPreference = "Stop"

$ROOT = git rev-parse --show-toplevel
$FRONTEND = Join-Path $ROOT "frontend"
$BACKEND = Join-Path $ROOT "backend"

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
# 2. 后端 Java 格式化
# --------------------------
Write-Host "Formatting Java backend..."
Set-Location $BACKEND
# Maven Spotless 格式化 Java
mvn spotless:apply

Write-Host "✅ Auto-format completed."
