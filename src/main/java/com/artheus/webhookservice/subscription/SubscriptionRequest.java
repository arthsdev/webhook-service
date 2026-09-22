package com.artheus.webhookservice.subscription;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionRequest(
        @NotBlank String targetUrl,
        @NotBlank String eventType
) {
}
