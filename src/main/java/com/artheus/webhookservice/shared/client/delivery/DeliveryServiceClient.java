package com.artheus.webhookservice.shared.client.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class DeliveryServiceClient {

    private final RestClient restClient;

    public DeliveryServiceClient(@Value("${delivery-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
            log.warn("Failed to dispatch to delivery-service: {}", ex.getMessage());
            return new DeliveryDispatchResult(false);
        }
    }
}