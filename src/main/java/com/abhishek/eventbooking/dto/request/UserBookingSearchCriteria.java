package com.abhishek.eventbooking.dto.request;

import com.abhishek.eventbooking.entity.BookingStatus;

import lombok.Getter;
import lombok.Setter;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserBookingSearchCriteria {

    private BookingStatus status;

    private String search;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;


    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;

    private int page = 0;

    private int size = 10;

    private String sortBy = "createdAt";

    private String direction = "desc";
}