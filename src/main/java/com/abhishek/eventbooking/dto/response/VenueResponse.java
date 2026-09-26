package com.abhishek.eventbooking.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponse {

    private Long id;

    private String name;

    private String city;

    private String address;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}