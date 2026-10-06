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

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;

import java.time.LocalDateTime;

import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Testcontainers
class MySQLBookingConcurrencyIntegrationTest {

    // =========================================================
    // REAL MYSQL CONTAINER
    // =========================================================

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(
                    "mysql:8.4"
            )
                    .withDatabaseName(
                            "event_booking_test"
                    )
                    .withUsername(
                            "testuser"
                    )
                    .withPassword(
                            "testpass"
                    );


    // =========================================================
    // SERVICES
    // =========================================================

    @Autowired
    private BookingService bookingService;

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
    // TRANSACTION / JDBC
    // =========================================================

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    // =========================================================
    // TEST DATA
    // =========================================================

    private User userA;

    private User userB;

    private Venue venue;

    private Hall hall;

    private Seat physicalSeat;

    private Event event;

    private EventSeat eventSeat;


    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        /*
         * IMPORTANT:
         *
         * This test class intentionally does NOT use
         * class-level @Transactional.
         *
         * The worker threads need to see committed rows
         * created during setup.
         */


        // =====================================================
        // CLEAN DATABASE
        //
        // Delete children before parents because of FKs.
        // =====================================================

        refundRepository.deleteAllInBatch();

        paymentRepository.deleteAllInBatch();

        bookingSeatRepository.deleteAllInBatch();

        /*
         * EventSeat has locked_by_booking FK, therefore
         * remove EventSeats before Bookings.
         */
        eventSeatRepository.deleteAllInBatch();

        bookingRepository.deleteAllInBatch();

        eventRepository.deleteAllInBatch();

        seatRepository.deleteAllInBatch();

        hallRepository.deleteAllInBatch();

        venueRepository.deleteAllInBatch();

        userRepository.deleteAllInBatch();


        // =====================================================
        // USER A
        // =====================================================

        userA =
                userRepository.saveAndFlush(
                        User.builder()
                                .name(
                                        "Concurrency User A"
                                )
                                .email(
                                        "mysql-user-a@example.com"
                                )
                                .password(
                                        "dummy-password"
                                )
                                .role(
                                        Role.USER
                                )
                                .build()
                );


        // =====================================================
        // USER B
        // =====================================================

        userB =
                userRepository.saveAndFlush(
                        User.builder()
                                .name(
                                        "Concurrency User B"
                                )
                                .email(
                                        "mysql-user-b@example.com"
                                )
                                .password(
                                        "dummy-password"
                                )
                                .role(
                                        Role.USER
                                )
                                .build()
                );


        // =====================================================
        // VENUE
        // =====================================================

        venue =
                venueRepository.saveAndFlush(
                        Venue.builder()
                                .name(
                                        "MySQL Test Venue"
                                )
                                .city(
                                        "Lucknow"
                                )
                                .address(
                                        "Test Address"
                                )
                                .build()
                );


        // =====================================================
        // HALL
        // =====================================================

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


        // =====================================================
        // PHYSICAL SEAT
        // =====================================================

        physicalSeat =
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


        // =====================================================
        // EVENT
        // =====================================================

        event =
                eventRepository.saveAndFlush(
                        Event.builder()
                                .name(
                                        "MySQL Concurrency Movie"
                                )
                                .description(
                                        "Real MySQL concurrency test event"
                                )
                                .category(
                                        EventCategory.MOVIE
                                )
                                .startTime(
                                        LocalDateTime.now()
                                                .plusDays(2)
                                )
                                .endTime(
                                        LocalDateTime.now()
                                                .plusDays(2)
                                                .plusHours(3)
                                )
                                .status(
                                        EventStatus.UPCOMING
                                )
                                .hall(
                                        hall
                                )
                                .build()
                );


        // =====================================================
        // EVENT SEAT
        // =====================================================

        eventSeat =
                eventSeatRepository.saveAndFlush(
                        EventSeat.builder()
                                .event(
                                        event
                                )
                                .seat(
                                        physicalSeat
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
    }


    // =========================================================
    // TEST 1
    //
    // TWO DIFFERENT USERS
    // SAME SEAT
    //
    // EXACTLY ONE SHOULD WIN
    // =========================================================

    @Test
    void twoDifferentUsersBookingSameSeatConcurrently_shouldAllowOnlyOneBooking()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        2
                );


        CountDownLatch ready =
                new CountDownLatch(
                        2
                );


        CountDownLatch start =
                new CountDownLatch(
                        1
                );


        Callable<BookingAttemptResult> userATask =
                createBookingAttempt(
                        userA.getEmail(),
                        "mysql-user-a-booking-001",
                        ready,
                        start
                );


        Callable<BookingAttemptResult> userBTask =
                createBookingAttempt(
                        userB.getEmail(),
                        "mysql-user-b-booking-001",
                        ready,
                        start
                );


        Future<BookingAttemptResult> futureA =
                executor.submit(
                        userATask
                );


        Future<BookingAttemptResult> futureB =
                executor.submit(
                        userBTask
                );


        try {

            /*
             * Ensure both worker threads have reached
             * the starting gate.
             */
            assertTrue(
                    ready.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Both booking threads did not become ready in time"
            );


            /*
             * Release both threads at almost the same time.
             */
            start.countDown();


            BookingAttemptResult resultA =
                    futureA.get(
                            20,
                            TimeUnit.SECONDS
                    );


            BookingAttemptResult resultB =
                    futureB.get(
                            20,
                            TimeUnit.SECONDS
                    );


            long successCount =
                    List.of(
                                    resultA,
                                    resultB
                            )
                            .stream()
                            .filter(
                                    BookingAttemptResult::success
                            )
                            .count();


            long conflictCount =
                    List.of(
                                    resultA,
                                    resultB
                            )
                            .stream()
                            .filter(
                                    result ->
                                            !result.success()
                            )
                            .count();


            assertEquals(
                    1,
                    successCount,
                    "Exactly one booking should succeed"
            );


            assertEquals(
                    1,
                    conflictCount,
                    "Exactly one booking should fail with a conflict"
            );


            BookingAttemptResult failedResult =
                    resultA.success()
                            ? resultB
                            : resultA;


            assertNotNull(
                    failedResult.errorMessage()
            );


            assertTrue(
                    failedResult
                            .errorMessage()
                            .toLowerCase()
                            .contains(
                                    "not available"
                            )
                            ||
                            failedResult
                                    .errorMessage()
                                    .toLowerCase()
                                    .contains(
                                            "unavailable"
                                    )
            );


            // =================================================
            // DATABASE ASSERTIONS
            // =================================================

            assertEquals(
                    1,
                    bookingRepository.count()
            );


            assertEquals(
                    1,
                    bookingSeatRepository.count()
            );


            EventSeat updatedSeat =
                    eventSeatRepository
                            .findById(
                                    eventSeat.getId()
                            )
                            .orElseThrow();


            assertEquals(
                    EventSeatStatus.LOCKED,
                    updatedSeat.getStatus()
            );


            assertNotNull(
                    updatedSeat.getLockedByBooking()
            );


            assertNotNull(
                    updatedSeat.getLockedUntil()
            );

        } finally {

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }


    // =========================================================
    // TEST 2
    //
    // SAME USER
    // SAME IDEMPOTENCY KEY
    // SAME REQUEST
    //
    // BOTH CALLS SHOULD RETURN SAME BOOKING
    // =========================================================

    @Test
    void sameUserSameIdempotencyKeyConcurrently_shouldCreateOnlyOneBooking()
            throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        2
                );


        CountDownLatch ready =
                new CountDownLatch(
                        2
                );


        CountDownLatch start =
                new CountDownLatch(
                        1
                );


        String idempotencyKey =
                "mysql-idempotency-retry-001";


        Callable<BookingResponse> task =
                () -> {

                    ready.countDown();


                    if (
                            !start.await(
                                    10,
                                    TimeUnit.SECONDS
                            )
                    ) {

                        throw new IllegalStateException(
                                "Start latch timed out"
                        );
                    }


                    return bookingService
                            .createBooking(
                                    userA.getEmail(),
                                    idempotencyKey,
                                    createBookingRequest()
                            );
                };


        Future<BookingResponse> future1 =
                executor.submit(
                        task
                );


        Future<BookingResponse> future2 =
                executor.submit(
                        task
                );


        try {

            assertTrue(
                    ready.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Both idempotency threads did not become ready in time"
            );


            start.countDown();


            BookingResponse response1 =
                    future1.get(
                            20,
                            TimeUnit.SECONDS
                    );


            BookingResponse response2 =
                    future2.get(
                            20,
                            TimeUnit.SECONDS
                    );


            // =================================================
            // SAME BUSINESS RESOURCE
            // =================================================

            assertNotNull(
                    response1
            );

            assertNotNull(
                    response2
            );


            assertEquals(
                    response1.getBookingReference(),
                    response2.getBookingReference(),
                    "Both retries must return the same Booking"
            );


            // =================================================
            // ONLY ONE DATABASE BOOKING
            // =================================================

            assertEquals(
                    1,
                    bookingRepository.count()
            );


            assertEquals(
                    1,
                    bookingSeatRepository.count()
            );


            // =================================================
            // VERIFY IDEMPOTENCY DATA
            // =================================================

            Booking savedBooking =
                    bookingRepository
                            .findByUser_IdAndIdempotencyKey(
                                    userA.getId(),
                                    idempotencyKey
                            )
                            .orElseThrow();


            assertEquals(
                    response1.getBookingReference(),
                    savedBooking.getBookingReference()
            );


            assertEquals(
                    idempotencyKey,
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


            assertEquals(
                    BookingStatus.PENDING,
                    savedBooking.getStatus()
            );


            // =================================================
            // VERIFY SEAT
            // =================================================

            EventSeat updatedSeat =
                    eventSeatRepository
                            .findById(
                                    eventSeat.getId()
                            )
                            .orElseThrow();


            assertEquals(
                    EventSeatStatus.LOCKED,
                    updatedSeat.getStatus()
            );


            assertNotNull(
                    updatedSeat.getLockedByBooking()
            );


            assertEquals(
                    savedBooking.getId(),
                    updatedSeat
                            .getLockedByBooking()
                            .getId()
            );


            assertNotNull(
                    updatedSeat.getLockedUntil()
            );

        } finally {

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            );
        }
    }


    // =========================================================
    // TEST 3
    //
    // REAL MYSQL TRANSACTION ROLLBACK
    // =========================================================

    @Test
    void realMySqlTransaction_whenRuntimeExceptionOccurs_shouldRollbackBooking() {

        long beforeCount =
                bookingRepository.count();


        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );


        assertThrows(
                RuntimeException.class,
                () ->
                        transactionTemplate
                                .executeWithoutResult(
                                        status -> {

                                            Booking booking =
                                                    Booking.builder()

                                                            .user(
                                                                    userA
                                                            )

                                                            .event(
                                                                    event
                                                            )

                                                            .bookingReference(
                                                                    "BK-ROLLBACK"
                                                            )

                                                            .idempotencyKey(
                                                                    "rollback-key"
                                                            )

                                                            .requestFingerprint(
                                                                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                                                            )

                                                            .totalAmount(
                                                                    new BigDecimal(
                                                                            "250.00"
                                                                    )
                                                            )

                                                            .status(
                                                                    BookingStatus.PENDING
                                                            )

                                                            .expiresAt(
                                                                    LocalDateTime.now()
                                                                            .plusMinutes(
                                                                                    5
                                                                            )
                                                            )

                                                            .build();


                                            bookingRepository
                                                    .saveAndFlush(
                                                            booking
                                                    );


                                            /*
                                             * Force transaction rollback.
                                             */
                                            throw new RuntimeException(
                                                    "Force rollback"
                                            );
                                        }
                                )
        );


        long afterCount =
                bookingRepository.count();


        assertEquals(
                beforeCount,
                afterCount,
                "Booking inserted inside the failed transaction must be rolled back"
        );


        assertTrue(
                bookingRepository
                        .findByUser_IdAndIdempotencyKey(
                                userA.getId(),
                                "rollback-key"
                        )
                        .isEmpty()
        );
    }


    // =========================================================
    // TEST 4
    //
    // PROVE TEST IS USING MYSQL, NOT H2
    // =========================================================

    @Test
    void testEnvironment_shouldUseRealMySql() {

        String version =
                jdbcTemplate
                        .queryForObject(
                                "SELECT VERSION()",
                                String.class
                        );


        assertNotNull(
                version
        );


        assertFalse(
                version.isBlank()
        );


        /*
         * This class is connected through the MySQL
         * Testcontainer, not the H2 integration DB.
         */
        String databaseName =
                jdbcTemplate
                        .queryForObject(
                                "SELECT DATABASE()",
                                String.class
                        );


        assertEquals(
                "event_booking_test",
                databaseName
        );
    }


    // =========================================================
    // CONCURRENT BOOKING HELPER
    // =========================================================

    private Callable<BookingAttemptResult> createBookingAttempt(
            String email,
            String idempotencyKey,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        return () -> {

            ready.countDown();


            if (
                    !start.await(
                            10,
                            TimeUnit.SECONDS
                    )
            ) {

                throw new IllegalStateException(
                        "Start latch timed out"
                );
            }


            try {

                BookingResponse response =
                        bookingService
                                .createBooking(
                                        email,
                                        idempotencyKey,
                                        createBookingRequest()
                                );


                return BookingAttemptResult.success(
                        response.getBookingReference()
                );

            } catch (
                    ConflictException exception
            ) {

                return BookingAttemptResult.conflict(
                        exception.getMessage()
                );
            }
        };
    }


    // =========================================================
    // BOOKING REQUEST HELPER
    // =========================================================

    private BookingRequest createBookingRequest() {

        BookingRequest request =
                new BookingRequest();


        request.setEventId(
                event.getId()
        );


        request.setEventSeatIds(
                List.of(
                        eventSeat.getId()
                )
        );


        return request;
    }


    // =========================================================
    // RESULT RECORD
    // =========================================================

    private record BookingAttemptResult(
            boolean success,
            String bookingReference,
            String errorMessage
    ) {

        static BookingAttemptResult success(
                String bookingReference
        ) {

            return new BookingAttemptResult(
                    true,
                    bookingReference,
                    null
            );
        }


        static BookingAttemptResult conflict(
                String errorMessage
        ) {

            return new BookingAttemptResult(
                    false,
                    null,
                    errorMessage
            );
        }
    }

    @Test
    void flyway_shouldApplyBaselineMigrationOnFreshMySql() {

        Integer migrationCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE version = '1'
                        AND success = TRUE
                        """,
                        Integer.class
                );

        assertNotNull(
                migrationCount
        );

        assertEquals(
                1,
                migrationCount
        );
    }

    @Test
    void flyway_shouldCreateCoreTables() {

        Integer bookingTableCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                        AND table_name = 'bookings'
                        """,
                        Integer.class
                );


        assertNotNull(
                bookingTableCount
        );


        assertEquals(
                1,
                bookingTableCount
        );
    }
}