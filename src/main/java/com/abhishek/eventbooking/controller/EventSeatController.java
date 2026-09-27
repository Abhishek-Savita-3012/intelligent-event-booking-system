package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.EventSeatResponse;
import com.abhishek.eventbooking.service.EventSeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
public class EventSeatController {

    private final EventSeatService eventSeatService;

    public EventSeatController(EventSeatService eventSeatService) {
        this.eventSeatService = eventSeatService;
    }

    @GetMapping
    public ResponseEntity<List<EventSeatResponse>> getEventSeats(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                eventSeatService.getEventSeats(eventId)
        );
    }
}