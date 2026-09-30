package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHistoryResponse {

    private Long id;

    private String bookingReference;

    private Long eventId;

    private String eventName;

    private String venueName;

    private String hallName;

    private BigDecimal totalAmount;

    private BookingStatus bookingStatus;

    private LocalDateTime createdAt;
}