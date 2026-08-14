package com.chronos.scheduler.kafka;

import com.chronos.scheduler.entity.JobPriority;
import com.chronos.scheduler.event.JobTriggeredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaJobTriggerConsumerTest {

    @Test
    void testConsumeEventLogsWithoutError() {
        KafkaJobTriggerConsumer consumer = new KafkaJobTriggerConsumer();
        JobTriggeredEvent event = new JobTriggeredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                JobPriority.NORMAL
        );

        assertDoesNotThrow(() -> consumer.consume(event));
    }

    @Test
    void testConsumeNullEventHandlesGracefully() {
        KafkaJobTriggerConsumer consumer = new KafkaJobTriggerConsumer();
        assertDoesNotThrow(() -> consumer.consume(null));
    }
}
