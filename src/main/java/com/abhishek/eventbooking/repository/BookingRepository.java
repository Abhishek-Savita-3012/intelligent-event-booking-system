package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Booking;
import com.abhishek.eventbooking.entity.BookingStatus;
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

    // ==============================
    // PESSIMISTIC LOCK
    // ==============================

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

    long countByStatus(BookingStatus status);

    // ==============================
    // ADMIN - ALL BOOKINGS
    // ==============================

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.event e
            JOIN FETCH e.hall h
            JOIN FETCH h.venue v
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findAllForAdmin();

    // ==============================
    // ADMIN - FILTER BY STATUS
    // ==============================

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.event e
            JOIN FETCH e.hall h
            JOIN FETCH h.venue v
            WHERE b.status = :status
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findAllForAdminByStatus(
            @Param("status")
            BookingStatus status
    );
}