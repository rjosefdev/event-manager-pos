package com.rjosefdev.eventos_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER_JWT = "bearer-jwt";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes(ESQUEMA_BEARER_JWT, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
