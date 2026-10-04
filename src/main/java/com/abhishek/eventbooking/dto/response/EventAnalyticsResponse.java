package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.EventStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    // VENUE / HALL
    // ==============================

    private Long venueId;
    private String venueName;
    private Long hallId;
    private String hallName;


    // ==============================
    // SEAT INVENTORY
    // ==============================

    private long totalSeats;
    private long availableSeats;
    private long lockedSeats;
    private long bookedSeats;
    private BigDecimal occupancyPercentage;


    // ==============================
    // BOOKINGS
    // ==============================

    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;


    // ==============================
    // FINANCIALS
    // ==============================

    private BigDecimal grossTicketSales;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
}