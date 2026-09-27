package com.artheus.webhookservice.subscription;

import com.artheus.webhookservice.shared.contract.subscription.SubscriberInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionService subscriptionService;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        subscriptionService = new SubscriptionService(subscriptionRepository, fixedClock);
    }

    @Test
    void shouldReturnSubscription(){
        Subscription subscription = Subscription.create("https://www.teste.com/teste", "Teste", fixedClock);

        when(subscriptionRepository.findByPublicId(subscription.getPublicId()))
                .thenReturn(Optional.of(subscription));

        SubscriptionResponse response = subscriptionService.findByPublicId(subscription.getPublicId());

        assertThat(response.publicId()).isEqualTo(subscription.getPublicId());
        assertThat(response.targetUrl()).isEqualTo("https://www.teste.com/teste");
    }

    @Test
    void shouldReturnActiveSubscribersByEventType() {
        Subscription subscription = Subscription.create("https://www.teste.com/teste", "Teste", fixedClock);

        when(subscriptionRepository.findByEventTypeAndActiveTrue("Teste"))
                .thenReturn(List.of(subscription));

        List<SubscriberInfo> result = subscriptionService.findActiveByEventType("Teste");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).publicId()).isEqualTo(subscription.getPublicId());
        assertThat(result.get(0).targetUrl()).isEqualTo(subscription.getTargetUrl());
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveSubscribersForEventType() {
        when(subscriptionRepository.findByEventTypeAndActiveTrue("Teste"))
                .thenReturn(List.of());

        List<SubscriberInfo> result = subscriptionService.findActiveByEventType("Teste");

        assertThat(result).isEmpty();
    }

}