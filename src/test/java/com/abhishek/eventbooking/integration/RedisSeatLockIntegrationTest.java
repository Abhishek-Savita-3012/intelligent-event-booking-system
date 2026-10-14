package com.abhishek.eventbooking.integration;

import com.abhishek.eventbooking.exception.ConflictException;

import com.abhishek.eventbooking.service.lock.RedisSeatLockHandle;
import com.abhishek.eventbooking.service.lock.RedisSeatLockService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisSeatLockIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    "redis:7-alpine"
            )
                    .withExposedPorts(
                            6379
                    );


    @DynamicPropertySource
    static void redisProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );


        registry.add(
                "spring.data.redis.port",
                () ->
                        REDIS.getMappedPort(
                                6379
                        )
        );


        registry.add(
                "booking.redis-seat-lock.enabled",
                () -> "true"
        );


        registry.add(
                "booking.redis-seat-lock.ttl",
                () -> "15s"
        );
    }


    @Autowired
    private RedisSeatLockService
            redisSeatLockService;


    // =========================================================
    // SAME SEAT CANNOT BE ACQUIRED TWICE
    // =========================================================

    @Test
    void sameSeat_shouldNotBeAcquiredByTwoRequests() {

        Optional<RedisSeatLockHandle>
                first =
                redisSeatLockService
                        .acquireAll(
                                List.of(
                                        101L
                                )
                        );


        assertTrue(
                first.isPresent()
        );


        assertThrows(
                ConflictException.class,
                () ->
                        redisSeatLockService
                                .acquireAll(
                                        List.of(
                                                101L
                                        )
                                )
        );


        redisSeatLockService.release(
                first.orElseThrow()
        );


        Optional<RedisSeatLockHandle>
                third =
                redisSeatLockService
                        .acquireAll(
                                List.of(
                                        101L
                                )
                        );


        assertTrue(
                third.isPresent()
        );


        redisSeatLockService.release(
                third.orElseThrow()
        );
    }


    // =========================================================
    // MULTI-SEAT PARTIAL FAILURE ROLLS BACK
    // =========================================================

    @Test
    void multiSeatFailure_shouldReleasePreviouslyAcquiredSeats() {

        Optional<RedisSeatLockHandle>
                existing =
                redisSeatLockService
                        .acquireAll(
                                List.of(
                                        202L
                                )
                        );


        assertTrue(
                existing.isPresent()
        );


        /*
         * Request attempts:
         *
         * 201 → can acquire
         * 202 → conflict
         *
         * RedisSeatLockService must release 201.
         */
        assertThrows(
                ConflictException.class,
                () ->
                        redisSeatLockService
                                .acquireAll(
                                        List.of(
                                                201L,
                                                202L
                                        )
                                )
        );


        /*
         * If 201 was cleaned up correctly,
         * this should now succeed.
         */
        Optional<RedisSeatLockHandle>
                seat201 =
                redisSeatLockService
                        .acquireAll(
                                List.of(
                                        201L
                                )
                        );


        assertTrue(
                seat201.isPresent()
        );


        redisSeatLockService.release(
                seat201.orElseThrow()
        );


        redisSeatLockService.release(
                existing.orElseThrow()
        );
    }
}