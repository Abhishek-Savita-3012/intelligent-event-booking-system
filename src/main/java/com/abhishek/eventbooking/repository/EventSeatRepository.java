package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.EventSeat;
import com.abhishek.eventbooking.entity.EventSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventIdOrderBySeatRowNameAscSeatSeatNumberAsc(
            Long eventId
    );

    boolean existsByEventIdAndSeatId(
            Long eventId,
            Long seatId
    );

    List<EventSeat> findAllByIdIn(
            List<Long> ids
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT es
            FROM EventSeat es
            WHERE es.id IN :ids
            ORDER BY es.id
            """)
    List<EventSeat> findAllByIdInForUpdate(
            @Param("ids")
            List<Long> ids
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT es
            FROM EventSeat es
            WHERE es.status = :status
            AND es.lockedUntil IS NOT NULL
            AND es.lockedUntil <= :now
            ORDER BY es.id
            """)
    List<EventSeat> findExpiredLockedSeatsForUpdate(
            @Param("status")
            EventSeatStatus status,

            @Param("now")
            LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT es
        FROM EventSeat es
        WHERE es.lockedByBooking.bookingReference = :bookingReference
        ORDER BY es.id
        """)
    List<EventSeat> findByBookingReferenceForUpdate(
            @Param("bookingReference")
            String bookingReference
    );
}