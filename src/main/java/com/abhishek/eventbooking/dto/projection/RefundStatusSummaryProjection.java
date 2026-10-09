package com.abhishek.eventbooking.dto.projection;

import com.abhishek.eventbooking.entity.RefundStatus;

import java.math.BigDecimal;

public interface RefundStatusSummaryProjection {

    RefundStatus getStatus();

    Long getCount();

    BigDecimal getTotalAmount();
}