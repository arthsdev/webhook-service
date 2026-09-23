package com.artheus.webhookservice.event;

import jakarta.validation.constraints.NotBlank;

public record EventRequest(
    @NotBlank(message = "EventType cannot be blank.")
    String eventType,

    @NotBlank(message = "Payload cannot be blank.")
    String payload
) {
}
