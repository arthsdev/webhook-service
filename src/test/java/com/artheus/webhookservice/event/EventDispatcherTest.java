package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchRequest;
import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchResult;
import com.artheus.webhookservice.shared.client.delivery.DeliveryServiceClient;
import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import com.artheus.webhookservice.shared.stream.EventStreamPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDispatcherTest {

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private EventStreamPublisher eventStreamPublisher;

    private EventDispatcher eventDispatcher;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-30T10:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        eventDispatcher = new EventDispatcher(deliveryServiceClient, eventStreamPublisher);
    }

    // SYNCHRONOUS DISPATCH TESTS (dispatch)

    @Test
    @DisplayName("dispatch: Should mark event as NO_SUBSCRIBERS when list is empty")
    void sync_shouldMarkNoSubscribersWhenListIsEmpty() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);

        eventDispatcher.dispatch(event, List.of());

        assertEquals(EventStatus.NO_SUBSCRIBERS, event.getStatus());
        verifyNoInteractions(deliveryServiceClient);
        verifyNoInteractions(eventStreamPublisher);
    }

    @Test
    @DisplayName("dispatch: Should mark event as DISPATCHED when all deliveries succeed")
    void sync_shouldDispatchWhenAllSucceed() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(deliveryServiceClient.dispatch(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(true));

        eventDispatcher.dispatch(event, subscribers);

        assertEquals(EventStatus.DISPATCHED, event.getStatus());
        verify(deliveryServiceClient, times(2)).dispatch(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(eventStreamPublisher);
    }

    @Test
    @DisplayName("dispatch: Should mark event as DISPATCH_FAILED when all deliveries fail")
    void sync_shouldFailWhenAllFail() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(deliveryServiceClient.dispatch(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(false));

        eventDispatcher.dispatch(event, subscribers);

        assertEquals(EventStatus.DISPATCH_FAILED, event.getStatus());
        verify(deliveryServiceClient, times(2)).dispatch(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(eventStreamPublisher);
    }

    @Test
    @DisplayName("dispatch: Should mark event as PARTIALLY_DISPATCHED when some deliveries succeed")
    void sync_shouldPartiallyDispatchWhenSomeSucceed() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(deliveryServiceClient.dispatch(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(true), new DeliveryDispatchResult(false));

        eventDispatcher.dispatch(event, subscribers);

        assertEquals(EventStatus.PARTIALLY_DISPATCHED, event.getStatus());
        verify(deliveryServiceClient, times(2)).dispatch(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(eventStreamPublisher);
    }

    // ASYNCHRONOUS DISPATCH TESTS (dispatchAsync)

    @Test
    @DisplayName("dispatchAsync: Should mark event as NO_SUBSCRIBERS when list is empty")
    void async_shouldMarkNoSubscribersWhenListIsEmpty() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);

        eventDispatcher.dispatchAsync(event, List.of());

        assertEquals(EventStatus.NO_SUBSCRIBERS, event.getStatus());
        verifyNoInteractions(deliveryServiceClient);
        verifyNoInteractions(eventStreamPublisher);
    }

    @Test
    @DisplayName("dispatchAsync: Should mark event as QUEUED when all messages are published successfully")
    void async_shouldQueueWhenAllPublishedSuccessfully() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(eventStreamPublisher.publish(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(true));

        eventDispatcher.dispatchAsync(event, subscribers);

        assertEquals(EventStatus.QUEUED, event.getStatus());
        verify(eventStreamPublisher, times(2)).publish(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(deliveryServiceClient);
    }

    @Test
    @DisplayName("dispatchAsync: Should mark event as DISPATCH_FAILED when all publish attempts fail")
    void async_shouldFailWhenAllPublishFail() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(eventStreamPublisher.publish(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(false));

        eventDispatcher.dispatchAsync(event, subscribers);

        assertEquals(EventStatus.DISPATCH_FAILED, event.getStatus());
        verify(eventStreamPublisher, times(2)).publish(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(deliveryServiceClient);
    }

    @Test
    @DisplayName("dispatchAsync: Should mark event as PARTIALLY_DISPATCHED when some publish attempts succeed")
    void async_shouldPartiallyDispatchWhenSomePublishSucceed() {
        Event event = Event.create("eventType", "{\"foo\":\"bar\"}", clock);
        List<SubscriberInfo> subscribers = List.of(
                new SubscriberInfo(UUID.randomUUID(), "https://url1.com"),
                new SubscriberInfo(UUID.randomUUID(), "https://url2.com")
        );

        when(eventStreamPublisher.publish(any(DeliveryDispatchRequest.class)))
                .thenReturn(new DeliveryDispatchResult(true), new DeliveryDispatchResult(false));

        eventDispatcher.dispatchAsync(event, subscribers);

        assertEquals(EventStatus.PARTIALLY_DISPATCHED, event.getStatus());
        verify(eventStreamPublisher, times(2)).publish(any(DeliveryDispatchRequest.class));
        verifyNoInteractions(deliveryServiceClient);
    }
}