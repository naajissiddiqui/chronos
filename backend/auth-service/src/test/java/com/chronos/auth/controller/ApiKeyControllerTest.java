package com.chronos.auth.controller;

import com.chronos.auth.dto.AuthResponse;
import com.chronos.auth.dto.RegisterRequest;
import com.chronos.auth.entity.Role;
import com.chronos.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setEmail("apikeyctrl@example.com");
        regReq.setPassword("Password123!");
        regReq.setFirstName("Alice");
        regReq.setLastName("Key");
        regReq.setOrganizationName("Key Corp");
        regReq.setRole(Role.OWNER);

        AuthResponse authResponse = authService.register(regReq);
        jwtToken = authResponse.getAccessToken();
    }

    @Test
    void testCreateApiKey_Authenticated_Returns201AndRawKey() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Deploy Key");

        mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Deploy Key"))
                .andExpect(jsonPath("$.rawKey").value(startsWith("chron_")))
                .andExpect(jsonPath("$.keyPrefix").exists())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void testCreateApiKey_Unauthenticated_Returns401() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Unauthorized Key");

        mockMvc.perform(post("/api/v1/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testListApiKeys_Authenticated_ReturnsListWithoutRawKeyOrHash() throws Exception {
        // Create a key first
        Map<String, Object> body = new HashMap<>();
        body.put("name", "List Test Key");

        mockMvc.perform(post("/api/v1/api-keys")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        mockMvc.perform(get("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("List Test Key"))
                .andExpect(jsonPath("$[0].rawKey").doesNotExist())
                .andExpect(jsonPath("$[0].keyHash").doesNotExist());
    }

    @Test
    void testRevokeApiKey_Authenticated_Returns204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Key to Delete");

        String responseStr = mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> created = objectMapper.readValue(responseStr, Map.class);
        String keyId = (String) created.get("id");

        mockMvc.perform(delete("/api/v1/api-keys/" + keyId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testInternalValidateApiKey_ReturnsResult() throws Exception {
        // Create key
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Internal Validate Key");

        String responseStr = mockMvc.perform(post("/api/v1/api-keys")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> created = objectMapper.readValue(responseStr, Map.class);
        String rawKey = (String) created.get("rawKey");

        // Validate via internal endpoint
        Map<String, String> validateBody = new HashMap<>();
        validateBody.put("apiKey", rawKey);

        mockMvc.perform(post("/internal/api-keys/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.organizationId").exists())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }
}
