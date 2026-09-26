package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.EventCategory;
import com.abhishek.eventbooking.entity.EventStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;

    private String name;

    private String description;

    private EventCategory category;

    private Long venueId;

    private String venueName;

    private String city;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private EventStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}