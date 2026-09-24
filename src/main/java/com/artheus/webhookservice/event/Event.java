package com.artheus.webhookservice.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    private String eventType;

    private String payload;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private LocalDateTime createdAt;

    public static Event create(String eventType, String payload, Clock clock) {
        if (eventType == null || eventType.isBlank() || payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("eventType or payload cannot be blank");
        }

        Event event = new Event();
        event.publicId = UUID.randomUUID();
        event.eventType = eventType;
        event.payload = payload;
        event.status = EventStatus.RECEIVED;
        event.createdAt = LocalDateTime.now(clock);
        return event;
    }

    public void dispatch() {
        status = EventStatus.DISPATCHED;
    }

    public void partiallyDispatch() {
        status = EventStatus.PARTIALLY_DISPATCHED;
    }

    public void failDispatch() {
        status = EventStatus.DISPATCH_FAILED;
    }

    public void markNoSubscribers() {
        status = EventStatus.NO_SUBSCRIBERS;
    }
}
