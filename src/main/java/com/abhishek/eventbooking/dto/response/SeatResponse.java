package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.SeatType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {

    private Long id;

    private Long venueId;

    private String rowName;

    private Integer seatNumber;

    private SeatType seatType;
}