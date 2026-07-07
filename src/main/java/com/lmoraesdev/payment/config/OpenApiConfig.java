package com.lmoraesdev.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Payment API")
                                .description(
                                        "Core de pagamentos Pix — Hexagonal Architecture, Spring Boot 3, PostgreSQL 18")
                                .version("0.1.0")
                                .contact(
                                        new Contact()
                                                .name("Leandro Moraes")
                                                .url("https://github.com/lmoraesdev")));
    }
}
