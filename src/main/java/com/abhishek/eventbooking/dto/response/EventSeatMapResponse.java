package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.EventStatus;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeatMapResponse {

    // ==============================
    // EVENT
    // ==============================
    private Long eventId;
    private String eventName;
    private EventStatus eventStatus;
    private LocalDateTime startTime;


    // ==============================
    // VENUE
    // ==============================
    private Long venueId;
    private String venueName;


    // ==============================
    // HALL
    // ==============================
    private Long hallId;
    private String hallName;


    // ==============================
    // INVENTORY SUMMARY
    // ==============================
    private long totalSeats;
    private long availableSeats;
    private long lockedSeats;
    private long bookedSeats;


    // ==============================
    // SEAT MAP
    // ==============================
    private List<SeatMapRowResponse> rows;
}