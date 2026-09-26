package com.artheus.webhookservice.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @Schema(description = "URL that will receive dispatched events", example = "https://webhook.site/421cf96b-fe43-4f10-ba12-7bdd412bd892")
        @NotBlank(message = "The targetUrl cannot be blank.")
        String targetUrl,

        @Schema(description = "Event type this subscription listens to", example = "order.created")
        @NotBlank(message = "The eventType cannot be blank. ")
        String eventType
) {
}