package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.EventStatus;
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
    public ResponseEntity<List<EventResponse>> searchEvents(

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            String city,

            @RequestParam(required = false)
            EventCategory category,

            @RequestParam(required = false)
            EventStatus status
    ) {

        return ResponseEntity.ok(
                eventService.searchEvents(
                        name,
                        city,
                        category,
                        status
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {

        return ResponseEntity.ok(
                eventService.getEventById(id)
        );
    }
}