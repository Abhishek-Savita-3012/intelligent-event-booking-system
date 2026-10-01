package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.DashboardSummaryResponse;
import com.abhishek.eventbooking.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {

        return ResponseEntity.ok(
                adminDashboardService.getDashboardSummary()
        );
    }
}