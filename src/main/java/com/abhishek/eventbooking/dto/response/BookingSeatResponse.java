package com.abhishek.eventbooking.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeatResponse {

    private Long eventSeatId;

    private String rowName;

    private Integer seatNumber;

    private BigDecimal price;
}