package com.chronos.scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CronCalculationServiceTest {

    private CronCalculationService cronCalculationService;

    @BeforeEach
    void setUp() {
        cronCalculationService = new CronCalculationService();
    }

    @Test
    void testCronCalculationWithTimezone() {
        // Schedule: Every day at 2:00 AM Kolkata time
        String schedule = "0 0 2 * * *";
        String timezone = "Asia/Kolkata";

        // Reference time: Aug 14, 2026, 00:00:00 UTC
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        Instant currentNextRun = Instant.parse("2026-08-13T20:30:00Z"); // Previous 2 AM IST (8/14 02:00 IST = 8/13 20:30 UTC)

        Instant calculated = cronCalculationService.calculateNextRunAt(schedule, timezone, currentNextRun, now);

        assertNotNull(calculated);
        assertTrue(calculated.isAfter(now), "Calculated nextRunAt must be after now");

        // Convert calculated to IST: should be Aug 15 2:00 AM IST -> Aug 14 20:30:00 UTC
        ZonedDateTime zonedCalculated = calculated.atZone(ZoneId.of("Asia/Kolkata"));
        assertEquals(2, zonedCalculated.getHour());
        assertEquals(0, zonedCalculated.getMinute());
        assertEquals(0, zonedCalculated.getSecond());
    }

    @Test
    void testFiveFieldCronExpression() {
        // Schedule: Every 5 minutes
        String schedule = "*/5 * * * *";
        String timezone = "UTC";

        Instant now = Instant.parse("2026-08-14T10:02:00Z");
        Instant calculated = cronCalculationService.calculateNextRunAt(schedule, timezone, now, now);

        assertNotNull(calculated);
        assertEquals(Instant.parse("2026-08-14T10:05:00Z"), calculated);
    }

    @Test
    void testMissedScheduleAdvancesToFutureOccurrence() {
        // Job schedule was supposed to run every hour, but service was down for 5 hours
        String schedule = "0 0 * * * *"; // Every hour at min 0 sec 0
        String timezone = "UTC";

        Instant missedRunAt = Instant.parse("2026-08-14T05:00:00Z");
        Instant now = Instant.parse("2026-08-14T10:15:00Z"); // 5 hours later

        Instant nextRunAt = cronCalculationService.calculateNextRunAt(schedule, timezone, missedRunAt, now);

        assertNotNull(nextRunAt);
        assertTrue(nextRunAt.isAfter(now), "Missed schedule must advance strictly into the future");
        assertEquals(Instant.parse("2026-08-14T11:00:00Z"), nextRunAt);
    }

    @Test
    void testInvalidTimezoneDefaultsToUtc() {
        String schedule = "0 0 12 * * *";
        String timezone = "INVALID_TIMEZONE_STRING";

        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        Instant nextRunAt = cronCalculationService.calculateNextRunAt(schedule, timezone, now, now);

        assertNotNull(nextRunAt);
        assertEquals(Instant.parse("2026-08-14T12:00:00Z"), nextRunAt);
    }
}
