package com.abhishek.eventbooking.dto.request;

import com.abhishek.eventbooking.entity.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatRequest {

    @NotBlank(message = "Row name is required")
    @Size(max = 10, message = "Row name is too long")
    private String rowName;

    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be at least 1")
    private Integer seatNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;
}