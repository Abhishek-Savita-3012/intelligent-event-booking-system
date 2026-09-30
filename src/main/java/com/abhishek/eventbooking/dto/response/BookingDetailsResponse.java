package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.BookingStatus;
import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetailsResponse {

    private Long id;

    private String bookingReference;

    private BookingStatus bookingStatus;

    // Event
    private Long eventId;

    private String eventName;

    private EventCategory eventCategory;

    private LocalDateTime eventStartTime;

    private LocalDateTime eventEndTime;

    // Venue
    private Long venueId;

    private String venueName;

    private String city;

    // Hall
    private Long hallId;

    private String hallName;

    // Seats
    private List<BookingDetailsSeatResponse> seats;

    // Booking amount
    private BigDecimal totalAmount;

    // Payment
    private String paymentReference;

    private PaymentStatus paymentStatus;

    // Booking timestamps
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}