package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.request.UserBookingSearchCriteria;
import com.abhishek.eventbooking.dto.response.BookingDetailsResponse;
import com.abhishek.eventbooking.dto.response.BookingHistoryResponse;
import com.abhishek.eventbooking.dto.response.BookingResponse;
import com.abhishek.eventbooking.dto.response.PagedResponse;
import com.abhishek.eventbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ==============================
    // CREATE BOOKING
    // ==============================

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(Authentication authentication,

            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,

            @Valid
            @RequestBody
            BookingRequest request
    ) {

        BookingResponse response = bookingService.createBooking(authentication.getName(), idempotencyKey, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Idempotency-Key", idempotencyKey)
                .body(response);
    }

    // ==============================
    // MY BOOKING HISTORY
    // ==============================

    @GetMapping("/my")
    public ResponseEntity<PagedResponse<BookingHistoryResponse>> getMyBookings(
            Authentication authentication,

            @ModelAttribute
            UserBookingSearchCriteria criteria
    ) {

        return ResponseEntity.ok(
                bookingService.getMyBookings(authentication.getName(), criteria)
        );
    }

    // ==============================
    // BOOKING DETAILS
    // ==============================

    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingDetailsResponse> getBookingDetails(@PathVariable String bookingReference,
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.getBookingDetails(authentication.getName(), bookingReference)
        );
    }
}