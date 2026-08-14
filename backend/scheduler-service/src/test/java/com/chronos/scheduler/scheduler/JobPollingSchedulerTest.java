package com.chronos.scheduler.scheduler;

import com.chronos.scheduler.service.JobSchedulerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobPollingSchedulerTest {

    @Test
    void testPollDueJobsInvokesService() {
        JobSchedulerService service = Mockito.mock(JobSchedulerService.class);
        when(service.processDueJobs()).thenReturn(3);

        JobPollingScheduler scheduler = new JobPollingScheduler(service);
        scheduler.pollDueJobs();

        verify(service).processDueJobs();
    }
}
