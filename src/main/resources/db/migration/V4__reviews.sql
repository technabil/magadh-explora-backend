-- =====================================================================
-- Phase 9: package reviews
-- =====================================================================

CREATE TABLE reviews (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    package_id   BIGINT       NOT NULL,
    author_name  VARCHAR(120) NOT NULL,
    author_email VARCHAR(180) NULL,
    rating       INT          NOT NULL,
    title        VARCHAR(180) NULL,
    body         TEXT         NOT NULL,
    approved     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_package FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE,
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reviews_package_approved ON reviews(package_id, approved);
CREATE INDEX idx_reviews_approved_created ON reviews(approved, created_at);
