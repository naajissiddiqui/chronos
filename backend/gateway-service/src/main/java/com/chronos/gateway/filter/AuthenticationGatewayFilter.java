package com.chronos.gateway.filter;

import com.chronos.gateway.dto.ApiKeyValidationResponse;
import com.chronos.gateway.security.AuthenticatedPrincipal;
import com.chronos.gateway.security.JwtTokenValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthenticationGatewayFilter implements WebFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationGatewayFilter.class);
    public static final String PRINCIPAL_ATTRIBUTE = "AUTHENTICATED_PRINCIPAL";

    public static final String HEADER_ORG_ID = "X-Organization-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_API_KEY = "X-API-Key";

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator"
    );

    private final JwtTokenValidator jwtTokenValidator;
    private final WebClient authWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry;

    @Autowired
    public AuthenticationGatewayFilter(JwtTokenValidator jwtTokenValidator,
                                      WebClient authWebClient,
                                      @Autowired(required = false) MeterRegistry meterRegistry) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.authWebClient = authWebClient;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public int getOrder() {
        return -100; // Run early
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Step 1: Prevent Header Spoofing — remove any client-supplied identity headers
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_ORG_ID);
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLE);
                });

        boolean isPublic = isPublicPath(path);

        // Public routes skip mandatory authentication checks completely
        if (isPublic) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String apiKeyHeader = exchange.getRequest().getHeaders().getFirst(HEADER_API_KEY);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Optional<AuthenticatedPrincipal> principalOpt = jwtTokenValidator.validateAndExtractPrincipal(token);

            if (principalOpt.isPresent()) {
                AuthenticatedPrincipal principal = principalOpt.get();
                ServerHttpRequest mutatedRequest = requestBuilder
                        .header(HEADER_ORG_ID, principal.getOrganizationId().toString())
                        .header(HEADER_USER_ID, principal.getUserId().toString())
                        .header(HEADER_USER_ROLE, principal.getRole())
                        .build();

                exchange.getAttributes().put(PRINCIPAL_ATTRIBUTE, principal);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } else {
                incrementCounter("gateway_auth_failures_total");
                return respondUnauthorized(exchange, "Invalid or expired JWT token");
            }
        } else if (apiKeyHeader != null && !apiKeyHeader.trim().isEmpty()) {
            return validateApiKeyWithAuthService(apiKeyHeader.trim())
                    .flatMap(validation -> {
                        if (validation.isValid()) {
                            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                                    validation.getUserId(),
                                    validation.getOrganizationId(),
                                    validation.getRole(),
                                    "API_KEY"
                            );
                            ServerHttpRequest mutatedRequest = requestBuilder
                                    .header(HEADER_ORG_ID, principal.getOrganizationId().toString())
                                    .header(HEADER_USER_ID, principal.getUserId().toString())
                                    .header(HEADER_USER_ROLE, principal.getRole())
                                    .build();

                            exchange.getAttributes().put(PRINCIPAL_ATTRIBUTE, principal);
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        } else {
                            incrementCounter("gateway_auth_failures_total");
                            return respondUnauthorized(exchange, validation.getErrorReason() != null ? validation.getErrorReason() : "Invalid API key");
                        }
                    })
                    .onErrorResume(ex -> {
                        logger.error("Error contacting Auth Service for API key validation: {}", ex.getMessage());
                        incrementCounter("gateway_auth_failures_total");
                        return respondUnauthorized(exchange, "Failed to validate API key with authentication service");
                    });
        }

        incrementCounter("gateway_auth_failures_total");
        return respondUnauthorized(exchange, "Full authentication is required to access this resource");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<ApiKeyValidationResponse> validateApiKeyWithAuthService(String apiKey) {
        Map<String, String> body = Map.of("apiKey", apiKey);
        return authWebClient.post()
                .uri("/internal/api-keys/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ApiKeyValidationResponse.class);
    }

    private Mono<Void> respondUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("path", exchange.getRequest().getPath().value());
        errorBody.put("status", HttpStatus.UNAUTHORIZED.value());
        errorBody.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorBody.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication failed\"}").getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private void incrementCounter(String metricName) {
        if (meterRegistry != null) {
            Counter.builder(metricName).register(meterRegistry).increment();
        }
    }
}
