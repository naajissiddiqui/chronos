package com.chronos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${gateway.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String origin : origins) {
            corsConfiguration.addAllowedOrigin(origin);
            corsConfiguration.addAllowedOriginPattern(origin);
        }

        corsConfiguration.addAllowedMethod("GET");
        corsConfiguration.addAllowedMethod("POST");
        corsConfiguration.addAllowedMethod("PUT");
        corsConfiguration.addAllowedMethod("PATCH");
        corsConfiguration.addAllowedMethod("DELETE");
        corsConfiguration.addAllowedMethod("OPTIONS");

        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }
}
