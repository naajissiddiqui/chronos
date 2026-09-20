# Chronos Load Testing & Benchmarking Guide

This directory contains the engineering load generator and benchmarking tool for the **Chronos Distributed Job Scheduling & Execution Platform**.

The benchmark exercises the real Chronos microservice APIs and event-driven pipeline:
`API Gateway (:8080) -> Job Service (:8082) -> Scheduler (:8083) -> Kafka (job.triggered) -> Execution Service (:8084) -> Worker (:8085) -> Kafka (execution.completed/failed) -> Database State Persistence`.

---

## 1. Prerequisites

- **Java JDK 21+**
- **Docker & Docker Compose** (for Kafka, Redis, Prometheus, Grafana)
- **PostgreSQL 15+** (running on port `5432`, database `chronos_job`)
- **PowerShell 7+** (or Windows PowerShell 5.1)

---

## 2. Infrastructure Setup

Before running the benchmark, start the core backing infrastructure:

```powershell
# Start Kafka, Redis, Prometheus, Grafana
cd c:\chronos\infrastructure
docker-compose up -d
```

Verify services are healthy:
- **Kafka**: `localhost:9092`
- **Redis**: `localhost:6379`
- **PostgreSQL**: `localhost:5432` (database: `chronos_job`, user: `postgres`, password: `postgres`)
- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3005` (admin / admin)

---

## 3. Starting Chronos Microservices

You can start all Chronos microservices using the root startup script:

```powershell
cd c:\chronos
.\start-all.ps1
```

Or start individual services manually in separate terminal windows:

```powershell
# 1. Auth Service (:8081)
cd c:\chronos\backend\auth-service
.\mvnw.cmd spring-boot:run

# 2. Job Service (:8082)
cd c:\chronos\backend\job-service
.\mvnw.cmd spring-boot:run

# 3. Scheduler Service (:8083)
cd c:\chronos\backend\scheduler-service
.\mvnw.cmd spring-boot:run

# 4. Execution Service (:8084)
cd c:\chronos\backend\execution-service
.\mvnw.cmd spring-boot:run

# 5. Worker Service (:8085)
cd c:\chronos\backend\worker-service
.\mvnw.cmd spring-boot:run

# 6. API Gateway (:8080)
cd c:\chronos\backend\gateway-service
.\mvnw.cmd spring-boot:run
```

---

## 4. Multi-Worker Scaling Setup (1 vs 2 vs 4 Workers)

To benchmark multi-worker scaling and consumer group load balancing, start multiple instances of `worker-service` with distinct worker IDs:

```powershell
# Worker Instance 1 (Port 8085)
$env:WORKER_ID="worker-local-1"
$env:PORT="8085"
cd c:\chronos\backend\worker-service
.\mvnw.cmd spring-boot:run

# Worker Instance 2 (Port 8086)
$env:WORKER_ID="worker-local-2"
$env:PORT="8086"
cd c:\chronos\backend\worker-service
.\mvnw.cmd spring-boot:run

# Worker Instance 3 (Port 8088)
$env:WORKER_ID="worker-local-3"
$env:PORT="8088"
cd c:\chronos\backend\worker-service
.\mvnw.cmd spring-boot:run

# Worker Instance 4 (Port 8089)
$env:WORKER_ID="worker-local-4"
$env:PORT="8089"
cd c:\chronos\backend\worker-service
.\mvnw.cmd spring-boot:run
```

The benchmark will automatically detect all registered workers in Redis (`worker:heartbeat:*`) and report the execution breakdown across each worker identity.

---

## 5. Running Benchmark Scenarios

### A. Small Benchmark (Smoke / Validation Test)
- 10 jobs, 10 executions

```powershell
cd c:\chronos
.\run-load-test.ps1 -Mode Small
```

### B. Medium Benchmark (Standard Baseline)
- 100 jobs, 100 executions

```powershell
cd c:\chronos
.\run-load-test.ps1 -Mode Medium
```

### C. Large Benchmark (1,000 Jobs & Executions)
- 1,000 jobs, 1,000 executions with 20 concurrent HTTP job creation threads

```powershell
cd c:\chronos
.\run-load-test.ps1 -Mode Large -Concurrency 20
```

### D. Stress Benchmark (Configurable High Load)
- Specify custom job count, execution count, rate limit, and timeout:

```powershell
cd c:\chronos
.\run-load-test.ps1 -Jobs 500 -Executions 2000 -Concurrency 25 -TimeoutSeconds 300
```

### E. Failure & DLQ Benchmark
- Injects deterministic failure tasks (`DEMO_REPORT_FAIL`) to measure retry exponential backoff and DLQ routing:

```powershell
cd c:\chronos
.\run-load-test.ps1 -Mode Failure -Jobs 50 -Executions 50 -FailureRate 0.2
```

---

## 6. Safe Cleanup Mode

To automatically remove only the jobs and executions created during the benchmark run:

```powershell
.\run-load-test.ps1 -Mode Small -Cleanup
```

The cleaner filters specifically by the isolated `X-Organization-Id` generated for that benchmark run, guaranteeing that existing production or developer tenant data is never affected.

---

## 7. Metric Definitions & Interpretation

| Metric | Description | Unit |
| :--- | :--- | :--- |
| **Jobs Created** | Total jobs successfully created via API Gateway | Count |
| **Job Creation Throughput** | HTTP request rate for creating job definitions | `jobs/sec` |
| **Job Creation Latency** | HTTP response latency (avg, p95) for job creation | `ms` |
| **Executions Requested** | Total execution trigger events dispatched into the pipeline | Count |
| **Executions Completed** | Number of executions reaching `SUCCEEDED` status | Count |
| **Executions Failed** | Number of executions in `FAILED` terminal state | Count |
| **Executions Retried** | Executions scheduled for exponential backoff retry | Count |
| **Executions DLQ** | Executions exceeding max retries routed to `execution.dlq` | Count |
| **Execution Throughput** | True pipeline completion throughput (`completed / duration`) | `executions/sec` |
| **Peak Processing Rate** | Highest sustained execution rate observed across 1-second sampling windows | `executions/sec` |
| **Execution Latency (p50, p95, p99)** | Time between execution dispatch/creation and terminal completion in DB | `ms` |
| **Worker Distribution** | Percentage of completed executions handled by each active worker ID | `% (count)` |
| **Prometheus Metrics Delta** | Counter increments scraped from Actuator endpoints during the test | Count |

### Understanding Throughput Differences
- **Job Creation Throughput**: Measures how fast the API Gateway and Job Service ingest job definitions into PostgreSQL.
- **Execution Throughput**: Measures the end-to-end processing capability of the asynchronous Kafka consumer pipeline, task handlers, and Execution Service database updates.

---

## 8. Automated JUnit 5 Integration

You can also run load scenarios directly through Maven test suites:

```powershell
cd c:\chronos\tests
.\mvnw.cmd test -Dtest=ExecutionPipelineLoadTest
```

Results are saved to `target/load-test-reports/benchmark-<runId>.json`.
