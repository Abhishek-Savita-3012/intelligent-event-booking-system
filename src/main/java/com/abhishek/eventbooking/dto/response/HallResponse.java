package com.abhishek.eventbooking.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallResponse {

    private Long id;

    private String name;

    private Long venueId;

    private String venueName;
}