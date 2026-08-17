package com.chronos.tests.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TestHelper {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static HttpClient getHttpClient() {
        return httpClient;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static HttpResponse<String> sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> sendPost(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static Map<String, Double> scrapePrometheusMetrics(String actuatorPrometheusUrl) {
        Map<String, Double> metrics = new HashMap<>();
        try {
            HttpResponse<String> response = sendGet(actuatorPrometheusUrl);
            if (response.statusCode() == 200) {
                String[] lines = response.body().split("\n");
                for (String line : lines) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            String metricName = parts[0].split("\\{")[0];
                            double value = Double.parseDouble(parts[1]);
                            metrics.put(metricName, value);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to scrape Prometheus metrics from " + actuatorPrometheusUrl + ": " + e.getMessage());
        }
        return metrics;
    }

    public static String executeCliCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            output.append("Execution error: ").append(e.getMessage());
        }
        return output.toString().trim();
    }

    public static int executeDatabaseUpdate(String dbUrl, String query) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl, TestContext.DB_USER, TestContext.DB_PASS);
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(query);
        }
    }

    public static long executeDatabaseCount(String dbUrl, String query) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl, TestContext.DB_USER, TestContext.DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }
}
