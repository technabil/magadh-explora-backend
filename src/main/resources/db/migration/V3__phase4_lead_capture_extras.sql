-- =====================================================================
-- Phase 4: extend quotes with the fields the QuoteModal already collects
-- =====================================================================

ALTER TABLE quotes
    ADD COLUMN country       VARCHAR(80)  NULL AFTER mobile,
    ADD COLUMN traveler_type VARCHAR(40)  NULL AFTER country,
    ADD COLUMN package_tier  VARCHAR(40)  NULL AFTER traveler_type,
    ADD COLUMN destinations  VARCHAR(500) NULL AFTER package_tier,
    ADD COLUMN budget        VARCHAR(40)  NULL AFTER destinations;

CREATE INDEX idx_contacts_status ON contacts(status, created_at);
CREATE INDEX idx_quotes_status   ON quotes(status, created_at);
CREATE INDEX idx_bookings_status ON bookings(status, created_at);
