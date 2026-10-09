package com.abhishek.eventbooking.dto.projection;

import com.abhishek.eventbooking.entity.BookingStatus;

public interface BookingStatusCountProjection {

    BookingStatus getStatus();

    Long getCount();
}