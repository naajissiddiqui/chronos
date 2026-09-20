# Chronos Benchmark Runner (tests subfolder)
[CmdletBinding()]
param(
    [ValidateSet("Small", "Medium", "Large", "Stress", "Failure")]
    [string]$Mode = "Small",

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
    [switch]$NoGateway
)

$rootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$targetScript = Join-Path $rootDir "run-load-test.ps1"

& $targetScript @PSBoundParameters
