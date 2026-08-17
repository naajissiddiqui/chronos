package com.chronos.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
public class SchedulerLockService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerLockService.class);

    private static final String RELEASE_LOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final String instanceId;
    private final String lockKey;
    private final long lockTtlSeconds;

    @Autowired
    public SchedulerLockService(
            StringRedisTemplate redisTemplate,
            @Value("${scheduler.lock.key:scheduler:lock}") String lockKey,
            @Value("${scheduler.lock.ttl-seconds:10}") long lockTtlSeconds) {
        this(redisTemplate, UUID.randomUUID().toString(), lockKey, lockTtlSeconds);
    }


    public SchedulerLockService(
            StringRedisTemplate redisTemplate,
            String instanceId,
            String lockKey,
            long lockTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.instanceId = instanceId;
        this.lockKey = lockKey;
        this.lockTtlSeconds = lockTtlSeconds;
    }

    public boolean tryAcquireOrRenewLock() {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    instanceId,
                    Duration.ofSeconds(lockTtlSeconds)
            );

            if (Boolean.TRUE.equals(acquired)) {
                logger.info("Scheduler instance {} acquired leader lock '{}' (TTL: {}s)", instanceId, lockKey, lockTtlSeconds);
                return true;
            }

            // Lock exists. Check if held by this instance and renew TTL.
            String currentOwner = redisTemplate.opsForValue().get(lockKey);
            if (instanceId.equals(currentOwner)) {
                Boolean renewed = redisTemplate.expire(lockKey, Duration.ofSeconds(lockTtlSeconds));
                if (Boolean.TRUE.equals(renewed)) {
                    logger.debug("Scheduler instance {} renewed lock '{}' (TTL: {}s)", instanceId, lockKey, lockTtlSeconds);
                    return true;
                }
            }

            logger.debug("Scheduler instance {} failed to acquire lock '{}' (currently held by {})", instanceId, lockKey, currentOwner);
            return false;
        } catch (Exception e) {
            logger.warn("Redis unavailable during scheduler lock acquisition/renewal for key '{}': {}", lockKey, e.getMessage());
            return false;
        }
    }

    public boolean releaseLock() {
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(RELEASE_LOCK_LUA_SCRIPT, Long.class);
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), instanceId);
            boolean released = Long.valueOf(1L).equals(result);
            if (released) {
                logger.info("Scheduler instance {} released lock '{}'", instanceId, lockKey);
            } else {
                logger.debug("Scheduler instance {} attempted to release lock '{}' but did not hold it", instanceId, lockKey);
            }
            return released;
        } catch (Exception e) {
            logger.warn("Redis unavailable during lock release for key '{}': {}", lockKey, e.getMessage());
            return false;
        }
    }

    public boolean holdsLock() {
        try {
            String currentOwner = redisTemplate.opsForValue().get(lockKey);
            return instanceId.equals(currentOwner);
        } catch (Exception e) {
            logger.warn("Redis unavailable during holdsLock check for key '{}': {}", lockKey, e.getMessage());
            return false;
        }
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getLockTtlSeconds() {
        return lockTtlSeconds;
    }
}
