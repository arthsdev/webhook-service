package com.artheus.webhookservice.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/app/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;


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

    @GetMapping("/{publicId}")
    public ResponseEntity<EventResponse> findByPublicId(@PathVariable UUID publicId) {
        EventResponse response = eventService.findByPublicId(publicId);
        return ResponseEntity.ok(response);
    }
}
