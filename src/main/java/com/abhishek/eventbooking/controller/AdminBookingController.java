package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.AdminBookingResponse;
import com.abhishek.eventbooking.dto.response.PagedResponse;
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
    public ResponseEntity<PagedResponse<AdminBookingResponse>> getBookings(

            @RequestParam(required = false)
            BookingStatus status,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction
    ) {

        return ResponseEntity.ok(
                adminBookingService
                        .getBookings(
                                status,
                                search,
                                page,
                                size,
                                sortBy,
                                direction
                        )
        );
    }
}