-- ============================================
-- LESSON 32
-- Query-performance indexes
-- ============================================

CREATE INDEX idx_bookings_user_created_at
    ON bookings(user_id, created_at DESC);

CREATE INDEX idx_bookings_event_status
    ON bookings(event_id, status);

CREATE INDEX idx_event_seats_event_status
    ON event_seats(event_id, status);

CREATE INDEX idx_event_seats_status_locked_until
    ON event_seats(status, locked_until);

CREATE INDEX idx_events_hall_start_end
    ON events(hall_id, start_time, end_time);

CREATE INDEX idx_payments_booking_status
    ON payments(booking_id, status);

CREATE INDEX idx_refunds_booking_status
    ON refunds(booking_id, status);