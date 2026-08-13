package com.chronos.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GlobalExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted() || CorsUtils.isPreFlightRequest(exchange.getRequest())) {
            return Mono.empty();
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected gateway error occurred";

        if (isConnectionError(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream auth service is unavailable";
            log.warn("Downstream connection failed for path {}: {}", exchange.getRequest().getPath(), ex.getMessage());
        } else if (isTimeoutError(ex)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Downstream service timed out";
            log.warn("Downstream timeout for path {}: {}", exchange.getRequest().getPath(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            log.warn("Response status exception for path {}: status {}, message {}", exchange.getRequest().getPath(), status, message);
        } else {
            log.error("Unhandled gateway exception for path {}", exchange.getRequest().getPath(), ex);
        }

        response.setStatusCode(status);

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("path", exchange.getRequest().getPath().value());
        errorBody.put("status", status.value());
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Failed to serialize error response\"}")
                    .getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isConnectionError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException || current.getClass().getName().contains("ConnectException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isTimeoutError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException || current.getClass().getName().contains("TimeoutException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
