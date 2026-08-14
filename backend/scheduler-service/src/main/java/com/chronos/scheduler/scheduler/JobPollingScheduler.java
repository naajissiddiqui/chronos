package com.chronos.scheduler.scheduler;

import com.chronos.scheduler.service.JobSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobPollingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobPollingScheduler.class);

    private final JobSchedulerService jobSchedulerService;

    public JobPollingScheduler(JobSchedulerService jobSchedulerService) {
        this.jobSchedulerService = jobSchedulerService;
    }

    @Scheduled(fixedDelayString = "${scheduler.polling-interval-ms:5000}")
    public void pollDueJobs() {
        try {
            int count = jobSchedulerService.processDueJobs();
            if (count > 0) {
                logger.info("Job polling run completed. Processed {} due job(s).", count);
            }
        } catch (Exception e) {
            logger.error("Error during scheduled job polling loop: {}", e.getMessage(), e);
        }
    }
}
