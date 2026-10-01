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
public class AdminBookingResponse {

    private Long bookingId;
    private String bookingReference;

    // User
    private Long userId;
    private String userName;
    private String userEmail;

    // Event
    private Long eventId;
    private String eventName;

    // Venue / Hall
    private String venueName;
    private String city;
    private String hallName;

    // Booking
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}