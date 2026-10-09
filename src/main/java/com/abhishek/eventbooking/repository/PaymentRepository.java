package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.dto.projection.PaymentStatusSummaryProjection;
import com.abhishek.eventbooking.entity.Payment;
import com.abhishek.eventbooking.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    Optional<Payment> findTopByBookingIdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentStatus status
    );

    long countByStatus(
            PaymentStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = :status
        """)
    BigDecimal sumAmountByStatus(
            @Param("status")
            PaymentStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.booking.event.id = :eventId
        AND p.status = :status
        """)
    BigDecimal sumAmountByEventIdAndStatus(
            @Param("eventId")
            Long eventId,

            @Param("status")
            PaymentStatus status
    );

    @Query("""
        SELECT
            p.status AS status,
            COUNT(p) AS count,
            COALESCE(
                SUM(p.amount),
                0
            ) AS totalAmount
        FROM Payment p
        WHERE p.booking.event.id = :eventId
        GROUP BY p.status
        """)
    List<PaymentStatusSummaryProjection> summarizePaymentsForEvent(
            @Param("eventId")
            Long eventId
    );
}