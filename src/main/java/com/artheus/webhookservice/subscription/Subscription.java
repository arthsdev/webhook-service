package com.artheus.webhookservice.subscription;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    private String targetUrl;

    private String eventType;

    private LocalDateTime createdAt;

    private boolean active;

    public static Subscription create(String targetUrl, String eventType, Clock clock) {
        if (targetUrl == null || targetUrl.isBlank() || eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("targetUrl or eventType cannot be blank");
        }

        Subscription subscription = new Subscription();
        subscription.publicId = UUID.randomUUID();
        subscription.targetUrl = targetUrl;
        subscription.eventType = eventType;
        subscription.createdAt = LocalDateTime.now(clock);
        subscription.active = true;
        return subscription;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }


}
