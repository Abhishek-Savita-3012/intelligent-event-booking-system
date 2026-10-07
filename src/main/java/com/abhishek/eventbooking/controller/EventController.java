package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.EventSearchCriteria;
import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(@ModelAttribute EventSearchCriteria criteria) {

        return ResponseEntity.ok(
                eventService.searchEvents(criteria)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {

        return ResponseEntity.ok(
                eventService.getEventById(id)
        );
    }
}