package com.chronos.tests.load;
import java.sql.*;

public class SpecificJobCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "neondb_owner", "npg_p56MgIUQXKrV")) {
            // Check job
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM jobs WHERE id = ?::uuid")) {
                ps.setString(1, "6703479b-4e21-44e7-a7f8-b70f567bc40b");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println("Job: name=" + rs.getString("name") + ", status=" + rs.getString("status") +
                        ", enabled=" + rs.getBoolean("enabled") + ", next_run_at=" + rs.getTimestamp("next_run_at"));
                }
            }

            // Check outbox
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM outbox_events WHERE aggregate_id = ?::uuid")) {
                ps.setString(1, "6703479b-4e21-44e7-a7f8-b70f567bc40b");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println("Outbox: id=" + rs.getString("id") + ", status=" + rs.getString("status") +
                        ", created=" + rs.getTimestamp("created_at") + ", published=" + rs.getTimestamp("published_at"));
                }
            }

            // Check executions
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM executions WHERE job_id = ?::uuid")) {
                ps.setString(1, "6703479b-4e21-44e7-a7f8-b70f567bc40b");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println("Execution: id=" + rs.getString("id") + ", status=" + rs.getString("status") +
                        ", worker=" + rs.getString("worker_id") + ", completed=" + rs.getTimestamp("completed_at"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
