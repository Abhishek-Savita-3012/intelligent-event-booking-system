package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Event;
import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByVenueId(Long venueId);

    List<Event> findByVenueCityIgnoreCase(
            String city
    );

    List<Event> findByCategory(
            EventCategory category
    );

    List<Event> findByStatus(
            EventStatus status
    );

    List<Event> findByVenueCityIgnoreCaseAndCategory(
            String city,
            EventCategory category
    );

    List<Event> findByVenueCityIgnoreCaseAndStatus(
            String city,
            EventStatus status
    );

    List<Event> findByCategoryAndStatus(
            EventCategory category,
            EventStatus status
    );

    List<Event> findByVenueCityIgnoreCaseAndCategoryAndStatus(
            String city,
            EventCategory category,
            EventStatus status
    );

    List<Event> findByNameContainingIgnoreCase(
            String name
    );
}