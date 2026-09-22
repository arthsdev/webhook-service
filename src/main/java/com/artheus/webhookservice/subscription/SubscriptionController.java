package com.artheus.webhookservice.subscription;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/app/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody SubscriptionRequest request,
            UriComponentsBuilder uriBuilder) {

        SubscriptionResponse response = subscriptionService.create(request);

        URI location = uriBuilder.path("/app/v1/subscriptions/{publicId}")
                .buildAndExpand(response.publicId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<SubscriptionResponse> findByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(subscriptionService.findByPublicId(publicId));
    }


}
