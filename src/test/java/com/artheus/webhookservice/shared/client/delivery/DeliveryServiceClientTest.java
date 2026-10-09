package com.artheus.webhookservice.shared.client.delivery;

import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryServiceClientTimeoutTest {

    @Test
    void shouldRejectInsteadOfWaitingWhenServerNeverResponds() throws Exception {
        try (ServerSocket silentServer = new ServerSocket(0)) {
            String baseUrl = "http://localhost:" + silentServer.getLocalPort();
            DeliveryServiceClient client =
                    new DeliveryServiceClient(baseUrl, Duration.ofSeconds(1), Duration.ofSeconds(1));
            DeliveryDispatchRequest request = new DeliveryDispatchRequest(
                    UUID.randomUUID(), UUID.randomUUID(), "https://example.com/hook", "{}");

            long start = System.nanoTime();
            DeliveryDispatchResult result = client.dispatch(request);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

            assertFalse(result.accepted());
            assertTrue(elapsed.compareTo(Duration.ofSeconds(5)) < 0,
                    "dispatch should give up after the read timeout, but took " + elapsed);
        }
    }
}