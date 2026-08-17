package com.chronos.scheduler.scheduler;

import com.chronos.scheduler.service.JobSchedulerService;
import com.chronos.scheduler.service.SchedulerLockService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobPollingSchedulerTest {

    @Test
    void testPollDueJobsInvokesServiceWhenLockAcquired() {
        JobSchedulerService service = Mockito.mock(JobSchedulerService.class);
        SchedulerLockService lockService = Mockito.mock(SchedulerLockService.class);

        when(lockService.tryAcquireOrRenewLock()).thenReturn(true);
        when(service.processDueJobs()).thenReturn(3);

        JobPollingScheduler scheduler = new JobPollingScheduler(service, lockService);
        scheduler.pollDueJobs();

        verify(lockService).tryAcquireOrRenewLock();
        verify(service).processDueJobs();
    }

    @Test
    void testPollDueJobsSkippedWhenLockNotAcquired() {
        JobSchedulerService service = Mockito.mock(JobSchedulerService.class);
        SchedulerLockService lockService = Mockito.mock(SchedulerLockService.class);

        when(lockService.tryAcquireOrRenewLock()).thenReturn(false);

        JobPollingScheduler scheduler = new JobPollingScheduler(service, lockService);
        scheduler.pollDueJobs();

        verify(lockService).tryAcquireOrRenewLock();
        verify(service, never()).processDueJobs();
    }
}

