package com.abhishek.eventbooking.repository;

import com.abhishek.eventbooking.dto.projection.BookingStatusCountProjection;
import com.abhishek.eventbooking.entity.Booking;
import com.abhishek.eventbooking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(
            String bookingReference
    );

    List<Booking> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // ==============================
    // PESSIMISTIC LOCK BOOKING
    // ==============================

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.bookingReference = :bookingReference
        """)
    Optional<Booking> findByBookingReferenceForUpdate(
            @Param("bookingReference")
            String bookingReference
    );

    long countByStatus(BookingStatus status);

    // ==============================
    // ADMIN SEARCH + FILTER
    // + PAGINATION + SORTING
    // ==============================

    @Query(
            value = """
                    SELECT b
                    FROM Booking b

                    JOIN FETCH b.user u
                    JOIN FETCH b.event e
                    JOIN FETCH e.hall h
                    JOIN FETCH h.venue v

                    WHERE
                        (:status IS NULL
                            OR b.status = :status)

                    AND
                        (
                            :search IS NULL

                            OR :search = ''

                            OR LOWER(b.bookingReference)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(u.name)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(u.email)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(e.name)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )
                        )
                    """,

            countQuery = """
                    SELECT COUNT(b)
                    FROM Booking b

                    JOIN b.user u
                    JOIN b.event e

                    WHERE
                        (:status IS NULL
                            OR b.status = :status)

                    AND
                        (
                            :search IS NULL

                            OR :search = ''

                            OR LOWER(b.bookingReference)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(u.name)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(u.email)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )

                            OR LOWER(e.name)
                                LIKE LOWER(
                                    CONCAT(
                                        '%',
                                        :search,
                                        '%'
                                    )
                                )
                        )
                    """
    )
    Page<Booking> searchAdminBookings(
            @Param("status")
            BookingStatus status,

            @Param("search")
            String search,

            Pageable pageable
    );

    long countByEvent_Id(Long eventId);

    long countByEvent_IdAndStatus(Long eventId, BookingStatus status);

    Optional<Booking> findByUser_IdAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    );

    @Query(
            value = """
                SELECT b
                FROM Booking b

                JOIN FETCH b.user u
                JOIN FETCH b.event e
                JOIN FETCH e.hall h
                JOIN FETCH h.venue v

                WHERE u.email = :email

                AND
                    (
                        :status IS NULL
                        OR b.status = :status
                    )

                AND
                    (
                        :search IS NULL

                        OR :search = ''

                        OR LOWER(b.bookingReference)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(e.name)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(v.name)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(v.city)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )
                    )

                AND
                    (
                        :createdFrom IS NULL
                        OR b.createdAt >= :createdFrom
                    )

                AND
                    (
                        :createdTo IS NULL
                        OR b.createdAt <= :createdTo
                    )
                """,

            countQuery = """
                SELECT COUNT(b)
                FROM Booking b

                JOIN b.user u
                JOIN b.event e
                JOIN e.hall h
                JOIN h.venue v

                WHERE u.email = :email

                AND
                    (
                        :status IS NULL
                        OR b.status = :status
                    )

                AND
                    (
                        :search IS NULL

                        OR :search = ''

                        OR LOWER(b.bookingReference)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(e.name)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(v.name)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )

                        OR LOWER(v.city)
                            LIKE LOWER(
                                CONCAT(
                                    '%',
                                    :search,
                                    '%'
                                )
                            )
                    )

                AND
                    (
                        :createdFrom IS NULL
                        OR b.createdAt >= :createdFrom
                    )

                AND
                    (
                        :createdTo IS NULL
                        OR b.createdAt <= :createdTo
                    )
                """
    )
    Page<Booking> searchUserBookings(

            @Param("email")
            String email,

            @Param("status")
            BookingStatus status,

            @Param("search")
            String search,

            @Param("createdFrom")
            LocalDateTime createdFrom,

            @Param("createdTo")
            LocalDateTime createdTo,

            Pageable pageable
    );

    @Query("""
        SELECT
            b.status AS status,
            COUNT(b) AS count
        FROM Booking b
        WHERE b.event.id = :eventId
        GROUP BY b.status
        """)
    List<BookingStatusCountProjection> countBookingsByStatusForEvent(
            @Param("eventId")
            Long eventId
    );
}