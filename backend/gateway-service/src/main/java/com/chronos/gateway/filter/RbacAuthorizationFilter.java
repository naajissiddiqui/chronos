package com.chronos.gateway.filter;

import com.chronos.gateway.security.AuthenticatedPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RbacAuthorizationFilter implements WebFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RbacAuthorizationFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator"
    );

    @Override
    public int getOrder() {
        return -50; // Runs after AuthenticationGatewayFilter (-100)
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        AuthenticatedPrincipal principal = exchange.getAttribute(AuthenticationGatewayFilter.PRINCIPAL_ATTRIBUTE);
        if (principal == null) {
            // Protected path without principal handled by auth filter, but double check
            return respondForbidden(exchange, "Access denied: unauthenticated");
        }

        HttpMethod method = exchange.getRequest().getMethod();
        String role = principal.getRole();

        boolean isAllowed = checkRolePermission(role, method);
        if (!isAllowed) {
            logger.warn("Access forbidden: user={} role={} method={} path={}", principal.getUserId(), role, method, path);
            return respondForbidden(exchange, "Access denied: insufficient permissions for role " + role);
        }

        return chain.filter(exchange);
    }

    private boolean checkRolePermission(String role, HttpMethod method) {
        if ("OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        if ("EDITOR".equalsIgnoreCase(role)) {
            // Editor can GET, POST, PUT, PATCH, but not DELETE
            return method != HttpMethod.DELETE;
        }

        if ("VIEWER".equalsIgnoreCase(role)) {
            // Viewer can only GET
            return method == HttpMethod.GET || method == HttpMethod.OPTIONS;
        }

        return false;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> respondForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("path", exchange.getRequest().getPath().value());
        errorBody.put("status", HttpStatus.FORBIDDEN.value());
        errorBody.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        errorBody.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}").getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
