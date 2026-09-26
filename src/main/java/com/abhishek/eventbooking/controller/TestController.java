package com.abhishek.eventbooking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/user/test")
    public ResponseEntity<String> userTest() {

        return ResponseEntity.ok(
                "USER endpoint accessed successfully"
        );
    }

    @GetMapping("/admin/test")
    public ResponseEntity<String> adminTest() {

        return ResponseEntity.ok(
                "ADMIN endpoint accessed successfully"
        );
    }
}