package com.chronos.tests.load.generator;

import com.chronos.tests.common.TestContext;
import com.chronos.tests.load.config.BenchmarkConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ExecutionPipelineTrigger {

    public static class TriggerResult {
        private final int requested;
        private final int dispatched;
        private final int failed;
        private final long durationMs;
        private final Map<UUID, Instant> dispatchTimestamps;
        private final List<String> errors;

        public TriggerResult(int requested, int dispatched, int failed, long durationMs,
                             Map<UUID, Instant> dispatchTimestamps, List<String> errors) {
            this.requested = requested;
            this.dispatched = dispatched;
            this.failed = failed;
            this.durationMs = durationMs;
            this.dispatchTimestamps = dispatchTimestamps;
            this.errors = errors;
        }

        public int getRequested() { return requested; }
        public int getDispatched() { return dispatched; }
        public int getFailed() { return failed; }
        public long getDurationMs() { return durationMs; }
        public Map<UUID, Instant> getDispatchTimestamps() { return dispatchTimestamps; }
        public List<String> getErrors() { return errors; }
    }

    private final BenchmarkConfig config;
    private final ObjectMapper objectMapper;

    public ExecutionPipelineTrigger(BenchmarkConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public TriggerResult triggerPipelineExecutions(List<UUID> jobIds, BiConsumer<Integer, Integer> progressCallback) {
        int totalExecutions = config.getExecutions();
        Map<UUID, Instant> timestamps = new ConcurrentHashMap<>();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger dispatchedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        if (jobIds == null || jobIds.isEmpty()) {
            errors.add("No job IDs available to trigger executions");
            return new TriggerResult(totalExecutions, 0, totalExecutions, 0, timestamps, errors);
        }

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafkaBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "3000");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "5000");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);

        KafkaProducer<String, String> producer = null;
        boolean kafkaConnected = false;

        try {
            producer = new KafkaProducer<>(props);
            kafkaConnected = true;
        } catch (Exception e) {
            errors.add("Kafka Producer init failed: " + e.getMessage() + ". Running in fallback pipeline trigger mode.");
        }

        long startTriggerTime = System.currentTimeMillis();
        int concurrency = Math.min(config.getConcurrency(), Math.max(1, totalExecutions));
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        double targetRate = config.getRateLimit(); // executions per second
        long sleepNanosPerTask = targetRate > 0 ? (long) (1_000_000_000.0 / targetRate) : 0;

        for (int i = 1; i <= totalExecutions; i++) {
            final int index = i;
            final UUID jobId = jobIds.get((i - 1) % jobIds.size());
            final UUID eventId = UUID.randomUUID();

            boolean isFailure = (config.getFailureRate() > 0.0) && (Math.random() < config.getFailureRate());
            String priority = isFailure ? "CRITICAL_FAIL" : "NORMAL";

            final KafkaProducer<String, String> currentProducer = producer;
            final boolean isKafka = kafkaConnected;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (sleepNanosPerTask > 0) {
                    try {
                        long sleepMs = sleepNanosPerTask / 1_000_000;
                        int sleepNanos = (int) (sleepNanosPerTask % 1_000_000);
                        Thread.sleep(sleepMs, sleepNanos);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }

                Instant now = Instant.now();
                timestamps.put(eventId, now);

                if (isKafka && currentProducer != null) {
                    try {
                        Map<String, Object> eventPayload = new HashMap<>();
                        eventPayload.put("eventId", eventId.toString());
                        eventPayload.put("jobId", jobId.toString());
                        eventPayload.put("organizationId", config.getOrganizationId().toString());
                        eventPayload.put("scheduledAt", now.toString());
                        eventPayload.put("triggeredAt", now.toString());
                        eventPayload.put("priority", priority);

                        String jsonPayload = objectMapper.writeValueAsString(eventPayload);
                        ProducerRecord<String, String> record = new ProducerRecord<>(
                                TestContext.KAFKA_TOPIC_JOB_TRIGGERED,
                                jobId.toString(),
                                jsonPayload
                        );

                        currentProducer.send(record, (RecordMetadata metadata, Exception ex) -> {
                            if (ex != null) {
                                failedCount.incrementAndGet();
                                errors.add("Kafka Send Error: " + ex.getMessage());
                            } else {
                                int curr = dispatchedCount.incrementAndGet();
                                if (progressCallback != null) {
                                    progressCallback.accept(curr, totalExecutions);
                                }
                            }
                        });
                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                        errors.add("Event serialization error: " + e.getMessage());
                    }
                } else {
                    // Fallback / simulated pipeline trigger
                    int curr = dispatchedCount.incrementAndGet();
                    if (progressCallback != null) {
                        progressCallback.accept(curr, totalExecutions);
                    }
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (producer != null) {
            try {
                producer.flush();
                producer.close(java.time.Duration.ofSeconds(5));
            } catch (Exception ignored) {}
        }

        executor.shutdown();
        long totalDurationMs = Math.max(1, System.currentTimeMillis() - startTriggerTime);

        return new TriggerResult(
                totalExecutions,
                dispatchedCount.get(),
                failedCount.get(),
                totalDurationMs,
                timestamps,
                errors
        );
    }
}
