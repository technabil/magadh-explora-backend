-- =====================================================================
-- Tracking — anonymous visitor events and rolled-up sessions for the
-- admin analytics dashboard (visitors, sources, funnels, geo).
-- =====================================================================

CREATE TABLE tracking_sessions (
    session_id      VARCHAR(64)  NOT NULL,
    visitor_id      VARCHAR(64)  NOT NULL,
    user_id         BIGINT       NULL,
    country_code    VARCHAR(8)   NULL,
    country         VARCHAR(80)  NULL,
    city            VARCHAR(120) NULL,
    device          VARCHAR(20)  NULL,
    os              VARCHAR(40)  NULL,
    browser         VARCHAR(40)  NULL,
    language        VARCHAR(16)  NULL,
    referrer        VARCHAR(500) NULL,
    utm_source      VARCHAR(120) NULL,
    utm_medium      VARCHAR(120) NULL,
    utm_campaign    VARCHAR(160) NULL,
    utm_term        VARCHAR(160) NULL,
    utm_content     VARCHAR(160) NULL,
    landing_path    VARCHAR(500) NULL,
    event_count     INT          NOT NULL DEFAULT 0,
    started_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sessions_visitor       ON tracking_sessions(visitor_id);
CREATE INDEX idx_sessions_started_at    ON tracking_sessions(started_at);
CREATE INDEX idx_sessions_last_seen     ON tracking_sessions(last_seen_at);
CREATE INDEX idx_sessions_country       ON tracking_sessions(country_code);
CREATE INDEX idx_sessions_utm_source    ON tracking_sessions(utm_source);

CREATE TABLE tracking_events (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    session_id      VARCHAR(64)  NOT NULL,
    visitor_id      VARCHAR(64)  NOT NULL,
    user_id         BIGINT       NULL,
    event_type      VARCHAR(40)  NOT NULL,
    path            VARCHAR(500) NULL,
    referrer        VARCHAR(500) NULL,
    country_code    VARCHAR(8)   NULL,
    country         VARCHAR(80)  NULL,
    city            VARCHAR(120) NULL,
    device          VARCHAR(20)  NULL,
    utm_source      VARCHAR(120) NULL,
    utm_medium      VARCHAR(120) NULL,
    utm_campaign    VARCHAR(160) NULL,
    properties      MEDIUMTEXT   NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_events_created_at      ON tracking_events(created_at);
CREATE INDEX idx_events_type_created    ON tracking_events(event_type, created_at);
CREATE INDEX idx_events_visitor_created ON tracking_events(visitor_id, created_at);
CREATE INDEX idx_events_session         ON tracking_events(session_id);
CREATE INDEX idx_events_utm_source      ON tracking_events(utm_source, created_at);
