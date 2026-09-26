package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.SeatRequest;
import com.abhishek.eventbooking.dto.response.SeatResponse;
import com.abhishek.eventbooking.entity.Seat;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.repository.SeatRepository;
import com.abhishek.eventbooking.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final VenueRepository venueRepository;

    public SeatService(SeatRepository seatRepository, VenueRepository venueRepository) {
        this.seatRepository = seatRepository;
        this.venueRepository = venueRepository;
    }

    public SeatResponse createSeat(Long venueId, SeatRequest request) {

        Venue venue = venueRepository
                .findById(venueId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: " + venueId
                        )
                );

        String rowName = request.getRowName().trim().toUpperCase();

        if (seatRepository.existsByVenueIdAndRowNameAndSeatNumber(venueId, rowName, request.getSeatNumber())) {
            throw new IllegalArgumentException(
                    "Seat already exists: "
                            + rowName
                            + request.getSeatNumber()
            );
        }

        Seat seat = Seat.builder()
                .venue(venue)
                .rowName(rowName)
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .build();

        Seat savedSeat = seatRepository.save(seat);

        return mapToResponse(savedSeat);
    }

    public List<SeatResponse> getSeatsByVenue(Long venueId) {

        if (!venueRepository.existsById(venueId)) {
            throw new IllegalArgumentException(
                    "Venue not found with id: " + venueId
            );
        }

        return seatRepository
                .findByVenueIdOrderByRowNameAscSeatNumberAsc(venueId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SeatResponse mapToResponse(Seat seat) {

        return SeatResponse.builder()
                .id(seat.getId())
                .venueId(seat.getVenue().getId())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .build();
    }
}