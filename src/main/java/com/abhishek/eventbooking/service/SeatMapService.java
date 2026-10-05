package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.response.EventSeatMapResponse;
import com.abhishek.eventbooking.dto.response.SeatMapRowResponse;
import com.abhishek.eventbooking.dto.response.SeatMapSeatResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.exception.ResourceNotFoundException;

import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SeatMapService {

    private final EventRepository eventRepository;

    private final EventSeatRepository eventSeatRepository;

    public SeatMapService(EventRepository eventRepository, EventSeatRepository eventSeatRepository) {

        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @Transactional(readOnly = true)
    public EventSeatMapResponse getSeatMap(Long eventId) {

        // ==============================
        // 1. LOAD EVENT
        // ==============================

        Event event = eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId
                                )
                        );


        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        // ==============================
        // 2. LOAD EVENT INVENTORY
        // ==============================

        List<EventSeat> eventSeats = eventSeatRepository.findSeatMapByEventId(eventId);

        /*
         * Use one consistent time for this
         * entire response.
         */
        LocalDateTime now = LocalDateTime.now();

        // ==============================
        // 3. GROUP SEATS BY ROW
        // ==============================

        Map<String, List<SeatMapSeatResponse>> seatsByRow = new LinkedHashMap<>();

        long availableSeats = 0;
        long lockedSeats = 0;
        long bookedSeats = 0;

        for (EventSeat eventSeat : eventSeats) {

            Seat seat = eventSeat.getSeat();

            EventSeatStatus effectiveStatus = resolveEffectiveStatus(eventSeat, now);

            if (effectiveStatus == EventSeatStatus.AVAILABLE) {
                availableSeats++;

            } else if (effectiveStatus == EventSeatStatus.LOCKED) {
                lockedSeats++;

            } else if (effectiveStatus == EventSeatStatus.BOOKED) {
                bookedSeats++;
            }

            SeatMapSeatResponse seatResponse = SeatMapSeatResponse.builder()

                            .eventSeatId(eventSeat.getId())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType())
                            .price(eventSeat.getPrice())
                            .status(effectiveStatus)
                            .selectable(effectiveStatus == EventSeatStatus.AVAILABLE)
                            .build();


            seatsByRow.computeIfAbsent(
                    seat.getRowName(), key -> new ArrayList<>())
                    .add(
                            seatResponse
                    );
        }

        // ==============================
        // 4. CONVERT MAP → ROW DTOs
        // ==============================

        List<SeatMapRowResponse> rows = seatsByRow
                        .entrySet()
                        .stream()
                        .map(entry ->
                                SeatMapRowResponse.builder()
                                        .rowName(entry.getKey())
                                        .seats(entry.getValue())
                                        .build()
                        )
                        .toList();


        // ==============================
        // 5. RESPONSE
        // ==============================
        EventSeatMapResponse response = EventSeatMapResponse.builder()
                        .eventId(event.getId())
                        .eventName(event.getName())
                        .eventStatus(event.getStatus())
                        .startTime(event.getStartTime())

                        // Venue
                        .venueId(venue.getId())
                        .venueName(venue.getName())

                        // Hall
                        .hallId(hall.getId())
                        .hallName(hall.getName())

                        // Inventory
                        .totalSeats(eventSeats.size())
                        .availableSeats(availableSeats)
                        .lockedSeats(lockedSeats)
                        .bookedSeats(bookedSeats)

                        // Rows
                        .rows(rows)
                        .build();


        log.debug(
                "Seat map generated eventId={} totalSeats={} available={} locked={} booked={}",
                eventId,
                eventSeats.size(),
                availableSeats,
                lockedSeats,
                bookedSeats
        );


        return response;
    }


    // ==========================================
    // EFFECTIVE SEAT STATUS
    // ==========================================

    private EventSeatStatus resolveEffectiveStatus(EventSeat eventSeat, LocalDateTime now) {

        /*
         * A DB row may still temporarily say LOCKED
         * after its timeout but before the scheduler
         * has cleaned it.
         *
         * BookingService already has lazy cleanup,
         * so an expired lock is effectively available.
         */

        if (eventSeat.getStatus() == EventSeatStatus.LOCKED
                        && eventSeat.getLockedUntil() != null
                        && !eventSeat.getLockedUntil().isAfter(now)) {

            return EventSeatStatus.AVAILABLE;
        }

        return eventSeat.getStatus();
    }
}