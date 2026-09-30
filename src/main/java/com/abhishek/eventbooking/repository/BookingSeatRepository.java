package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(
            Long bookingId
    );

    @Query("""
            SELECT bs
            FROM BookingSeat bs
            WHERE bs.booking.id = :bookingId
            ORDER BY
                bs.eventSeat.seat.rowName ASC,
                bs.eventSeat.seat.seatNumber ASC
            """)
    List<BookingSeat> findByBookingIdOrdered(
            @Param("bookingId")
            Long bookingId
    );
}