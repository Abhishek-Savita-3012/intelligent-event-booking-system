package com.abhishek.eventbooking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotNull(message = "Event id is required")
    private Long eventId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> eventSeatIds;
}