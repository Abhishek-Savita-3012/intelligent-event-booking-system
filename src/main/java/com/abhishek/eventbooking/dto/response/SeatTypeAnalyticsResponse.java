package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.SeatType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatTypeAnalyticsResponse {

    private SeatType seatType;
    private long totalSeats;
    private long availableSeats;
    private long lockedSeats;
    private long bookedSeats;
    private BigDecimal occupancyPercentage;
}