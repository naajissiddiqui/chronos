package com.chronos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${gateway.services.auth-internal-url:http://localhost:8081}")
    private String authInternalUrl;

    @Bean
    public WebClient authWebClient(WebClient.Builder builder) {
        return builder.baseUrl(authInternalUrl).build();
    }
}
