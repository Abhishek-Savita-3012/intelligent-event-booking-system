package com.abhishek.eventbooking.dto.response;

import com.abhishek.eventbooking.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {

    private Long id;

    private String name;

    private String email;

    private Role role;

    private String message;
}