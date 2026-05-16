-- =====================================================================
-- Add payment_method column to bookings (PAY_LATER, RAZORPAY, STRIPE, etc.)
-- =====================================================================

ALTER TABLE bookings
    ADD COLUMN payment_method VARCHAR(40) NULL AFTER currency;

CREATE INDEX idx_bookings_payment_method ON bookings(payment_method);
