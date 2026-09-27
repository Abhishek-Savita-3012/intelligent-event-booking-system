package com.abhishek.eventbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HallRequest {

    @NotBlank(message = "Hall name is required")
    @Size(max = 100, message = "Hall name is too long")
    private String name;
}