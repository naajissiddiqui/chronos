package com.chronos.tests.load;

import com.chronos.tests.common.TestContext;
import java.sql.*;

public class JobQueryTest {
    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(TestContext.DB_URL_EXECUTION, TestContext.DB_USER, TestContext.DB_PASS)) {
            System.out.println("Connected to DB successfully!");
            try (Statement st = conn.createStatement()) {
                int paused = st.executeUpdate("UPDATE jobs SET status = 'PAUSED', enabled = false WHERE name LIKE 'LoadTest-%'");
                System.out.println("Deactivated old load test jobs: " + paused);
                int deletedOutbox = st.executeUpdate("DELETE FROM outbox_events WHERE status = 'PENDING'");
                System.out.println("Cleared stale pending outbox events: " + deletedOutbox);
                
                ResultSet rs = st.executeQuery("SELECT count(*), status, enabled FROM jobs GROUP BY status, enabled");
                while (rs.next()) {
                    System.out.printf("Jobs: count=%d, status=%s, enabled=%s%n", rs.getInt(1), rs.getString(2), rs.getBoolean(3));
                }
                ResultSet rs2 = st.executeQuery("SELECT id, name, organization_id, status, enabled, next_run_at FROM jobs ORDER BY created_at DESC LIMIT 15");
                while (rs2.next()) {
                    System.out.printf("Job: id=%s, name=%s, org=%s, status=%s, enabled=%s, next_run_at=%s%n",
                            rs2.getString(1), rs2.getString(2), rs2.getString(3), rs2.getString(4), rs2.getBoolean(5), rs2.getTimestamp(6));
                }
                ResultSet rs3 = st.executeQuery("SELECT count(*), status FROM outbox_events GROUP BY status");
                while (rs3.next()) {
                    System.out.printf("Outbox: count=%d, status=%s%n", rs3.getInt(1), rs3.getString(2));
                }
                ResultSet rs4 = st.executeQuery("SELECT count(*), status FROM executions GROUP BY status");
                while (rs4.next()) {
                    System.out.printf("Executions: count=%d, status=%s%n", rs4.getInt(1), rs4.getString(2));
                }
                ResultSet rs5 = st.executeQuery("SELECT count(*), status FROM executions WHERE organization_id = '95100367-39fa-4391-849e-823e5fa4bb09' GROUP BY status");
                while (rs5.next()) {
                    System.out.printf("Benchmark Org Executions: count=%d, status=%s%n", rs5.getInt(1), rs5.getString(2));
                }
                ResultSet rs6 = st.executeQuery("SELECT count(*), status FROM outbox_events WHERE payload LIKE '%95100367-39fa-4391-849e-823e5fa4bb09%' GROUP BY status");
                while (rs6.next()) {
                    System.out.printf("Benchmark Org Outbox: count=%d, status=%s%n", rs6.getInt(1), rs6.getString(2));
                }
            }
        }
    }
}
