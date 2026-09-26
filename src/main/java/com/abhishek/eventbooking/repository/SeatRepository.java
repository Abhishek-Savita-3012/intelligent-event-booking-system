package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByVenueIdOrderByRowNameAscSeatNumberAsc(
            Long venueId
    );

    boolean existsByVenueIdAndRowNameAndSeatNumber(
            Long venueId,
            String rowName,
            Integer seatNumber
    );
}