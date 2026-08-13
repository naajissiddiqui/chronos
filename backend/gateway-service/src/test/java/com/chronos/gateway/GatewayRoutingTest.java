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
        "gateway.services.auth-url=http://localhost:${wiremock.server.port}"
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
}
