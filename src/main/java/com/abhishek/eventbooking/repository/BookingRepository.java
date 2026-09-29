package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    List<Booking> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.bookingReference = :bookingReference
        """)
    Optional<Booking> findByBookingReferenceForUpdate(
            @Param("bookingReference")
            String bookingReference
    );
}