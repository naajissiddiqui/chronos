package com.chronos.worker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerHeartbeatServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final String workerId = "worker-test-1";
    private final String expectedKey = "worker:heartbeat:worker-test-1";
    private final long intervalMs = 5000;
    private final long ttlSeconds = 15;

    private WorkerHeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        heartbeatService = new WorkerHeartbeatService(redisTemplate, workerId, intervalMs, ttlSeconds);
    }

    @Test
    void testHeartbeatCreationAndRefresh() {
        doNothing().when(valueOperations).set(eq(expectedKey), anyString(), eq(Duration.ofSeconds(ttlSeconds)));

        boolean published = heartbeatService.publishHeartbeat();

        assertTrue(published);
        verify(valueOperations).set(eq(expectedKey), anyString(), eq(Duration.ofSeconds(ttlSeconds)));
    }

    @Test
    void testWorkerOnlineStatusWhenHeartbeatExists() {
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        assertTrue(heartbeatService.isWorkerOnline(workerId));
        assertEquals("ONLINE", heartbeatService.getWorkerStatus(workerId));
    }

    @Test
    void testWorkerOfflineStatusWhenHeartbeatExpired() {
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        assertFalse(heartbeatService.isWorkerOnline(workerId));
        assertEquals("OFFLINE", heartbeatService.getWorkerStatus(workerId));
    }

    @Test
    void testWorkerBecomingOfflineAfterHeartbeatExpirySimulation() {
        // Initially ONLINE
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);
        assertEquals("ONLINE", heartbeatService.getWorkerStatus(workerId));

        // Heartbeat key expires in Redis (hasKey returns false)
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);
        assertEquals("OFFLINE", heartbeatService.getWorkerStatus(workerId));
        assertFalse(heartbeatService.isWorkerOnline(workerId));
    }

    @Test
    void testGracefulUnregisterOnShutdown() {
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        boolean unregistered = heartbeatService.unregisterHeartbeat();

        assertTrue(unregistered);
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void testRedisFailureBehavior() {
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        boolean published = heartbeatService.publishHeartbeat();
        assertFalse(published, "Heartbeat refresh should return false safely when Redis is down");

        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        assertFalse(heartbeatService.isWorkerOnline(workerId), "isWorkerOnline should return false safely when Redis is down");
        assertEquals("OFFLINE", heartbeatService.getWorkerStatus(workerId), "getWorkerStatus should return OFFLINE safely when Redis is down");

        when(redisTemplate.delete(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));

        assertFalse(heartbeatService.unregisterHeartbeat(), "unregisterHeartbeat should return false safely when Redis is down");
    }
}
