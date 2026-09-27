package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.EventSeatStatus;
import com.abhishek.eventbooking.entity.SeatType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeatResponse {

    private Long eventSeatId;

    private Long eventId;

    private Long seatId;

    private Long hallId;

    private String rowName;

    private Integer seatNumber;

    private SeatType seatType;

    private BigDecimal price;

    private EventSeatStatus status;
}