package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.config.CacheNames;
import com.abhishek.eventbooking.dto.request.EventSeatGenerationRequest;
import com.abhishek.eventbooking.dto.response.EventSeatResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.SeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class EventSeatService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventSeatRepository eventSeatRepository;

    public EventSeatService(EventRepository eventRepository, SeatRepository seatRepository, EventSeatRepository eventSeatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @CacheEvict(
            cacheNames = CacheNames.EVENT_ANALYTICS,
            key = "#eventId"
    )
    @Transactional
    public List<EventSeatResponse> generateEventSeats(Long eventId, EventSeatGenerationRequest request) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + eventId
                        )
                );

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException(
                    "Cannot generate seats for a cancelled event"
            );
        }

        List<Seat> seats = seatRepository
                        .findByHallIdOrderByRowNameAscSeatNumberAsc(
                                event.getHall().getId()
                        );

        if (seats.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No seats found for event hall"
            );
        }

        if (!eventSeatRepository.findByEventIdOrderBySeatRowNameAscSeatSeatNumberAsc(eventId).isEmpty()) {
            throw new ConflictException(
                    "Event seats have already been generated"
            );
        }

        List<EventSeat> eventSeats =
                seats.stream()
                        .map(seat -> {
                            BigDecimal price = resolvePrice(seat.getSeatType(), request);

                            return EventSeat.builder()
                                    .event(event)
                                    .seat(seat)
                                    .price(price)
                                    .status(EventSeatStatus.AVAILABLE)
                                    .build();
                        })
                        .toList();

        List<EventSeat> savedEventSeats = eventSeatRepository.saveAll(eventSeats);

        log.info(
                "EventSeat inventory generated eventId={} seatCount={}",
                event.getId(),
                eventSeats.size()
        );

        return savedEventSeats
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<EventSeatResponse> getEventSeats(Long eventId) {

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException(
                    "Event not found with id: " + eventId
            );
        }

        return eventSeatRepository
                .findByEventIdOrderBySeatRowNameAscSeatSeatNumberAsc(eventId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private BigDecimal resolvePrice(SeatType seatType, EventSeatGenerationRequest request) {

        return switch (seatType) {

            case REGULAR ->
                    request.getRegularPrice();

            case PREMIUM ->
                    request.getPremiumPrice();

            case VIP ->
                    request.getVipPrice();
        };
    }

    private EventSeatResponse mapToResponse(EventSeat eventSeat) {

        Seat seat = eventSeat.getSeat();

        return EventSeatResponse.builder()
                .eventSeatId(eventSeat.getId())
                .eventId(eventSeat.getEvent().getId())
                .seatId(seat.getId())
                .hallId(seat.getHall().getId())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .price(eventSeat.getPrice())
                .status(eventSeat.getStatus())
                .build();
    }
}