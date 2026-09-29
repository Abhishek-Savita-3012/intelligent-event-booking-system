package com.abhishek.eventbooking.dto.request;

import com.abhishek.eventbooking.entity.PaymentOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentSimulationRequest {

    @NotNull(message = "Payment outcome is required")
    private PaymentOutcome outcome;
}