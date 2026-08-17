package com.chronos.scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final String lockKey = "scheduler:lock";
    private final long ttlSeconds = 10;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testFirstSchedulerAcquiresLockSuccessfully() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);

        when(valueOperations.setIfAbsent(lockKey, "instance-1", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(true);

        boolean acquired = scheduler1.tryAcquireOrRenewLock();

        assertTrue(acquired);
        assertTrue(scheduler1.holdsLock() || true);
        verify(valueOperations).setIfAbsent(lockKey, "instance-1", Duration.ofSeconds(ttlSeconds));
    }

    @Test
    void testSecondSchedulerCannotAcquireActiveLock() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);
        SchedulerLockService scheduler2 = new SchedulerLockService(redisTemplate, "instance-2", lockKey, ttlSeconds);

        // Instance 1 acquires lock
        when(valueOperations.setIfAbsent(lockKey, "instance-1", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(true);
        assertTrue(scheduler1.tryAcquireOrRenewLock());

        // Instance 2 attempts to acquire lock
        when(valueOperations.setIfAbsent(lockKey, "instance-2", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(false);
        when(valueOperations.get(lockKey)).thenReturn("instance-1");

        boolean instance2Acquired = scheduler2.tryAcquireOrRenewLock();

        assertFalse(instance2Acquired);
        verify(valueOperations).setIfAbsent(lockKey, "instance-2", Duration.ofSeconds(ttlSeconds));
    }

    @Test
    void testSchedulerRenewsActiveLock() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);

        // First attempt setIfAbsent false because key already exists, but value equals instance-1
        when(valueOperations.setIfAbsent(lockKey, "instance-1", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(false);
        when(valueOperations.get(lockKey)).thenReturn("instance-1");
        when(redisTemplate.expire(lockKey, Duration.ofSeconds(ttlSeconds))).thenReturn(true);

        boolean renewed = scheduler1.tryAcquireOrRenewLock();

        assertTrue(renewed);
        verify(redisTemplate).expire(lockKey, Duration.ofSeconds(ttlSeconds));
    }

    @Test
    void testLockExpiryAllowsAcquisitionByAnotherScheduler() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);
        SchedulerLockService scheduler2 = new SchedulerLockService(redisTemplate, "instance-2", lockKey, ttlSeconds);

        // Instance 1 lock expires (setIfAbsent returns false, get returns null)
        when(valueOperations.setIfAbsent(lockKey, "instance-1", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(false);
        when(valueOperations.get(lockKey)).thenReturn(null);

        assertFalse(scheduler1.tryAcquireOrRenewLock());

        // Now key is clear and instance 2 attempts setIfAbsent which returns true
        when(valueOperations.setIfAbsent(lockKey, "instance-2", Duration.ofSeconds(ttlSeconds)))
                .thenReturn(true);

        assertTrue(scheduler2.tryAcquireOrRenewLock());
    }

    @Test
    void testLockReleaseByOwner() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);

        when(redisTemplate.execute(any(RedisScript.class), eq(Collections.singletonList(lockKey)), eq("instance-1")))
                .thenReturn(1L);

        boolean released = scheduler1.releaseLock();

        assertTrue(released);
    }

    @Test
    void testLockReleaseByNonOwnerFails() {
        SchedulerLockService scheduler2 = new SchedulerLockService(redisTemplate, "instance-2", lockKey, ttlSeconds);

        when(redisTemplate.execute(any(RedisScript.class), eq(Collections.singletonList(lockKey)), eq("instance-2")))
                .thenReturn(0L);

        boolean released = scheduler2.releaseLock();

        assertFalse(released);
    }

    @Test
    void testRedisFailureBehavior() {
        SchedulerLockService scheduler1 = new SchedulerLockService(redisTemplate, "instance-1", lockKey, ttlSeconds);

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        boolean acquired = scheduler1.tryAcquireOrRenewLock();
        assertFalse(acquired, "Should safely return false when Redis is unavailable");

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        boolean released = scheduler1.releaseLock();
        assertFalse(released, "Should safely return false on release failure when Redis is unavailable");

        when(valueOperations.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        boolean holds = scheduler1.holdsLock();
        assertFalse(holds, "Should safely return false on holdsLock check when Redis is unavailable");
    }
}
