package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.BookingStatus;
import com.abhishek.eventbooking.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;

    private String paymentReference;

    private String bookingReference;

    private BigDecimal amount;

    private PaymentStatus paymentStatus;

    private BookingStatus bookingStatus;

    private LocalDateTime processedAt;

    private String message;
}