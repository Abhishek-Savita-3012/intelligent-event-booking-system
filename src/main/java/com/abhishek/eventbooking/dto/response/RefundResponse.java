package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.BookingStatus;
import com.abhishek.eventbooking.entity.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {

    private Long refundId;

    private String refundReference;

    private String bookingReference;

    private String paymentReference;

    private BigDecimal amount;

    private RefundStatus refundStatus;

    private BookingStatus bookingStatus;

    private LocalDateTime processedAt;

    private String message;
}