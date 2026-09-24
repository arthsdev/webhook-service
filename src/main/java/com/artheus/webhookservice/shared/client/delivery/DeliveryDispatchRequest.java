package com.artheus.webhookservice.shared.client.delivery;

import java.util.UUID;

public record DeliveryDispatchRequest(
        UUID eventId,
        UUID subscriptionId,
        String targetUrl,
        String payload
) {}