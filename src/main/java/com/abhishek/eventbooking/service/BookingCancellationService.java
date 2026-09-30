package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.RefundSimulationRequest;
import com.abhishek.eventbooking.dto.response.RefundResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import com.abhishek.eventbooking.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingCancellationService {

    private final EventSeatRepository eventSeatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public BookingCancellationService(
            EventSeatRepository eventSeatRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository
    ) {
        this.eventSeatRepository = eventSeatRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    @Transactional
    public RefundResponse cancelBooking(String email, String bookingReference, RefundSimulationRequest request) {

        /*
         * Keep the lock order consistent with
         * payment processing:
         *
         * EventSeat rows first
         * Booking row second
         */

        List<EventSeat> eventSeats =
                eventSeatRepository.findByBookingReferenceThroughBookingSeatsForUpdate(bookingReference);

        Booking booking = bookingRepository.findByBookingReferenceForUpdate(bookingReference)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking not found: "
                                                + bookingReference
                                )
                        );

        validateBookingOwner(booking, email);
        validateCancellation(booking);
        validateBookedSeats(eventSeats);

        if (refundRepository.existsByBookingIdAndStatus(booking.getId(), RefundStatus.SUCCESS)) {

            throw new IllegalArgumentException(
                    "Booking has already been refunded"
            );
        }

        Payment successfulPayment = paymentRepository
                        .findTopByBookingIdAndStatusOrderByCreatedAtDesc(booking.getId(), PaymentStatus.SUCCESS)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Successful payment record not found"
                                )
                        );

        Refund refund;
        String message;

        /*
         * Simulated refund failure.
         *
         * Keep booking CONFIRMED and seats BOOKED.
         */
        if (request.getOutcome() == RefundOutcome.FAILED) {

            refund = createRefund(booking, successfulPayment, RefundStatus.FAILED);

            refund = refundRepository.save(refund);

            message = "Refund failed. Booking remains confirmed.";

            return mapToResponse(refund, booking, successfulPayment, message);
        }

        /*
         * Refund succeeded.
         *
         * Now cancellation becomes final.
         */

        booking.setStatus(BookingStatus.CANCELLED);

        for (EventSeat eventSeat : eventSeats) {

            eventSeat.setStatus(EventSeatStatus.AVAILABLE);
            eventSeat.setLockedByBooking(null);
            eventSeat.setLockedUntil(null);
        }

        refund = createRefund(booking, successfulPayment, RefundStatus.SUCCESS);

        Refund savedRefund = refundRepository.save(refund);

        bookingRepository.save(booking);

        eventSeatRepository.saveAll(eventSeats);

        message = "Booking cancelled successfully and refund completed";

        return mapToResponse(savedRefund, booking, successfulPayment, message);
    }

    private void validateBookingOwner(Booking booking, String email) {

        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {

            throw new IllegalArgumentException(
                    "You are not allowed to cancel this booking"
            );
        }
    }

    private void validateCancellation(Booking booking) {

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {

            throw new IllegalArgumentException(
                    "Expired booking cannot be cancelled"
            );
        }

        if (booking.getStatus() == BookingStatus.FAILED) {

            throw new IllegalArgumentException(
                    "Failed booking cannot be cancelled"
            );
        }

        if (booking.getStatus() == BookingStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Pending booking cannot be cancelled through refund flow"
            );
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "Only confirmed bookings can be cancelled"
            );
        }

        if (!booking.getEvent().getStartTime().isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Booking cannot be cancelled after the event has started"
            );
        }
    }

    private void validateBookedSeats(List<EventSeat> eventSeats) {

        if (eventSeats.isEmpty()) {
            throw new IllegalArgumentException(
                    "No seats found for this booking"
            );
        }

        boolean invalidSeat = eventSeats.stream()
                        .anyMatch(eventSeat ->
                                eventSeat.getStatus() != EventSeatStatus.BOOKED
                        );

        if (invalidSeat) {
            throw new IllegalArgumentException(
                    "One or more booking seats are not in BOOKED state"
            );
        }
    }

    private Refund createRefund(Booking booking, Payment payment, RefundStatus refundStatus) {

        return Refund.builder()
                .booking(booking)
                .payment(payment)
                .refundReference(generateRefundReference())
                .amount(booking.getTotalAmount())
                .status(refundStatus)
                .build();
    }

    private String generateRefundReference() {

        return "REF-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private RefundResponse mapToResponse(Refund refund, Booking booking, Payment payment, String message) {

        return RefundResponse.builder()
                .refundId(refund.getId())
                .refundReference(refund.getRefundReference())
                .bookingReference(booking.getBookingReference())
                .paymentReference(payment.getPaymentReference())
                .amount(refund.getAmount())
                .refundStatus(refund.getStatus())
                .bookingStatus(booking.getStatus())
                .processedAt(refund.getCreatedAt())
                .message(message)
                .build();
    }
}