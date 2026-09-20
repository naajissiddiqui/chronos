package com.chronos.tests.load;

import java.sql.*;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "neondb_owner", "npg_p56MgIUQXKrV")) {
            System.out.println("=== Connected to REAL NeonDB PostgreSQL successfully ===");

            // Print executions by status
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT status, count(*) FROM executions GROUP BY status")) {
                System.out.println("\n--- EXECUTIONS BY STATUS ---");
                while (rs.next()) {
                    System.out.println("Status: " + rs.getString(1) + " -> " + rs.getInt(2));
                }
            } catch (Exception e) {
                System.out.println("Executions query error: " + e.getMessage());
            }

            // Print latest 5 executions
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, job_id, status, worker_id, attempt, scheduled_at, completed_at FROM executions ORDER BY created_at DESC LIMIT 5")) {
                System.out.println("\n--- LATEST 5 EXECUTIONS ---");
                while (rs.next()) {
                    System.out.println("Execution: id=" + rs.getString("id") + ", job=" + rs.getString("job_id") +
                        ", status=" + rs.getString("status") + ", worker=" + rs.getString("worker_id") + ", attempt=" + rs.getInt("attempt") +
                        ", scheduled=" + rs.getTimestamp("scheduled_at") + ", completed=" + rs.getTimestamp("completed_at"));
                }
            } catch (Exception e) {
                System.out.println("Latest executions error: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
