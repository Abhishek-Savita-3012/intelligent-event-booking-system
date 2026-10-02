package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.response.AdminBookingResponse;
import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.exception.BadRequestException;
import com.abhishek.eventbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.abhishek.eventbooking.dto.response.PagedResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    public AdminBookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminBookingResponse>
    getBookings(
            BookingStatus status,
            String search,
            int page,
            int size,
            String sortBy,
            String direction
    ) {

        validatePagination(page, size);
        String validatedSortField = validateSortField(sortBy);

        Sort.Direction sortDirection = parseSortDirection(direction);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                sortDirection,
                                validatedSortField
                        )
                );

        String normalizedSearch = search == null ? null : search.trim();

        Page<Booking> bookingPage = bookingRepository.searchAdminBookings(status, normalizedSearch, pageable);

        List<AdminBookingResponse> content = bookingPage
                        .getContent()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return new PagedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isFirst(),
                bookingPage.isLast()
        );
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {

            throw new BadRequestException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new BadRequestException(
                    "Page size must be between 1 and 100"
            );
        }
    }

    private String validateSortField(String sortBy) {

        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }

        return switch (sortBy) {
            case "id",
                 "bookingReference",
                 "totalAmount",
                 "status",
                 "createdAt"
                    -> sortBy;

            default ->
                    throw new BadRequestException(
                            "Invalid sort field: "
                                    + sortBy
                    );
        };
    }

    private Sort.Direction parseSortDirection(String direction) {

        if (direction == null || direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }

        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        }

        throw new BadRequestException(
                "Sort direction must be 'asc' or 'desc'"
        );
    }

    private AdminBookingResponse mapToResponse(Booking booking) {

        User user = booking.getUser();
        Event event = booking.getEvent();
        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        return AdminBookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())

                // User
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())

                // Event
                .eventId(event.getId())
                .eventName(event.getName())

                // Venue
                .venueName(venue.getName())
                .city(venue.getCity())
                .hallName(hall.getName())

                // Booking
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getExpiresAt())
                .build();
    }
}