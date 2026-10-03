package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.RefundSimulationRequest;
import com.abhishek.eventbooking.dto.response.RefundResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import com.abhishek.eventbooking.repository.RefundRepository;

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
class BookingCancellationServiceTest {

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private BookingCancellationService
            bookingCancellationService;

    private User user;
    private Event event;
    private Booking booking;
    private EventSeat eventSeat;
    private Payment payment;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        event = Event.builder()
                .id(1L)
                .name("Future Event")
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
                .status(EventStatus.UPCOMING)
                .build();

        booking = Booking.builder()
                .id(20L)
                .user(user)
                .event(event)
                .bookingReference("BK-CANCEL01")
                .totalAmount(new BigDecimal("600.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        eventSeat = EventSeat.builder()
                .id(101L)
                .event(event)
                .price(new BigDecimal("600.00"))
                .status(EventSeatStatus.BOOKED)
                .build();

        payment = Payment.builder()
                .id(30L)
                .booking(booking)
                .paymentReference("PAY-TEST123")
                .amount(new BigDecimal("600.00"))
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void cancelBooking_whenRefundSucceeds_shouldCancelAndReleaseSeats() {

        RefundSimulationRequest request = new RefundSimulationRequest();

        request.setOutcome(RefundOutcome.SUCCESS);

        when(eventSeatRepository.findByBookingReferenceThroughBookingSeatsForUpdate("BK-CANCEL01")
        ).thenReturn(
                List.of(eventSeat)
        );

        when(bookingRepository.findByBookingReferenceForUpdate("BK-CANCEL01")
        ).thenReturn(
                Optional.of(booking)
        );

        when(refundRepository.existsByBookingIdAndStatus(20L, RefundStatus.SUCCESS)
        ).thenReturn(false);

        when(paymentRepository.findTopByBookingIdAndStatusOrderByCreatedAtDesc(20L, PaymentStatus.SUCCESS)
        ).thenReturn(
                Optional.of(payment)
        );

        when(refundRepository.save(any(Refund.class))
        ).thenAnswer(invocation -> {

            Refund refund = invocation.getArgument(0);

            refund.setId(40L);

            refund.setCreatedAt(LocalDateTime.now());

            return refund;
        });

        RefundResponse response =
                bookingCancellationService
                        .cancelBooking(
                                "user@example.com",
                                "BK-CANCEL01",
                                request
                        );

        assertEquals(
                RefundStatus.SUCCESS,
                response.getRefundStatus()
        );

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );

        assertEquals(
                EventSeatStatus.AVAILABLE,
                eventSeat.getStatus()
        );

        assertEquals(
                new BigDecimal("600.00"),
                response.getAmount()
        );
    }

    @Test
    void cancelBooking_whenRefundFails_shouldKeepBookingConfirmed() {

        RefundSimulationRequest request = new RefundSimulationRequest();

        request.setOutcome(RefundOutcome.FAILED);

        when(eventSeatRepository.findByBookingReferenceThroughBookingSeatsForUpdate("BK-CANCEL01")
        ).thenReturn(
                List.of(eventSeat)
        );

        when(bookingRepository.findByBookingReferenceForUpdate("BK-CANCEL01")
        ).thenReturn(
                Optional.of(booking)
        );

        when(refundRepository.existsByBookingIdAndStatus(20L, RefundStatus.SUCCESS)
        ).thenReturn(false);

        when(paymentRepository.findTopByBookingIdAndStatusOrderByCreatedAtDesc(20L, PaymentStatus.SUCCESS)
        ).thenReturn(
                Optional.of(payment)
        );

        when(refundRepository.save(any(Refund.class))
        ).thenAnswer(invocation -> {

            Refund refund = invocation.getArgument(0);

            refund.setId(41L);

            refund.setCreatedAt(LocalDateTime.now());

            return refund;
        });

        RefundResponse response =
                bookingCancellationService
                        .cancelBooking(
                                "user@example.com",
                                "BK-CANCEL01",
                                request
                        );

        assertEquals(
                RefundStatus.FAILED,
                response.getRefundStatus()
        );

        assertEquals(
                BookingStatus.CONFIRMED,
                booking.getStatus()
        );

        assertEquals(
                EventSeatStatus.BOOKED,
                eventSeat.getStatus()
        );

        verify(eventSeatRepository, never()
        ).saveAll(
                anyList()
        );
    }
}