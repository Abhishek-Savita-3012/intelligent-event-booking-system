package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.SeatType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetailsSeatResponse {

    private Long eventSeatId;

    private String rowName;

    private Integer seatNumber;

    private SeatType seatType;

    private BigDecimal price;
}