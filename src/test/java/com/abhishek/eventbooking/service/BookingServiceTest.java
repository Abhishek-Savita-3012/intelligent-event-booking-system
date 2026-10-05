package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.response.BookingResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.exception.ConflictException;

import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.BookingSeatRepository;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.LocalDateTime;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    // =========================================================
    // MOCK REPOSITORIES
    // =========================================================

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    // =========================================================
    // SERVICE UNDER TEST
    // =========================================================

    @InjectMocks
    private BookingService bookingService;

    // =========================================================
    // TEST DATA
    // =========================================================

    private User user;
    private Venue venue;
    private Hall hall;
    private Seat physicalSeat;
    private Event event;
    private EventSeat eventSeat;
    private BookingRequest request;

    private static final String EMAIL = "booking-test@example.com";
    private static final String IDEMPOTENCY_KEY = "unit-booking-key-001";

    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        /*
         * BookingService normally receives this from:
         *
         * booking.lock-duration-seconds=300
         *
         * Unit tests don't load Spring configuration,
         * so inject the value manually.
         */
        ReflectionTestUtils.setField(bookingService, "lockDurationSeconds", 300L);

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        user = User.builder()
                        .id(1L)
                        .name("Booking Test User")
                        .email(EMAIL)
                        .password("encoded-password")
                        .role(Role.USER)
                        .build();

        // -----------------------------------------------------
        // VENUE
        // -----------------------------------------------------

        venue = Venue.builder()
                        .id(10L)
                        .name("Test Venue")
                        .city("Lucknow")
                        .address("Test Address")
                        .build();

        // -----------------------------------------------------
        // HALL
        // -----------------------------------------------------

        hall = Hall.builder()
                        .id(20L)
                        .name("Screen 1")
                        .venue(venue)
                        .build();

        // -----------------------------------------------------
        // PHYSICAL SEAT
        // -----------------------------------------------------

        physicalSeat = Seat.builder()
                        .id(30L)
                        .hall(hall)
                        .rowName("A")
                        .seatNumber(1)
                        .seatType(SeatType.REGULAR)
                        .build();

        // -----------------------------------------------------
        // EVENT
        // -----------------------------------------------------

        event = Event.builder()
                        .id(40L)
                        .name("Unit Test Movie")
                        .description("Booking service unit test event")
                        .category(EventCategory.MOVIE)
                        .startTime(LocalDateTime.now().plusDays(2))
                        .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                        .status(EventStatus.UPCOMING)
                        .hall(hall)
                        .build();

        // -----------------------------------------------------
        // EVENT SEAT
        // -----------------------------------------------------

        eventSeat = EventSeat.builder()
                        .id(50L)
                        .event(event)
                        .seat(physicalSeat)
                        .price(new BigDecimal("250.00"))
                        .status(EventSeatStatus.AVAILABLE)
                        .build();

        // -----------------------------------------------------
        // BOOKING REQUEST
        // -----------------------------------------------------

        request = createRequest(event.getId(), List.of(eventSeat.getId()));
    }

    // =========================================================
    // TEST 1
    //
    // NEW REQUEST SHOULD CREATE BOOKING
    // =========================================================

    @Test
    void createBooking_newRequest_shouldCreatePendingBookingAndLockSeat() {

        when(
                userRepository
                        .findByEmailForUpdate(
                                EMAIL
                        )
        )
                .thenReturn(
                        Optional.of(user)
                );


        when(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                user.getId(),
                                IDEMPOTENCY_KEY
                        )
        )
                .thenReturn(
                        Optional.empty()
                );


        when(
                eventRepository
                        .findById(
                                event.getId()
                        )
        )
                .thenReturn(
                        Optional.of(event)
                );


        when(
                eventSeatRepository
                        .findAllByIdInForUpdate(
                                List.of(
                                        eventSeat.getId()
                                )
                        )
        )
                .thenReturn(
                        List.of(eventSeat)
                );


        /*
         * Simulate database-generated Booking ID.
         */
        when(
                bookingRepository
                        .save(
                                any(Booking.class)
                        )
        )
                .thenAnswer(invocation -> {

                    Booking booking =
                            invocation.getArgument(0);

                    if (
                            booking.getId() == null
                    ) {

                        booking.setId(100L);
                    }

                    return booking;
                });


        BookingResponse response =
                bookingService
                        .createBooking(
                                EMAIL,
                                IDEMPOTENCY_KEY,
                                request
                        );


        assertNotNull(
                response
        );


        // -----------------------------------------------------
        // CAPTURE CREATED BOOKING
        // -----------------------------------------------------

        ArgumentCaptor<Booking> bookingCaptor =
                ArgumentCaptor.forClass(
                        Booking.class
                );


        verify(
                bookingRepository
        )
                .save(
                        bookingCaptor.capture()
                );


        Booking savedBooking =
                bookingCaptor.getValue();


        // -----------------------------------------------------
        // BOOKING ASSERTIONS
        // -----------------------------------------------------

        assertEquals(
                user,
                savedBooking.getUser()
        );

        assertEquals(
                event,
                savedBooking.getEvent()
        );

        assertEquals(
                BookingStatus.PENDING,
                savedBooking.getStatus()
        );

        assertEquals(
                IDEMPOTENCY_KEY,
                savedBooking.getIdempotencyKey()
        );

        assertNotNull(
                savedBooking.getRequestFingerprint()
        );

        assertEquals(
                64,
                savedBooking
                        .getRequestFingerprint()
                        .length()
        );

        assertNotNull(
                savedBooking.getExpiresAt()
        );


        assertEquals(
                0,
                savedBooking
                        .getTotalAmount()
                        .compareTo(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
        );


        // -----------------------------------------------------
        // EVENT SEAT ASSERTIONS
        // -----------------------------------------------------

        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat.getStatus()
        );

        assertEquals(
                savedBooking,
                eventSeat.getLockedByBooking()
        );

        assertNotNull(
                eventSeat.getLockedUntil()
        );


        // -----------------------------------------------------
        // VERIFY IMPORTANT CALLS
        // -----------------------------------------------------

        verify(
                userRepository
        )
                .findByEmailForUpdate(
                        EMAIL
                );


        verify(
                bookingRepository
        )
                .findByUser_IdAndIdempotencyKey(
                        user.getId(),
                        IDEMPOTENCY_KEY
                );


        verify(
                eventSeatRepository
        )
                .findAllByIdInForUpdate(
                        List.of(
                                eventSeat.getId()
                        )
                );


        verify(
                bookingSeatRepository
        )
                .saveAll(
                        anyList()
                );


        verify(
                eventSeatRepository
        )
                .saveAll(
                        anyList()
                );
    }


    // =========================================================
    // TEST 2
    //
    // MISSING IDEMPOTENCY KEY
    // =========================================================

    @Test
    void createBooking_missingIdempotencyKey_shouldThrowBadRequest() {

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                bookingService
                                        .createBooking(
                                                EMAIL,
                                                null,
                                                request
                                        )
                );


        assertEquals(
                "Idempotency-Key header is required",
                exception.getMessage()
        );


        /*
         * Validation should fail before any database
         * lock or booking creation.
         */
        verifyNoInteractions(
                userRepository,
                eventRepository,
                eventSeatRepository,
                bookingRepository,
                bookingSeatRepository
        );
    }


    // =========================================================
    // TEST 3
    //
    // DUPLICATE EVENT SEAT IDs
    // =========================================================

    @Test
    void createBooking_duplicateSeatIds_shouldThrowBadRequest() {

        BookingRequest duplicateRequest =
                createRequest(
                        event.getId(),
                        List.of(
                                eventSeat.getId(),
                                eventSeat.getId()
                        )
                );


        assertThrows(
                BadRequestException.class,
                () ->
                        bookingService
                                .createBooking(
                                        EMAIL,
                                        "duplicate-seat-key",
                                        duplicateRequest
                                )
        );


        /*
         * Duplicate request validation happens before
         * the User PESSIMISTIC_WRITE lock.
         */
        verify(
                userRepository,
                never()
        )
                .findByEmailForUpdate(
                        any()
                );


        verify(
                eventSeatRepository,
                never()
        )
                .findAllByIdInForUpdate(
                        anyList()
                );


        verify(
                bookingRepository,
                never()
        )
                .save(
                        any()
                );
    }


    // =========================================================
    // TEST 4
    //
    // SAME KEY + SAME PAYLOAD
    //
    // SHOULD RETURN EXISTING BOOKING
    // =========================================================

    @Test
    void createBooking_sameIdempotencyKeySamePayload_shouldReturnExistingBooking() {

        String fingerprint =
                fingerprint(
                        event.getId(),
                        List.of(
                                eventSeat.getId()
                        )
                );


        Booking existingBooking =
                Booking.builder()
                        .id(200L)
                        .user(user)
                        .event(event)
                        .bookingReference(
                                "BK-IDEMP001"
                        )
                        .idempotencyKey(
                                IDEMPOTENCY_KEY
                        )
                        .requestFingerprint(
                                fingerprint
                        )
                        .totalAmount(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
                        .status(
                                BookingStatus.PENDING
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(5)
                        )
                        .build();


        BookingSeat existingBookingSeat =
                BookingSeat.builder()
                        .id(300L)
                        .booking(
                                existingBooking
                        )
                        .eventSeat(
                                eventSeat
                        )
                        .price(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
                        .build();


        when(
                userRepository
                        .findByEmailForUpdate(
                                EMAIL
                        )
        )
                .thenReturn(
                        Optional.of(user)
                );


        when(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                user.getId(),
                                IDEMPOTENCY_KEY
                        )
        )
                .thenReturn(
                        Optional.of(
                                existingBooking
                        )
                );


        when(
                bookingSeatRepository
                        .findByBookingId(
                                existingBooking.getId()
                        )
        )
                .thenReturn(
                        List.of(
                                existingBookingSeat
                        )
                );


        BookingResponse response =
                bookingService
                        .createBooking(
                                EMAIL,
                                IDEMPOTENCY_KEY,
                                request
                        );


        assertNotNull(
                response
        );


        assertEquals(
                existingBooking.getBookingReference(),
                response.getBookingReference()
        );


        /*
         * Since this is a replay, there must be no new
         * EventSeat lock or Booking insert.
         */
        verify(
                eventRepository,
                never()
        )
                .findById(
                        any()
                );


        verify(
                eventSeatRepository,
                never()
        )
                .findAllByIdInForUpdate(
                        anyList()
                );


        verify(
                bookingRepository,
                never()
        )
                .save(
                        any()
                );


        verify(
                bookingSeatRepository,
                never()
        )
                .saveAll(
                        anyList()
                );
    }


    // =========================================================
    // TEST 5
    //
    // SAME KEY + DIFFERENT PAYLOAD
    //
    // SHOULD RETURN CONFLICT
    // =========================================================

    @Test
    void createBooking_sameIdempotencyKeyDifferentPayload_shouldThrowConflict() {

        Booking existingBooking =
                Booking.builder()
                        .id(201L)
                        .user(user)
                        .event(event)
                        .bookingReference(
                                "BK-OLDKEY01"
                        )
                        .idempotencyKey(
                                IDEMPOTENCY_KEY
                        )

                        /*
                         * Deliberately different fingerprint.
                         */
                        .requestFingerprint(
                                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        )

                        .totalAmount(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
                        .status(
                                BookingStatus.PENDING
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(5)
                        )
                        .build();


        when(
                userRepository
                        .findByEmailForUpdate(
                                EMAIL
                        )
        )
                .thenReturn(
                        Optional.of(user)
                );


        when(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                user.getId(),
                                IDEMPOTENCY_KEY
                        )
        )
                .thenReturn(
                        Optional.of(
                                existingBooking
                        )
                );


        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                bookingService
                                        .createBooking(
                                                EMAIL,
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );


        assertEquals(
                "Idempotency-Key has already been used for a different booking request",
                exception.getMessage()
        );


        verify(
                eventRepository,
                never()
        )
                .findById(
                        any()
                );


        verify(
                eventSeatRepository,
                never()
        )
                .findAllByIdInForUpdate(
                        anyList()
                );


        verify(
                bookingRepository,
                never()
        )
                .save(
                        any()
                );
    }


    // =========================================================
    // TEST 6
    //
    // NON-EXPIRED LOCKED SEAT
    // =========================================================

    @Test
    void createBooking_nonExpiredLockedSeat_shouldThrowConflict() {

        Booking existingLockOwner =
                Booking.builder()
                        .id(400L)
                        .user(user)
                        .event(event)
                        .bookingReference(
                                "BK-LOCKED01"
                        )
                        .totalAmount(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
                        .status(
                                BookingStatus.PENDING
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(5)
                        )
                        .build();


        eventSeat.setStatus(
                EventSeatStatus.LOCKED
        );

        eventSeat.setLockedByBooking(
                existingLockOwner
        );

        eventSeat.setLockedUntil(
                LocalDateTime.now()
                        .plusMinutes(5)
        );


        when(
                userRepository
                        .findByEmailForUpdate(
                                EMAIL
                        )
        )
                .thenReturn(
                        Optional.of(user)
                );


        when(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                user.getId(),
                                "new-booking-key"
                        )
        )
                .thenReturn(
                        Optional.empty()
                );


        when(
                eventRepository
                        .findById(
                                event.getId()
                        )
        )
                .thenReturn(
                        Optional.of(event)
                );


        when(
                eventSeatRepository
                        .findAllByIdInForUpdate(
                                List.of(
                                        eventSeat.getId()
                                )
                        )
        )
                .thenReturn(
                        List.of(
                                eventSeat
                        )
                );


        assertThrows(
                ConflictException.class,
                () ->
                        bookingService
                                .createBooking(
                                        EMAIL,
                                        "new-booking-key",
                                        request
                                )
        );


        verify(
                bookingRepository,
                never()
        )
                .save(
                        argThat(
                                booking ->
                                        booking
                                                .getIdempotencyKey()
                                                != null
                        )
                );


        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat.getStatus()
        );


        assertEquals(
                existingLockOwner,
                eventSeat.getLockedByBooking()
        );
    }


    // =========================================================
    // TEST 7
    //
    // EXPIRED OLD LOCK SHOULD BE RELEASED AND REUSED
    // =========================================================

    @Test
    void createBooking_expiredLock_shouldExpireOldBookingAndCreateNewBooking() {

        Booking oldBooking =
                Booking.builder()
                        .id(500L)
                        .user(user)
                        .event(event)
                        .bookingReference(
                                "BK-EXPIRED1"
                        )
                        .totalAmount(
                                new BigDecimal(
                                        "250.00"
                                )
                        )
                        .status(
                                BookingStatus.PENDING
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .minusMinutes(1)
                        )
                        .build();


        eventSeat.setStatus(
                EventSeatStatus.LOCKED
        );

        eventSeat.setLockedByBooking(
                oldBooking
        );

        eventSeat.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );


        String newKey =
                "expired-lock-new-key";


        when(
                userRepository
                        .findByEmailForUpdate(
                                EMAIL
                        )
        )
                .thenReturn(
                        Optional.of(user)
                );


        when(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                user.getId(),
                                newKey
                        )
        )
                .thenReturn(
                        Optional.empty()
                );


        when(
                eventRepository
                        .findById(
                                event.getId()
                        )
        )
                .thenReturn(
                        Optional.of(event)
                );


        when(
                eventSeatRepository
                        .findAllByIdInForUpdate(
                                List.of(
                                        eventSeat.getId()
                                )
                        )
        )
                .thenReturn(
                        List.of(
                                eventSeat
                        )
                );


        /*
         * save() may potentially also be called by your
         * expiry cleanup implementation.
         *
         * Only assign an ID if the Booking is new.
         */
        when(
                bookingRepository
                        .save(
                                any(Booking.class)
                        )
        )
                .thenAnswer(invocation -> {

                    Booking booking =
                            invocation.getArgument(0);

                    if (
                            booking.getId() == null
                    ) {

                        booking.setId(600L);
                    }

                    return booking;
                });


        BookingResponse response =
                bookingService
                        .createBooking(
                                EMAIL,
                                newKey,
                                request
                        );


        assertNotNull(
                response
        );


        /*
         * Old lock owner should have expired.
         */
        assertEquals(
                BookingStatus.EXPIRED,
                oldBooking.getStatus()
        );


        /*
         * Seat is immediately reused by the new booking,
         * so its final state after createBooking() is
         * LOCKED again, not AVAILABLE.
         */
        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat.getStatus()
        );


        assertNotNull(
                eventSeat.getLockedByBooking()
        );


        assertNotSame(
                oldBooking,
                eventSeat.getLockedByBooking()
        );


        Booking newBooking =
                eventSeat.getLockedByBooking();


        assertEquals(
                BookingStatus.PENDING,
                newBooking.getStatus()
        );


        assertEquals(
                newKey,
                newBooking.getIdempotencyKey()
        );


        assertNotNull(
                newBooking.getRequestFingerprint()
        );


        assertNotNull(
                eventSeat.getLockedUntil()
        );
    }


    // =========================================================
    // TEST HELPERS
    // =========================================================

    private BookingRequest createRequest(
            Long eventId,
            List<Long> eventSeatIds
    ) {

        BookingRequest request =
                new BookingRequest();

        request.setEventId(
                eventId
        );

        request.setEventSeatIds(
                eventSeatIds
        );

        return request;
    }


    /**
     * Test-side implementation of the canonical request
     * fingerprint.
     *
     * This lets the idempotency replay test create an
     * existing Booking containing the exact fingerprint
     * expected by BookingService.
     */
    private String fingerprint(
            Long eventId,
            List<Long> eventSeatIds
    ) {

        List<Long> sortedSeatIds =
                eventSeatIds
                        .stream()
                        .sorted()
                        .toList();


        String canonical =
                eventId
                        + "|"
                        + sortedSeatIds
                        .stream()
                        .map(
                                String::valueOf
                        )
                        .collect(
                                Collectors.joining(",")
                        );


        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );


            return HexFormat
                    .of()
                    .formatHex(
                            digest.digest(
                                    canonical.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
                    );

        } catch (
                NoSuchAlgorithmException exception
        ) {

            throw new IllegalStateException(
                    exception
            );
        }
    }
}