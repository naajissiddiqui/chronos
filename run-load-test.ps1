# Chronos Load Testing and Benchmark Runner
[CmdletBinding()]
param(
    [ValidateSet("Small", "Medium", "Large", "Stress", "Failure")]
    [string]$Mode = "Small",

    [ValidateSet("Scheduler", "DirectKafka")]
    [string]$TriggerMode = "Scheduler",

    [int]$Jobs = 0,
    [int]$Executions = 0,
    [int]$Concurrency = 10,
    [double]$FailureRate = -1.0,
    [int]$RateLimit = 0,
    [int]$TimeoutSeconds = 180,
    [switch]$Cleanup,
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$JobServiceUrl = "http://localhost:8082",
    [string]$ExecutionServiceUrl = "http://localhost:8084",
    [string]$WorkerServiceUrl = "http://localhost:8085",
    [string]$KafkaBootstrap = "localhost:9092",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379,
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$ApiKey = "",
    [switch]$NoGateway,
    [switch]$Help
)

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$testsDir = Join-Path $rootDir "tests"

if ($Help) {
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "  Chronos Load Testing and Benchmark CLI Runner          " -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Cyan
    Write-Host "Usage:"
    Write-Host "  .\run-load-test.ps1 [-Mode <Small|Medium|Large|Stress|Failure>] [Options]"
    Write-Host ""
    Write-Host "Parameters:"
    Write-Host "  -Mode <Preset>           Small (10 jobs), Medium (100 jobs), Large (1000 jobs), Stress, Failure"
    Write-Host "  -Jobs <N>                Override number of jobs to create"
    Write-Host "  -Executions <N>          Override number of executions to generate"
    Write-Host "  -Concurrency <N>         Worker thread concurrency for job creation (default: 10)"
    Write-Host "  -FailureRate <0.0-1.0>   Target task failure rate (e.g. 0.1 for 10% failures)"
    Write-Host "  -RateLimit <N>           Execution trigger rate limit (execs/sec, 0 for max)"
    Write-Host "  -TimeoutSeconds <N>      Max wait timeout (default: 180s)"
    Write-Host "  -Cleanup                 Safely remove benchmark-created jobs/executions after test"
    Write-Host "  -GatewayUrl <URL>        API Gateway endpoint (default: http://localhost:8080)"
    Write-Host "  -KafkaBootstrap <URL>    Kafka broker address (default: localhost:9092)"
    Write-Host "  -PrometheusUrl <URL>     Prometheus endpoint (default: http://localhost:9090)"
    Write-Host "  -ApiKey <Key>            API Key for Chronos authentication"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\run-load-test.ps1 -Mode Small"
    Write-Host "  .\run-load-test.ps1 -Mode Medium -Jobs 100 -Executions 100"
    Write-Host "  .\run-load-test.ps1 -Jobs 1000 -Executions 1000 -Concurrency 20"
    Write-Host "  .\run-load-test.ps1 -Mode Failure -FailureRate 0.2"
    Write-Host "  .\run-load-test.ps1 -Mode Small -Cleanup"
    exit 0
}

# Build CLI argument list
$cliArgs = @("--mode", $Mode.ToUpper(), "--trigger", $TriggerMode.ToUpper())

if ($Jobs -gt 0) { $cliArgs += @("--jobs", $Jobs) }
if ($Executions -gt 0) { $cliArgs += @("--executions", $Executions) }
if ($Concurrency -gt 0) { $cliArgs += @("--concurrency", $Concurrency) }
if ($FailureRate -ge 0.0) { $cliArgs += @("--failure-rate", $FailureRate) }
if ($RateLimit -gt 0) { $cliArgs += @("--rate", $RateLimit) }
if ($TimeoutSeconds -gt 0) { $cliArgs += @("--timeout", $TimeoutSeconds) }
if ($Cleanup) { $cliArgs += "--cleanup" }
if ($GatewayUrl) { $cliArgs += @("--gateway-url", $GatewayUrl) }
if ($JobServiceUrl) { $cliArgs += @("--job-service-url", $JobServiceUrl) }
if ($ExecutionServiceUrl) { $cliArgs += @("--execution-service-url", $ExecutionServiceUrl) }
if ($WorkerServiceUrl) { $cliArgs += @("--worker-service-url", $WorkerServiceUrl) }
if ($KafkaBootstrap) { $cliArgs += @("--kafka-bootstrap", $KafkaBootstrap) }
if ($RedisHost) { $cliArgs += @("--redis-host", $RedisHost) }
if ($RedisPort -gt 0) { $cliArgs += @("--redis-port", $RedisPort) }
if ($PrometheusUrl) { $cliArgs += @("--prometheus-url", $PrometheusUrl) }
if ($ApiKey) { $cliArgs += @("--api-key", $ApiKey) }
if ($NoGateway) { $cliArgs += "--no-gateway" }

$argString = $cliArgs -join " "

Write-Host "`n>>> Compiling and executing Chronos Load Benchmark ($Mode)..." -ForegroundColor Cyan

Push-Location $testsDir
try {
    & .\mvnw.cmd -q compile test-compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
        exit $LASTEXITCODE
    }

    $mvnArgs = @(
        "-q",
        "exec:java",
        "-Dexec.mainClass=com.chronos.tests.load.ChronosBenchmarkRunner",
        "-Dexec.classpathScope=test",
        "-Dexec.args=$argString"
    )
    & .\mvnw.cmd $mvnArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
