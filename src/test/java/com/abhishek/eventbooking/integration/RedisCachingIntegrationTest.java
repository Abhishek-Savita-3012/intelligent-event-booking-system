package com.abhishek.eventbooking.integration;

import com.abhishek.eventbooking.config.CacheNames;

import com.abhishek.eventbooking.dto.response.EventResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.repository.EventRepository;
import com.abhishek.eventbooking.repository.HallRepository;
import com.abhishek.eventbooking.repository.VenueRepository;

import com.abhishek.eventbooking.service.EventService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisCachingIntegrationTest {

    // =========================================================
    // REDIS CONTAINER
    // =========================================================

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    "redis:7-alpine"
            )
                    .withExposedPorts(
                            6379
                    );


    // =========================================================
    // OVERRIDE TEST CACHE CONFIG
    // =========================================================

    @DynamicPropertySource
    static void redisProperties(
            DynamicPropertyRegistry registry
    ) {

        /*
         * application-test.properties says cache=none.
         *
         * This dedicated test overrides it back to Redis.
         */
        registry.add(
                "spring.cache.type",
                () -> "redis"
        );


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
    }


    // =========================================================
    // DEPENDENCIES
    // =========================================================

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;


    // =========================================================
    // TEST DATA
    // =========================================================

    private Event event;


    @BeforeEach
    void setUp() {

        Cache eventDetailsCache =
                cacheManager.getCache(
                        CacheNames.EVENT_DETAILS
                );


        assertNotNull(
                eventDetailsCache
        );


        eventDetailsCache.clear();


        // -----------------------------------------------------
        // Venue
        // -----------------------------------------------------

        Venue venue =
                venueRepository.saveAndFlush(
                        Venue.builder()
                                .name(
                                        "Redis Test Venue"
                                )
                                .city(
                                        "Lucknow"
                                )
                                .address(
                                        "Redis Test Address"
                                )
                                .build()
                );


        // -----------------------------------------------------
        // Hall
        // -----------------------------------------------------

        Hall hall =
                hallRepository.saveAndFlush(
                        Hall.builder()
                                .name(
                                        "Redis Screen"
                                )
                                .venue(
                                        venue
                                )
                                .build()
                );


        // -----------------------------------------------------
        // Event
        // -----------------------------------------------------

        LocalDateTime start =
                LocalDateTime.now()
                        .plusDays(2);


        event =
                eventRepository.saveAndFlush(
                        Event.builder()

                                .name(
                                        "Redis Cached Movie"
                                )

                                .description(
                                        "Redis cache integration test"
                                )

                                .category(
                                        EventCategory.MOVIE
                                )

                                .startTime(
                                        start
                                )

                                .endTime(
                                        start.plusHours(3)
                                )

                                .status(
                                        EventStatus.UPCOMING
                                )

                                .hall(
                                        hall
                                )

                                .build()
                );
    }


    // =========================================================
    // CACHE HIT TEST
    // =========================================================

    @Test
    void getEventById_shouldReturnCachedValueUntilEvicted() {

        // =====================================================
        // FIRST CALL
        //
        // Redis MISS → database
        // =====================================================

        EventResponse firstResponse =
                eventService.getEventById(
                        event.getId()
                );


        assertEquals(
                "Redis Cached Movie",
                firstResponse.getName()
        );


        // =====================================================
        // VERIFY REDIS KEY
        // =====================================================

        String redisKey =
                "eventbooking::"
                        + CacheNames.EVENT_DETAILS
                        + "::"
                        + event.getId();


        assertTrue(
                Boolean.TRUE.equals(
                        redisTemplate.hasKey(
                                redisKey
                        )
                )
        );


        // =====================================================
        // CHANGE DATABASE DIRECTLY
        //
        // Do NOT go through the normal Event update service,
        // because that would correctly evict the cache.
        // =====================================================

        Event dbEvent =
                eventRepository
                        .findById(
                                event.getId()
                        )
                        .orElseThrow();


        dbEvent.setName(
                "Changed Directly In Database"
        );


        eventRepository.saveAndFlush(
                dbEvent
        );


        // =====================================================
        // SECOND CALL
        //
        // Should still return OLD cached value.
        // =====================================================

        EventResponse cachedResponse =
                eventService.getEventById(
                        event.getId()
                );


        assertEquals(
                "Redis Cached Movie",
                cachedResponse.getName()
        );


        // =====================================================
        // EVICT
        // =====================================================

        Cache eventDetailsCache =
                cacheManager.getCache(
                        CacheNames.EVENT_DETAILS
                );


        assertNotNull(
                eventDetailsCache
        );


        eventDetailsCache.evict(
                event.getId()
        );


        // =====================================================
        // THIRD CALL
        //
        // Redis MISS again → fresh database value.
        // =====================================================

        EventResponse freshResponse =
                eventService.getEventById(
                        event.getId()
                );


        assertEquals(
                "Changed Directly In Database",
                freshResponse.getName()
        );
    }
}