package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    List<Booking> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );
}