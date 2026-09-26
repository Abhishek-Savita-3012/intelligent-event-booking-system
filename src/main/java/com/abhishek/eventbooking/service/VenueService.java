package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.VenueRequest;
import com.abhishek.eventbooking.dto.response.VenueResponse;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public VenueResponse createVenue(VenueRequest request) {

        Venue venue = Venue.builder()
                .name(request.getName().trim())
                .city(request.getCity().trim())
                .address(request.getAddress().trim())
                .build();

        Venue savedVenue = venueRepository.save(venue);

        return mapToResponse(savedVenue);
    }

    public List<VenueResponse> getAllVenues() {

        return venueRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VenueResponse getVenueById(Long id) {

        Venue venue = venueRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: " + id
                        )
                );

        return mapToResponse(venue);
    }

    public VenueResponse updateVenue(Long id, VenueRequest request) {

        Venue venue = venueRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: " + id
                        )
                );

        venue.setName(request.getName().trim());
        venue.setCity(request.getCity().trim());
        venue.setAddress(request.getAddress().trim());

        Venue updatedVenue = venueRepository.save(venue);

        return mapToResponse(updatedVenue);
    }

    public void deleteVenue(Long id) {

        Venue venue = venueRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: " + id
                        )
                );

        venueRepository.delete(venue);
    }

    private VenueResponse mapToResponse(Venue venue) {

        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .city(venue.getCity())
                .address(venue.getAddress())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}