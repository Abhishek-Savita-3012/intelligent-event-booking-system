package com.abhishek.eventbooking.config;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "booking.redis-seat-lock")
public class RedisSeatLockProperties {

    /**
     * Allows us to disable Redis coordination
     * without changing booking correctness.
     */
    private boolean enabled = true;


    /**
     * This is NOT the user's 5-minute booking reservation.
     *
     * It only protects the short database transaction.
     */
    private Duration ttl = Duration.ofSeconds(15);
}