# Chronos Multi-Worker Scaling Benchmark Runner
# Measures throughput and latency scaling across 1, 2, and 4 worker instances.
[CmdletBinding()]
param(
    [int]$Executions = 100,
    [int]$TimeoutSeconds = 120,
    [int[]]$WorkerCounts = @(1, 2, 4),
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$JobServiceUrl = "http://localhost:8082",
    [string]$ExecutionServiceUrl = "http://localhost:8084",
    [string]$KafkaBootstrap = "localhost:9092",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [switch]$Help
)

if ($Help) {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  Chronos Multi-Worker Scaling Benchmark Runner          " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Usage:"
    Write-Host "  .\run-scaling-benchmark.ps1 [-Executions <N>] [-TimeoutSeconds <Sec>]"
    Write-Host ""
    Write-Host "Parameters:"
    Write-Host "  -Executions <N>       Number of jobs & executions per run (default: 100)"
    Write-Host "  -TimeoutSeconds <N>   Timeout per benchmark run (default: 120s)"
    Write-Host "  -WorkerCounts <Array> Worker scaling points (default: 1, 2, 4)"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\run-scaling-benchmark.ps1 -Executions 100"
    Write-Host "  .\run-scaling-benchmark.ps1 -Executions 500 -TimeoutSeconds 180"
    exit 0
}

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workerDir = Join-Path $rootDir "backend\worker-service"
$testsDir = Join-Path $rootDir "tests"
$jarPath = Join-Path $workerDir "target\worker-service-0.0.1-SNAPSHOT.jar"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  CHRONOS MULTI-WORKER SCALING BENCHMARK SUITE             " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Workload per Run:  $Executions Jobs / $Executions Executions"
Write-Host "Scaling Matrix:    $($WorkerCounts -join ', ') Workers"
Write-Host "Trigger Pipeline:  SCHEDULER (True E2E)"
Write-Host "Consumer Group:    chronos-worker"
Write-Host "============================================================`n"

# Step 1: Ensure worker-service is packaged
if (-not (Test-Path $jarPath)) {
    Write-Host ">>> Packaging worker-service JAR..." -ForegroundColor Yellow
    Push-Location $workerDir
    & .\mvnw.cmd package -DskipTests -q
    Pop-Location
}

# Function to stop any running local worker instances
function Stop-LocalWorkers {
    Write-Host ">>> Stopping any existing worker instances..." -ForegroundColor Gray
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
        $_.CommandLine -like "*worker-service*" -or $_.CommandLine -like "*worker.id=worker-local-*"
    } | ForEach-Object {
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
}

# Function to start N workers
function Start-Workers([int]$count) {
    Write-Host ">>> Starting $count Worker Instance(s)..." -ForegroundColor Cyan
    $procs = @()
    for ($i = 1; $i -le $count; $i++) {
        $port = 8084 + $i
        $workerId = "worker-local-$i"
        
        Write-Host "    Starting $workerId on port $port (group: chronos-worker)..." -ForegroundColor Gray
        $proc = Start-Process -FilePath "java" -ArgumentList "-Dserver.port=$port", "-Dworker.id=$workerId", "-Dspring.kafka.consumer.group-id=chronos-worker", "-Dspring.kafka.bootstrap-servers=$KafkaBootstrap", "-Dspring.data.redis.host=$RedisHost", "-Dspring.data.redis.port=$RedisPort", "-jar", "`"$jarPath`"" -PassThru -WindowStyle Hidden
        $procs += $proc
    }

    # Wait for all workers to become UP
    Write-Host ">>> Waiting for $count worker(s) to pass health checks and publish heartbeats..." -ForegroundColor Gray
    for ($i = 1; $i -le $count; $i++) {
        $port = 8084 + $i
        $url = "http://localhost:$port/actuator/health"
        $up = $false
        $attempts = 0
        while (-not $up -and $attempts -lt 40) {
            Start-Sleep -Milliseconds 500
            $attempts++
            try {
                $res = Invoke-RestMethod -Uri $url -TimeoutSec 2 -ErrorAction SilentlyContinue
                if ($res.status -eq "UP") {
                    $up = $true
                }
            } catch {}
        }
        if ($up) {
            Write-Host "    [OK] worker-local-$i is UP & ONLINE (port $port)" -ForegroundColor Green
        } else {
            Write-Host "    [WARN] worker-local-$i did not report UP in time on port $port" -ForegroundColor Red
        }
    }
    # Allow 2 seconds for Kafka consumer group assignment & rebalance
    Start-Sleep -Seconds 2
    return $procs
}

# Array to hold results for comparison
$allResults = @()

foreach ($workerCount in $WorkerCounts) {
    Write-Host "`n------------------------------------------------------------" -ForegroundColor Magenta
    Write-Host "  RUNNING BENCHMARK: $workerCount WORKER(S) ($Executions Executions)" -ForegroundColor Magenta
    Write-Host "------------------------------------------------------------" -ForegroundColor Magenta

    Stop-LocalWorkers
    $workerProcs = Start-Workers -count $workerCount

    # Run the benchmark
    Push-Location $testsDir
    $reportsBefore = @(Get-ChildItem -Path "target\load-test-reports\benchmark-*.json" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    
    $benchmarkOutput = & powershell.exe -ExecutionPolicy Bypass -File "..\run-load-test.ps1" -Mode Medium -Jobs $Executions -Executions $Executions -TimeoutSeconds $TimeoutSeconds
    $benchmarkOutput | Out-Host

    $reportsAfter = @(Get-ChildItem -Path "target\load-test-reports\benchmark-*.json" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    $newReportFiles = $reportsAfter | Where-Object { $_ -notin $reportsBefore }
    $latestReport = if ($newReportFiles) { $newReportFiles[-1] } else { $reportsAfter[-1] }

    if ($latestReport -and (Test-Path $latestReport)) {
        $reportJson = Get-Content $latestReport -Raw | ConvertFrom-Json
        $resultRecord = [PSCustomObject]@{
            Workers            = $workerCount
            JobsCreated        = $reportJson.results.jobsCreated
            Completed          = $reportJson.results.executionsCompleted
            Failed             = $reportJson.results.executionsFailed
            SuccessRate        = "$([math]::Round($reportJson.results.successRate, 2))%"
            DurationSec        = [math]::Round($reportJson.results.durationSec, 2)
            Throughput         = [math]::Round($reportJson.results.throughputExecPerSec, 2)
            PeakRate           = [math]::Round($reportJson.results.peakProcessingRateExecPerSec, 2)
            p50Ms              = $reportJson.results.latencyMs.p50
            p95Ms              = $reportJson.results.latencyMs.p95
            p99Ms              = $reportJson.results.latencyMs.p99
            WorkerDistribution = $reportJson.results.workerDistribution
        }
        $allResults += $resultRecord
    }
    Pop-Location

    # Stop extra workers before next round
    Stop-LocalWorkers
}

# Step 4: Restart 1 baseline worker so Chronos platform remains fully functional
Start-Workers -count 1 | Out-Null

# Step 5: Render Comparison Table
Write-Host "`n====================================================================================" -ForegroundColor Cyan
Write-Host "                CHRONOS MULTI-WORKER SCALING BENCHMARK RESULTS                     " -ForegroundColor Cyan
Write-Host "====================================================================================" -ForegroundColor Cyan

$allResults | Select-Object Workers, Completed, DurationSec, Throughput, PeakRate, p50Ms, p95Ms, p99Ms, SuccessRate | Format-Table -AutoSize

Write-Host "`nWorker Distribution Summary:" -ForegroundColor Cyan
Write-Host "----------------------------" -ForegroundColor Cyan
foreach ($r in $allResults) {
    Write-Host "[$($r.Workers) Worker Run - Total Completed: $($r.Completed)]" -ForegroundColor Yellow
    if ($r.WorkerDistribution) {
        $r.WorkerDistribution.PSObject.Properties | ForEach-Object {
            $count = $_.Value
            $pct = if ($r.Completed -gt 0) { [math]::Round(($count / $r.Completed) * 100, 1) } else { 0 }
            Write-Host "   $($_.Name): $count executions ($pct%)"
        }
    } else {
        Write-Host "   None recorded"
    }
}
Write-Host "====================================================================================`n"

# Save Markdown report
$reportMdPath = Join-Path $testsDir "target\load-test-reports\multi-worker-scaling-report.md"
$mdContent = @"
# Chronos Multi-Worker Scaling Benchmark Report

## Configuration
- **Workload**: $Executions jobs / $Executions executions per run
- **Pipeline**: True E2E Scheduler (Job Service -> Outbox -> Scheduler -> Kafka -> Execution -> Worker -> Execution -> PostgreSQL)
- **Consumer Group**: \`chronos-worker\`
- **Kafka Partitions**: 4 partitions on \`execution.dispatch\`

## Scaling Comparison Table

| Workers | Completed | Duration (s) | Throughput (exec/s) | Peak Rate (exec/s) | p50 (ms) | p95 (ms) | p99 (ms) | Success Rate |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
"@

foreach ($r in $allResults) {
    $mdContent += "`n| $($r.Workers) | $($r.Completed) | $($r.DurationSec) s | $($r.Throughput) | $($r.PeakRate) | $($r.p50Ms) ms | $($r.p95Ms) ms | $($r.p99Ms) ms | $($r.SuccessRate) |"
}

$mdContent += @"


## Worker Distribution

"@

foreach ($r in $allResults) {
    $mdContent += "### $($r.Workers) Worker(s) Run`n"
    if ($r.WorkerDistribution) {
        $r.WorkerDistribution.PSObject.Properties | ForEach-Object {
            $count = $_.Value
            $pct = if ($r.Completed -gt 0) { [math]::Round(($count / $r.Completed) * 100, 1) } else { 0 }
            $mdContent += "- **$($_.Name)**: $count executions ($pct%)`n"
        }
    }
    $mdContent += "`n"
}

[System.IO.File]::WriteAllText($reportMdPath, $mdContent)
Write-Host "[INFO] Markdown comparison saved to: $reportMdPath" -ForegroundColor Green
