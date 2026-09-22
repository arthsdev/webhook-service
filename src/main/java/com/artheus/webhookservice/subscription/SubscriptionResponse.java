package com.artheus.webhookservice.subscription;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID publicId,
        String targetUrl,
        String eventType,
        LocalDateTime createdAt,
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
