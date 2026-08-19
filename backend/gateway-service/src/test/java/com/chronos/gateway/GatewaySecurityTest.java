package com.chronos.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
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

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "gateway.services.auth-url=http://localhost:${wiremock.server.port}",
        "gateway.services.auth-internal-url=http://localhost:${wiremock.server.port}",
        "gateway.services.job-url=http://localhost:${wiremock.server.port}",
        "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
})
class GatewaySecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    private SecretKey jwtKey;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.jwtKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private String createJwtToken(UUID userId, UUID orgId, String role, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject("testuser@example.com")
                .claim("userId", userId.toString())
                .claim("organizationId", orgId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(jwtKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // 1. Public Auth Routes remain accessible without credentials
    // -------------------------------------------------------------------------

    @Test
    void testPublicAuthRoutes_AccessibleWithoutCredentials() {
        stubFor(post(urlEqualTo("/api/v1/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"accessToken\":\"token123\"}")));

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"user@example.com\",\"password\":\"secret\"}")
                .exchange()
                .expectStatus().isOk();
    }

    // -------------------------------------------------------------------------
    // 2. Protected Route without credentials -> 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    void testProtectedRoute_WithoutCredentials_Returns401() {
        webTestClient.get()
                .uri("/api/v1/jobs")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized");
    }

    // -------------------------------------------------------------------------
    // 3. Valid JWT -> Accepted & identity headers forwarded to downstream
    // -------------------------------------------------------------------------

    @Test
    void testValidJwt_Accepted_AndHeadersForwarded() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String token = createJwtToken(userId, orgId, "OWNER", 3600000);

        stubFor(get(urlEqualTo("/api/v1/jobs"))
                .withHeader("X-Organization-Id", equalTo(orgId.toString()))
                .withHeader("X-User-Id", equalTo(userId.toString()))
                .withHeader("X-User-Role", equalTo("OWNER"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    // -------------------------------------------------------------------------
    // 4. Expired JWT -> 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    void testExpiredJwt_Returns401() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String token = createJwtToken(userId, orgId, "OWNER", -1000); // Expired 1 second ago

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401);
    }

    // -------------------------------------------------------------------------
    // 5. Malformed JWT -> 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    void testMalformedJwt_Returns401() {
        webTestClient.get()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer malformed.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // -------------------------------------------------------------------------
    // 6. Valid API Key -> Accepted & headers forwarded
    // -------------------------------------------------------------------------

    @Test
    void testValidApiKey_Accepted() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String rawKey = "chron_validprefix_secretkey123456789";

        // Stub internal auth validation call
        stubFor(post(urlEqualTo("/internal/api-keys/validate"))
                .withRequestBody(matchingJsonPath("$.apiKey", equalTo(rawKey)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format(
                                "{\"valid\":true,\"userId\":\"%s\",\"organizationId\":\"%s\",\"role\":\"ADMIN\"}",
                                userId, orgId
                        ))));

        stubFor(get(urlEqualTo("/api/v1/jobs"))
                .withHeader("X-Organization-Id", equalTo(orgId.toString()))
                .withHeader("X-User-Id", equalTo(userId.toString()))
                .withHeader("X-User-Role", equalTo("ADMIN"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header("X-API-Key", rawKey)
                .exchange()
                .expectStatus().isOk();
    }

    // -------------------------------------------------------------------------
    // 7. Revoked API Key -> 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    void testRevokedApiKey_Returns401() {
        String rawKey = "chron_revokedprefix_secretkey123456789";

        stubFor(post(urlEqualTo("/internal/api-keys/validate"))
                .withRequestBody(matchingJsonPath("$.apiKey", equalTo(rawKey)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":false,\"errorReason\":\"API key is revoked\"}")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header("X-API-Key", rawKey)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("API key is revoked");
    }

    // -------------------------------------------------------------------------
    // 8. Expired API Key -> 401 Unauthorized
    // -------------------------------------------------------------------------

    @Test
    void testExpiredApiKey_Returns401() {
        String rawKey = "chron_expiredprefix_secretkey123456789";

        stubFor(post(urlEqualTo("/internal/api-keys/validate"))
                .withRequestBody(matchingJsonPath("$.apiKey", equalTo(rawKey)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"valid\":false,\"errorReason\":\"API key is expired\"}")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header("X-API-Key", rawKey)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("API key is expired");
    }

    // -------------------------------------------------------------------------
    // 9. Header Spoofing Prevention -> Client X-Organization-Id is overwritten
    // -------------------------------------------------------------------------

    @Test
    void testHeaderSpoofingPrevention_OverwrittenByJwtClaim() {
        UUID trueOrgId = UUID.randomUUID();
        UUID spoofedOrgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String token = createJwtToken(userId, trueOrgId, "OWNER", 3600000);

        // Downstream MUST receive trueOrgId, NOT spoofedOrgId
        stubFor(get(urlEqualTo("/api/v1/jobs"))
                .withHeader("X-Organization-Id", equalTo(trueOrgId.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        webTestClient.get()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Organization-Id", spoofedOrgId.toString()) // Attempted spoof
                .exchange()
                .expectStatus().isOk();
    }

    // -------------------------------------------------------------------------
    // 10. RBAC Authorization Rules
    // -------------------------------------------------------------------------

    @Test
    void testRbac_ViewerRole_CanRead_CannotDelete() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String viewerToken = createJwtToken(userId, orgId, "VIEWER", 3600000);

        stubFor(get(urlEqualTo("/api/v1/jobs"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        // GET is allowed for VIEWER
        webTestClient.get()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                .exchange()
                .expectStatus().isOk();

        // DELETE is forbidden for VIEWER -> 403
        webTestClient.delete()
                .uri("/api/v1/jobs/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403);
    }

    @Test
    void testRbac_EditorRole_CanPost_CannotDelete() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String editorToken = createJwtToken(userId, orgId, "EDITOR", 3600000);

        stubFor(post(urlEqualTo("/api/v1/jobs"))
                .willReturn(aResponse().withStatus(201).withBody("{}")));

        // POST is allowed for EDITOR
        webTestClient.post()
                .uri("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"Job 1\"}")
                .exchange()
                .expectStatus().isCreated();

        // DELETE is forbidden for EDITOR -> 403
        webTestClient.delete()
                .uri("/api/v1/jobs/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + editorToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void testRbac_OwnerRole_CanDelete() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        String ownerToken = createJwtToken(userId, orgId, "OWNER", 3600000);

        UUID jobId = UUID.randomUUID();
        stubFor(delete(urlEqualTo("/api/v1/jobs/" + jobId))
                .willReturn(aResponse().withStatus(204)));

        // DELETE is allowed for OWNER
        webTestClient.delete()
                .uri("/api/v1/jobs/" + jobId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isNoContent();
    }
}
