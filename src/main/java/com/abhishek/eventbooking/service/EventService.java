package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.EventRequest;
import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.entity.Event;
import com.abhishek.eventbooking.entity.EventStatus;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
    }

    public EventResponse createEvent(EventRequest request) {

        validateEventTimes(request);

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: "
                                        + request.getVenueId()
                        )
                );

        Event event = Event.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .venue(venue)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(EventStatus.UPCOMING)
                .build();

        Event savedEvent = eventRepository.save(event);

        return mapToResponse(savedEvent);
    }

    public List<EventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public EventResponse getEventById(Long id) {

        Event event = eventRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found with id: " + id
                        )
                );

        return mapToResponse(event);
    }

    public EventResponse updateEvent(Long id, EventRequest request) {

        validateEventTimes(request);

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found with id: " + id
                        )
                );

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venue not found with id: "
                                        + request.getVenueId()
                        )
                );

        event.setName(request.getName().trim());
        event.setDescription(request.getDescription().trim());
        event.setCategory(request.getCategory());
        event.setVenue(venue);
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());

        Event updatedEvent = eventRepository.save(event);

        return mapToResponse(updatedEvent);
    }

    public void cancelEvent(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found with id: " + id
                        )
                );

        event.setStatus(EventStatus.CANCELLED);

        eventRepository.save(event);
    }

    private void validateEventTimes(EventRequest request) {

        if (!request.getEndTime().isAfter(request.getStartTime())) {

            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }

    private EventResponse mapToResponse(Event event) {

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .category(event.getCategory())
                .venueId(event.getVenue().getId())
                .venueName(event.getVenue().getName())
                .city(event.getVenue().getCity())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}