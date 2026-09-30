package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Refund;
import com.abhishek.eventbooking.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    Optional<Refund> findTopByBookingIdOrderByCreatedAtDesc(
            Long bookingId
    );

    boolean existsByBookingIdAndStatus(
            Long bookingId,
            RefundStatus status
    );
}