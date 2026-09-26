package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.SeatRequest;
import com.abhishek.eventbooking.dto.response.SeatResponse;
import com.abhishek.eventbooking.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/venues/{venueId}/seats")
public class AdminSeatController {

    private final SeatService seatService;

    public AdminSeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@PathVariable Long venueId, @Valid @RequestBody SeatRequest request) {

        SeatResponse response = seatService.createSeat(venueId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}