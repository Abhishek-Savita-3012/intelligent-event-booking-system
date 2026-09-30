package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(
            String paymentReference
    );

    List<Payment> findByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );
}