-- =====================================================================
-- Abandoned leads — captures partial form data when users don't submit
-- =====================================================================

CREATE TABLE abandoned_leads (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    source          VARCHAR(40)  NOT NULL,
    name            VARCHAR(120) NULL,
    email           VARCHAR(180) NOT NULL,
    mobile          VARCHAR(32)  NULL,
    form_state      MEDIUMTEXT   NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    notes           TEXT         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_abandoned_email   ON abandoned_leads(email);
CREATE INDEX idx_abandoned_source  ON abandoned_leads(source);
CREATE INDEX idx_abandoned_status  ON abandoned_leads(status, created_at);
