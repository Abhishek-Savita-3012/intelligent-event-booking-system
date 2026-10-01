package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.AdminBookingResponse;
import com.abhishek.eventbooking.entity.BookingStatus;
import com.abhishek.eventbooking.service.AdminBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    public AdminBookingController(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @GetMapping
    public ResponseEntity<List<AdminBookingResponse>> getBookings(
            @RequestParam(required = false)
            BookingStatus status)
    {

        return ResponseEntity.ok(
                adminBookingService.getBookings(status)
        );
    }
}