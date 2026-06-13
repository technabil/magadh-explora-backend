-- =====================================================================
-- Journey posts — user-generated photos / videos / reels submitted from
-- the homepage "Share Your Journey" section. Moderated before public.
-- =====================================================================

CREATE TABLE journey_posts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120) NOT NULL,
    location        VARCHAR(160) NULL,
    caption         VARCHAR(500) NULL,
    media_type      VARCHAR(16)  NOT NULL DEFAULT 'photo',
    media_url       VARCHAR(500) NOT NULL,
    video_url       VARCHAR(500) NULL,
    likes           INT          NOT NULL DEFAULT 0,
    approved        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_journey_approved_type ON journey_posts(approved, media_type, created_at);
CREATE INDEX idx_journey_created       ON journey_posts(created_at);
