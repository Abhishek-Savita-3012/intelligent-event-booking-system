package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.request.PaymentSimulationRequest;
import com.abhishek.eventbooking.dto.response.PaymentResponse;
import com.abhishek.eventbooking.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings/{bookingReference}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/simulate")
    public ResponseEntity<PaymentResponse> simulatePayment(@PathVariable String bookingReference,
            Authentication authentication, @Valid @RequestBody PaymentSimulationRequest request) {

        PaymentResponse response = paymentService.simulatePayment(authentication.getName(), bookingReference, request);

        return ResponseEntity.ok(
                response
        );
    }
}