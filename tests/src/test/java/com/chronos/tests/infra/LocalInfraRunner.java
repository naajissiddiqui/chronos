package com.chronos.tests.infra;

import org.h2.tools.Server;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class LocalInfraRunner {

    private static Server pgServer;
    private static EmbeddedKafkaBroker kafkaBroker;

    public static void startInfrastructure() throws Exception {
        // 1. Start PostgreSQL-compatible Server on port 5432
        try {
            File dbDir = new File("target/local-pgdata");
            if (!dbDir.exists()) dbDir.mkdirs();

            pgServer = Server.createPgServer(
                    "-pgPort", "5432",
                    "-pgAllowOthers",
                    "-baseDir", dbDir.getAbsolutePath()
            ).start();
            System.out.println("[INFRA] PostgreSQL server started on port 5432 (status: " + pgServer.getStatus() + ")");

            // Initialize chronos databases
            String h2Url = "jdbc:h2:" + dbDir.getAbsolutePath() + "/chronos_job;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
            try (Connection conn = DriverManager.getConnection(h2Url, "postgres", "postgres");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
                System.out.println("[INFRA] Database chronos_job initialized.");
            }
        } catch (Exception e) {
            System.out.println("[INFRA] Note on PG Server (might already be running): " + e.getMessage());
        }

        // 2. Start Kafka Broker on port 9092
        try {
            Map<String, String> brokerProps = new HashMap<>();
            brokerProps.put("listeners", "PLAINTEXT://0.0.0.0:9092");
            brokerProps.put("advertised.listeners", "PLAINTEXT://localhost:9092");
            brokerProps.put("auto.create.topics.enable", "true");
            brokerProps.put("transaction.state.log.replication.factor", "1");
            brokerProps.put("transaction.state.log.min.isr", "1");
            brokerProps.put("offsets.topic.replication.factor", "1");

            kafkaBroker = new org.springframework.kafka.test.EmbeddedKafkaZKBroker(1, false,
                    "job.triggered",
                    "execution.dispatch",
                    "execution.completed",
                    "execution.failed",
                    "execution.retry",
                    "execution.dlq"
            );
            kafkaBroker.kafkaPorts(9092);
            kafkaBroker.brokerProperties(brokerProps);
            kafkaBroker.afterPropertiesSet();
            System.out.println("[INFRA] Apache Kafka broker started on port 9092.");
        } catch (Exception e) {
            System.out.println("[INFRA] Note on Kafka (might already be running): " + e.getMessage());
        }
    }

    public static void stopInfrastructure() {
        if (kafkaBroker != null) {
            try { kafkaBroker.destroy(); } catch (Exception ignored) {}
        }
        if (pgServer != null) {
            try { pgServer.stop(); } catch (Exception ignored) {}
        }
    }

    public static void main(String[] args) throws Exception {
        startInfrastructure();
        System.out.println("[INFRA] Ready! Press Ctrl+C to terminate.");
        Thread.currentThread().join();
    }
}
