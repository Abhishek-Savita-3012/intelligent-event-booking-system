package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventIdOrderBySeatRowNameAscSeatSeatNumberAsc(
            Long eventId
    );

    boolean existsByEventIdAndSeatId(
            Long eventId,
            Long seatId
    );
}