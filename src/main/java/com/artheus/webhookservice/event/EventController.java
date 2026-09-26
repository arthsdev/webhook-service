package com.artheus.webhookservice.event;

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
@RequestMapping("/app/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Publish events and dispatch them to matching subscriptions")
public class EventController {

    private final EventService eventService;

    @Operation(
            summary = "Publish a new event",
            description = "Creates an event and dispatches it to every active subscription matching its eventType, via the delivery-service."
    )
    @ApiResponse(responseCode = "201", description = "Event created and processed (status reflects the dispatch outcome: DISPATCHED, PARTIALLY_DISPATCHED, DISPATCH_FAILED or NO_SUBSCRIBERS)")
    @ApiResponse(responseCode = "400", description = "Invalid payload")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody @Valid EventRequest eventRequest,
            UriComponentsBuilder uriBuilder) {

        EventResponse response = eventService.createEvent(eventRequest);

        URI location = uriBuilder.path("/app/v1/events/{publicId}")
                .buildAndExpand(response.publicId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Find an event by its publicId")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @GetMapping("/{publicId}")
    public ResponseEntity<EventResponse> findByPublicId(@PathVariable UUID publicId) {
        EventResponse response = eventService.findByPublicId(publicId);
        return ResponseEntity.ok(response);
    }
}