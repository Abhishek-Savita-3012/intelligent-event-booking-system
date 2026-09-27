package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.HallResponse;
import com.abhishek.eventbooking.service.HallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues/{venueId}/halls")
public class HallController {

    private final HallService hallService;

    public HallController(HallService hallService) {
        this.hallService = hallService;
    }

    @GetMapping
    public ResponseEntity<List<HallResponse>> getHalls(@PathVariable Long venueId) {

        return ResponseEntity.ok(
                hallService.getHallsByVenue(venueId)
        );
    }
}