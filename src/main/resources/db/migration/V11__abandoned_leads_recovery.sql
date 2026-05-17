-- =====================================================================
-- Phase A recovery automation — 3-touch email sequence for abandoned leads
-- =====================================================================

ALTER TABLE abandoned_leads
    ADD COLUMN attempts            INT          NOT NULL DEFAULT 0,
    ADD COLUMN last_touched_at     TIMESTAMP    NULL,
    ADD COLUMN next_touch_at       TIMESTAMP    NULL,
    ADD COLUMN last_touch_channel  VARCHAR(20)  NULL,
    ADD COLUMN recovery_token      VARCHAR(64)  NULL;

-- Token must be unique so /r/{token} can resolve a single lead
CREATE UNIQUE INDEX uq_abandoned_recovery_token ON abandoned_leads(recovery_token);

-- Scheduled job query: WHERE status='NEW' AND attempts<3 AND next_touch_at<=now
CREATE INDEX idx_abandoned_due_touch ON abandoned_leads(status, attempts, next_touch_at);
