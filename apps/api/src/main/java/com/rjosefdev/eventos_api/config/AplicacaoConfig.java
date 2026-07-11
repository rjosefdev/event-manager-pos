package com.rjosefdev.eventos_api.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AplicacaoConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
