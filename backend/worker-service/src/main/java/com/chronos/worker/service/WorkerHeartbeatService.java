package com.chronos.worker.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class WorkerHeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerHeartbeatService.class);
    public static final String KEY_PREFIX = "worker:heartbeat:";

    private final StringRedisTemplate redisTemplate;
    private final String workerId;
    private final long heartbeatIntervalMs;
    private final long heartbeatTtlSeconds;

    @Autowired
    public WorkerHeartbeatService(
            StringRedisTemplate redisTemplate,
            @Value("${worker.id:worker-local-1}") String workerId,
            @Value("${worker.heartbeat.interval-ms:5000}") long heartbeatIntervalMs,
            @Value("${worker.heartbeat.ttl-seconds:15}") long heartbeatTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.workerId = workerId;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
    }

    @Scheduled(fixedRateString = "${worker.heartbeat.interval-ms:5000}")
    public boolean publishHeartbeat() {
        String key = buildHeartbeatKey(workerId);
        try {
            String value = Instant.now().toString();
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(heartbeatTtlSeconds));
            logger.debug("Worker '{}' refreshed heartbeat key '{}' (TTL: {}s)", workerId, key, heartbeatTtlSeconds);
            return true;
        } catch (Exception e) {
            logger.warn("Worker '{}' failed to refresh heartbeat in Redis for key '{}': {}", workerId, key, e.getMessage());
            return false;
        }
    }

    public boolean isWorkerOnline(String targetWorkerId) {
        if (targetWorkerId == null || targetWorkerId.isBlank()) {
            return false;
        }
        String key = buildHeartbeatKey(targetWorkerId);
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.warn("Failed to query worker status from Redis for key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    public String getWorkerStatus(String targetWorkerId) {
        return isWorkerOnline(targetWorkerId) ? "ONLINE" : "OFFLINE";
    }

    @PreDestroy
    public boolean unregisterHeartbeat() {
        String key = buildHeartbeatKey(workerId);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                logger.info("Worker '{}' unregistered heartbeat key '{}' during shutdown", workerId, key);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.warn("Worker '{}' failed to unregister heartbeat key '{}' during shutdown: {}", workerId, key, e.getMessage());
            return false;
        }
    }

    public String buildHeartbeatKey(String id) {
        return KEY_PREFIX + id;
    }

    public String getWorkerId() {
        return workerId;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public long getHeartbeatTtlSeconds() {
        return heartbeatTtlSeconds;
    }
}
