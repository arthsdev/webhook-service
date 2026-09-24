package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchRequest;
import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchResult;
import com.artheus.webhookservice.shared.client.delivery.DeliveryServiceClient;
import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final DeliveryServiceClient deliveryServiceClient;

    public void dispatch(Event event, List<SubscriberInfo> subscribers) {
        if (subscribers.isEmpty()) {
            event.markNoSubscribers();
            return;
        }

        long successCount = subscribers.stream()
                .map(subscriber -> buildRequest(event, subscriber))
                .map(deliveryServiceClient::dispatch)
                .filter(DeliveryDispatchResult::accepted)
                .count();

        if (successCount == subscribers.size()) {
            event.dispatch();
        } else if (successCount == 0) {
            event.failDispatch();
        } else {
            event.partiallyDispatch();
        }
    }

    private DeliveryDispatchRequest buildRequest(Event event, SubscriberInfo subscriber) {
        return new DeliveryDispatchRequest(
                event.getPublicId(),
                subscriber.publicId(),
                subscriber.targetUrl(),
                event.getPayload()
        );
    }
}