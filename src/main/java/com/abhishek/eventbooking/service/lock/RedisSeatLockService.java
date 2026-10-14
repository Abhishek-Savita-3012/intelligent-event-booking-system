package com.abhishek.eventbooking.service.lock;

import com.abhishek.eventbooking.config.RedisSeatLockProperties;
import com.abhishek.eventbooking.exception.ConflictException;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisSeatLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisSeatLockService.class);

    private static final String KEY_PREFIX = "eventbooking::seat-lock::";

    /**
     * Atomically:
     *
     * 1. Check that the current value still belongs to us.
     * 2. Delete only if ownership matches.
     */
    private static final DefaultRedisScript<Long>
            RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );


    private final StringRedisTemplate redisTemplate;
    private final RedisSeatLockProperties properties;

    // =========================================================
    // ACQUIRE ALL REQUESTED SEATS
    // =========================================================

    public Optional<RedisSeatLockHandle> acquireAll(List<Long> eventSeatIds) {

        if (!properties.isEnabled()) {

            return Optional.empty();
        }

        /*
         * Always acquire in deterministic order.
         *
         * Your DB pessimistic locks already follow the same idea.
         */
        List<Long> sortedSeatIds = eventSeatIds
                        .stream()
                        .sorted()
                        .toList();


        String ownerToken = UUID.randomUUID().toString();

        List<Long> acquiredSeatIds = new ArrayList<>();

        try {

            for (Long eventSeatId : sortedSeatIds) {

                String key = buildKey(eventSeatId);

                Boolean acquired = redisTemplate
                                .opsForValue()
                                .setIfAbsent(
                                        key,
                                        ownerToken,
                                        properties.getTtl()
                                );

                if (!Boolean.TRUE.equals(acquired)) {

                    /*
                     * We already locked some other seats for
                     * this multi-seat booking.
                     *
                     * Release them before returning the conflict.
                     */
                    releasePartial(
                            ownerToken,
                            acquiredSeatIds
                    );


                    throw new ConflictException(
                            "One or more selected seats "
                                    + "are currently being processed. "
                                    + "Please try again."
                    );
                }


                acquiredSeatIds.add(eventSeatId);
            }


            return Optional.of(
                    new RedisSeatLockHandle(
                            ownerToken,
                            List.copyOf(
                                    acquiredSeatIds
                            )
                    )
            );

        } catch (ConflictException exception) {

            throw exception;

        } catch (DataAccessException exception) {

            /*
             * Redis is an optimization here.
             *
             * If it is unavailable:
             *
             * release whatever we can
             * and fall back to MySQL locking.
             */

            log.warn(
                    "Redis seat coordination unavailable. "
                            + "Falling back to database locks.",
                    exception
            );


            releasePartialSafely(
                    ownerToken,
                    acquiredSeatIds
            );


            return Optional.empty();
        }
    }

    // =========================================================
    // RELEASE AFTER DATABASE TRANSACTION COMPLETES
    // =========================================================

    public void releaseAfterTransaction(RedisSeatLockHandle handle) {

        if (handle == null) {

            return;
        }

        /*
         * BookingService#createBooking is transactional.
         *
         * We do NOT want to release Redis before MySQL commits.
         */
        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCompletion(
                                        int status
                                ) {

                                    release(handle);
                                }
                            }
                    );


            return;
        }

        /*
         * Defensive fallback if this service is ever used
         * outside a transaction.
         */
        release(handle);
    }

    // =========================================================
    // RELEASE HANDLE
    // =========================================================

    public void release(RedisSeatLockHandle handle) {

        for (Long eventSeatId : handle.eventSeatIds()) {

            releaseSingleSafely(
                    eventSeatId,
                    handle.ownerToken()
            );
        }
    }

    // =========================================================
    // RELEASE PARTIAL ACQUISITION
    // =========================================================

    private void releasePartial(String ownerToken, List<Long> acquiredSeatIds) {

        for (Long eventSeatId : acquiredSeatIds) {

            releaseSingleSafely(
                    eventSeatId,
                    ownerToken
            );
        }
    }


    private void releasePartialSafely(String ownerToken, List<Long> acquiredSeatIds) {

        try {

            releasePartial(ownerToken, acquiredSeatIds);

        } catch (Exception exception) {

            /*
             * TTL is still our final cleanup mechanism.
             */
            log.warn(
                    "Could not immediately release partial "
                            + "Redis seat locks. "
                            + "They will expire by TTL.",
                    exception
            );
        }
    }


    // =========================================================
    // SAFE ATOMIC RELEASE
    // =========================================================

    private void releaseSingleSafely(Long eventSeatId, String ownerToken) {

        try {

            redisTemplate.execute(
                    RELEASE_SCRIPT,
                    Collections.singletonList(
                            buildKey(
                                    eventSeatId
                            )
                    ),
                    ownerToken
            );

        } catch (DataAccessException exception) {

            /*
             * Never break the completed booking transaction
             * merely because Redis cleanup failed.
             *
             * TTL will eventually remove the lock.
             */

            log.warn(
                    "Failed to release Redis coordination "
                            + "lock eventSeatId={}",
                    eventSeatId,
                    exception
            );
        }
    }


    // =========================================================
    // KEY
    // =========================================================

    private String buildKey(Long eventSeatId) {

        return KEY_PREFIX + eventSeatId;
    }
}