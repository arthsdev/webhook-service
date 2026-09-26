package com.artheus.webhookservice.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Subscriptions", description = "Manage subscriptions — who receives events for which eventType")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(
            summary = "Create a new subscription",
            description = "Registers a target URL that will receive events for a given eventType via the delivery-service."
    )
    @ApiResponse(responseCode = "201", description = "Subscription created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload (e.g. blank targetUrl)")
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

    @Operation(summary = "Find a subscription by its publicId")
    @ApiResponse(responseCode = "200", description = "Subscription found")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @GetMapping("/{publicId}")
    public ResponseEntity<SubscriptionResponse> findByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(subscriptionService.findByPublicId(publicId));
    }
}