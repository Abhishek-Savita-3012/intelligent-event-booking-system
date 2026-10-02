package com.abhishek.eventbooking.service;

import com.abhishek.eventbooking.dto.request.SeatRequest;
import com.abhishek.eventbooking.dto.response.SeatResponse;
import com.abhishek.eventbooking.entity.Hall;
import com.abhishek.eventbooking.entity.Seat;
import com.abhishek.eventbooking.entity.Venue;
import com.abhishek.eventbooking.exception.ConflictException;
import com.abhishek.eventbooking.exception.ResourceNotFoundException;
import com.abhishek.eventbooking.repository.HallRepository;
import com.abhishek.eventbooking.repository.SeatRepository;
import com.abhishek.eventbooking.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final HallRepository hallRepository;

    public SeatService(SeatRepository seatRepository, HallRepository hallRepository) {
        this.seatRepository = seatRepository;
        this.hallRepository = hallRepository;
    }

    public SeatResponse createSeat(Long hallId, SeatRequest request) {

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Hall not found with id: "
                                        + hallId
                        )
                );

        String rowName = request.getRowName().trim().toUpperCase();

        if (seatRepository.existsByHallIdAndRowNameAndSeatNumber(
                        hallId,
                        rowName,
                        request.getSeatNumber()
                )) {

            throw new ConflictException(
                    "Seat already exists: "
                            + rowName
                            + request.getSeatNumber()
            );
        }

        Seat seat = Seat.builder()
                .hall(hall)
                .rowName(rowName)
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType())
                .build();

        return mapToResponse(seatRepository.save(seat));
    }

    public List<SeatResponse> getSeatsByHall(Long hallId) {

        if (!hallRepository.existsById(hallId)) {
            throw new ResourceNotFoundException(
                    "Hall not found with id: " + hallId
            );
        }

        return seatRepository.findByHallIdOrderByRowNameAscSeatNumberAsc(hallId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SeatResponse mapToResponse(Seat seat) {

        return SeatResponse.builder()
                .id(seat.getId())
                .hallId(seat.getHall().getId())
                .hallName(seat.getHall().getName())
                .venueId(seat.getHall().getVenue().getId())
                .rowName(seat.getRowName())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .build();
    }
}