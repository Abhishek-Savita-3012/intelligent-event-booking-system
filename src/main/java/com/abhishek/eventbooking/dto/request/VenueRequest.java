package com.abhishek.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueRequest {

    @NotBlank(message = "Venue name is required")
    @Size(max = 150, message = "Venue name is too long")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name is too long")
    private String city;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address is too long")
    private String address;
}