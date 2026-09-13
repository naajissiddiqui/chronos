# Chronos Full-Stack Shutdown Script
Write-Host "=========================================" -ForegroundColor Red
Write-Host "  Stopping Chronos Distributed System   " -ForegroundColor Red
Write-Host "=========================================" -ForegroundColor Red

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Stop Docker Containers
Write-Host "`n[1/2] Stopping Infrastructure Containers..." -ForegroundColor Yellow
docker compose -f "$rootDir\infrastructure\docker-compose.yml" down

# 2. Stop Java and Node processes related to Chronos
Write-Host "`n[2/2] Terminating Background Services..." -ForegroundColor Yellow
Get-Process -Name "node", "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "`nAll Chronos services stopped successfully." -ForegroundColor Green
