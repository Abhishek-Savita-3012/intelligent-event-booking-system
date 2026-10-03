package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.response.BookingResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Venue venue;
    private Hall hall;
    private Event event;
    private Seat seat1;
    private Seat seat2;
    private EventSeat eventSeat1;
    private EventSeat eventSeat2;

    @BeforeEach
    void setUp() {

        /*
         * @Value does not get injected because this is
         * a pure Mockito unit test rather than a
         * Spring application context.
         */
        ReflectionTestUtils.setField(
                bookingService,
                "lockDurationSeconds",
                300L
        );

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        venue = Venue.builder()
                .id(1L)
                .name("Test Venue")
                .city("Lucknow")
                .address("Test Address")
                .build();

        hall = Hall.builder()
                .id(1L)
                .name("Screen 1")
                .venue(venue)
                .build();

        event = Event.builder()
                .id(1L)
                .name("Test Movie")
                .category(EventCategory.MOVIE)
                .hall(hall)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .status(EventStatus.UPCOMING)
                .build();

        seat1 = Seat.builder()
                .id(1L)
                .hall(hall)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.REGULAR)
                .build();

        seat2 = Seat.builder()
                .id(2L)
                .hall(hall)
                .rowName("A")
                .seatNumber(2)
                .seatType(SeatType.REGULAR)
                .build();

        eventSeat1 = EventSeat.builder()
                .id(101L)
                .event(event)
                .seat(seat1)
                .price(new BigDecimal("250.00"))
                .status(EventSeatStatus.AVAILABLE)
                .build();

        eventSeat2 = EventSeat.builder()
                .id(102L)
                .event(event)
                .seat(seat2)
                .price(new BigDecimal("250.00"))
                .status(EventSeatStatus.AVAILABLE)
                .build();
    }

    @Test
    void createBooking_whenSeatsAvailable_shouldCreatePendingBooking() {

        // ==============================
        // ARRANGE
        // ==============================

        BookingRequest request = new BookingRequest();

        request.setEventId(1L);

        request.setEventSeatIds(
                List.of(101L, 102L)
        );

        when(userRepository.findByEmail("user@example.com")).thenReturn(
                Optional.of(user)
        );

        when(eventRepository.findById(1L)).thenReturn(
                Optional.of(event)
        );

        when(eventSeatRepository.findAllByIdInForUpdate(List.of(101L, 102L)))
                .thenReturn(
                List.of(
                        eventSeat1,
                        eventSeat2
                )
        );

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {

            Booking booking = invocation.getArgument(0);
            booking.setId(50L);
            return booking;
        });

        when(bookingSeatRepository.saveAll(anyList())).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(eventSeatRepository.saveAll(anyList())).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        // ==============================
        // ACT
        // ==============================

        BookingResponse response =
                bookingService.createBooking(
                        "user@example.com",
                        request
                );

        // ==============================
        // ASSERT
        // ==============================

        assertNotNull(response);

        assertEquals(
                BookingStatus.PENDING,
                response.getStatus()
        );

        assertEquals(
                new BigDecimal("500.00"),
                response.getTotalAmount()
        );

        assertNotNull(
                response.getBookingReference()
        );

        assertTrue(
                response.getBookingReference()
                        .startsWith("BK-")
        );

        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat1.getStatus()
        );

        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat2.getStatus()
        );

        assertNotNull(
                eventSeat1.getLockedUntil()
        );

        assertNotNull(
                eventSeat1.getLockedByBooking()
        );

        verify(bookingRepository, times(1)).save(
                any(Booking.class)
        );

        verify(bookingSeatRepository, times(1)).saveAll(
                anyList()
        );

        verify(eventSeatRepository, times(1)).saveAll(
                anyList()
        );
    }

    @Test
    void createBooking_whenDuplicateSeatIds_shouldThrowBadRequest() {

        BookingRequest request = new BookingRequest();

        request.setEventId(1L);

        request.setEventSeatIds(
                List.of(101L, 101L)
        );

        when(userRepository.findByEmail("user@example.com")).thenReturn(
                Optional.of(user)
        );

        when(eventRepository.findById(1L)).thenReturn(
                Optional.of(event)
        );

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () ->
                                bookingService.createBooking(
                                        "user@example.com",
                                        request
                                )
                );

        assertEquals(
                "Duplicate event seat ids are not allowed",
                exception.getMessage()
        );

        verify(eventSeatRepository, never()).findAllByIdInForUpdate(
                anyList()
        );

        verify(bookingRepository, never()).save(
                any()
        );
    }

    @Test
    void createBooking_whenSeatAlreadyLocked_shouldThrowConflict() {

        eventSeat1.setStatus(EventSeatStatus.LOCKED);

        eventSeat1.setLockedUntil(LocalDateTime.now().plusMinutes(3));

        Booking oldBooking = Booking.builder()
                        .id(99L)
                        .user(user)
                        .event(event)
                        .status(BookingStatus.PENDING)
                        .expiresAt(LocalDateTime.now().plusMinutes(3))
                        .build();

        eventSeat1.setLockedByBooking(oldBooking);

        BookingRequest request = new BookingRequest();

        request.setEventId(1L);

        request.setEventSeatIds(List.of(101L));

        when(userRepository.findByEmail("user@example.com")).thenReturn(
                Optional.of(user)
        );

        when(eventRepository.findById(1L)).thenReturn(
                Optional.of(event)
        );

        when(eventSeatRepository.findAllByIdInForUpdate(List.of(101L))).thenReturn(
                List.of(eventSeat1)
        );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                bookingService.createBooking(
                                        "user@example.com",
                                        request
                                )
                );

        assertEquals(
                "One or more selected seats are not available",
                exception.getMessage()
        );

        verify(bookingRepository, never())
                .save(any()
        );
    }

    @Test
    void createBooking_whenExistingLockExpired_shouldReuseSeat() {

        Booking oldBooking =
                Booking.builder()
                        .id(70L)
                        .user(user)
                        .event(event)
                        .status(
                                BookingStatus.PENDING
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .minusMinutes(1)
                        )
                        .build();

        eventSeat1.setStatus(
                EventSeatStatus.LOCKED
        );

        eventSeat1.setLockedByBooking(
                oldBooking
        );

        eventSeat1.setLockedUntil(
                LocalDateTime.now()
                        .minusSeconds(10)
        );

        BookingRequest request =
                new BookingRequest();

        request.setEventId(1L);

        request.setEventSeatIds(
                List.of(101L)
        );

        when(
                userRepository.findByEmail(
                        "user@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findById(1L)
        ).thenReturn(
                Optional.of(event)
        );

        when(
                eventSeatRepository
                        .findAllByIdInForUpdate(
                                List.of(101L)
                        )
        ).thenReturn(
                List.of(eventSeat1)
        );

        when(
                bookingRepository.save(
                        any(Booking.class)
                )
        ).thenAnswer(invocation -> {

            Booking booking =
                    invocation.getArgument(0);

            /*
             * Don't overwrite the ID when the service
             * saves the OLD expired booking.
             */
            if (booking.getId() == null) {
                booking.setId(71L);
            }

            return booking;
        });

        when(
                bookingSeatRepository.saveAll(
                        anyList()
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        BookingResponse response =
                bookingService.createBooking(
                        "user@example.com",
                        request
                );

        assertEquals(
                BookingStatus.EXPIRED,
                oldBooking.getStatus()
        );

        assertEquals(
                EventSeatStatus.LOCKED,
                eventSeat1.getStatus()
        );

        assertNotEquals(
                oldBooking,
                eventSeat1.getLockedByBooking()
        );

        assertEquals(
                BookingStatus.PENDING,
                response.getStatus()
        );
    }
}