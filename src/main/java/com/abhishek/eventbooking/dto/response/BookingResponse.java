package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;

    private String bookingReference;

    private Long eventId;

    private String eventName;

    private Long userId;

    private BigDecimal totalAmount;

    private BookingStatus status;

    private LocalDateTime createdAt;

    private List<BookingSeatResponse> seats;
}