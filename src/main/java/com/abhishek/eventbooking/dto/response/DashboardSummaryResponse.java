package com.abhishek.eventbooking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    // Booking counts
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long failedBookings;
    private long expiredBookings;

    // Payment / Refund
    private long successfulPayments;
    private long successfulRefunds;

    // Revenue
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;

    // Ticket inventory
    private long currentlyBookedSeats;
}