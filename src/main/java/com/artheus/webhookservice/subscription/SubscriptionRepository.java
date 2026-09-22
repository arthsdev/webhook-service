package com.artheus.webhookservice.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByEventTypeAndActiveTrue(String eventType);
    Optional<Subscription> findByPublicId(UUID publicId);
}
