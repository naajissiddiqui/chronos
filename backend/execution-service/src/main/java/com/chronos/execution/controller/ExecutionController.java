package com.chronos.execution.controller;

import com.chronos.execution.dto.ExecutionResponse;
import com.chronos.execution.exception.UnauthorizedException;
import com.chronos.execution.service.ExecutionService;
import com.chronos.execution.util.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        UUID orgId = getRequiredOrganizationId();
        List<ExecutionResponse> executions = executionService.getExecutionsForOrganization(orgId);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionResponse> getExecutionById(@PathVariable("id") UUID id) {
        UUID orgId = getRequiredOrganizationId();
        ExecutionResponse execution = executionService.getExecutionByIdAndOrganization(id, orgId);
        return ResponseEntity.ok(execution);
    }

    @GetMapping("/jobs/{jobId}/executions")
    public ResponseEntity<List<ExecutionResponse>> getExecutionsByJobId(@PathVariable("jobId") UUID jobId) {
        UUID orgId = getRequiredOrganizationId();
        List<ExecutionResponse> executions = executionService.getExecutionsByJobIdAndOrganization(jobId, orgId);
        return ResponseEntity.ok(executions);
    }

    private UUID getRequiredOrganizationId() {
        UUID orgId = TenantContext.getOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedException("Missing required X-Organization-Id header");
        }
        return orgId;
    }
}
