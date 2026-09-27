package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.SeatResponse;
import com.abhishek.eventbooking.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/halls/{hallId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeatsByHall(@PathVariable Long hallId) {

        return ResponseEntity.ok(
                seatService.getSeatsByHall(hallId)
        );
    }
}