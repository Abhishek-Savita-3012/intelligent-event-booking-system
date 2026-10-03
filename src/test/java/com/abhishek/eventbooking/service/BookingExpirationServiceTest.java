package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExpirationServiceTest {

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingExpirationService bookingExpirationService;

    @Test
    void releaseExpiredSeatLocks_shouldReleaseSeatAndExpireBooking() {

        Booking booking = Booking.builder()
                        .id(1L)
                        .bookingReference("BK-EXPIRED1")
                        .status(BookingStatus.PENDING)
                        .expiresAt(LocalDateTime.now().minusMinutes(1))
                        .build();

        EventSeat eventSeat =
                EventSeat.builder()
                        .id(10L)
                        .status(EventSeatStatus.LOCKED)
                        .lockedByBooking(booking)
                        .lockedUntil(LocalDateTime.now().minusMinutes(1))
                        .build();

        when(eventSeatRepository.findExpiredLockedSeatsForUpdate(eq(EventSeatStatus.LOCKED), any(LocalDateTime.class))
        ).thenReturn(
                List.of(eventSeat)
        );

        bookingExpirationService.releaseExpiredSeatLocks();

        assertEquals(
                EventSeatStatus.AVAILABLE,
                eventSeat.getStatus()
        );

        assertNull(
                eventSeat.getLockedByBooking()
        );

        assertNull(
                eventSeat.getLockedUntil()
        );

        assertEquals(
                BookingStatus.EXPIRED,
                booking.getStatus()
        );

        verify(bookingRepository, times(1)
        ).save(
                booking
        );

        verify(eventSeatRepository, times(1)
        ).saveAll(
                List.of(eventSeat)
        );
    }
}