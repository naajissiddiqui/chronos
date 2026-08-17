package com.chronos.scheduler.service;

import com.chronos.scheduler.scheduler.JobPollingScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerMetricsTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JobSchedulerService jobSchedulerService;

    private MeterRegistry meterRegistry;
    private SchedulerLockService schedulerLockService;
    private JobPollingScheduler jobPollingScheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        schedulerLockService = new SchedulerLockService(redisTemplate, meterRegistry, "instance-1", "scheduler:lock", 10);
        jobPollingScheduler = new JobPollingScheduler(jobSchedulerService, schedulerLockService, meterRegistry);
    }

    @Test
    void testLockAcquisitionAndPollMetricsIncrement() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(jobSchedulerService.processDueJobs()).thenReturn(2);

        jobPollingScheduler.pollDueJobs();

        assertEquals(1.0, meterRegistry.find("scheduler_lock_acquisitions_total").counter().count());
        assertEquals(1.0, meterRegistry.find("scheduler_poll_runs_total").counter().count());
        assertEquals(1, meterRegistry.find("scheduler_poll_duration").timer().count());
    }

    @Test
    void testLockMissMetricsIncrement() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get(anyString())).thenReturn("other-instance");

        jobPollingScheduler.pollDueJobs();

        assertEquals(1.0, meterRegistry.find("scheduler_lock_misses_total").counter().count());
    }
}
