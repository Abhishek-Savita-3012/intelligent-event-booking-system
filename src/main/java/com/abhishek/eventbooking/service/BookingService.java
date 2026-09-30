package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.response.BookingHistoryResponse;
import com.abhishek.eventbooking.dto.response.BookingResponse;
import com.abhishek.eventbooking.dto.response.BookingSeatResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.abhishek.eventbooking.dto.response.BookingDetailsResponse;
import com.abhishek.eventbooking.dto.response.BookingDetailsSeatResponse;
import com.abhishek.eventbooking.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            EventRepository eventRepository,
            EventSeatRepository eventSeatRepository,
            UserRepository userRepository, PaymentRepository paymentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @Value("${booking.lock-duration-seconds:300}")
    private long lockDurationSeconds;

    @Transactional
    public BookingResponse createBooking(String email, BookingRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Event not found with id: "
                                        + request.getEventId()
                        )
                );

        validateEvent(event);

        validateDuplicateSeatIds(request.getEventSeatIds());

        List<Long> sortedSeatIds =
                request.getEventSeatIds()
                        .stream()
                        .sorted()
                        .toList();

        /*
         * IMPORTANT:
         * Seats are loaded with PESSIMISTIC_WRITE.
         *
         * No other booking transaction can modify
         * these seats until this transaction finishes.
         */
        List<EventSeat> eventSeats = eventSeatRepository.findAllByIdInForUpdate(sortedSeatIds);

        validateAllSeatsFound(eventSeats, request.getEventSeatIds());

        validateSeatsBelongToEvent(eventSeats, event);

        /*
         * If one of these seats has an old expired
         * lock, release it before validating.
         */
        releaseExpiredLocks(eventSeats);

        validateSeatsAvailable(eventSeats);

        BigDecimal totalAmount = calculateTotal(eventSeats);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(lockDurationSeconds);

        Booking booking = Booking.builder()
                        .user(user)
                        .event(event)
                        .bookingReference(generateBookingReference())
                        .totalAmount(totalAmount)
                        .status(BookingStatus.PENDING)
                        .expiresAt(expiresAt)
                        .build();

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = eventSeats.stream()
                        .map(eventSeat ->
                                BookingSeat.builder()
                                        .booking(savedBooking)
                                        .eventSeat(eventSeat)
                                        .price(eventSeat.getPrice())
                                        .build()
                        )
                        .toList();

        bookingSeatRepository.saveAll(bookingSeats);

        /*
         * Temporary business reservation.
         *
         * This is different from the database
         * PESSIMISTIC_WRITE lock.
         */
        eventSeats.forEach(eventSeat -> {

            eventSeat.setStatus(EventSeatStatus.LOCKED);
            eventSeat.setLockedByBooking(savedBooking);
            eventSeat.setLockedUntil(expiresAt);
        });

        eventSeatRepository.saveAll(eventSeats);

        return mapToResponse(savedBooking, bookingSeats);
    }

    private void releaseExpiredLocks(List<EventSeat> eventSeats) {

        LocalDateTime now = LocalDateTime.now();

        for (EventSeat eventSeat : eventSeats) {

            if (
                    eventSeat.getStatus() == EventSeatStatus.LOCKED
                            && eventSeat.getLockedUntil() != null
                            && !eventSeat.getLockedUntil().isAfter(now)
            ) {

                Booking oldBooking = eventSeat.getLockedByBooking();

                eventSeat.setStatus(EventSeatStatus.AVAILABLE);
                eventSeat.setLockedUntil(null);
                eventSeat.setLockedByBooking(null);

                if (oldBooking != null && oldBooking.getStatus() == BookingStatus.PENDING) {

                    oldBooking.setStatus(BookingStatus.EXPIRED);

                    bookingRepository.save(oldBooking);
                }
            }
        }
    }

    private void validateEvent(Event event) {

        if (event.getStatus() == EventStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Cannot book seats for a cancelled event"
            );
        }

        if (!event.getStartTime().isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Cannot book seats for an event that has already started"
            );
        }
    }

    private void validateDuplicateSeatIds(List<Long> eventSeatIds) {

        Set<Long> uniqueIds = new HashSet<>(eventSeatIds);

        if (uniqueIds.size() != eventSeatIds.size()) {

            throw new IllegalArgumentException(
                    "Duplicate event seat ids are not allowed"
            );
        }
    }

    private void validateAllSeatsFound(List<EventSeat> eventSeats, List<Long> requestedIds) {

        if (eventSeats.size() != requestedIds.size()) {

            throw new IllegalArgumentException(
                    "One or more selected event seats do not exist"
            );
        }
    }

    private void validateSeatsBelongToEvent(List<EventSeat> eventSeats, Event event) {

        boolean invalidSeat =
                eventSeats.stream()
                        .anyMatch(eventSeat ->
                                !eventSeat
                                        .getEvent()
                                        .getId()
                                        .equals(event.getId())
                        );

        if (invalidSeat) {

            throw new IllegalArgumentException(
                    "One or more selected seats do not belong to this event"
            );
        }
    }

    private void validateSeatsAvailable(List<EventSeat> eventSeats) {

        boolean unavailable =
                eventSeats.stream()
                        .anyMatch(eventSeat ->
                                eventSeat.getStatus() != EventSeatStatus.AVAILABLE
                        );

        if (unavailable) {

            throw new IllegalArgumentException(
                    "One or more selected seats are not available"
            );
        }
    }

    private BigDecimal calculateTotal(List<EventSeat> eventSeats) {

        return eventSeats.stream()
                .map(EventSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateBookingReference() {

        return "BK-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getMyBookings(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return bookingRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    private BookingHistoryResponse mapToHistoryResponse(Booking booking) {

        Event event = booking.getEvent();
        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        return BookingHistoryResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .eventId(event.getId())
                .eventName(event.getName())
                .venueName(venue.getName())
                .hallName(hall.getName())
                .totalAmount(booking.getTotalAmount())
                .bookingStatus(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingDetailsResponse getBookingDetails(String email, String bookingReference) {

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking not found: "
                                                + bookingReference
                                )
                        );

        validateBookingOwner(booking, email);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingIdOrdered(booking.getId());

        Payment payment = paymentRepository
                        .findTopByBookingIdOrderByCreatedAtDesc(booking.getId())
                        .orElse(null);

        return mapToDetailsResponse(booking, bookingSeats, payment);
    }

    private void validateBookingOwner(Booking booking, String email) {

        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "You are not allowed to access this booking"
            );
        }
    }

    private BookingDetailsResponse mapToDetailsResponse(Booking booking, List<BookingSeat> bookingSeats, Payment payment) {

        Event event = booking.getEvent();
        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        List<BookingDetailsSeatResponse> seats =
                bookingSeats.stream()
                        .map(bookingSeat -> {

                            EventSeat eventSeat = bookingSeat.getEventSeat();
                            Seat seat = eventSeat.getSeat();

                            return BookingDetailsSeatResponse
                                    .builder()
                                    .eventSeatId(eventSeat.getId())
                                    .rowName(seat.getRowName())
                                    .seatNumber(seat.getSeatNumber())
                                    .seatType(seat.getSeatType())
                                    .price(bookingSeat.getPrice())
                                    .build();
                        })
                        .toList();

        return BookingDetailsResponse.builder()

                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingStatus(booking.getStatus())

                // Event
                .eventId(event.getId())
                .eventName(event.getName())
                .eventCategory(event.getCategory())
                .eventStartTime(event.getStartTime())
                .eventEndTime(event.getEndTime())

                // Venue
                .venueId(venue.getId())
                .venueName(venue.getName())
                .city(venue.getCity())

                // Hall
                .hallId(hall.getId())
                .hallName(hall.getName())

                // Seats
                .seats(seats)

                // Money
                .totalAmount(booking.getTotalAmount())

                // Payment
                .paymentReference(payment != null ? payment.getPaymentReference() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)

                // Times
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }

    private BookingResponse mapToResponse(Booking booking, List<BookingSeat> bookingSeats) {

        List<BookingSeatResponse> seatResponses =
                bookingSeats.stream()
                        .map(bookingSeat -> {

                            EventSeat eventSeat = bookingSeat.getEventSeat();
                            Seat seat = eventSeat.getSeat();

                            return BookingSeatResponse.builder()
                                    .eventSeatId(eventSeat.getId())
                                    .rowName(seat.getRowName())
                                    .seatNumber(seat.getSeatNumber())
                                    .price(bookingSeat.getPrice())
                                    .build();
                        })
                        .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .eventId(booking.getEvent().getId())
                .eventName(booking.getEvent().getName())
                .userId(booking.getUser().getId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .seats(seatResponses)
                .build();
    }
}