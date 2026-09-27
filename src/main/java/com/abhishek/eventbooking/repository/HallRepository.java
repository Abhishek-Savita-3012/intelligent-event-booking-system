package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallRepository extends JpaRepository<Hall, Long> {

    List<Hall> findByVenueIdOrderByNameAsc(
            Long venueId
    );

    boolean existsByVenueIdAndNameIgnoreCase(
            Long venueId,
            String name
    );
}