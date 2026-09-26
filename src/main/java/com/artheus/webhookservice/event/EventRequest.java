package com.artheus.webhookservice.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EventRequest(
        @Schema(description = "Event type, matched against active subscriptions", example = "order.created")
        @NotBlank(message = "EventType cannot be blank.")
        String eventType,

        @Schema(description = "Event body, serialized as a JSON string", example = "{\"orderId\":123,\"productId\":10}")
        @NotBlank(message = "Payload cannot be blank.")
        String payload
) {
}