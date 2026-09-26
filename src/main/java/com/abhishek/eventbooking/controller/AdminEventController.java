package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.EventRequest;
import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {

        EventResponse response = eventService.createEvent(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest request) {

        return ResponseEntity.ok(
                eventService.updateEvent(
                        id,
                        request
                )
        );
    }

    @PatchMapping("/{id}/cancel") public ResponseEntity<Void> cancelEvent(@PathVariable Long id) {

        eventService.cancelEvent(id);

        return ResponseEntity.noContent().build();
    }
}