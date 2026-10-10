package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.projection.BookingStatusCountProjection;
import com.abhishek.eventbooking.dto.projection.PaymentStatusSummaryProjection;
import com.abhishek.eventbooking.dto.projection.RefundStatusSummaryProjection;
import com.abhishek.eventbooking.dto.projection.SeatTypeStatusCountProjection;

import com.abhishek.eventbooking.dto.response.EventAnalyticsResponse;
import com.abhishek.eventbooking.dto.response.SeatTypeAnalyticsResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.exception.ResourceNotFoundException;

import com.abhishek.eventbooking.config.CacheNames;
import org.springframework.cache.annotation.Cacheable;

import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import com.abhishek.eventbooking.repository.RefundRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminEventAnalyticsService {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    @Cacheable(
            cacheNames = CacheNames.EVENT_ANALYTICS,
            key = "#eventId"
    )
    @Transactional(readOnly = true)
    public EventAnalyticsResponse getEventAnalytics(Long eventId) {

        // =====================================================
        // EVENT
        // =====================================================

        Event event = eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId
                                )
                        );


        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        // =====================================================
        // 1. SEAT INVENTORY
        // =====================================================

        List<SeatTypeStatusCountProjection> seatRows = eventSeatRepository
                        .summarizeSeatInventoryForEvent(
                                eventId
                        );


        Map<SeatType, Map<EventSeatStatus, Long>> seatCounts = createSeatCountMap();


        for (SeatTypeStatusCountProjection row : seatRows) {

            seatCounts.get(row.getSeatType()).put(row.getStatus(), safeLong(row.getCount()));
        }


        long totalSeats = 0;
        long availableSeats = 0;
        long lockedSeats = 0;
        long bookedSeats = 0;


        List<SeatTypeAnalyticsResponse> seatTypeBreakdown = new ArrayList<>();

        for (SeatType seatType : SeatType.values()) {

            Map<EventSeatStatus, Long> statusCounts = seatCounts.get(seatType);

            long typeAvailable = statusCounts.getOrDefault(EventSeatStatus.AVAILABLE, 0L);

            long typeLocked = statusCounts.getOrDefault(EventSeatStatus.LOCKED, 0L);

            long typeBooked = statusCounts.getOrDefault(EventSeatStatus.BOOKED, 0L);

            long typeTotal = typeAvailable + typeLocked + typeBooked;

            totalSeats += typeTotal;

            availableSeats += typeAvailable;

            lockedSeats += typeLocked;

            bookedSeats += typeBooked;

            seatTypeBreakdown.add(SeatTypeAnalyticsResponse
                            .builder()
                            .seatType(seatType)
                            .totalSeats(typeTotal)
                            .availableSeats(typeAvailable)
                            .lockedSeats(typeLocked)
                            .bookedSeats(typeBooked)
                            .occupancyPercentage(percentage(typeBooked, typeTotal))
                            .build()
            );
        }


        BigDecimal occupancyPercentage = percentage(bookedSeats, totalSeats);

        BigDecimal lockedPercentage = percentage(lockedSeats, totalSeats);

        // =====================================================
        // 2. BOOKING STATUS SUMMARY
        // =====================================================

        List<BookingStatusCountProjection> bookingRows = bookingRepository
                        .countBookingsByStatusForEvent(
                                eventId
                        );


        Map<BookingStatus, Long> bookingCounts = new EnumMap<>(BookingStatus.class);

        for (BookingStatus status : BookingStatus.values()) {

            bookingCounts.put(status, 0L);
        }


        for (BookingStatusCountProjection row : bookingRows) {

            bookingCounts.put(row.getStatus(), safeLong(row.getCount())
            );
        }

        long pendingBookings = bookingCounts.get(BookingStatus.PENDING);

        long confirmedBookings = bookingCounts.get(BookingStatus.CONFIRMED);

        long cancelledBookings = bookingCounts.get(BookingStatus.CANCELLED);

        long failedBookings = bookingCounts.get(BookingStatus.FAILED);

        long expiredBookings = bookingCounts.get(BookingStatus.EXPIRED);

        long totalBookings = pendingBookings + confirmedBookings + cancelledBookings + failedBookings + expiredBookings;

        BigDecimal confirmationRate = percentage(confirmedBookings, totalBookings);

        BigDecimal cancellationRate = percentage(cancelledBookings, totalBookings);

        BigDecimal failureRate = percentage(failedBookings, totalBookings);

        BigDecimal expirationRate = percentage(expiredBookings, totalBookings);

        // =====================================================
        // 3. PAYMENT SUMMARY
        // =====================================================

        List<PaymentStatusSummaryProjection> paymentRows = paymentRepository
                        .summarizePaymentsForEvent(
                                eventId
                        );


        long successfulPayments = 0;
        long failedPayments = 0;
        long pendingPayments = 0;

        BigDecimal grossTicketSales = moneyZero();

        for (PaymentStatusSummaryProjection row : paymentRows) {

            long count = safeLong(row.getCount());

            switch (row.getStatus()) {

                case SUCCESS -> {

                    successfulPayments = count;

                    grossTicketSales = safeMoney(row.getTotalAmount());
                }


                case FAILED ->

                        failedPayments = count;

                case PENDING ->

                        pendingPayments = count;
            }
        }

        long paymentAttempts = successfulPayments + failedPayments + pendingPayments;

        BigDecimal paymentSuccessRate = percentage(successfulPayments, paymentAttempts);

        BigDecimal averageSuccessfulPaymentAmount = averageMoney(grossTicketSales, successfulPayments);

        // =====================================================
        // 4. REFUND SUMMARY
        // =====================================================

        List<RefundStatusSummaryProjection> refundRows = refundRepository
                        .summarizeRefundsForEvent(
                                eventId
                        );


        long successfulRefunds = 0;
        long failedRefunds = 0;

        BigDecimal refundedAmount = moneyZero();

        for (RefundStatusSummaryProjection row : refundRows) {

            long count = safeLong(row.getCount());

            switch (row.getStatus()) {

                case SUCCESS -> {

                    successfulRefunds = count;

                    refundedAmount = safeMoney(row.getTotalAmount());
                }

                case FAILED ->

                        failedRefunds = count;
            }
        }

        long refundAttempts = successfulRefunds + failedRefunds;

        BigDecimal refundSuccessRate = percentage(successfulRefunds, refundAttempts);

        // =====================================================
        // 5. REVENUE
        // =====================================================

        BigDecimal netRevenue = grossTicketSales.subtract(refundedAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal refundedPercentageOfGross = percentage(refundedAmount, grossTicketSales);


        // =====================================================
        // RESPONSE
        // =====================================================

        return EventAnalyticsResponse
                .builder()

                // Event
                .eventId(event.getId())
                .eventName(event.getName())
                .eventStatus(event.getStatus())
                .startTime(event.getStartTime())

                // Location
                .venueId(venue.getId())
                .venueName(venue.getName())
                .hallId(hall.getId())
                .hallName(hall.getName())

                // Seats
                .totalSeats(totalSeats)
                .availableSeats(availableSeats)
                .lockedSeats(lockedSeats)
                .bookedSeats(bookedSeats)
                .occupancyPercentage(occupancyPercentage)
                .lockedPercentage(lockedPercentage)
                .seatTypeBreakdown(seatTypeBreakdown)

                // Bookings
                .totalBookings(totalBookings)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .failedBookings(failedBookings)
                .expiredBookings(expiredBookings)

                // Booking rates
                .confirmationRate(confirmationRate)
                .cancellationRate(cancellationRate)
                .failureRate(failureRate)
                .expirationRate(expirationRate)

                // Payments
                .paymentAttempts(paymentAttempts)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .pendingPayments(pendingPayments)
                .paymentSuccessRate(paymentSuccessRate)
                .averageSuccessfulPaymentAmount(averageSuccessfulPaymentAmount)

                // Refunds
                .refundAttempts(refundAttempts)
                .successfulRefunds(successfulRefunds)
                .failedRefunds(failedRefunds)
                .refundSuccessRate(refundSuccessRate)

                // Revenue
                .grossTicketSales(grossTicketSales)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .refundedPercentageOfGross(refundedPercentageOfGross)
                .build();
    }

    // =========================================================
    // INITIALIZE SEAT MAP
    // =========================================================

    private Map<SeatType, Map<EventSeatStatus, Long>> createSeatCountMap() {

        Map<SeatType, Map<EventSeatStatus, Long>> result = new EnumMap<>(SeatType.class);


        for (SeatType seatType : SeatType.values()) {

            Map<EventSeatStatus, Long> statusMap = new EnumMap<>(EventSeatStatus.class);


            for (EventSeatStatus status : EventSeatStatus.values()) {

                statusMap.put(status, 0L);
            }

            result.put(seatType, statusMap);
        }

        return result;
    }

    // =========================================================
    // LONG NULL SAFETY
    // =========================================================

    private long safeLong(Long value) {

        return value == null ? 0L : value;
    }

    // =========================================================
    // MONEY ZERO
    // =========================================================

    private BigDecimal moneyZero() {

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // MONEY NULL SAFETY
    // =========================================================

    private BigDecimal safeMoney(BigDecimal value) {

        if (value == null) {

            return moneyZero();
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // LONG PERCENTAGE
    // =========================================================

    private BigDecimal percentage(long numerator, long denominator) {

        if (denominator == 0) {

            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }


        return BigDecimal
                .valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // MONEY PERCENTAGE
    // =========================================================

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {

        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {

            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }


        return safeMoney(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    // =========================================================
    // AVERAGE MONEY
    // =========================================================

    private BigDecimal averageMoney(BigDecimal total, long count) {

        if (count == 0) {

            return moneyZero();
        }

        return safeMoney(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}