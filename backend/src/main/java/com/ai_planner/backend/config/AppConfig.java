package com.ai_planner.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration   // ← tells Spring this class declares beans
public class AppConfig {

    @Bean  // ← Spring manages this RestTemplate instance
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
