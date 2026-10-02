package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.HallRequest;
import com.abhishek.eventbooking.dto.response.HallResponse;
import com.abhishek.eventbooking.entity.Hall;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.HallRepository;
import com.abhishek.eventbooking.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HallService {

    private final HallRepository hallRepository;
    private final VenueRepository venueRepository;

    public HallService(HallRepository hallRepository, VenueRepository venueRepository) {
        this.hallRepository = hallRepository;
        this.venueRepository = venueRepository;
    }

    public HallResponse createHall(Long venueId, HallRequest request) {

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Venue not found with id: " + venueId
                        )
                );

        String hallName = request.getName().trim();

        if (hallRepository.existsByVenueIdAndNameIgnoreCase(venueId, hallName)) {
            throw new ConflictException(
                    "Hall already exists in this venue"
            );
        }

        Hall hall = Hall.builder()
                .name(hallName)
                .venue(venue)
                .build();

        return mapToResponse(hallRepository.save(hall));
    }

    public List<HallResponse> getHallsByVenue(Long venueId) {

        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException(
                    "Venue not found with id: " + venueId
            );
        }

        return hallRepository
                .findByVenueIdOrderByNameAsc(venueId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private HallResponse mapToResponse(Hall hall) {

        return HallResponse.builder()
                .id(hall.getId())
                .name(hall.getName())
                .venueId(hall.getVenue().getId())
                .venueName(hall.getVenue().getName())
                .build();
    }
}