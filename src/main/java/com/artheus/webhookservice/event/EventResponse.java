package com.artheus.webhookservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(

        UUID publicId,

        String eventType,

        String payload,

        EventStatus status,

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
