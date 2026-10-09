package com.abhishek.eventbooking.dto.projection;

import com.abhishek.eventbooking.entity.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentStatusSummaryProjection {

    PaymentStatus getStatus();

    Long getCount();

    BigDecimal getTotalAmount();
}