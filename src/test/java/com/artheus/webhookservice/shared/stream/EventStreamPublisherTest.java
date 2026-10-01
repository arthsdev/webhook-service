package com.artheus.webhookservice.shared.stream;

import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchRequest;
import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventStreamPublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    private EventStreamPublisher eventStreamPublisher;

    @Captor
    private ArgumentCaptor<MapRecord<String, String, String>> recordCaptor;

    private static final String STREAM_KEY = "test-stream-deliveries";

    @BeforeEach
    void setUp() {
        eventStreamPublisher = new EventStreamPublisher(redisTemplate, STREAM_KEY);
    }

    @Test
    @DisplayName("Should successfully publish the event to the stream and return accepted as true")
    @SuppressWarnings("unchecked")
    void shouldPublishSuccessfully() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        DeliveryDispatchRequest request = new DeliveryDispatchRequest(
                eventId,
                subscriptionId,
                "https://api.example.com/webhook",
                "{\"foo\":\"bar\"}"
        );

        RecordId mockRecordId = RecordId.of("1680000000000-0");

        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenReturn(mockRecordId);

        // Act
        DeliveryDispatchResult result = eventStreamPublisher.publish(request);

        // Assert
        assertTrue(result.accepted());

        verify(streamOperations).add(recordCaptor.capture());
        MapRecord<String, String, String> capturedRecord = recordCaptor.getValue();

        assertEquals(STREAM_KEY, capturedRecord.getStream());

        Map<String, String> capturedValues = capturedRecord.getValue();
        assertEquals(eventId.toString(), capturedValues.get("eventId"));
        assertEquals(subscriptionId.toString(), capturedValues.get("subscriptionId"));
        assertEquals("https://api.example.com/webhook", capturedValues.get("targetUrl"));
        assertEquals("{\"foo\":\"bar\"}", capturedValues.get("payload"));
        assertEquals("1", capturedValues.get("schemaVersion"));
    }

    @Test
    @DisplayName("Should return accepted as false when Redis add returns null")
    @SuppressWarnings("unchecked")
    void shouldReturnFalseWhenAddReturnsNull() {
        // Arrange
        DeliveryDispatchRequest request = new DeliveryDispatchRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "https://api.example.com/webhook",
                "{\"foo\":\"bar\"}"
        );

        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenReturn(null);

        // Act
        DeliveryDispatchResult result = eventStreamPublisher.publish(request);

        // Assert
        assertFalse(result.accepted());
        verify(streamOperations).add(any(MapRecord.class));
    }

    @Test
    @DisplayName("Should handle RedisConnectionFailureException and return accepted as false")
    @SuppressWarnings("unchecked")
    void shouldHandleRedisConnectionFailureExceptionAndReturnFalse() {
        // Arrange
        DeliveryDispatchRequest request = new DeliveryDispatchRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "https://api.example.com/webhook",
                "{\"foo\":\"bar\"}"
        );

        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenThrow(new RedisConnectionFailureException("Redis connection failed"));

        // Act
        DeliveryDispatchResult result = eventStreamPublisher.publish(request);

        // Assert
        assertFalse(result.accepted());
        verify(streamOperations).add(any(MapRecord.class));
    }
}