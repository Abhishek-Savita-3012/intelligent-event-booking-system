package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.EventRequest;
import com.abhishek.eventbooking.dto.request.EventSearchCriteria;
import com.abhishek.eventbooking.dto.response.EventResponse;
import com.abhishek.eventbooking.entity.Event;
import com.abhishek.eventbooking.entity.EventStatus;
import com.abhishek.eventbooking.entity.Hall;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.HallRepository;
import com.abhishek.eventbooking.specification.EventSpecification;
import com.abhishek.eventbooking.config.CacheNames;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
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

        log.info(
                "Event created eventId={} hallId={} startTime={} endTime={}",
                savedEvent.getId(),
                hall.getId(),
                savedEvent.getStartTime(),
                savedEvent.getEndTime()
        );

        return mapToResponse(savedEvent);
    }

    // ==============================
    // GET EVENT BY ID
    // ==============================

    @Cacheable(
            cacheNames = CacheNames.EVENT_DETAILS,
            key = "#eventId"
    )
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + eventId
                        )
                );

        return mapToResponse(event);
    }

    // ==============================
    // UPDATE EVENT
    // ==============================

    @CacheEvict(
            cacheNames = {
                    CacheNames.EVENT_DETAILS,
                    CacheNames.EVENT_ANALYTICS
            },
            key = "#eventId"
    )
    public EventResponse updateEvent(Long eventId, EventRequest request) {

        validateEventTimes(request);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + eventId
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

    @CacheEvict(
            cacheNames = {
                    CacheNames.EVENT_DETAILS,
                    CacheNames.EVENT_ANALYTICS
            },
            key = "#eventId"
    )
    public void cancelEvent(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + eventId
                        )
                );

        event.setStatus(EventStatus.CANCELLED);

        eventRepository.save(event);
    }

    // ==============================
    // SEARCH / FILTER EVENTS
    // ==============================

    @Transactional(readOnly = true)
    public List<EventResponse> searchEvents(EventSearchCriteria criteria) {

        validateSearchCriteria(
                criteria
        );

        Specification<Event> specification = EventSpecification.withFilters(criteria);

        List<Event> events = eventRepository.findAll(specification);

        return events
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateSearchCriteria(EventSearchCriteria criteria) {

        if (criteria.getStartFrom() != null && criteria.getStartTo() != null
                        && criteria.getStartFrom().isAfter(criteria.getStartTo())
        ) {

            throw new BadRequestException(
                    "startFrom must not be after startTo"
            );
        }
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

            log.warn(
                    "Event scheduling conflict hallId={} requestedStart={} requestedEnd={}",
                    hallId,
                    startTime,
                    endTime
            );

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