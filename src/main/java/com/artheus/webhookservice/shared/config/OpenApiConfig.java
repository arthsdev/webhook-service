package com.artheus.webhookservice.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI webhookServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Webhook Service API")
                        .description("Manages subscriptions and dispatches events to the delivery-service. Part of the Microservices Lab, a distributed systems study project.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Artheus")
                                .url("https://github.com/arthsdev/webhook-service")));
    }
}