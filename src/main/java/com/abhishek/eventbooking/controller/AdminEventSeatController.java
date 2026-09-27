package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.EventSeatGenerationRequest;
import com.abhishek.eventbooking.dto.response.EventSeatResponse;
import com.abhishek.eventbooking.service.EventSeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events/{eventId}/seats")
public class AdminEventSeatController {

    private final EventSeatService eventSeatService;

    public AdminEventSeatController(EventSeatService eventSeatService) {
        this.eventSeatService = eventSeatService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<EventSeatResponse>> generateEventSeats(
            @PathVariable Long eventId,
            @Valid
            @RequestBody
            EventSeatGenerationRequest request
    ) {

        List<EventSeatResponse> response = eventSeatService.generateEventSeats(eventId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}