package com.abhishek.eventbooking.dto.request;

import com.abhishek.eventbooking.entity.RefundOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundSimulationRequest {

    @NotNull(message = "Refund outcome is required")
    private RefundOutcome outcome;
}