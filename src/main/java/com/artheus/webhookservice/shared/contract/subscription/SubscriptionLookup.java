package com.artheus.webhookservice.shared.contract.subscription;

import java.util.List;

public interface SubscriptionLookup {
    List<SubscriberInfo> findActiveByEventType(String eventType);
}