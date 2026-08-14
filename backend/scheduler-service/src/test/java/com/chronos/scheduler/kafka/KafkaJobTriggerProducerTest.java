package com.chronos.scheduler.kafka;

import com.chronos.scheduler.entity.JobPriority;
import com.chronos.scheduler.event.JobTriggeredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaJobTriggerProducerTest {

    private KafkaTemplate<String, JobTriggeredEvent> kafkaTemplate;
    private KafkaJobTriggerProducer producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new KafkaJobTriggerProducer(kafkaTemplate, "job.triggered");
    }

    @Test
    void testSendJobTriggeredPublishesWithCorrectTopicKeyAndEvent() {
        UUID eventId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant now = Instant.now();

        JobTriggeredEvent event = new JobTriggeredEvent(eventId, jobId, orgId, now, now, JobPriority.HIGH);

        when(kafkaTemplate.send(eq("job.triggered"), eq(jobId.toString()), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.sendJobTriggered(event);

        ArgumentCaptor<JobTriggeredEvent> captor = ArgumentCaptor.forClass(JobTriggeredEvent.class);
        verify(kafkaTemplate).send(eq("job.triggered"), eq(jobId.toString()), captor.capture());

        JobTriggeredEvent capturedEvent = captor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(eventId, capturedEvent.getEventId());
        assertEquals(jobId, capturedEvent.getJobId());
        assertEquals(orgId, capturedEvent.getOrganizationId());
        assertEquals(JobPriority.HIGH, capturedEvent.getPriority());
    }

    @Test
    void testSendJobTriggeredWithNullEventDoesNotPublish() {
        producer.sendJobTriggered(null);
        verifyNoInteractions(kafkaTemplate);
    }
}
