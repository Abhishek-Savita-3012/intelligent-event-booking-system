package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.RefundSimulationRequest;
import com.abhishek.eventbooking.dto.response.RefundResponse;
import com.abhishek.eventbooking.service.BookingCancellationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingCancellationController {

    private final BookingCancellationService bookingCancellationService;

    public BookingCancellationController(BookingCancellationService bookingCancellationService) {
        this.bookingCancellationService = bookingCancellationService;
    }

    @PostMapping("/{bookingReference}/cancel")
    public ResponseEntity<RefundResponse>
    cancelBooking(@PathVariable String bookingReference, Authentication authentication,
                  @Valid @RequestBody RefundSimulationRequest request) {

        return ResponseEntity.ok(
                bookingCancellationService.cancelBooking(authentication.getName(), bookingReference, request)
        );
    }
}