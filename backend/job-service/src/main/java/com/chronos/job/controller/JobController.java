package com.chronos.job.controller;

import com.chronos.job.dto.*;
import com.chronos.job.exception.InvalidTenantException;
import com.chronos.job.service.JobService;
import com.chronos.job.util.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        JobResponse response = jobService.createJob(request, organizationId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        List<JobResponse> jobs = jobService.getJobsForOrganization(organizationId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        JobResponse job = jobService.getJobById(id, organizationId);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        JobResponse updated = jobService.updateJob(id, request, organizationId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobResponse> updateJobStatus(
            @PathVariable UUID id,
            @Valid @RequestBody JobStatusUpdateRequest request,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        JobResponse updated = jobService.updateJobStatus(id, request, organizationId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID headerOrgId) {
        UUID organizationId = resolveOrganizationId(headerOrgId);
        jobService.deleteJob(id, organizationId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveOrganizationId(UUID headerOrgId) {
        if (headerOrgId != null) {
            return headerOrgId;
        }
        UUID contextOrgId = TenantContext.getOrganizationId();
        if (contextOrgId != null) {
            return contextOrgId;
        }
        throw new InvalidTenantException("Organization ID is required in X-Organization-Id header");
    }
}
