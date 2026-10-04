package com.abhishek.eventbooking.integration;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.repository.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private UserRepository userRepository;

    private Long eventId;
    private Long eventSeatId;

    @BeforeEach
    void setUp() {

        Venue venue = Venue.builder()
                .name("Integration Test Venue")
                .city("Lucknow")
                .address("Test Address")
                .build();

        venue = venueRepository.save(venue);

        Hall hall = Hall.builder()
                .name("Screen 1")
                .venue(venue)
                .build();

        hall = hallRepository.save(hall);

        Seat seat = Seat.builder()
                .hall(hall)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.REGULAR)
                .build();

        seat = seatRepository.save(seat);

        Event event = Event.builder()
                .name("Integration Test Movie")
                .description("Movie used for integration testing")
                .category(EventCategory.MOVIE)
                .hall(hall)
                .startTime(LocalDateTime.now().plusDays(5))
                .endTime(LocalDateTime.now().plusDays(5).plusHours(3))
                .status(EventStatus.UPCOMING)
                .build();

        event = eventRepository.save(event);

        EventSeat eventSeat = EventSeat.builder()
                .event(event)
                .seat(seat)
                .price(new BigDecimal("250.00"))
                .status(EventSeatStatus.AVAILABLE)
                .build();

        eventSeat = eventSeatRepository.save(eventSeat);

        eventId = event.getId();
        eventSeatId = eventSeat.getId();
    }

    @Test
    void register_withValidRequest_shouldReturn201() throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "name",
                        "Integration User",

                        "email",
                        "integration@example.com",

                        "password",
                        "Password123"
                )
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )

                .andExpect(status().isCreated())

                .andExpect(jsonPath("$.id").exists())

                .andExpect(jsonPath("$.email").value("integration@example.com"))

                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_withInvalidFields_shouldReturn400WithFieldErrors() throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "name",
                        "",

                        "email",
                        "not-an-email",

                        "password",
                        "123"
                )
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )

                .andExpect(status().isBadRequest())

                .andExpect(jsonPath("$.status").value(400))

                .andExpect(jsonPath("$.error").value("Bad Request"))

                .andExpect(jsonPath("$.message").value("Validation failed"))

                .andExpect(jsonPath("$.fieldErrors.email").exists())

                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void getEvents_withoutJwt_shouldReturn200() throws Exception {

        mockMvc.perform(get("/api/events"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMyBookings_withoutJwt_shouldReturn401() throws Exception {

        mockMvc.perform(get("/api/bookings/my"))

                .andExpect(status().isUnauthorized());
    }

    private String registerAndLoginUser(String email) throws Exception {

        String registerBody = objectMapper.writeValueAsString(
                Map.of(
                        "name",
                        "Integration User",

                        "email",
                        email,

                        "password",
                        "Password123"
                )
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody)
                )
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of(
                        "email",
                        email,

                        "password",
                        "Password123"
                )
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.token").exists())

                .andReturn();

        JsonNode json = objectMapper.readTree(
                loginResult
                        .getResponse()
                        .getContentAsString()
        );

        return json.get("token").asText();
    }

    @Test
    void getAdminBookings_withUserJwt_shouldReturn403() throws Exception {

        String token = registerAndLoginUser("normaluser@example.com"
        );

        mockMvc.perform(
                        get("/api/admin/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(status().isForbidden());
    }

    private String registerAndLoginAdmin(String email) throws Exception {

        String registerBody = objectMapper.writeValueAsString(
                Map.of(
                        "name",
                        "Integration Admin",

                        "email",
                        email,

                        "password",
                        "Password123"
                )
        );

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody)
                )
                .andExpect(status().isCreated());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow();

        user.setRole(Role.ADMIN);

        userRepository.saveAndFlush(user);

        String loginBody = objectMapper.writeValueAsString(
                Map.of(
                        "email",
                        email,

                        "password",
                        "Password123"
                )
        );

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )

                .andExpect(status().isOk())

                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());

        return json.get("token").asText();
    }

    @Test
    void getAdminBookings_withAdminJwt_shouldReturn200() throws Exception {

        String token = registerAndLoginAdmin("admin@example.com");

        mockMvc.perform(
                        get("/api/admin/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.content").isArray())

                .andExpect(jsonPath("$.pageNumber").value(0));
    }

    @Test
    void getEvent_whenEventDoesNotExist_shouldReturn404() throws Exception {

        mockMvc.perform(
                        get("/api/events/999999")
                )

                .andExpect(status().isNotFound())

                .andExpect(jsonPath("$.status").value(404))

                .andExpect(jsonPath("$.error").value("Not Found"))

                .andExpect(
                        jsonPath("$.path")
                                .value("/api/events/999999")
                );
    }

    @Test
    void createBooking_withValidJwt_shouldCreatePendingBookingAndLockSeat() throws Exception {

        String token = registerAndLoginUser("booker@example.com");

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "eventId",
                        eventId,

                        "eventSeatIds",
                        new Long[]{
                                eventSeatId
                        }
                )
        );

        MvcResult result = mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )

                .andExpect(status().isCreated())

                .andExpect(jsonPath("$.bookingReference").exists())

                .andExpect(jsonPath("$.status").value("PENDING"))

                .andExpect(jsonPath("$.totalAmount").value(250.00))

                .andExpect(jsonPath("$.expiresAt").exists())

                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

        String bookingReference = response.get("bookingReference").asText();

        assertTrue(bookingReference.startsWith("BK-"));

        // ==========================
        // VERIFY DATABASE
        // ==========================

        EventSeat updatedSeat = eventSeatRepository.findById(eventSeatId)
                .orElseThrow();

        assertEquals(EventSeatStatus.LOCKED, updatedSeat.getStatus());

        assertNotNull(updatedSeat.getLockedUntil());

        assertNotNull(updatedSeat.getLockedByBooking());

        Booking savedBooking = bookingRepository
                .findByBookingReference(bookingReference)
                .orElseThrow();

        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());

        assertEquals(new BigDecimal("250.00"), savedBooking.getTotalAmount());
    }

    @Test
    void createBooking_whenSeatAlreadyLocked_shouldReturn409() throws Exception {

        String firstUserToken = registerAndLoginUser("first@example.com");

        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "eventId",
                        eventId,

                        "eventSeatIds",
                        new Long[]{
                                eventSeatId
                        }
                )
        );

        // First user locks the seat.

        mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + firstUserToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )

                .andExpect(status().isCreated());

        // Register second user.

        String secondUserToken = registerAndLoginUser("second@example.com");

        // Same EventSeat.

        mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + secondUserToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )

                .andExpect(status().isConflict())

                .andExpect(jsonPath("$.status").value(409))

                .andExpect(jsonPath("$.message")
                        .value("One or more selected seats are not available")
                );
    }

    @Test
    void paymentSuccess_shouldConfirmBookingAndMarkSeatBooked() throws Exception {

        String token = registerAndLoginUser("payer@example.com");

        // ==========================
        // CREATE BOOKING
        // ==========================

        String bookingBody = objectMapper.writeValueAsString(
                Map.of(
                        "eventId",
                        eventId,

                        "eventSeatIds",
                        new Long[]{
                                eventSeatId
                        }
                )
        );

        MvcResult bookingResult = mockMvc.perform(
                        post("/api/bookings")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bookingBody)
                )

                .andExpect(status().isCreated())

                .andReturn();

        JsonNode bookingJson = objectMapper.readTree(bookingResult.getResponse().getContentAsString());

        String bookingReference = bookingJson.get("bookingReference").asText();

        // ==========================
        // PAYMENT
        // ==========================

        String paymentBody = objectMapper.writeValueAsString(
                Map.of(
                        "outcome",
                        "SUCCESS"
                )
        );

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(paymentBody)
                )

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))

                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))

                .andExpect(jsonPath("$.paymentReference").exists());

        // ==========================
        // VERIFY DB
        // ==========================

        Booking booking = bookingRepository
                .findByBookingReference(bookingReference)
                .orElseThrow();

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        EventSeat seat = eventSeatRepository
                .findById(eventSeatId)
                .orElseThrow();

        assertEquals(EventSeatStatus.BOOKED, seat.getStatus());

        assertNull(seat.getLockedByBooking());

        assertNull(seat.getLockedUntil());
    }

    @Test
    void request_shouldContainRequestIdHeader() throws Exception {

        mockMvc.perform(
                        get("/api/events")
                )

                .andExpect(status().isOk())

                .andExpect(
                        header().exists(
                                "X-Request-ID"
                        )
                );
    }

    @Test
    void request_withProvidedRequestId_shouldReturnSameRequestId() throws Exception {

        mockMvc.perform(
                        get("/api/events")
                                .header(
                                        "X-Request-ID",
                                        "integration-test-123"
                                )
                )

                .andExpect(status().isOk())

                .andExpect(
                        header().string(
                                "X-Request-ID",
                                "integration-test-123"
                        )
                );
    }
}