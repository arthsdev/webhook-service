package com.artheus.webhookservice.shared.client.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Component
public class DeliveryServiceClient {

    private final RestClient restClient;

    public DeliveryServiceClient(
            @Value("${delivery-service.base-url}") String baseUrl,
            @Value("${delivery-service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${delivery-service.read-timeout:10s}") Duration readTimeout) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public DeliveryDispatchResult dispatch(DeliveryDispatchRequest request) {
        try {
            restClient.post()
                    .uri("/app/v1/deliveries")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            return new DeliveryDispatchResult(true);

        } catch (RestClientException ex) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
            log.warn("Failed to dispatch to delivery-service [eventId={}, subscriptionId={}]: {} ({})",
                    request.eventId(), request.subscriptionId(),
                    cause.getClass().getSimpleName(), cause.getMessage());
            return new DeliveryDispatchResult(false);
        }
    }
}