package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.HallRequest;
import com.abhishek.eventbooking.dto.response.HallResponse;
import com.abhishek.eventbooking.service.HallService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/venues/{venueId}/halls")
public class AdminHallController {

    private final HallService hallService;

    public AdminHallController(HallService hallService) {
        this.hallService = hallService;
    }

    @PostMapping
    public ResponseEntity<HallResponse> createHall(@PathVariable Long venueId, @Valid @RequestBody HallRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        hallService.createHall(venueId, request)
                );
    }
}