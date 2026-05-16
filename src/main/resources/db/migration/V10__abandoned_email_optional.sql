-- =====================================================================
-- Make email nullable on abandoned_leads — capture if either email OR mobile present
-- =====================================================================

ALTER TABLE abandoned_leads
    MODIFY email VARCHAR(180) NULL;

-- Index on mobile so dedup lookup by mobile is fast
CREATE INDEX idx_abandoned_mobile ON abandoned_leads(mobile);
