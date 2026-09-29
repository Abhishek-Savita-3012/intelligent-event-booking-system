package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.entity.Booking;
import com.abhishek.eventbooking.entity.BookingStatus;
import com.abhishek.eventbooking.entity.EventSeat;
import com.abhishek.eventbooking.entity.EventSeatStatus;
import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BookingExpirationService {

    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;

    public BookingExpirationService(EventSeatRepository eventSeatRepository, BookingRepository bookingRepository) {

        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void releaseExpiredSeatLocks() {

        LocalDateTime now = LocalDateTime.now();

        List<EventSeat> expiredSeats = eventSeatRepository.findExpiredLockedSeatsForUpdate(EventSeatStatus.LOCKED, now);

        if (expiredSeats.isEmpty()) {
            return;
        }

        Set<Long> processedBookingIds = new HashSet<>();

        for (EventSeat eventSeat : expiredSeats) {

            Booking booking = eventSeat.getLockedByBooking();

            eventSeat.setStatus(EventSeatStatus.AVAILABLE);
            eventSeat.setLockedUntil(null);
            eventSeat.setLockedByBooking(null);

            if (
                    booking != null &&
                            booking.getStatus() == BookingStatus.PENDING
                            && processedBookingIds.add(booking.getId())
            ) {

                booking.setStatus(BookingStatus.EXPIRED);

                bookingRepository.save(booking);
            }
        }

        eventSeatRepository.saveAll(expiredSeats);
    }
}