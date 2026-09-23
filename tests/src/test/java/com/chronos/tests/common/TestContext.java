package com.chronos.tests.common;

public class TestContext {

    // Microservice Base URLs
    public static final String AUTH_SERVICE_URL = "http://localhost:8081";
    public static final String GATEWAY_SERVICE_URL = "http://localhost:8080";
    public static final String JOB_SERVICE_URL = "http://localhost:8082";
    public static final String SCHEDULER_SERVICE_URL = "http://localhost:8083";
    public static final String EXECUTION_SERVICE_URL = "http://localhost:8084";
    public static final String WORKER_SERVICE_URL = "http://localhost:8085";

    // Kafka Topics
    public static final String KAFKA_TOPIC_JOB_TRIGGERED = "job.triggered";
    public static final String KAFKA_TOPIC_EXECUTION_DISPATCH = "execution.dispatch";
    public static final String KAFKA_TOPIC_EXECUTION_COMPLETED = "execution.completed";
    public static final String KAFKA_TOPIC_EXECUTION_FAILED = "execution.failed";
    public static final String KAFKA_TOPIC_EXECUTION_RETRY = "execution.retry";
    public static final String KAFKA_TOPIC_EXECUTION_DLQ = "execution.dlq";

    // Kafka Consumer Group
    public static final String WORKER_CONSUMER_GROUP = "worker-group";

    // Redis Keys & Patterns
    public static final String SCHEDULER_LOCK_KEY = "scheduler:lock";
    public static final String WORKER_HEARTBEAT_PREFIX = "worker:heartbeat:";

    // Database Connection Parameters
    public static final String DB_USER = System.getenv().getOrDefault("DB_USERNAME", "neondb_owner");
    public static final String DB_PASS = System.getenv().getOrDefault("DB_PASSWORD", "npg_p56MgIUQXKrV");
    public static final String DB_URL_JOB = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");
    public static final String DB_URL_SCHEDULER = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");
    public static final String DB_URL_EXECUTION = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require");
}
