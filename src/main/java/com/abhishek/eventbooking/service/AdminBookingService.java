package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.response.AdminBookingResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    public AdminBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getBookings(BookingStatus status) {

        List<Booking> bookings;

        if (status == null) {
            bookings = bookingRepository.findAllForAdmin();

        } else {
            bookings = bookingRepository.findAllForAdminByStatus(status);
        }

        return bookings.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AdminBookingResponse mapToResponse(Booking booking) {

        User user = booking.getUser();
        Event event = booking.getEvent();
        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        return AdminBookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())

                // User
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())

                // Event
                .eventId(event.getId())
                .eventName(event.getName())

                // Venue
                .venueName(venue.getName())
                .city(venue.getCity())
                .hallName(hall.getName())

                // Booking
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }
}