package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.response.EventAnalyticsResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.exception.ResourceNotFoundException;

import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import com.abhishek.eventbooking.repository.RefundRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class AdminEventAnalyticsService {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;


    public AdminEventAnalyticsService(
            EventRepository eventRepository,
            EventSeatRepository eventSeatRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository
    ) {

        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }


    @Transactional(readOnly = true)
    public EventAnalyticsResponse getEventAnalytics(Long eventId) {

        log.debug("Generating analytics eventId={}", eventId);

        // ==============================
        // EVENT
        // ==============================

        Event event = eventRepository
                        .findById(eventId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId
                                )
                        );

        Hall hall = event.getHall();

        Venue venue = hall.getVenue();


        // ==============================
        // SEAT COUNTS
        // ==============================

        long totalSeats = eventSeatRepository.countByEvent_Id(eventId);

        long availableSeats = eventSeatRepository.countByEvent_IdAndStatus(eventId, EventSeatStatus.AVAILABLE);

        long lockedSeats = eventSeatRepository.countByEvent_IdAndStatus(eventId, EventSeatStatus.LOCKED);

        long bookedSeats = eventSeatRepository.countByEvent_IdAndStatus(eventId, EventSeatStatus.BOOKED);

        // ==============================
        // OCCUPANCY
        // ==============================

        BigDecimal occupancyPercentage = calculateOccupancyPercentage(bookedSeats, totalSeats);

        // ==============================
        // BOOKING COUNTS
        // ==============================

        long totalBookings = bookingRepository.countByEvent_Id(eventId);

        long confirmedBookings = bookingRepository.countByEvent_IdAndStatus(eventId, BookingStatus.CONFIRMED);

        long cancelledBookings = bookingRepository.countByEvent_IdAndStatus(eventId, BookingStatus.CANCELLED);

        // ==============================
        // FINANCIALS
        // ==============================

        BigDecimal grossTicketSales = paymentRepository.sumAmountByEventIdAndStatus(eventId, PaymentStatus.SUCCESS);

        BigDecimal refundedAmount = refundRepository.sumAmountByEventIdAndStatus(eventId, RefundStatus.SUCCESS);

        BigDecimal netRevenue = grossTicketSales.subtract(refundedAmount);

        // ==============================
        // LOGGING
        // ==============================

        log.info(
                "Event analytics generated eventId={} totalSeats={} bookedSeats={} occupancy={} netRevenue={}",
                eventId,
                totalSeats,
                bookedSeats,
                occupancyPercentage,
                netRevenue
        );

        // ==============================
        // RESPONSE
        // ==============================

        return EventAnalyticsResponse.builder()
                .eventId(event.getId())
                .eventName(event.getName())
                .eventStatus(event.getStatus())
                .startTime(event.getStartTime())

                // Venue
                .venueId(venue.getId())
                .venueName(venue.getName())

                // Hall
                .hallId(hall.getId())
                .hallName(hall.getName())

                // Seats
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .lockedSeats(lockedSeats)
                .bookedSeats(bookedSeats)
                .occupancyPercentage(occupancyPercentage)

                // Bookings
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)

                // Money
                .grossTicketSales(grossTicketSales)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .build();
    }


    private BigDecimal calculateOccupancyPercentage(long bookedSeats, long totalSeats) {

        if (totalSeats == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal
                .valueOf(bookedSeats)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP);
    }
}