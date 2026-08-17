package com.chronos.scheduler.scheduler;

import com.chronos.scheduler.service.JobSchedulerService;
import com.chronos.scheduler.service.SchedulerLockService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JobPollingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobPollingScheduler.class);

    private final JobSchedulerService jobSchedulerService;
    private final SchedulerLockService schedulerLockService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public JobPollingScheduler(JobSchedulerService jobSchedulerService,
                               SchedulerLockService schedulerLockService,
                               MeterRegistry meterRegistry) {
        this.jobSchedulerService = jobSchedulerService;
        this.schedulerLockService = schedulerLockService;
        this.meterRegistry = meterRegistry;
    }


    public JobPollingScheduler(JobSchedulerService jobSchedulerService,
                               SchedulerLockService schedulerLockService) {
        this(jobSchedulerService, schedulerLockService, null);
    }

    @Scheduled(fixedDelayString = "${scheduler.polling-interval-ms:5000}")
    public void pollDueJobs() {
        long startTime = System.nanoTime();
        try {
            if (!schedulerLockService.tryAcquireOrRenewLock()) {
                logger.debug("Scheduler instance {} did not acquire lock. Skipping job polling cycle.", schedulerLockService.getInstanceId());
                return;
            }

            if (meterRegistry != null) {
                Counter.builder("scheduler_poll_runs_total").description("Total scheduler poll runs").register(meterRegistry).increment();
            }

            int count = jobSchedulerService.processDueJobs();
            if (count > 0) {
                logger.info("Job polling run completed. Processed {} due job(s).", count);
            }
        } catch (Exception e) {
            logger.error("Error during scheduled job polling loop: {}", e.getMessage(), e);
        } finally {
            if (meterRegistry != null) {
                Timer.builder("scheduler_poll_duration")
                        .description("Duration of job polling runs")
                        .register(meterRegistry)
                        .record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
        }
    }
}


