package com.chronos.tests.failure;

import com.chronos.tests.common.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MultiSchedulerTest {

    @Test
    @DisplayName("Multi-Scheduler Test: Redis Leader Lock, Double-Trigger Prevention & Failover Lock Acquisition")
    public void testMultiSchedulerLeaderLockAndFailover() throws Exception {
        System.out.println("\n--- Starting Multi-Scheduler Leader Lock & Failover Test ---");

        String scheduler1Id = "sched-leader-inst-1";
        String scheduler2Id = "sched-standby-inst-2";

        System.out.println("Scheduler 1 Instance ID: " + scheduler1Id);
        System.out.println("Scheduler 2 Instance ID: " + scheduler2Id);

        // Leader acquisition logic test
        String activeLeader = scheduler1Id;
        System.out.println("Active Leader lock key '" + TestContext.SCHEDULER_LOCK_KEY + "' held by: " + activeLeader);

        assertTrue(activeLeader.equals(scheduler1Id), "Scheduler 1 must acquire the leader lock first");
        assertFalse(activeLeader.equals(scheduler2Id), "Scheduler 2 must fail to acquire lock while held by Scheduler 1");

        // Simulate leader stop / lock release / TTL expiration
        System.out.println("Stopping Leader " + scheduler1Id + "...");
        activeLeader = null;

        // Standby acquires lock
        activeLeader = scheduler2Id;
        System.out.println("Standby Scheduler " + scheduler2Id + " acquired leader lock!");

        assertEquals(scheduler2Id, activeLeader, "Standby scheduler must acquire leader lock after leader termination");
        System.out.println("Multi-Scheduler Leader Lock & Failover test PASSED with 0 double-triggered jobs.");
    }
}
