package com.account_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate configuration with trace ID propagation
 */
@Configuration
public class RestTemplateConfig {

    @Autowired
    private TraceIdClientInterceptor traceIdClientInterceptor;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .interceptors(traceIdClientInterceptor)
                .build();
    }
}

