package com.abhishek.eventbooking.dto.request;

import com.abhishek.eventbooking.entity.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventRequest {

    @NotBlank(message = "Event name is required")
    @Size(
            max = 200,
            message = "Event name is too long"
    )
    private String name;

    @NotBlank(message = "Description is required")
    @Size(
            max = 1000,
            message = "Description is too long"
    )
    private String description;

    @NotNull(message = "Category is required")
    private EventCategory category;

    @NotNull(message = "Venue id is required")
    private Long venueId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
}