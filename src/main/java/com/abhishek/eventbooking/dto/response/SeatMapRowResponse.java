package com.abhishek.eventbooking.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatMapRowResponse {

    private String rowName;

    private List<SeatMapSeatResponse> seats;
}