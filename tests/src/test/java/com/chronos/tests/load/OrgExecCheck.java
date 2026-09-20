package com.chronos.tests.load;

import java.sql.*;

public class OrgExecCheck {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://ep-broad-mud-ayi20kcf-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "neondb_owner", "npg_p56MgIUQXKrV")) {
            System.out.println("=== Connected to PostgreSQL ===");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT e.id, e.job_id, e.organization_id, e.status, e.worker_id, e.created_at, e.completed_at, j.name FROM executions e LEFT JOIN jobs j ON e.job_id = j.id ORDER BY e.created_at DESC LIMIT 10")) {
                while (rs.next()) {
                    System.out.println("Exec: id=" + rs.getString("id") + ", job=" + rs.getString("name") + ", org=" + rs.getString("organization_id") +
                        ", status=" + rs.getString("status") + ", worker=" + rs.getString("worker_id") + ", created=" + rs.getTimestamp("created_at") + ", completed=" + rs.getTimestamp("completed_at"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
