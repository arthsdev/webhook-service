package com.artheus.webhookservice.shared.stream;

import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchRequest;
import com.artheus.webhookservice.shared.client.delivery.DeliveryDispatchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EventStreamPublisher {

    private final StringRedisTemplate redisTemplate;
    private final String streamKey;

    public EventStreamPublisher(
            StringRedisTemplate redisTemplate,
            @Value("${event-stream.deliveries-key}") String streamKey) {
        this.redisTemplate = redisTemplate;
        this.streamKey = streamKey;
    }

    public DeliveryDispatchResult publish(DeliveryDispatchRequest request) {
        try {
            Map<String, String> fields = Map.of(
                    "eventId", request.eventId().toString(),
                    "subscriptionId", request.subscriptionId().toString(),
                    "targetUrl", request.targetUrl(),
                    "payload", request.payload(),
                    "schemaVersion", "1"
            );

            var record = StreamRecords.newRecord()
                    .ofMap(fields)
                    .withStreamKey(this.streamKey);

            RecordId recordId = redisTemplate.opsForStream().add(record);

            if (recordId == null) {
                log.warn("Redis stream add returned null for key [{}]. Event not published.", this.streamKey);
                return new DeliveryDispatchResult(false);
            }

            log.debug("Published to stream [key={}, recordId={}, eventId={}, subscriptionId={}]",
                    this.streamKey, recordId.getValue(), request.eventId(), request.subscriptionId());

            return new DeliveryDispatchResult(true);

        } catch (DataAccessException e) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
            log.warn("Failed to publish to Redis stream [key={}, eventId={}, subscriptionId={}]: {} ({})",
                    this.streamKey, request.eventId(), request.subscriptionId(),
                    cause.getClass().getSimpleName(), cause.getMessage());
            return new DeliveryDispatchResult(false);
        }
    }
}
