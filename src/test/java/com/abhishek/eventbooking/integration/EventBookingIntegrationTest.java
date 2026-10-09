package com.abhishek.eventbooking.integration;

import com.abhishek.eventbooking.entity.*;
import com.abhishek.eventbooking.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventBookingIntegrationTest {

    // =========================================================
    // SPRING / HTTP
    // =========================================================

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    // =========================================================
    // REPOSITORIES
    // =========================================================

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;


    // =========================================================
    // TEST DATA
    // =========================================================

    private Venue venue;

    private Hall hall;

    private Seat seat;

    private Event event;

    private EventSeat eventSeat;

    private Long eventId;

    private Long eventSeatId;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        // -----------------------------------------------------
        // VENUE
        // -----------------------------------------------------

        venue =
                venueRepository.saveAndFlush(
                        Venue.builder()

                                .name(
                                        "Integration Test Venue"
                                )

                                .city(
                                        "Lucknow"
                                )

                                .address(
                                        "Integration Test Address"
                                )

                                .build()
                );


        // -----------------------------------------------------
        // HALL
        // -----------------------------------------------------

        hall =
                hallRepository.saveAndFlush(
                        Hall.builder()

                                .name(
                                        "Screen 1"
                                )

                                .venue(
                                        venue
                                )

                                .build()
                );


        // -----------------------------------------------------
        // PHYSICAL SEAT
        // -----------------------------------------------------

        seat =
                seatRepository.saveAndFlush(
                        Seat.builder()

                                .hall(
                                        hall
                                )

                                .rowName(
                                        "A"
                                )

                                .seatNumber(
                                        1
                                )

                                .seatType(
                                        SeatType.REGULAR
                                )

                                .build()
                );


        // -----------------------------------------------------
        // EVENT
        // -----------------------------------------------------

        LocalDateTime startTime =
                LocalDateTime.now()
                        .plusDays(2);


        event =
                eventRepository.saveAndFlush(
                        Event.builder()

                                .name(
                                        "Integration Test Movie"
                                )

                                .description(
                                        "Integration test event"
                                )

                                .category(
                                        EventCategory.MOVIE
                                )

                                .startTime(
                                        startTime
                                )

                                .endTime(
                                        startTime.plusHours(3)
                                )

                                .status(
                                        EventStatus.UPCOMING
                                )

                                .hall(
                                        hall
                                )

                                .build()
                );


        eventId =
                event.getId();


        // -----------------------------------------------------
        // EVENT SEAT
        // -----------------------------------------------------

        eventSeat =
                eventSeatRepository.saveAndFlush(
                        EventSeat.builder()

                                .event(
                                        event
                                )

                                .seat(
                                        seat
                                )

                                .price(
                                        new BigDecimal(
                                                "250.00"
                                        )
                                )

                                .status(
                                        EventSeatStatus.AVAILABLE
                                )

                                .build()
                );


        eventSeatId =
                eventSeat.getId();
    }


    // =========================================================
    // AUTH TESTS
    // =========================================================

    @Test
    void register_withValidData_shouldReturn201()
            throws Exception {

        String body =
                """
                {
                  "name": "Abhishek Test",
                  "email": "register-test@example.com",
                  "password": "Password@123"
                }
                """;


        mockMvc.perform(
                        post(
                                "/api/auth/register"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        body
                                )
                )

                .andExpect(
                        status().isCreated()
                )

                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "register-test@example.com"
                                )
                )

                /*
                 * Password must never be returned.
                 */
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                );
    }


    @Test
    void register_withInvalidData_shouldReturn400()
            throws Exception {

        String body =
                """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": ""
                }
                """;


        mockMvc.perform(
                        post(
                                "/api/auth/register"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        body
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }


    // =========================================================
    // PUBLIC EVENT TEST
    // =========================================================

    @Test
    void getEvents_withoutJwt_shouldReturn200()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/events"
                        )
                )

                .andExpect(
                        status().isOk()
                );
    }


    // =========================================================
    // GLOBAL EXCEPTION HANDLING
    // =========================================================

    @Test
    void getEvent_whenEventDoesNotExist_shouldReturn404()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/events/999999"
                        )
                )

                .andExpect(
                        status().isNotFound()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }


    // =========================================================
    // SECURITY
    // =========================================================

    @Test
    void getMyBookings_withoutJwt_shouldReturn401()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/bookings/my"
                        )
                )

                .andExpect(
                        status().isUnauthorized()
                );
    }


    @Test
    void adminEndpoint_withUserJwt_shouldReturn403()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "normal-user@example.com"
                );


        mockMvc.perform(
                        get(
                                "/api/admin/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isForbidden()
                );
    }


    @Test
    void adminBookings_withAdminJwt_shouldReturn200()
            throws Exception {

        String token =
                registerAndLoginAdmin(
                        "integration-admin@example.com"
                );


        mockMvc.perform(
                        get(
                                "/api/admin/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "page",
                                        "0"
                                )

                                .param(
                                        "size",
                                        "10"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                )

                .andExpect(
                        jsonPath("$.pageNumber")
                                .value(0)
                );
    }


    // =========================================================
    // LESSON 31
    //
    // MISSING IDEMPOTENCY KEY
    // =========================================================

    @Test
    void createBooking_withoutIdempotencyKey_shouldReturn400()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "missing-idempotency@example.com"
                );


        String bookingBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        mockMvc.perform(
                        post(
                                "/api/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        bookingBody
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )

                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Idempotency-Key header is required"
                                )
                );


        assertEquals(
                0,
                bookingRepository.count()
        );
    }


    // =========================================================
    // VALID BOOKING
    // =========================================================

    @Test
    void createBooking_withAvailableSeat_shouldReturn201AndLockSeat()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "booking-user@example.com"
                );


        String bookingBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/bookings"
                                )

                                        .header(
                                                "Authorization",
                                                "Bearer " + token
                                        )

                                        // LESSON 31
                                        .header(
                                                "Idempotency-Key",
                                                "integration-booking-001"
                                        )

                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )

                                        .content(
                                                bookingBody
                                        )
                        )

                        .andExpect(
                                status().isCreated()
                        )

                        .andExpect(
                                jsonPath("$.status")
                                        .value(
                                                "PENDING"
                                        )
                        )

                        .andExpect(
                                jsonPath("$.totalAmount")
                                        .value(
                                                250.00
                                        )
                        )

                        .andExpect(
                                jsonPath("$.bookingReference")
                                        .exists()
                        )

                        .andExpect(
                                jsonPath("$.expiresAt")
                                        .exists()
                        )

                        .andReturn();


        JsonNode response =
                objectMapper.readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                );


        String bookingReference =
                response
                        .get(
                                "bookingReference"
                        )
                        .asText();


        assertNotNull(
                bookingReference
        );


        // -----------------------------------------------------
        // DATABASE ASSERTIONS
        // -----------------------------------------------------

        assertEquals(
                1,
                bookingRepository.count()
        );


        assertEquals(
                1,
                bookingSeatRepository.count()
        );


        Booking savedBooking =
                bookingRepository
                        .findAll()
                        .get(0);


        assertEquals(
                BookingStatus.PENDING,
                savedBooking.getStatus()
        );


        assertEquals(
                "integration-booking-001",
                savedBooking.getIdempotencyKey()
        );


        assertNotNull(
                savedBooking.getRequestFingerprint()
        );


        assertEquals(
                64,
                savedBooking
                        .getRequestFingerprint()
                        .length()
        );


        assertNotNull(
                savedBooking.getExpiresAt()
        );


        EventSeat updatedSeat =
                eventSeatRepository
                        .findById(
                                eventSeatId
                        )
                        .orElseThrow();


        assertEquals(
                EventSeatStatus.LOCKED,
                updatedSeat.getStatus()
        );


        assertNotNull(
                updatedSeat.getLockedUntil()
        );


        assertNotNull(
                updatedSeat.getLockedByBooking()
        );
    }


    // =========================================================
    // LESSON 31
    //
    // SAME IDEMPOTENCY KEY + SAME REQUEST
    // =========================================================

    @Test
    void createBooking_sameIdempotencyKeySameRequest_shouldReturnSameBooking()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "idempotency-replay@example.com"
                );


        String bookingBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        String idempotencyKey =
                "integration-idempotency-replay-001";


        // =====================================================
        // FIRST REQUEST
        // =====================================================

        MvcResult firstResult =
                mockMvc.perform(
                                post(
                                        "/api/bookings"
                                )

                                        .header(
                                                "Authorization",
                                                "Bearer " + token
                                        )

                                        .header(
                                                "Idempotency-Key",
                                                idempotencyKey
                                        )

                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )

                                        .content(
                                                bookingBody
                                        )
                        )

                        .andExpect(
                                status().isCreated()
                        )

                        .andReturn();


        JsonNode firstJson =
                objectMapper.readTree(
                        firstResult
                                .getResponse()
                                .getContentAsString()
                );


        String firstReference =
                firstJson
                        .get(
                                "bookingReference"
                        )
                        .asText();


        // =====================================================
        // RETRY
        //
        // SAME KEY + SAME BODY
        // =====================================================

        MvcResult secondResult =
                mockMvc.perform(
                                post(
                                        "/api/bookings"
                                )

                                        .header(
                                                "Authorization",
                                                "Bearer " + token
                                        )

                                        .header(
                                                "Idempotency-Key",
                                                idempotencyKey
                                        )

                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )

                                        .content(
                                                bookingBody
                                        )
                        )

                        .andExpect(
                                status().isCreated()
                        )

                        .andReturn();


        JsonNode secondJson =
                objectMapper.readTree(
                        secondResult
                                .getResponse()
                                .getContentAsString()
                );


        String secondReference =
                secondJson
                        .get(
                                "bookingReference"
                        )
                        .asText();


        // =====================================================
        // IDEMPOTENCY ASSERTIONS
        // =====================================================

        assertEquals(
                firstReference,
                secondReference
        );


        /*
         * Critical:
         *
         * Two HTTP calls,
         * but only ONE business Booking.
         */
        assertEquals(
                1,
                bookingRepository.count()
        );


        assertEquals(
                1,
                bookingSeatRepository.count()
        );


        Booking booking =
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                getUserId(
                                        "idempotency-replay@example.com"
                                ),
                                idempotencyKey
                        )
                        .orElseThrow();


        assertEquals(
                firstReference,
                booking.getBookingReference()
        );


        assertNotNull(
                booking.getRequestFingerprint()
        );


        assertEquals(
                64,
                booking
                        .getRequestFingerprint()
                        .length()
        );
    }


    // =========================================================
    // LESSON 31
    //
    // SAME KEY + DIFFERENT REQUEST
    // =========================================================

    @Test
    void createBooking_sameIdempotencyKeyDifferentRequest_shouldReturn409()
            throws Exception {

        String email =
                "idempotency-conflict@example.com";


        String token =
                registerAndLoginUser(
                        email
                );


        String idempotencyKey =
                "integration-idempotency-conflict-001";


        String originalBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        // =====================================================
        // FIRST REQUEST
        // =====================================================

        mockMvc.perform(
                        post(
                                "/api/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        originalBody
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        // =====================================================
        // SAME KEY
        // DIFFERENT EVENT ID
        // =====================================================

        String differentBody =
                createBookingBody(
                        999999L,
                        eventSeatId
                );


        mockMvc.perform(
                        post(
                                "/api/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        differentBody
                                )
                )

                .andExpect(
                        status().isConflict()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )

                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Idempotency-Key has already been used for a different booking request"
                                )
                );


        /*
         * Still only one booking.
         */
        assertEquals(
                1,
                bookingRepository.count()
        );
    }


    // =========================================================
    // NEW KEY + SAME LOCKED SEAT
    //
    // THIS IS NOT AN IDEMPOTENCY REPLAY
    // =========================================================

    @Test
    void createBooking_newIdempotencyKeyForLockedSeat_shouldReturn409()
            throws Exception {

        String firstToken =
                registerAndLoginUser(
                        "first-seat-user@example.com"
                );


        String secondToken =
                registerAndLoginUser(
                        "second-seat-user@example.com"
                );


        String bookingBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        // -----------------------------------------------------
        // USER 1 LOCKS SEAT
        // -----------------------------------------------------

        mockMvc.perform(
                        post(
                                "/api/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + firstToken
                                )

                                .header(
                                        "Idempotency-Key",
                                        "first-user-key"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        bookingBody
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        // -----------------------------------------------------
        // USER 2 TRIES SAME SEAT
        // -----------------------------------------------------

        mockMvc.perform(
                        post(
                                "/api/bookings"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + secondToken
                                )

                                .header(
                                        "Idempotency-Key",
                                        "second-user-key"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        bookingBody
                                )
                )

                .andExpect(
                        status().isConflict()
                );


        assertEquals(
                1,
                bookingRepository.count()
        );


        assertEquals(
                1,
                bookingSeatRepository.count()
        );
    }


    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    @Test
    void simulateSuccessfulPayment_shouldConfirmBookingAndBookSeat()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "payment-user@example.com"
                );


        String bookingBody =
                createBookingBody(
                        eventId,
                        eventSeatId
                );


        // -----------------------------------------------------
        // CREATE BOOKING
        // -----------------------------------------------------

        MvcResult bookingResult =
                mockMvc.perform(
                                post(
                                        "/api/bookings"
                                )

                                        .header(
                                                "Authorization",
                                                "Bearer " + token
                                        )

                                        .header(
                                                "Idempotency-Key",
                                                "payment-test-booking-key"
                                        )

                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )

                                        .content(
                                                bookingBody
                                        )
                        )

                        .andExpect(
                                status().isCreated()
                        )

                        .andReturn();


        JsonNode bookingJson =
                objectMapper.readTree(
                        bookingResult
                                .getResponse()
                                .getContentAsString()
                );


        String bookingReference =
                bookingJson
                        .get(
                                "bookingReference"
                        )
                        .asText();


        // -----------------------------------------------------
        // PAY
        // -----------------------------------------------------

        String paymentBody =
                """
                {
                  "outcome": "SUCCESS"
                }
                """;


        mockMvc.perform(
                        post(
                                "/api/bookings/"
                                        + bookingReference
                                        + "/payments/simulate"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        paymentBody
                                )
                )

                .andExpect(
                        status().isOk()
                );


        // -----------------------------------------------------
        // DATABASE ASSERTIONS
        // -----------------------------------------------------

        Booking booking =
                bookingRepository
                        .findAll()
                        .get(0);


        assertEquals(
                BookingStatus.CONFIRMED,
                booking.getStatus()
        );


        EventSeat updatedSeat =
                eventSeatRepository
                        .findById(
                                eventSeatId
                        )
                        .orElseThrow();


        assertEquals(
                EventSeatStatus.BOOKED,
                updatedSeat.getStatus()
        );


        assertNull(
                updatedSeat.getLockedByBooking()
        );


        assertNull(
                updatedSeat.getLockedUntil()
        );


        assertEquals(
                1,
                paymentRepository.count()
        );
    }


    // =========================================================
    // LESSON 29
    //
    // EVENT ANALYTICS
    // =========================================================

    @Test
    void eventAnalytics_withAdminJwt_shouldReturn200()
            throws Exception {

        String adminToken =
                registerAndLoginAdmin(
                        "analytics-admin@example.com"
                );


        mockMvc.perform(
                        get(
                                "/api/admin/events/"
                                        + eventId
                                        + "/analytics"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.eventId")
                                .value(
                                        eventId
                                )
                )

                .andExpect(
                        jsonPath("$.totalSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.lockedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.bookedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.occupancyPercentage")
                                .value(0.00)
                )

                .andExpect(
                        jsonPath("$.grossTicketSales")
                                .value(0.00)
                )

                .andExpect(
                        jsonPath("$.refundedAmount")
                                .value(0.00)
                )

                .andExpect(
                        jsonPath("$.netRevenue")
                                .value(0.00)
                );
    }


    @Test
    void eventAnalytics_withUserJwt_shouldReturn403()
            throws Exception {

        String token =
                registerAndLoginUser(
                        "analytics-user@example.com"
                );


        mockMvc.perform(
                        get(
                                "/api/admin/events/"
                                        + eventId
                                        + "/analytics"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isForbidden()
                );
    }


    @Test
    void eventAnalytics_withoutJwt_shouldReturn401()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/events/"
                                        + eventId
                                        + "/analytics"
                        )
                )

                .andExpect(
                        status().isUnauthorized()
                );
    }


    // =========================================================
    // LESSON 30
    //
    // PUBLIC SEAT MAP
    // =========================================================

    @Test
    void getSeatMap_existingEvent_shouldReturnGroupedSeatMap()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/events/"
                                        + eventId
                                        + "/seat-map"
                        )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.eventId")
                                .value(
                                        eventId
                                )
                )

                .andExpect(
                        jsonPath("$.eventName")
                                .value(
                                        "Integration Test Movie"
                                )
                )

                .andExpect(
                        jsonPath("$.totalSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.lockedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.bookedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.rows.length()")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.rows[0].rowName")
                                .value("A")
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats.length()"
                        )
                                .value(1)
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].eventSeatId"
                        )
                                .value(
                                        eventSeatId
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].seatNumber"
                        )
                                .value(1)
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].seatType"
                        )
                                .value(
                                        "REGULAR"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].status"
                        )
                                .value(
                                        "AVAILABLE"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].selectable"
                        )
                                .value(true)
                );
    }


    @Test
    void getSeatMap_missingEvent_shouldReturn404()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/events/999999/seat-map"
                        )
                )

                .andExpect(
                        status().isNotFound()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }


    @Test
    void getSeatMap_bookedSeat_shouldMarkSeatNotSelectable()
            throws Exception {

        EventSeat currentSeat =
                eventSeatRepository
                        .findById(
                                eventSeatId
                        )
                        .orElseThrow();


        currentSeat.setStatus(
                EventSeatStatus.BOOKED
        );


        eventSeatRepository.saveAndFlush(
                currentSeat
        );


        mockMvc.perform(
                        get(
                                "/api/events/"
                                        + eventId
                                        + "/seat-map"
                        )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.bookedSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].status"
                        )
                                .value(
                                        "BOOKED"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].selectable"
                        )
                                .value(false)
                );
    }


    @Test
    void getSeatMap_expiredLock_shouldDisplaySeatAsAvailable()
            throws Exception {

        EventSeat currentSeat =
                eventSeatRepository
                        .findById(
                                eventSeatId
                        )
                        .orElseThrow();


        currentSeat.setStatus(
                EventSeatStatus.LOCKED
        );


        currentSeat.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );


        eventSeatRepository.saveAndFlush(
                currentSeat
        );


        mockMvc.perform(
                        get(
                                "/api/events/"
                                        + eventId
                                        + "/seat-map"
                        )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.lockedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].status"
                        )
                                .value(
                                        "AVAILABLE"
                                )
                )

                .andExpect(
                        jsonPath(
                                "$.rows[0].seats[0].selectable"
                        )
                                .value(true)
                );
    }


    // =========================================================
    // REQUEST-ID FILTER
    // =========================================================

    @Test
    void requestWithoutRequestId_shouldGenerateRequestId()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/events"
                        )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        header()
                                .exists(
                                        "X-Request-ID"
                                )
                );
    }


    @Test
    void requestWithRequestId_shouldPreserveRequestId()
            throws Exception {

        String requestId =
                "integration-request-123";


        mockMvc.perform(
                        get(
                                "/api/events"
                        )

                                .header(
                                        "X-Request-ID",
                                        requestId
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        header()
                                .string(
                                        "X-Request-ID",
                                        requestId
                                )
                );
    }


    // =========================================================
    // HELPER:
    // REGISTER + LOGIN NORMAL USER
    // =========================================================

    private String registerAndLoginUser(
            String email
    ) throws Exception {

        registerUser(
                "Integration User",
                email
        );


        return loginUser(
                email
        );
    }


    // =========================================================
    // HELPER:
    // REGISTER + MAKE ADMIN + LOGIN
    // =========================================================

    private String registerAndLoginAdmin(
            String email
    ) throws Exception {

        registerUser(
                "Integration Admin",
                email
        );


        User user =
                userRepository
                        .findByEmail(
                                email
                        )
                        .orElseThrow();


        user.setRole(
                Role.ADMIN
        );


        userRepository.saveAndFlush(
                user
        );


        return loginUser(
                email
        );
    }


    // =========================================================
    // HELPER:
    // REGISTER
    // =========================================================

    private void registerUser(
            String name,
            String email
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "name",
                                name,

                                "email",
                                email,

                                "password",
                                "Password@123"
                        )
                );


        mockMvc.perform(
                        post(
                                "/api/auth/register"
                        )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        requestBody
                                )
                )

                .andExpect(
                        status().isCreated()
                );
    }


    // =========================================================
    // HELPER:
    // LOGIN
    // =========================================================

    private String loginUser(
            String email
    ) throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "email",
                                email,

                                "password",
                                "Password@123"
                        )
                );


        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/auth/login"
                                )

                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )

                                        .content(
                                                requestBody
                                        )
                        )

                        .andExpect(
                                status().isOk()
                        )

                        .andExpect(
                                jsonPath("$.token")
                                        .exists()
                        )

                        .andReturn();


        JsonNode response =
                objectMapper.readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                );


        return response
                .get(
                        "token"
                )
                .asText();
    }


    // =========================================================
    // HELPER:
    // CREATE BOOKING JSON
    // =========================================================

    private String createBookingBody(
            Long eventId,
            Long eventSeatId
    ) throws Exception {

        return objectMapper
                .writeValueAsString(
                        Map.of(
                                "eventId",
                                eventId,

                                "eventSeatIds",
                                new Long[]{
                                        eventSeatId
                                }
                        )
                );
    }


    // =========================================================
    // HELPER:
    // USER ID
    // =========================================================

    private Long getUserId(
            String email
    ) {

        return userRepository
                .findByEmail(
                        email
                )
                .orElseThrow()
                .getId();
    }

    @Test
    void getEvents_filterByCity_shouldReturnMatchingEvents() throws Exception {

        mockMvc.perform(
                        get("/api/events")
                                .param("city", "Lucknow")
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$").isArray())

                .andExpect(jsonPath("$[0].id").value(eventId));
    }

    @Test
    void getEvents_filterByCategoryAndStatus_shouldReturnMatchingEvents() throws Exception {

        mockMvc.perform(
                        get("/api/events")

                                .param("category", "MOVIE")
                                .param("status", "UPCOMING")
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getEvents_whenFiltersDoNotMatch_shouldReturnEmptyArray() throws Exception {

        mockMvc.perform(
                        get("/api/events")

                                .param("city", "NonExistingCity")
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getEvents_filterByHallId_shouldReturnMatchingEvent() throws Exception {

        mockMvc.perform(
                        get("/api/events")

                                .param("hallId", hall.getId().toString())
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getEvents_filterByVenueId_shouldReturnMatchingEvent() throws Exception {

        mockMvc.perform(
                        get("/api/events")

                                .param("venueId", venue.getId().toString())
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getEvents_startFromAfterStartTo_shouldReturn400() throws Exception {

        mockMvc.perform(
                        get("/api/events")

                                .param("startFrom", "2027-12-01T00:00:00")
                                .param("startTo", "2027-01-01T00:00:00")
                )

                .andExpect(status().isBadRequest())

                .andExpect(jsonPath("$.message")
                                .value(
                                        "startFrom must not be after startTo"
                                )
                );
    }

    @Test
    void getEvents_startRange_shouldReturnMatchingEvent() throws Exception {

        LocalDateTime from = event.getStartTime().minusHours(1);

        LocalDateTime to = event.getStartTime().plusHours(1);

        mockMvc.perform(
                        get("/api/events")

                                .param("startFrom", from.toString())

                                .param("startTo", to.toString())
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getMyBookings_shouldReturnPagedResponse() throws Exception {

        String token = registerAndLoginUser("history-user@example.com");

        String bookingBody = createBookingBody(eventId, eventSeatId);

        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        "history-booking-001"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        bookingBody
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "page",
                                        "0"
                                )

                                .param(
                                        "size",
                                        "10"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                )

                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.pageNumber")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.pageSize")
                                .value(10)
                )

                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )

                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                );
    }

    @Test
    void getMyBookings_filterByStatus_shouldReturnMatchingBooking() throws Exception {

        String token = registerAndLoginUser("status-history@example.com");

        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        "history-status-001"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        createBookingBody(
                                                eventId,
                                                eventSeatId
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "status",
                                        "PENDING"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.content[0].bookingStatus")
                                .value("PENDING")
                );
    }

    @Test
    void getMyBookings_nonMatchingStatus_shouldReturnEmptyPage() throws Exception {

        String token = registerAndLoginUser("empty-history@example.com");

        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        "empty-history-key"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        createBookingBody(
                                                eventId,
                                                eventSeatId
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "status",
                                        "CONFIRMED"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content.length()")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.totalElements")
                                .value(0)
                );
    }

    @Test
    void getMyBookings_searchByEventName_shouldReturnMatchingBooking() throws Exception {

        String token = registerAndLoginUser("search-history@example.com");

        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .header(
                                        "Idempotency-Key",
                                        "history-search-001"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        createBookingBody(
                                                eventId,
                                                eventSeatId
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "search",
                                        "Integration Test"
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                );
    }

    @Test
    void getMyBookings_shouldNeverReturnAnotherUsersBookings() throws Exception {

        String userAToken = registerAndLoginUser("owner-a@example.com");

        String userBToken = registerAndLoginUser("owner-b@example.com");

        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer " + userAToken
                                )

                                .header(
                                        "Idempotency-Key",
                                        "owner-a-booking"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        createBookingBody(
                                                eventId,
                                                eventSeatId
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + userBToken
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.content.length()")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.totalElements")
                                .value(0)
                );
    }

    @Test
    void getMyBookings_invalidPageSize_shouldReturn400() throws Exception {

        String token = registerAndLoginUser("invalid-page@example.com");

        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "size",
                                        "101"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void getMyBookings_invalidCreatedDateRange_shouldReturn400() throws Exception {

        String token = registerAndLoginUser("invalid-range@example.com");

        mockMvc.perform(
                        get("/api/bookings/my")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )

                                .param(
                                        "createdFrom",
                                        "2026-12-01T00:00:00"
                                )

                                .param(
                                        "createdTo",
                                        "2026-10-01T00:00:00"
                                )
                )

                .andExpect(
                        status().isBadRequest()
                )

                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "createdFrom must not be after createdTo"
                                )
                );
    }

    @Test
    void eventAnalytics_withNoBookings_shouldReturnZeroMetrics()
            throws Exception {

        String adminToken =
                registerAndLoginAdmin(
                        "analytics-zero-admin@example.com"
                );


        mockMvc.perform(
                        get(
                                "/api/admin/events/"
                                        + eventId
                                        + "/analytics"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + adminToken
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.totalSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.availableSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.lockedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.bookedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.totalBookings")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.pendingBookings")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.confirmedBookings")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.paymentAttempts")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.refundAttempts")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.grossTicketSales")
                                .value(0.00)
                )

                .andExpect(
                        jsonPath("$.netRevenue")
                                .value(0.00)
                );
    }

    @Test
    void eventAnalytics_afterPendingBooking_shouldShowLockedSeatAndPendingBooking()
            throws Exception {

        String userToken =
                registerAndLoginUser(
                        "analytics-pending-user@example.com"
                );


        String adminToken =
                registerAndLoginAdmin(
                        "analytics-pending-admin@example.com"
                );


        mockMvc.perform(
                        post("/api/bookings")

                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + userToken
                                )

                                .header(
                                        "Idempotency-Key",
                                        "analytics-pending-001"
                                )

                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )

                                .content(
                                        createBookingBody(
                                                eventId,
                                                eventSeatId
                                        )
                                )
                )

                .andExpect(
                        status().isCreated()
                );


        mockMvc.perform(
                        get(
                                "/api/admin/events/"
                                        + eventId
                                        + "/analytics"
                        )

                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + adminToken
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        jsonPath("$.totalBookings")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.pendingBookings")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.confirmedBookings")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.lockedSeats")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$.bookedSeats")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.paymentAttempts")
                                .value(0)
                )

                .andExpect(
                        jsonPath("$.grossTicketSales")
                                .value(0.00)
                );
    }
}