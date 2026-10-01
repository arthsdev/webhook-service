package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchResult;
import com.artheus.webhookservice.shared.client.delivery.DeliveryServiceClient;
import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import com.artheus.webhookservice.shared.contract.subscription.SubscriptionLookup;
import com.artheus.webhookservice.shared.stream.EventStreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private SubscriptionLookup subscriptionLookup;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private EventStreamPublisher eventStreamPublisher;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        EventDispatcher eventDispatcher = new EventDispatcher(deliveryServiceClient, eventStreamPublisher);
        eventService = new EventService(subscriptionLookup, eventRepository, fixedClock, eventDispatcher);
    }

    @Test
    void shouldMarkAsDispatchedWhenAllSubscribersAccept() {
        EventRequest request = new EventRequest("order.created", "{\"orderId\":123}");
        SubscriberInfo subscriber = new SubscriberInfo(UUID.randomUUID(), "https://webhook.site/test");

        when(subscriptionLookup.findActiveByEventType("order.created"))
                .thenReturn(List.of(subscriber));
        when(deliveryServiceClient.dispatch(any()))
                .thenReturn(new DeliveryDispatchResult(true));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.status()).isEqualTo(EventStatus.DISPATCHED);
    }

    @Test
    void shouldMarkAsNoSubscribersWhenNoneMatch() {
        EventRequest request = new EventRequest("order.created", "{\"orderId\":123}");

        when(subscriptionLookup.findActiveByEventType("order.created"))
                .thenReturn(List.of());

        EventResponse response = eventService.createEvent(request);

        assertThat(response.status()).isEqualTo(EventStatus.NO_SUBSCRIBERS);
    }

    @Test
    void shouldMarkAsDispatchFailedWhenAllSubscribersReject() {
        EventRequest request = new EventRequest("order.created", "{\"orderId\":123}");
        SubscriberInfo subscriber = new SubscriberInfo(UUID.randomUUID(), "https://webhook.site/test");

        when(subscriptionLookup.findActiveByEventType("order.created"))
                .thenReturn(List.of(subscriber));
        when(deliveryServiceClient.dispatch(any()))
                .thenReturn(new DeliveryDispatchResult(false));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.status()).isEqualTo(EventStatus.DISPATCH_FAILED);
    }

    @Test
    void shouldMarkAsPartiallyDispatchedWhenSomeSubscribersReject() {
        EventRequest request = new EventRequest("order.created", "{\"orderId\":123}");
        SubscriberInfo accepted = new SubscriberInfo(UUID.randomUUID(), "https://webhook.site/accepted");
        SubscriberInfo rejected = new SubscriberInfo(UUID.randomUUID(), "https://webhook.site/rejected");

        when(subscriptionLookup.findActiveByEventType("order.created"))
                .thenReturn(List.of(accepted, rejected));
        when(deliveryServiceClient.dispatch(any()))
                .thenReturn(new DeliveryDispatchResult(true))
                .thenReturn(new DeliveryDispatchResult(false));

        EventResponse response = eventService.createEvent(request);

        assertThat(response.status()).isEqualTo(EventStatus.PARTIALLY_DISPATCHED);
    }

}