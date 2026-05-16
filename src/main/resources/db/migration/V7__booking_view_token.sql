-- =====================================================================
-- Add view_token (magic link) to bookings — used for passwordless booking access
-- =====================================================================

ALTER TABLE bookings
    ADD COLUMN view_token VARCHAR(64) NULL AFTER status;

-- Backfill any existing rows with a random UUID (hex, 32 chars)
UPDATE bookings
SET view_token = REPLACE(UUID(), '-', '')
WHERE view_token IS NULL;

ALTER TABLE bookings
    MODIFY view_token VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX uk_bookings_view_token ON bookings(view_token);
