package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.PaymentSimulationRequest;
import com.abhishek.eventbooking.dto.response.PaymentResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ForbiddenOperationException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.BookingRepository;
import com.abhishek.eventbooking.repository.EventSeatRepository;
import com.abhishek.eventbooking.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventSeatRepository eventSeatRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            EventSeatRepository eventSeatRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @Transactional
    public PaymentResponse simulatePayment(String email, String bookingReference, PaymentSimulationRequest request) {

        log.info(
                "Payment simulation started bookingReference={} outcome={}",
                bookingReference,
                request.getOutcome()
        );

        /*
         * Lock the EventSeat rows first.
         *
         * Our expiration scheduler also locks
         * EventSeat rows, so using a consistent
         * locking strategy reduces race conditions.
         */
        List<EventSeat> eventSeats = eventSeatRepository.findByBookingReferenceForUpdate(bookingReference);

        /*
         * Lock the Booking row.
         */
        Booking booking = bookingRepository.findByBookingReferenceForUpdate(bookingReference)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Booking not found: "
                                                + bookingReference
                                )
                        );

        validateBookingOwner(booking, email);

        validateBookingForPayment(booking);

        validateSeatReservation(booking, eventSeats);

        PaymentStatus paymentStatus;

        String message;

        if (request.getOutcome() == PaymentOutcome.SUCCESS) {
            handleSuccessfulPayment(booking, eventSeats);
            paymentStatus = PaymentStatus.SUCCESS;
            message = "Payment successful and booking confirmed";

        } else {

            handleFailedPayment(booking, eventSeats);
            paymentStatus = PaymentStatus.FAILED;
            message = "Payment failed and reserved seats were released";

            log.warn(
                    "Payment failed bookingReference={} amount={} seatsReleased={}",
                    booking.getBookingReference(),
                    booking.getTotalAmount(),
                    eventSeats.size()
            );
        }

        Payment payment = Payment.builder()
                        .booking(booking)
                        .paymentReference(generatePaymentReference())
                        .amount(booking.getTotalAmount())
                        .status(paymentStatus)
                        .build();

        Payment savedPayment = paymentRepository.save(payment);
        bookingRepository.save(booking);
        eventSeatRepository.saveAll(eventSeats);

        log.info(
                "Payment successful bookingReference={} paymentReference={} amount={} seatCount={}",
                booking.getBookingReference(),
                savedPayment.getPaymentReference(),
                savedPayment.getAmount(),
                eventSeats.size()
        );

        return mapToResponse(savedPayment, booking, message);
    }

    private void validateBookingOwner(Booking booking, String email) {

        if (!booking.getUser().getEmail().equalsIgnoreCase(email)) {

            log.warn(
                    "Booking ownership check failed bookingReference={}",
                    booking.getBookingReference()
            );

            throw new ForbiddenOperationException(
                    "You are not allowed to pay for this booking"
            );
        }
    }

    private void validateBookingForPayment(Booking booking) {

        if (booking.getStatus() == BookingStatus.EXPIRED) {

            log.warn(
                    "Payment rejected because booking expired bookingReference={} expiresAt={}",
                    booking.getBookingReference(),
                    booking.getExpiresAt()
            );

            throw new ConflictException(
                    "Booking reservation has expired"
            );
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {

            throw new ConflictException(
                    "Booking is already confirmed"
            );
        }

        if (booking.getStatus() == BookingStatus.FAILED) {

            throw new ConflictException(
                    "Booking payment has already failed"
            );
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            throw new ConflictException(
                    "Cancelled booking cannot be paid"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {

            throw new ConflictException(
                    "Booking is not eligible for payment"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(now)) {

            throw new ConflictException(
                    "Booking reservation has expired"
            );
        }
    }

    private void validateSeatReservation(Booking booking, List<EventSeat> eventSeats) {

        if (eventSeats.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No active seat reservation found for this booking"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        boolean invalidReservation =
                eventSeats.stream()
                        .anyMatch(eventSeat ->

                                eventSeat.getStatus() != EventSeatStatus.LOCKED
                                        || eventSeat.getLockedByBooking() == null
                                        || !eventSeat
                                                .getLockedByBooking()
                                                .getId()
                                                .equals(booking.getId())
                                        || eventSeat.getLockedUntil() == null
                                        || !eventSeat.getLockedUntil().isAfter(now)
                        );

        if (invalidReservation) {

            throw new ConflictException(
                    "Seat reservation is no longer valid"
            );
        }
    }

    private void handleSuccessfulPayment(Booking booking, List<EventSeat> eventSeats) {

        booking.setStatus(BookingStatus.CONFIRMED);

        for (EventSeat eventSeat : eventSeats) {
            eventSeat.setStatus(EventSeatStatus.BOOKED);
            eventSeat.setLockedByBooking(null);
            eventSeat.setLockedUntil(null);
        }
    }

    private void handleFailedPayment(Booking booking, List<EventSeat> eventSeats) {

        booking.setStatus(BookingStatus.FAILED);

        for (EventSeat eventSeat : eventSeats) {
            eventSeat.setStatus(EventSeatStatus.AVAILABLE);
            eventSeat.setLockedByBooking(null);
            eventSeat.setLockedUntil(null);
        }
    }

    private String generatePaymentReference() {

        return "PAY-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    private PaymentResponse mapToResponse(Payment payment, Booking booking, String message) {

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .bookingReference(booking.getBookingReference())
                .amount(payment.getAmount())
                .paymentStatus(payment.getStatus())
                .bookingStatus(booking.getStatus())
                .processedAt(payment.getCreatedAt())
                .message(message)
                .build();
    }
}