package com.chronos.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "gateway.services.auth-url=http://localhost:59999"
})
class GatewayDownstreamUnavailableTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testDownstreamServiceUnavailableErrorHandling() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password123\"}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.message").isEqualTo("Downstream auth service is unavailable")
                .jsonPath("$.path").isEqualTo("/api/v1/auth/login")
                .jsonPath("$.timestamp").exists();
    }
}
