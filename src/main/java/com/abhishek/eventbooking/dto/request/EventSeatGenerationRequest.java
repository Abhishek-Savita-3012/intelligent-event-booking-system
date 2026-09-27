package com.abhishek.eventbooking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EventSeatGenerationRequest {

    @NotNull(message = "Regular price is required")
    @Positive(message = "Regular price must be greater than 0")
    private BigDecimal regularPrice;

    @NotNull(message = "Premium price is required")
    @Positive(message = "Premium price must be greater than 0")
    private BigDecimal premiumPrice;

    @NotNull(message = "VIP price is required")
    @Positive(message = "VIP price must be greater than 0")
    private BigDecimal vipPrice;
}