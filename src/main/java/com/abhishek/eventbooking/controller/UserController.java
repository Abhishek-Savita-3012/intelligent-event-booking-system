package com.abhishek.eventbooking.controller;

import com.abhishek.eventbooking.dto.response.UserResponse;
import com.abhishek.eventbooking.entity.User;
import com.abhishek.eventbooking.repository.UserRepository;
import com.abhishek.eventbooking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {

        UserResponse response = userService.getCurrentUser(authentication.getName());

        return ResponseEntity.ok(response);
    }
}