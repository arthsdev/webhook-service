package com.artheus.webhookservice.subscription;

import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import com.artheus.webhookservice.shared.contract.subscription.SubscriptionLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionLookup {

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

    @Override
    @Transactional(readOnly = true)
    public List<SubscriberInfo> findActiveByEventType(String eventType) {
        List<Subscription> subscriptions = subscriptionRepository.findByEventTypeAndActiveTrue(eventType);

        return subscriptions.stream()
                .map(sub -> new SubscriberInfo(
                        sub.getPublicId(),
                        sub.getTargetUrl()
                ))
                .toList();
    }
}