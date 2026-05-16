-- =====================================================================
-- Split booking status into lifecycle + payment, add cancellation reason and admin notes
-- =====================================================================

ALTER TABLE bookings
    ADD COLUMN payment_status      VARCHAR(32)   NOT NULL DEFAULT 'UNPAID' AFTER status,
    ADD COLUMN cancellation_reason VARCHAR(500)  NULL                       AFTER payment_status,
    ADD COLUMN internal_notes      TEXT          NULL                       AFTER cancellation_reason,
    ADD COLUMN cancelled_at        TIMESTAMP     NULL                       AFTER internal_notes,
    ADD COLUMN confirmed_at        TIMESTAMP     NULL                       AFTER cancelled_at,
    ADD COLUMN paid_at             TIMESTAMP     NULL                       AFTER confirmed_at;

-- Backfill: existing CONVERTED bookings → PAID, PENDING → UNPAID, CANCELLED stays
UPDATE bookings SET payment_status = 'PAID'  WHERE status = 'CONVERTED';
UPDATE bookings SET paid_at = updated_at     WHERE status = 'CONVERTED';
UPDATE bookings SET confirmed_at = updated_at WHERE status IN ('CONVERTED','CONTACTED');
UPDATE bookings SET cancelled_at = updated_at WHERE status = 'CANCELLED';

CREATE INDEX idx_bookings_payment_status   ON bookings(payment_status);
CREATE INDEX idx_bookings_status_payment   ON bookings(status, payment_status);
