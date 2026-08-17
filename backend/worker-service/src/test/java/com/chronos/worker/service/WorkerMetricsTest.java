package com.chronos.worker.service;

import com.chronos.worker.event.ExecutionDispatchedEvent;
import com.chronos.worker.kafka.KafkaExecutionDispatchConsumer;
import com.chronos.worker.kafka.KafkaWorkerResultProducer;
import com.chronos.worker.task.DemoReportTaskHandler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerMetricsTest {

    @Mock
    private DemoReportTaskHandler taskHandler;

    @Mock
    private KafkaWorkerResultProducer resultProducer;

    @Mock
    private StringRedisTemplate redisTemplate;

    private MeterRegistry meterRegistry;
    private KafkaExecutionDispatchConsumer consumer;
    private WorkerHeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        consumer = new KafkaExecutionDispatchConsumer("worker-1", taskHandler, resultProducer, meterRegistry);
        heartbeatService = new WorkerHeartbeatService(redisTemplate, meterRegistry, "worker-1", 5000, 15);
    }

    @Test
    void testWorkerExecutionSuccessIncrementsProcessedAndSucceededCountersAndTimer() throws Exception {
        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "DEMO_REPORT",
                "payload",
                Instant.now()
        );

        when(taskHandler.execute(any())).thenReturn("Completed");

        consumer.consume(event);

        assertEquals(1.0, meterRegistry.find("worker_executions_processed_total").counter().count());
        assertEquals(1.0, meterRegistry.find("worker_executions_succeeded_total").counter().count());
        assertEquals(1, meterRegistry.find("worker_execution_duration").timer().count());
    }

    @Test
    void testWorkerExecutionFailureIncrementsProcessedAndFailedCounters() throws Exception {
        ExecutionDispatchedEvent event = new ExecutionDispatchedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "DEMO_REPORT_FAIL",
                "payload",
                Instant.now()
        );

        when(taskHandler.execute(any())).thenThrow(new RuntimeException("Simulated Failure"));

        consumer.consume(event);

        assertEquals(1.0, meterRegistry.find("worker_executions_processed_total").counter().count());
        assertEquals(1.0, meterRegistry.find("worker_executions_failed_total").counter().count());
        assertEquals(1, meterRegistry.find("worker_execution_duration").timer().count());
    }

    @Test
    void testWorkersOnlineGaugeReflectsStatus() {
        when(redisTemplate.hasKey("worker:heartbeat:worker-1")).thenReturn(true);
        assertEquals(1.0, meterRegistry.find("workers_online").gauge().value());

        when(redisTemplate.hasKey("worker:heartbeat:worker-1")).thenReturn(false);
        assertEquals(0.0, meterRegistry.find("workers_online").gauge().value());
    }
}
