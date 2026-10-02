package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.EventRequest;
import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.entity.Event;
import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.EventStatus;
import com.abhishek.eventbooking.entity.Hall;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.HallRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final HallRepository hallRepository;

    public EventService(EventRepository eventRepository, HallRepository hallRepository) {
        this.eventRepository = eventRepository;
        this.hallRepository = hallRepository;
    }

    // ==============================
    // CREATE EVENT
    // ==============================

    public EventResponse createEvent(EventRequest request) {

        validateEventTimes(request);

        Hall hall = hallRepository
                .findById(request.getHallId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hall not found with id: "
                                        + request.getHallId()
                        )
                );

        validateHallAvailability(hall.getId(), request.getStartTime(), request.getEndTime());

        Event event = Event.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .hall(hall)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(EventStatus.UPCOMING)
                .build();

        Event savedEvent = eventRepository.save(event);

        return mapToResponse(savedEvent);
    }

    // ==============================
    // GET EVENT BY ID
    // ==============================

    public EventResponse getEventById(Long id) {

        Event event = eventRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        )
                );

        return mapToResponse(event);
    }

    // ==============================
    // UPDATE EVENT
    // ==============================

    public EventResponse updateEvent(Long id, EventRequest request) {

        validateEventTimes(request);

        Event event = eventRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        )
                );

        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hall not found with id: "
                                        + request.getHallId()
                        )
                );

        validateHallAvailabilityForUpdate(
                hall.getId(),
                event.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        event.setName(request.getName().trim());
        event.setDescription(request.getDescription().trim());
        event.setCategory(request.getCategory());
        event.setHall(hall);
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());

        Event updatedEvent = eventRepository.save(event);

        return mapToResponse(updatedEvent);
    }

    // ==============================
    // CANCEL EVENT
    // ==============================

    public void cancelEvent(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        )
                );

        event.setStatus(EventStatus.CANCELLED);

        eventRepository.save(event);
    }

    // ==============================
    // SEARCH / FILTER EVENTS
    // ==============================

    public List<EventResponse> searchEvents(
            String name,
            String city,
            EventCategory category,
            EventStatus status
    ) {

        List<Event> events;

        boolean hasName =
                name != null
                        && !name.isBlank();

        boolean hasCity =
                city != null
                        && !city.isBlank();

        boolean hasCategory =
                category != null;

        boolean hasStatus =
                status != null;

        if (
                hasName
                        && !hasCity
                        && !hasCategory
                        && !hasStatus
        ) {

            events =
                    eventRepository
                            .findByNameContainingIgnoreCase(
                                    name.trim()
                            );

        } else if (
                hasCity
                        && hasCategory
                        && hasStatus
        ) {

            events =
                    eventRepository
                            .findByHallVenueCityIgnoreCaseAndCategoryAndStatus(
                                    city.trim(),
                                    category,
                                    status
                            );

        } else if (
                hasCity
                        && hasCategory
        ) {

            events =
                    eventRepository
                            .findByHallVenueCityIgnoreCaseAndCategory(
                                    city.trim(),
                                    category
                            );

        } else if (
                hasCity
                        && hasStatus
        ) {

            events =
                    eventRepository
                            .findByHallVenueCityIgnoreCaseAndStatus(
                                    city.trim(),
                                    status
                            );

        } else if (
                hasCategory
                        && hasStatus
        ) {

            events =
                    eventRepository
                            .findByCategoryAndStatus(
                                    category,
                                    status
                            );

        } else if (hasCity) {

            events =
                    eventRepository
                            .findByHallVenueCityIgnoreCase(
                                    city.trim()
                            );

        } else if (hasCategory) {

            events =
                    eventRepository
                            .findByCategory(
                                    category
                            );

        } else if (hasStatus) {

            events =
                    eventRepository
                            .findByStatus(
                                    status
                            );

        } else {

            events =
                    eventRepository.findAll();
        }

        return events
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==============================
    // VALIDATE EVENT TIMES
    // ==============================

    private void validateEventTimes(
            EventRequest request
    ) {

        if (!request
                .getEndTime()
                .isAfter(
                        request.getStartTime()
                )) {

            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
    }

    // ==============================
    // VALIDATE HALL AVAILABILITY
    // CREATE
    // ==============================

    private void validateHallAvailability(
            Long hallId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        boolean conflict =
                eventRepository
                        .existsOverlappingEvent(
                                hallId,
                                startTime,
                                endTime,
                                EventStatus.CANCELLED
                        );

        if (conflict) {

            throw new ConflictException(
                    "Hall already has an event scheduled during this time"
            );
        }
    }

    // ==============================
    // VALIDATE HALL AVAILABILITY
    // UPDATE
    // ==============================

    private void validateHallAvailabilityForUpdate(
            Long hallId,
            Long eventId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        boolean conflict =
                eventRepository.existsOverlappingEventExcludingId(
                                hallId,
                                eventId,
                                startTime,
                                endTime,
                                EventStatus.CANCELLED
                        );

        if (conflict) {

            throw new ConflictException(
                    "Hall already has an event scheduled during this time"
            );
        }
    }

    // ==============================
    // ENTITY -> RESPONSE DTO
    // ==============================

    private EventResponse mapToResponse(Event event) {

        Hall hall = event.getHall();

        Venue venue = hall.getVenue();

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .category(event.getCategory())
                .hallId(hall.getId())
                .hallName(hall.getName())
                .venueId(venue.getId())
                .venueName(venue.getName())
                .city(venue.getCity())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}