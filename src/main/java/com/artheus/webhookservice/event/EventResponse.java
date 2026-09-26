package com.artheus.webhookservice.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(

        @Schema(description = "Public identifier of the event", example = "405b3ebc-afbe-4cd4-adf6-83194c1bf279")
        UUID publicId,

        @Schema(description = "Event type, matched against active subscriptions", example = "order.created")
        String eventType,

        @Schema(description = "Event body, serialized as a JSON string", example = "{\"orderId\":123,\"productId\":10}")
        String payload,

        @Schema(description = "Outcome of the dispatch attempt to matching subscriptions")
        EventStatus status,

        @Schema(description = "Timestamp when the event was created")
        LocalDateTime createdAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getPublicId(),
                event.getEventType(),
                event.getPayload(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }
}