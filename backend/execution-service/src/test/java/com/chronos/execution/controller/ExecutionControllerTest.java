package com.chronos.execution.controller;

import com.chronos.execution.entity.Execution;
import com.chronos.execution.entity.ExecutionStatus;
import com.chronos.execution.event.JobTriggeredEvent;
import com.chronos.execution.repository.ExecutionRepository;
import com.chronos.execution.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionService executionService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionDispatchProducer kafkaExecutionDispatchProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionRetryProducer kafkaExecutionRetryProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.kafka.KafkaExecutionDlqProducer kafkaExecutionDlqProducer;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.chronos.execution.client.JobServiceClient jobServiceClient;

    private UUID orgA;
    private UUID orgB;
    private UUID jobIdA;

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
        jobIdA = UUID.randomUUID();
    }

    @Test
    void testGetAllExecutionsReturnsOnlyCurrentOrganizationExecutions() throws Exception {
        JobTriggeredEvent eventA = new JobTriggeredEvent(UUID.randomUUID(), jobIdA, orgA, Instant.now(), Instant.now(), "NORMAL");
        JobTriggeredEvent eventB = new JobTriggeredEvent(UUID.randomUUID(), UUID.randomUUID(), orgB, Instant.now(), Instant.now(), "NORMAL");

        executionService.createExecutionFromEvent(eventA);
        executionService.createExecutionFromEvent(eventB);

        // Org A request
        mockMvc.perform(get("/api/v1/executions")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jobId").value(jobIdA.toString()))
                .andExpect(jsonPath("$[0].organizationId").value(orgA.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Org B request
        mockMvc.perform(get("/api/v1/executions")
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].organizationId").value(orgB.toString()));
    }

    @Test
    void testGetExecutionByIdWorksAndEnforcesOrganizationIsolation() throws Exception {
        JobTriggeredEvent eventA = new JobTriggeredEvent(UUID.randomUUID(), jobIdA, orgA, Instant.now(), Instant.now(), "HIGH");
        Execution executionA = executionService.createExecutionFromEvent(eventA).orElseThrow();

        // Org A can access execution A
        mockMvc.perform(get("/api/v1/executions/" + executionA.getId())
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(executionA.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempt").value(1));

        // Org B cannot access execution A (returns 404)
        mockMvc.perform(get("/api/v1/executions/" + executionA.getId())
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetExecutionsByJobIdWorks() throws Exception {
        JobTriggeredEvent eventA1 = new JobTriggeredEvent(UUID.randomUUID(), jobIdA, orgA, Instant.now(), Instant.now(), "HIGH");
        JobTriggeredEvent eventA2 = new JobTriggeredEvent(UUID.randomUUID(), jobIdA, orgA, Instant.now(), Instant.now(), "HIGH");

        executionService.createExecutionFromEvent(eventA1);
        executionService.createExecutionFromEvent(eventA2);

        mockMvc.perform(get("/api/v1/jobs/" + jobIdA + "/executions")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testMissingOrganizationHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/executions"))
                .andExpect(status().isUnauthorized());
    }
}
