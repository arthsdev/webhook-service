package com.artheus.webhookservice.event;

import com.artheus.webhookservice.shared.testsupport.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class EventServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private EventService eventService;

    @Test
    void shouldCreateAndPersistEvent() {
        EventRequest eventRequest = new EventRequest("Test", "{\"orderId\":123,\"productId\":10}");

        EventResponse eventResponse = eventService.createEvent(eventRequest);

        assertThat(eventResponse.publicId()).isNotNull();
        assertThat(eventResponse.eventType()).isEqualTo("Test");
        assertThat(eventResponse.payload()).isEqualTo(eventRequest.payload());
        assertThat(eventResponse.status()).isEqualTo(EventStatus.NO_SUBSCRIBERS);
        assertThat(eventResponse.createdAt()).isNotNull();

        EventResponse found = eventService.findByPublicId(eventResponse.publicId());
        assertThat(found.publicId()).isEqualTo(eventResponse.publicId());
    }

    @Test
    void shouldThrowExceptionWhenEventNotFound() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> eventService.findByPublicId(randomId))
                .isInstanceOf(EventNotFoundException.class);
    }
}