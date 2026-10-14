package com.abhishek.eventbooking.service.lock;

import java.util.List;

public record RedisSeatLockHandle(

        String ownerToken,

        List<Long> eventSeatIds

) {
}