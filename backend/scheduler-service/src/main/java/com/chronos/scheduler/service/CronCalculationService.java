package com.chronos.scheduler.service;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class CronCalculationService {

    public Instant calculateNextRunAt(String schedule, String timezoneStr, Instant currentNextRunAt, Instant now) {
        if (schedule == null || schedule.trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule cron expression cannot be null or empty");
        }

        ZoneId zoneId = parseZoneId(timezoneStr);
        CronExpression cronExpression = parseCronExpression(schedule.trim());

        // Base point for next run calculation is currentNextRunAt if it is in the future, otherwise now
        ZonedDateTime startPoint = (currentNextRunAt != null && currentNextRunAt.isAfter(now))
                ? currentNextRunAt.atZone(zoneId)
                : now.atZone(zoneId);

        ZonedDateTime nextZoned = cronExpression.next(startPoint);

        // Ensure next execution is strictly in the future relative to 'now' (missed schedule forward calculation)
        while (nextZoned != null && !nextZoned.toInstant().isAfter(now)) {
            nextZoned = cronExpression.next(nextZoned);
        }

        if (nextZoned == null) {
            throw GebException("Could not calculate next execution time for schedule: " + schedule);
        }

        return nextZoned.toInstant();
    }

    private ZoneId parseZoneId(String timezoneStr) {
        if (timezoneStr == null || timezoneStr.trim().isEmpty()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezoneStr.trim());
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    private CronExpression parseCronExpression(String cronStr) {
        String[] parts = cronStr.split("\\s+");
        // Spring CronExpression expects 6 fields: second, minute, hour, day-of-month, month, day-of-week
        if (parts.length == 5) {
            cronStr = "0 " + cronStr;
        }
        return CronExpression.parse(cronStr);
    }

    private RuntimeException GebException(String message) {
        return new IllegalArgumentException(message);
    }
}
