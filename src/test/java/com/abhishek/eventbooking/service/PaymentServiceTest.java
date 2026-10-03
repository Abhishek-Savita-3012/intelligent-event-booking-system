package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.PaymentSimulationRequest;
import com.abhishek.eventbooking.dto.response.PaymentResponse;

import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.ConflictException;

import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Event event;
    private Booking booking;
    private EventSeat eventSeat;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();

        event = Event.builder()
                .id(1L)
                .name("Test Event")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .status(EventStatus.UPCOMING)
                .build();

        booking = Booking.builder()
                .id(20L)
                .user(user)
                .event(event)
                .bookingReference("BK-TEST1234")
                .totalAmount(new BigDecimal("500.00"))
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        eventSeat = EventSeat.builder()
                .id(101L)
                .event(event)
                .price(new BigDecimal("500.00"))
                .status(EventSeatStatus.LOCKED)
                .lockedByBooking(booking)
                .lockedUntil(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Test
    void simulatePayment_whenSuccess_shouldConfirmBookingAndBookSeats() {

        PaymentSimulationRequest request = new PaymentSimulationRequest();

        request.setOutcome(PaymentOutcome.SUCCESS);

        when(eventSeatRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                List.of(eventSeat)
        );

        when(bookingRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                Optional.of(booking)
        );

        when(paymentRepository.save(any(Payment.class))
        ).thenAnswer(invocation -> {

            Payment payment = invocation.getArgument(0);

            payment.setId(1L);

            payment.setCreatedAt(
                    LocalDateTime.now()
            );

            return payment;
        });

        PaymentResponse response =
                paymentService.simulatePayment(
                        "user@example.com",
                        "BK-TEST1234",
                        request
                );

        assertEquals(
                PaymentStatus.SUCCESS,
                response.getPaymentStatus()
        );

        assertEquals(
                BookingStatus.CONFIRMED,
                booking.getStatus()
        );

        assertEquals(
                EventSeatStatus.BOOKED,
                eventSeat.getStatus()
        );

        assertNull(
                eventSeat.getLockedUntil()
        );

        assertNull(
                eventSeat.getLockedByBooking()
        );

        assertNotNull(
                response.getPaymentReference()
        );

        assertTrue(
                response.getPaymentReference()
                        .startsWith("PAY-")
        );

        verify(paymentRepository, times(1)
        ).save(
                any(Payment.class)
        );
    }

    @Test
    void simulatePayment_whenFailed_shouldFailBookingAndReleaseSeats() {

        PaymentSimulationRequest request = new PaymentSimulationRequest();

        request.setOutcome(PaymentOutcome.FAILED);

        when(eventSeatRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                List.of(eventSeat)
        );

        when(bookingRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                Optional.of(booking)
        );

        when(paymentRepository.save(any(Payment.class))
        ).thenAnswer(invocation -> {

            Payment payment = invocation.getArgument(0);

            payment.setId(2L);

            payment.setCreatedAt(
                    LocalDateTime.now()
            );

            return payment;
        });

        PaymentResponse response =
                paymentService.simulatePayment(
                        "user@example.com",
                        "BK-TEST1234",
                        request
                );

        assertEquals(
                PaymentStatus.FAILED,
                response.getPaymentStatus()
        );

        assertEquals(
                BookingStatus.FAILED,
                booking.getStatus()
        );

        assertEquals(
                EventSeatStatus.AVAILABLE,
                eventSeat.getStatus()
        );

        assertNull(
                eventSeat.getLockedUntil()
        );

        assertNull(
                eventSeat.getLockedByBooking()
        );
    }

    @Test
    void simulatePayment_whenBookingExpired_shouldThrowConflict() {

        booking.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        eventSeat.setLockedUntil(LocalDateTime.now().minusSeconds(1));

        PaymentSimulationRequest request = new PaymentSimulationRequest();

        request.setOutcome(PaymentOutcome.SUCCESS);

        when(eventSeatRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                List.of(eventSeat)
        );

        when(bookingRepository.findByBookingReferenceForUpdate("BK-TEST1234")
        ).thenReturn(
                Optional.of(booking)
        );

        ConflictException exception =
                assertThrows(
                        ConflictException.class,
                        () ->
                                paymentService.simulatePayment(
                                        "user@example.com",
                                        "BK-TEST1234",
                                        request
                                )
                );

        assertEquals(
                "Booking reservation has expired",
                exception.getMessage()
        );

        verify(paymentRepository, never()
        ).save(
                any()
        );

        assertEquals(
                BookingStatus.PENDING,
                booking.getStatus()
        );
    }
}