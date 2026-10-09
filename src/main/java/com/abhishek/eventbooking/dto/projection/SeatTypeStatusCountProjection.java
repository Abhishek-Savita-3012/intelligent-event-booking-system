package com.abhishek.eventbooking.dto.projection;

import com.abhishek.eventbooking.entity.EventSeatStatus;
import com.abhishek.eventbooking.entity.SeatType;

public interface SeatTypeStatusCountProjection {

    SeatType getSeatType();

    EventSeatStatus getStatus();

    Long getCount();
}