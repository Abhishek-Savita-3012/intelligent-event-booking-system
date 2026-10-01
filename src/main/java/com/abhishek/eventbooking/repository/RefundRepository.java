package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Refund;
import com.abhishek.eventbooking.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    long countByStatus(
            RefundStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(r.amount), 0)
        FROM Refund r
        WHERE r.status = :status
        """)
    BigDecimal sumAmountByStatus(
            @Param("status")
            RefundStatus status
    );
}