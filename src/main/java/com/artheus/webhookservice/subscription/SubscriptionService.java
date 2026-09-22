package com.artheus.webhookservice.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public SubscriptionResponse create(SubscriptionRequest request) {
        Subscription subscription = Subscription.create(request.targetUrl(), request.eventType(), clock);

        subscriptionRepository.save(subscription);

        return SubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse findByPublicId(UUID publicId) {
        Subscription subscription = subscriptionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SubscriptionNotFoundException(publicId));

        return SubscriptionResponse.from(subscription);
    }
}