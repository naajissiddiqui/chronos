package com.chronos.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "gateway.services.auth-url=http://localhost:${wiremock.server.port}",
        "gateway.services.job-url=http://localhost:${wiremock.server.port}"
})
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @Test
    void contextLoadsAndHealthCheckWorks() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    // --- Auth Service Routing Tests ---

    @Test
    void testRegisterRouting() {
        stubFor(post(urlEqualTo("/api/v1/auth/register"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"user-123\",\"email\":\"test@example.com\"}")));

        webTestClient.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password123\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("user-123");

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/register")));
    }

    @Test
    void testLoginRouting() {
        stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"accessToken\":\"access-123\",\"refreshToken\":\"refresh-123\"}")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-123");

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/login")));
    }

    @Test
    void testRefreshRouting() {
        stubFor(post(urlEqualTo("/api/v1/auth/refresh"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"accessToken\":\"access-456\",\"refreshToken\":\"refresh-456\"}")));

        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\":\"refresh-123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-456");

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/refresh")));
    }

    @Test
    void testAuthorizationHeaderForwarding() {
        stubFor(post(urlEqualTo("/api/v1/auth/refresh"))
                .withHeader("Authorization", equalTo("Bearer sample-token-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"accessToken\":\"new-token\"}")));

        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer sample-token-123")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\":\"refresh-123\"}")
                .exchange()
                .expectStatus().isOk();

        verify(postRequestedFor(urlEqualTo("/api/v1/auth/refresh"))
                .withHeader("Authorization", equalTo("Bearer sample-token-123")));
    }

    @Test
    void testCorsConfigurationForAllowedOrigin() {
        webTestClient.options()
                .uri("http://localhost:" + port + "/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    // --- Job Service Routing Tests ---

    @Test
    void testCreateJobRouting() {
        stubFor(post(urlEqualTo("/api/v1/jobs"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"job-123\",\"name\":\"Daily Report\",\"status\":\"ACTIVE\"}")));

        webTestClient.post()
                .uri("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Daily Report\",\"schedule\":\"0 0 2 * * *\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("job-123")
                .jsonPath("$.name").isEqualTo("Daily Report");

        verify(postRequestedFor(urlEqualTo("/api/v1/jobs")));
    }

    @Test
    void testGetJobsListRouting() {
        stubFor(get(urlEqualTo("/api/v1/jobs"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{\"id\":\"job-123\",\"name\":\"Daily Report\"}]")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo("job-123");

        verify(getRequestedFor(urlEqualTo("/api/v1/jobs")));
    }

    @Test
    void testGetJobByIdRouting() {
        stubFor(get(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"name\":\"Daily Report\"}")));

        webTestClient.get()
                .uri("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("550e8400-e29b-41d4-a716-446655440000");

        verify(getRequestedFor(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000")));
    }

    @Test
    void testPatchJobStatusRouting() {
        stubFor(patch(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"status\":\"PAUSED\"}")));

        webTestClient.patch()
                .uri("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"status\":\"PAUSED\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PAUSED");

        verify(patchRequestedFor(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000/status")));
    }

    @Test
    void testDeleteJobRouting() {
        stubFor(delete(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000"))
                .willReturn(aResponse()
                        .withStatus(204)));

        webTestClient.delete()
                .uri("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000")
                .exchange()
                .expectStatus().isNoContent();

        verify(deleteRequestedFor(urlEqualTo("/api/v1/jobs/550e8400-e29b-41d4-a716-446655440000")));
    }

    @Test
    void testOrganizationIdHeaderForwardingToJobService() {
        stubFor(post(urlEqualTo("/api/v1/jobs"))
                .withHeader("X-Organization-Id", equalTo("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"id\":\"job-456\",\"organizationId\":\"a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d\"}")));

        webTestClient.post()
                .uri("/api/v1/jobs")
                .header("X-Organization-Id", "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Tenant Job\"}")
                .exchange()
                .expectStatus().isCreated();

        verify(postRequestedFor(urlEqualTo("/api/v1/jobs"))
                .withHeader("X-Organization-Id", equalTo("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")));
    }
}
