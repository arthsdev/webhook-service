package com.artheus.webhookservice.subscription;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @NotBlank(message = "The targetUrl cannot be blank.")
        String targetUrl,

        @NotBlank(message = "The eventType cannot be blank. ")
        String eventType
) {
}
