package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.response.DashboardSummaryResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.BookingSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import com.abhishek.eventbooking.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public AdminDashboardService(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {

        // ------------------------------
        // BOOKING COUNTS
        // ------------------------------
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long failedBookings = bookingRepository.countByStatus(BookingStatus.FAILED);
        long expiredBookings = bookingRepository.countByStatus(BookingStatus.EXPIRED);

        // ------------------------------
        // PAYMENTS
        // ------------------------------
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        BigDecimal grossRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);

        // ------------------------------
        // REFUNDS
        // ------------------------------
        long successfulRefunds = refundRepository.countByStatus(RefundStatus.SUCCESS);
        BigDecimal refundedAmount = refundRepository.sumAmountByStatus(RefundStatus.SUCCESS);

        // ------------------------------
        // NET REVENUE
        // ------------------------------
        BigDecimal netRevenue = grossRevenue.subtract(refundedAmount);

        // ------------------------------
        // CURRENTLY SOLD SEATS
        // ------------------------------
        long currentlyBookedSeats = bookingSeatRepository.countSeatsByBookingStatus(BookingStatus.CONFIRMED);

        return DashboardSummaryResponse.builder()
                .totalBookings(totalBookings)
                .pendingBookings(pendingBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .failedBookings(failedBookings)
                .expiredBookings(expiredBookings)
                .successfulPayments(successfulPayments)
                .successfulRefunds(successfulRefunds)
                .grossRevenue(grossRevenue)
                .refundedAmount(refundedAmount)
                .netRevenue(netRevenue)
                .currentlyBookedSeats(currentlyBookedSeats)
                .build();
    }
}