package com.artheus.webhookservice.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        @Schema(description = "Public identifier of the subscription", example = "f78b013b-83d7-4e9a-be78-665c4fb0ef81")
        UUID publicId,

        @Schema(description = "URL that will receive dispatched events", example = "https://webhook.site/421cf96b-fe43-4f10-ba12-7bdd412bd892")
        String targetUrl,

        @Schema(description = "Event type this subscription listens to", example = "order.created")
        String eventType,

        @Schema(description = "Timestamp when the subscription was created")
        LocalDateTime createdAt,

        @Schema(description = "Whether the subscription is currently active and eligible to receive events")
        boolean active
) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getPublicId(),
                subscription.getTargetUrl(),
                subscription.getEventType(),
                subscription.getCreatedAt(),
                subscription.isActive()
        );
    }
}