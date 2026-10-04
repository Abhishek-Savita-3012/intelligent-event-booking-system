package com.abhishek.eventbooking.integration;

import com.abhishek.eventbooking.dto.request.BookingRequest;
import com.abhishek.eventbooking.dto.response.BookingResponse;

import com.abhishek.eventbooking.entity.*;

import com.abhishek.eventbooking.exception.ConflictException;

import com.abhishek.eventbooking.repository.*;

import com.abhishek.eventbooking.service.BookingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.test.context.ActiveProfiles;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Testcontainers
class MySqlBookingConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4")
                    .withDatabaseName("event_booking_test")
                    .withUsername("testuser")
                    .withPassword("testpass");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

    private User userA;
    private User userB;
    private Event event;
    private EventSeat eventSeat;

    @BeforeEach
    void cleanAndPrepareDatabase() {

        /*
         * Delete child tables before parent tables
         * because of foreign keys.
         */

        refundRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        eventSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
        prepareTestData();
    }

    private void prepareTestData() {

        userA = User.builder()
                .name("Concurrent User A")
                .email("usera@example.com")
                .password("not-used-in-this-test")
                .role(Role.USER)
                .build();

        userA = userRepository.saveAndFlush(userA);

        userB = User.builder()
                .name("Concurrent User B")
                .email("userb@example.com")
                .password("not-used-in-this-test")
                .role(Role.USER)
                .build();

        userB = userRepository.saveAndFlush(userB);

        Venue venue = Venue.builder()
                .name("Concurrency Test Venue")
                .city("Lucknow")
                .address("Integration Test Address")
                .build();

        venue = venueRepository.saveAndFlush(venue);

        Hall hall = Hall.builder()
                .name("Screen 1")
                .venue(venue)
                .build();

        hall = hallRepository.saveAndFlush(hall);

        Seat seat = Seat.builder()
                .hall(hall)
                .rowName("A")
                .seatNumber(1)
                .seatType(SeatType.REGULAR)
                .build();

        seat = seatRepository.saveAndFlush(seat);

        event =
                Event.builder()
                        .name("Concurrency Test Movie")
                        .description("Used for real MySQL locking test")
                        .category(EventCategory.MOVIE)
                        .hall(hall)
                        .startTime(LocalDateTime.now().plusDays(5))
                        .endTime(LocalDateTime.now().plusDays(5).plusHours(3))
                        .status(EventStatus.UPCOMING)
                        .build();

        event = eventRepository.saveAndFlush(event);

        eventSeat = EventSeat.builder()
                .event(event)
                .seat(seat)
                .price(new BigDecimal("250.00"))
                .status(EventSeatStatus.AVAILABLE)
                .build();

        eventSeat = eventSeatRepository.saveAndFlush(eventSeat);
    }

    private record BookingAttemptResult(boolean success, String bookingReference, String errorMessage) {

        static BookingAttemptResult success(String bookingReference) {

            return new BookingAttemptResult(true, bookingReference, null);
        }

        static BookingAttemptResult conflict(String errorMessage) {

            return new BookingAttemptResult(false, null, errorMessage);
        }
    }

    private BookingRequest createBookingRequest() {

        BookingRequest request = new BookingRequest();

        request.setEventId(event.getId());
        request.setEventSeatIds(List.of(eventSeat.getId()));

        return request;
    }

    @Test
    void testDatabase_shouldActuallyBeMySql() {

        String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

        assertNotNull(version);

        System.out.println("Testcontainers MySQL version: " + version);
    }

    @Test
    void twoUsersBookingSameSeatConcurrently_shouldAllowOnlyOneBooking() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        /*
         * Both worker threads tell us:
         * "I'm ready."
         */
        CountDownLatch ready = new CountDownLatch(2);

        /*
         * Both threads wait here until
         * we release them together.
         */
        CountDownLatch start = new CountDownLatch(1);

        Callable<BookingAttemptResult> userATask = () -> {

            ready.countDown();
            start.await();

            try {
                BookingResponse response = bookingService
                        .createBooking(
                                userA.getEmail(),
                                createBookingRequest()
                        );

                return BookingAttemptResult.success(response.getBookingReference());

            } catch (ConflictException ex) {

                return BookingAttemptResult
                        .conflict(ex.getMessage());
            }
        };

        Callable<BookingAttemptResult> userBTask = () -> {

            ready.countDown();
            start.await();

            try {
                BookingResponse response = bookingService
                        .createBooking(
                                userB.getEmail(),
                                createBookingRequest()
                        );

                return BookingAttemptResult.success(response.getBookingReference());

            } catch (ConflictException ex) {

                return BookingAttemptResult
                        .conflict(ex.getMessage());
            }
        };

        Future<BookingAttemptResult> futureA = executor.submit(userATask);

        Future<BookingAttemptResult> futureB = executor.submit(userBTask);

        /*
         * Wait until both threads reached
         * the starting line.
         */
        ready.await();

        /*
         * GO!
         */
        start.countDown();

        BookingAttemptResult resultA = futureA.get();

        BookingAttemptResult resultB = futureB.get();

        executor.shutdown();

        long successCount =
                List.of(resultA, resultB)
                        .stream()
                        .filter(BookingAttemptResult::success)
                        .count();

        long conflictCount =
                List.of(resultA, resultB)
                        .stream()
                        .filter(result -> !result.success())
                        .count();

        assertEquals(1, successCount);
        assertEquals(1, conflictCount);

        BookingAttemptResult failedResult = resultA.success() ? resultB : resultA;

        assertEquals(
                "One or more selected seats are not available",
                failedResult.errorMessage()
        );

        /*
         * DATABASE INTEGRITY CHECKS
         */

        assertEquals(1, bookingRepository.count());
        assertEquals(1, bookingSeatRepository.count());

        EventSeat updatedSeat = eventSeatRepository
                .findById(eventSeat.getId())
                .orElseThrow();

        assertEquals(EventSeatStatus.LOCKED, updatedSeat.getStatus());

        assertNotNull(
                updatedSeat.getLockedUntil()
        );
    }

    @Test
    void realMySqlTransaction_whenRuntimeExceptionOccurs_shouldRollback() throws Exception {

        long before = bookingRepository.count();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThrows(
                RuntimeException.class,
                () ->
                        transactionTemplate
                                .executeWithoutResult(
                                        status -> {

                                            Booking booking = Booking.builder()
                                                    .user(userA)
                                                    .event(event)
                                                    .bookingReference("BK-ROLLBACK")
                                                    .totalAmount(new BigDecimal("250.00"))
                                                    .status(BookingStatus.PENDING)
                                                    .createdAt(LocalDateTime.now())
                                                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                                                    .build();

                                            bookingRepository.save(booking);

                                            /*
                                             * Force failure before commit.
                                             */
                                            throw new RuntimeException("Force rollback");
                                        }
                                )
        );

        long after = bookingRepository.count();

        assertEquals(before, after);
    }
}