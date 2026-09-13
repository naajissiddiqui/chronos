# Chronos Full-Stack Startup Script
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Starting Chronos Distributed System   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. Start Infrastructure (Docker)
Write-Host "`n[1/3] Starting Infrastructure (Kafka, Redis, Prometheus, Grafana)..." -ForegroundColor Yellow
docker compose -f "$rootDir\infrastructure\docker-compose.yml" up -d

# 2. Start Backend Microservices
Write-Host "`n[2/3] Starting Backend Microservices..." -ForegroundColor Yellow
$services = @(
    "auth-service",
    "job-service",
    "scheduler-service",
    "execution-service",
    "worker-service",
    "notification-service",
    "gateway-service"
)

foreach ($service in $services) {
    Write-Host " -> Launching $service..." -ForegroundColor DarkCyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$rootDir\backend\$service'; `$Host.UI.RawUI.WindowTitle = '$service'; .\mvnw.cmd spring-boot:run"
}

# 3. Start Frontend (Next.js)
Write-Host "`n[3/3] Starting Frontend (Next.js)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$rootDir\frontend'; `$Host.UI.RawUI.WindowTitle = 'chronos-frontend'; npm run dev"

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host " All services triggered! Access points: " -ForegroundColor Green
Write-Host "  - Frontend:      http://localhost:3000" -ForegroundColor White
Write-Host "  - API Gateway:   http://localhost:8080" -ForegroundColor White
Write-Host "  - Grafana:       http://localhost:3005 (admin/admin)" -ForegroundColor White
Write-Host "  - Prometheus:    http://localhost:9090" -ForegroundColor White
Write-Host "=========================================" -ForegroundColor Green
