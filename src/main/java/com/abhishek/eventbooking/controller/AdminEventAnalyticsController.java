package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.EventAnalyticsResponse;
import com.abhishek.eventbooking.service.AdminEventAnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventAnalyticsController {

    private final AdminEventAnalyticsService adminEventAnalyticsService;

    public AdminEventAnalyticsController(AdminEventAnalyticsService adminEventAnalyticsService) {

        this.adminEventAnalyticsService = adminEventAnalyticsService;
    }

    @GetMapping("/{eventId}/analytics")
    public ResponseEntity<EventAnalyticsResponse> getEventAnalytics(@PathVariable Long eventId) {

        return ResponseEntity.ok(
                adminEventAnalyticsService.getEventAnalytics(eventId)
        );
    }
}