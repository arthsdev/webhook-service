package com.artheus.webhookservice.subscription;

import com.artheus.webhookservice.shared.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SubscriptionService subscriptionService;

    @Test
    void shouldCreateAndPersistSubscription() {
        SubscriptionRequest request = new SubscriptionRequest(
                "https://webhook.site/test-id",
                "order.created"
        );

        SubscriptionResponse response = subscriptionService.create(request);

        assertThat(response.publicId()).isNotNull();
        assertThat(response.targetUrl()).isEqualTo("https://webhook.site/test-id");
        assertThat(response.eventType()).isEqualTo("order.created");
        assertThat(response.active()).isTrue();

        SubscriptionResponse found = subscriptionService.findByPublicId(response.publicId());
        assertThat(found.publicId()).isEqualTo(response.publicId());
    }

    @Test
    void shouldThrowExceptionWhenSubscriptionNotFound(){
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> subscriptionService.findByPublicId(randomId))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }
}