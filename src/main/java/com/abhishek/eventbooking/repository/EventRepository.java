package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Event;
import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // ==============================
    // FIND EVENTS BY HALL
    // ==============================

    List<Event> findByHallId(Long hallId);

    // ==============================
    // SEARCH BY EVENT NAME
    // ==============================

    List<Event> findByNameContainingIgnoreCase(String name);

    // ==============================
    // FILTER BY CITY
    // Event -> Hall -> Venue -> City
    // ==============================

    List<Event> findByHallVenueCityIgnoreCase(String city);

    // ==============================
    // FILTER BY CATEGORY
    // ==============================

    List<Event> findByCategory(EventCategory category);

    // ==============================
    // FILTER BY STATUS
    // ==============================

    List<Event> findByStatus(EventStatus status);

    // ==============================
    // CITY + CATEGORY
    // ==============================

    List<Event> findByHallVenueCityIgnoreCaseAndCategory(
            String city,
            EventCategory category
    );

    // ==============================
    // CITY + STATUS
    // ==============================

    List<Event> findByHallVenueCityIgnoreCaseAndStatus(
            String city,
            EventStatus status
    );

    // ==============================
    // CATEGORY + STATUS
    // ==============================

    List<Event> findByCategoryAndStatus(
            EventCategory category,
            EventStatus status
    );

    // ==============================
    // CITY + CATEGORY + STATUS
    // ==============================

    List<Event> findByHallVenueCityIgnoreCaseAndCategoryAndStatus(
            String city,
            EventCategory category,
            EventStatus status
    );

    // ==============================
    // CHECK OVERLAPPING EVENT
    // WHILE CREATING
    // ==============================

    @Query("""
            SELECT COUNT(e) > 0
            FROM Event e
            WHERE e.hall.id = :hallId
            AND e.status <> :cancelledStatus
            AND e.startTime < :endTime
            AND e.endTime > :startTime
            """)
    boolean existsOverlappingEvent(
            @Param("hallId")
            Long hallId,

            @Param("startTime")
            LocalDateTime startTime,

            @Param("endTime")
            LocalDateTime endTime,

            @Param("cancelledStatus")
            EventStatus cancelledStatus
    );

    // ==============================
    // CHECK OVERLAPPING EVENT
    // WHILE UPDATING
    // EXCLUDES CURRENT EVENT
    // ==============================

    @Query("""
            SELECT COUNT(e) > 0
            FROM Event e
            WHERE e.hall.id = :hallId
            AND e.id <> :eventId
            AND e.status <> :cancelledStatus
            AND e.startTime < :endTime
            AND e.endTime > :startTime
            """)
    boolean existsOverlappingEventExcludingId(
            @Param("hallId")
            Long hallId,

            @Param("eventId")
            Long eventId,

            @Param("startTime")
            LocalDateTime startTime,

            @Param("endTime")
            LocalDateTime endTime,

            @Param("cancelledStatus")
            EventStatus cancelledStatus
    );
}