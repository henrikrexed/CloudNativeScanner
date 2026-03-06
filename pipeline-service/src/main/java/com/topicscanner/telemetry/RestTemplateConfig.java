package com.topicscanner.telemetry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Configures RestTemplate with the RateLimitInterceptor for scanner HTTP calls.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate scannerRestTemplate(RateLimitInterceptor rateLimitInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(rateLimitInterceptor));
        return restTemplate;
    }
}
