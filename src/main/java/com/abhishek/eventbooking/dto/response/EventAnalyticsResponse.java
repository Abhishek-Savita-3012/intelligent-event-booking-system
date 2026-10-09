package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.EventStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventAnalyticsResponse {

    // ==============================
    // EVENT
    // ==============================

    private Long eventId;
    private String eventName;
    private EventStatus eventStatus;
    private LocalDateTime startTime;

    // ==============================
    // VENUE / HALL (LOCATION)
    // ==============================

    private Long venueId;
    private String venueName;
    private Long hallId;
    private String hallName;

    // ==============================
    // CURRENT SEAT INVENTORY
    // ==============================

    private long totalSeats;
    private long availableSeats;
    private long lockedSeats;
    private long bookedSeats;
    private BigDecimal occupancyPercentage;
    private BigDecimal lockedPercentage;

    // =========================================================
    // SEAT TYPE BREAKDOWN
    // =========================================================

    private List<SeatTypeAnalyticsResponse> seatTypeBreakdown;

    // ==============================
    // BOOKING LIFECYCLE
    // ==============================

    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long failedBookings;
    private long expiredBookings;

    // =========================================================
    // BOOKING RATES
    // =========================================================

    private BigDecimal confirmationRate;
    private BigDecimal cancellationRate;
    private BigDecimal failureRate;
    private BigDecimal expirationRate;

    // =========================================================
    // PAYMENT ANALYTICS
    // =========================================================

    private long paymentAttempts;
    private long successfulPayments;
    private long failedPayments;
    private long pendingPayments;
    private BigDecimal paymentSuccessRate;
    private BigDecimal averageSuccessfulPaymentAmount;

    // =========================================================
    // REFUND ANALYTICS
    // =========================================================

    private long refundAttempts;
    private long successfulRefunds;
    private long failedRefunds;
    private BigDecimal refundSuccessRate;

    // ==============================
    // REVENUE
    // ==============================

    private BigDecimal grossTicketSales;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
    private BigDecimal refundedPercentageOfGross;
}