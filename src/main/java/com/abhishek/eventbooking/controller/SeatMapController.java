package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.EventSeatMapResponse;
import com.abhishek.eventbooking.service.SeatMapService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class SeatMapController {

    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService seatMapService) {

        this.seatMapService = seatMapService;
    }

    @GetMapping("/{eventId}/seat-map")
    public ResponseEntity<EventSeatMapResponse> getSeatMap(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                seatMapService.getSeatMap(eventId)
        );
    }
}