package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.config.CacheNames;
import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.request.UserBookingSearchCriteria;
import com.abhishek.eventbooking.dto.response.*;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ForbiddenOperationException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.abhishek.eventbooking.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
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

    @CacheEvict(
            cacheNames = CacheNames.EVENT_ANALYTICS,
            key = "#request.eventId"
    )
    @Transactional
    public BookingResponse createBooking(String email, String rawIdempotencyKey, BookingRequest request) {

        log.info(
                "Booking creation started eventId={} requestedSeatCount={}",
                request.getEventId(),
                request.getEventSeatIds().size()
        );


        // =========================================================
        // 1. VALIDATE + NORMALIZE IDEMPOTENCY KEY
        // =========================================================

        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);

        // =========================================================
        // 2. VALIDATE DUPLICATE SEAT IDs
        // =========================================================

        /*
         * Do this before creating the fingerprint.
         *
         * [101, 101] is still an invalid request and should
         * not be treated as a valid idempotent operation.
         */
        validateDuplicateSeatIds(request.getEventSeatIds());

        // =========================================================
        // 3. CREATE REQUEST FINGERPRINT
        // =========================================================

        /*
         * Fingerprint is based on:
         *
         * eventId
         * +
         * sorted eventSeatIds
         *
         * Example:
         *
         * eventId = 1
         * seats = [103, 101, 102]
         *
         * canonical:
         * 1|101,102,103
         *
         * Then SHA-256 is generated.
         */
        String requestFingerprint = buildRequestFingerprint(request.getEventId(), request.getEventSeatIds());

        // =========================================================
        // 4. LOCK USER ROW
        // =========================================================

        /*
         * Important for concurrent retries.
         *
         * Suppose the same user sends:
         *
         * Request A
         * Request B
         *
         * with the same Idempotency-Key at almost exactly
         * the same time.
         *
         * Request A gets the User row lock first.
         *
         * Request B waits.
         *
         * After Request A creates and commits the Booking,
         * Request B continues and sees that the booking
         * for this idempotency key already exists.
         */
        User user = userRepository.findByEmailForUpdate(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        // =========================================================
        // 5. CHECK WHETHER THIS IDEMPOTENCY KEY ALREADY EXISTS
        // =========================================================

        Optional<Booking> existingBooking = bookingRepository.findByUser_IdAndIdempotencyKey(user.getId(), idempotencyKey);

        if (existingBooking.isPresent()) {

            Booking existing = existingBooking.get();

            // -----------------------------------------------------
            // Same key but DIFFERENT request
            // -----------------------------------------------------

            if (!requestFingerprint.equals(existing.getRequestFingerprint())) {

                log.warn(
                        "Idempotency key reused with different booking request userId={} idempotencyKey={}",
                        user.getId(),
                        idempotencyKey
                );

                throw new ConflictException(
                        "Idempotency-Key has already been used for a different booking request"
                );
            }

            // -----------------------------------------------------
            // Same key + same request = legitimate retry
            // -----------------------------------------------------

            log.info("Booking idempotency replay bookingReference={} idempotencyKey={}",
                    existing.getBookingReference(),
                    idempotencyKey
            );

            /*
             * We need the BookingSeat records because your
             * existing mapToResponse(...) accepts:
             *
             * Booking
             * +
             * List<BookingSeat>
             *
             */
            List<BookingSeat> existingBookingSeats = bookingSeatRepository.findByBookingId(existing.getId());

            return mapToResponse(existing, existingBookingSeats);
        }


        // =========================================================
        // 6. LOAD EVENT
        // =========================================================

        Event event = eventRepository
                        .findById(request.getEventId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + request.getEventId()
                                )
                        );

        // =========================================================
        // 7. VALIDATE EVENT
        // =========================================================

        validateEvent(event);

        // =========================================================
        // 8. SORT SEAT IDS
        // =========================================================

        /*
         * Consistent lock ordering reduces the possibility
         * of database deadlocks when multiple seats are
         * selected.
         */
        List<Long> sortedSeatIds = request.getEventSeatIds()
                        .stream()
                        .sorted()
                        .toList();

        // =========================================================
        // 9. PESSIMISTICALLY LOCK EVENT SEATS
        // =========================================================

        /*
         * IMPORTANT:
         *
         * This is the technical DATABASE lock.
         *
         * Another booking transaction trying to acquire
         * PESSIMISTIC_WRITE on these same EventSeat rows
         * has to wait until this transaction completes.
         */
        List<EventSeat> eventSeats = eventSeatRepository.findAllByIdInForUpdate(sortedSeatIds);

        // =========================================================
        // 10. CHECK THAT ALL REQUESTED SEATS EXIST
        // =========================================================

        validateAllSeatsFound(eventSeats, request.getEventSeatIds());

        // =========================================================
        // 11. VERIFY SEATS BELONG TO REQUESTED EVENT
        // =========================================================

        validateSeatsBelongToEvent(eventSeats, event);

        // =========================================================
        // 12. LAZY CLEANUP OF EXPIRED LOCKS
        // =========================================================

        /*
         * Example:
         *
         * EventSeat says LOCKED
         *
         * but:
         *
         * lockedUntil < current time
         *
         * The scheduler may not have cleaned it yet.
         *
         * Because these rows are already pessimistically
         * locked by this transaction, it is safe to release
         * an old expired reservation here.
         */
        releaseExpiredLocks(eventSeats);

        // =========================================================
        // 13. VERIFY SEATS ARE AVAILABLE
        // =========================================================

        validateSeatsAvailable(eventSeats);

        // =========================================================
        // 14. CALCULATE TOTAL ON BACKEND
        // =========================================================

        /*
         * Client never decides booking price.
         *
         * EventSeat prices are the source used for
         * calculating the booking total.
         */
        BigDecimal totalAmount = calculateTotal(eventSeats);

        // =========================================================
        // 15. CALCULATE TEMPORARY BOOKING EXPIRATION
        // =========================================================

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(lockDurationSeconds);

        // =========================================================
        // 16. CREATE BOOKING
        // =========================================================

        Booking booking = Booking.builder()
                        .user(user)
                        .event(event)
                        .bookingReference(generateBookingReference())
                        .idempotencyKey(idempotencyKey)
                        .requestFingerprint(requestFingerprint)
                        .totalAmount(totalAmount)
                        .status(BookingStatus.PENDING)
                        .expiresAt(expiresAt)
                        .build();

        Booking savedBooking = bookingRepository.save(booking);

        // =========================================================
        // 17. CREATE BOOKING-SEAT SNAPSHOTS
        // =========================================================

        /*
         * BookingSeat.price stores the historical price
         * paid/selected at booking time.
         *
         * EventSeat.price can potentially change later.
         */
        List<BookingSeat> bookingSeats = eventSeats
                        .stream()
                        .map(eventSeat ->
                                BookingSeat.builder()
                                        .booking(savedBooking)
                                        .eventSeat(eventSeat)
                                        .price(eventSeat.getPrice())
                                        .build()
                        )
                        .toList();


        bookingSeatRepository.saveAll(bookingSeats);

        // =========================================================
        // 18. CREATE DURABLE TEMPORARY SEAT RESERVATION
        // =========================================================

        /*
         * IMPORTANT:
         *
         * EventSeatStatus.LOCKED
         *
         * is NOT the same thing as:
         *
         * PESSIMISTIC_WRITE.
         *
         *
         * PESSIMISTIC_WRITE
         * -----------------
         * Database technical lock.
         * Exists only until transaction ends.
         *
         *
         * EventSeatStatus.LOCKED
         * ----------------------
         * Business reservation.
         * Persists after transaction commits.
         * Gives the user time to complete payment.
         */
        eventSeats.forEach(
                eventSeat -> {
                    eventSeat.setStatus(EventSeatStatus.LOCKED);
                    eventSeat.setLockedByBooking(savedBooking);
                    eventSeat.setLockedUntil(expiresAt);
                }
        );

        eventSeatRepository.saveAll(eventSeats);

        // =========================================================
        // 19. LOG SUCCESS
        // =========================================================

        log.info(
                "Booking created bookingReference={} eventId={} seatCount={} totalAmount={} expiresAt={} idempotencyKey={}",
                savedBooking.getBookingReference(),
                event.getId(),
                eventSeats.size(),
                savedBooking.getTotalAmount(),
                savedBooking.getExpiresAt(),
                idempotencyKey
        );

        // =========================================================
        // 20. RESPONSE
        // =========================================================

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

                log.info(
                        "Lazy cleanup releasing expired seat lock eventSeatId={} oldBookingId={} lockedUntil={}",
                        eventSeat.getId(),
                        oldBooking != null ? oldBooking.getId() : null,
                        eventSeat.getLockedUntil()
                );

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

            throw new ConflictException(
                    "Cannot book seats for a cancelled event"
            );
        }

        if (!event.getStartTime().isAfter(LocalDateTime.now())) {

            throw new ConflictException(
                    "Cannot book seats for an event that has already started"
            );
        }
    }

    private void validateDuplicateSeatIds(List<Long> eventSeatIds) {

        Set<Long> uniqueIds = new HashSet<>(eventSeatIds);

        if (uniqueIds.size() != eventSeatIds.size()) {

            throw new BadRequestException(
                    "Duplicate event seat ids are not allowed"
            );
        }
    }

    private void validateAllSeatsFound(List<EventSeat> eventSeats, List<Long> requestedIds) {

        if (eventSeats.size() != requestedIds.size()) {

            log.warn(
                    "Booking rejected because one or more EventSeats are unavailable eventSeatIds={}",
                    eventSeats.stream()
                            .map(EventSeat::getId)
                            .toList()
            );

            throw new ResourceNotFoundException(
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

            throw new BadRequestException(
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

            throw new ConflictException(
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
                                new ResourceNotFoundException(
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
            throw new ForbiddenOperationException(
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

    private String normalizeIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {

            throw new BadRequestException(
                    "Idempotency-Key header is required"
            );
        }

        String normalized = idempotencyKey.trim();

        if (normalized.length() > 100) {

            throw new BadRequestException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }

        return normalized;
    }

    private String buildRequestFingerprint(Long eventId, List<Long> eventSeatIds) {

        List<Long> sortedSeatIds = eventSeatIds.stream().sorted().toList();

        String canonicalRequest = eventId
                        + "|"
                        + sortedSeatIds
                        .stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));


        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            /*
             * SHA-256 is required by Java,
             * so this should never normally happen.
             */

            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingHistoryResponse> getMyBookings(String email, UserBookingSearchCriteria criteria) {

        validateUserBookingSearchCriteria(criteria);

        String normalizedSearch = normalizeSearch(criteria.getSearch());

        Sort sort = buildUserBookingSort(criteria.getSortBy(), criteria.getDirection());

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Page<Booking> bookingPage = bookingRepository.searchUserBookings(
                        email,
                        criteria.getStatus(),
                        normalizedSearch,
                        criteria.getCreatedFrom(),
                        criteria.getCreatedTo(),
                        pageable
                );

        List<BookingHistoryResponse> content = bookingPage
                        .getContent()
                        .stream()
                        .map(this::mapToHistoryResponse)
                        .toList();

        return new PagedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isFirst(),
                bookingPage.isLast()
        );
    }

    private void validateUserBookingSearchCriteria(UserBookingSearchCriteria criteria) {

        if (criteria.getPage() < 0) {

            throw new BadRequestException(
                    "Page number must be 0 or greater"
            );
        }

        if (criteria.getSize() < 1 || criteria.getSize() > 100) {

            throw new BadRequestException(
                    "Page size must be between 1 and 100"
            );
        }

        if (criteria.getCreatedFrom() != null && criteria.getCreatedTo() != null &&
                        criteria.getCreatedFrom().isAfter(criteria.getCreatedTo())) {

            throw new BadRequestException(
                    "createdFrom must not be after createdTo"
            );
        }
    }

    private String normalizeSearch(String search) {

        if (search == null || search.isBlank()) {

            return null;
        }

        return search.trim();
    }

    private Sort buildUserBookingSort(String sortBy, String direction) {

        String safeSortBy = switch (sortBy == null ? "" : sortBy) {

                    case "bookingReference" -> "bookingReference";
                    case "totalAmount" -> "totalAmount";
                    case "status" -> "status";
                    case "createdAt" -> "createdAt";

                    default -> "createdAt";
                };


        Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(safeDirection, safeSortBy);
    }
}