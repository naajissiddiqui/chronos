package com.chronos.job.controller;

import com.chronos.job.dto.CreateJobRequest;
import com.chronos.job.dto.JobStatusUpdateRequest;
import com.chronos.job.dto.UpdateJobRequest;
import com.chronos.job.entity.JobPriority;
import com.chronos.job.entity.JobStatus;
import com.chronos.job.repository.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    private UUID orgA;
    private UUID orgB;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        orgA = UUID.randomUUID();
        orgB = UUID.randomUUID();
    }

    @Test
    void testCreateJobSuccess() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "Daily Analytics Report",
                "Generates system report daily at 2 AM",
                "0 0 2 * * *",
                "Asia/Kolkata",
                JobPriority.HIGH,
                600,
                3,
                60
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.organizationId").value(orgA.toString()))
                .andExpect(jsonPath("$.name").value("Daily Analytics Report"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.nextRunAt").exists());
    }

    @Test
    void testGetJobSuccess() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "Cleanup Inactive Sessions",
                "Deletes old session records",
                "0 0 * * * *",
                "UTC",
                JobPriority.NORMAL,
                300,
                2,
                30
        );

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String jobId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.name").value("Cleanup Inactive Sessions"))
                .andExpect(jsonPath("$.organizationId").value(orgA.toString()));
    }

    @Test
    void testListJobsForOrganization() throws Exception {
        CreateJobRequest job1 = new CreateJobRequest("Job A1", "Desc A1", "0 0 * * * *", "UTC", JobPriority.LOW, 100, 1, 10);
        CreateJobRequest job2 = new CreateJobRequest("Job A2", "Desc A2", "0 0 * * * *", "UTC", JobPriority.HIGH, 200, 2, 20);
        CreateJobRequest job3 = new CreateJobRequest("Job B1", "Desc B1", "0 0 * * * *", "UTC", JobPriority.NORMAL, 300, 3, 30);

        mockMvc.perform(post("/api/v1/jobs").header("X-Organization-Id", orgA.toString())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job1))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs").header("X-Organization-Id", orgA.toString())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job2))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs").header("X-Organization-Id", orgB.toString())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(job3))).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Job A1", "Job A2")));
    }

    @Test
    void testUpdateJobSuccess() throws Exception {
        CreateJobRequest createReq = new CreateJobRequest("Original Name", "Original Desc", "0 0 * * * *", "UTC", JobPriority.NORMAL, 300, 1, 10);

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        UpdateJobRequest updateReq = new UpdateJobRequest(
                "Updated Name",
                "Updated Description",
                "0 30 * * * *",
                "Europe/London",
                JobPriority.CRITICAL,
                1200,
                5,
                120
        );

        mockMvc.perform(put("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.timeoutSeconds").value(1200))
                .andExpect(jsonPath("$.timezone").value("Europe/London"));
    }

    @Test
    void testPauseAndDisableJobStatus() throws Exception {
        CreateJobRequest createReq = new CreateJobRequest("Active Job", "Running job", "0 0 * * * *", "UTC", JobPriority.NORMAL, 300, 1, 10);

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        JobStatusUpdateRequest statusReq = new JobStatusUpdateRequest(JobStatus.PAUSED);

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void testDeleteJobSuccess() throws Exception {
        CreateJobRequest createReq = new CreateJobRequest("Job to Delete", "Temporary job", "0 0 * * * *", "UTC", JobPriority.LOW, 100, 0, 0);

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testValidationFailures() throws Exception {
        CreateJobRequest invalidReq = new CreateJobRequest(
                "",
                "Description",
                "invalid cron pattern",
                "Invalid/Timezone",
                JobPriority.NORMAL,
                -10,
                -1,
                -5
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void testJobNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/jobs/" + nonExistentId)
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Job not found with ID: " + nonExistentId));
    }

    @Test
    void testOrganizationIsolation() throws Exception {
        CreateJobRequest createReq = new CreateJobRequest("Org A Confidential Job", "Secret", "0 0 * * * *", "UTC", JobPriority.HIGH, 300, 1, 10);

        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .header("X-Organization-Id", orgA.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Org B attempts GET -> 404
        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isNotFound());

        // Org B attempts PUT -> 404
        UpdateJobRequest updateReq = new UpdateJobRequest("Hacked Name", "Hacked", "0 0 * * * *", "UTC", JobPriority.LOW, 100, 0, 0);
        mockMvc.perform(put("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgB.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Org B attempts PATCH -> 404
        JobStatusUpdateRequest statusReq = new JobStatusUpdateRequest(JobStatus.DISABLED);
        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("X-Organization-Id", orgB.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isNotFound());

        // Org B attempts DELETE -> 404
        mockMvc.perform(delete("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgB.toString()))
                .andExpect(status().isNotFound());

        // Verify Org A can still access the job unaffected
        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("X-Organization-Id", orgA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Org A Confidential Job"));
    }
}
