package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallIdOrderByRowNameAscSeatNumberAsc(
            Long hallId
    );

    boolean existsByHallIdAndRowNameAndSeatNumber(
            Long hallId,
            String rowName,
            Integer seatNumber
    );
}