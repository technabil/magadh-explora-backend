-- =====================================================================
-- Magadh Explora — baseline schema
-- =====================================================================

-- Roles & Users -------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    mobile        VARCHAR(32),
    password_hash VARCHAR(120) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Categories ----------------------------------------------------------
CREATE TABLE categories (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug          VARCHAR(80)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    description   TEXT,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Destinations --------------------------------------------------------
CREATE TABLE destinations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug            VARCHAR(80)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    region          VARCHAR(120),
    description     TEXT,
    hero_image_url  VARCHAR(500),
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Packages ------------------------------------------------------------
CREATE TABLE packages (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug             VARCHAR(120)   NOT NULL UNIQUE,
    title            VARCHAR(200)   NOT NULL,
    summary          VARCHAR(500),
    description      TEXT,
    price_inr        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    duration_days    INT,
    mode             VARCHAR(20)    NOT NULL DEFAULT 'HOLIDAY',  -- PILGRIMAGE | HOLIDAY
    traveler_types   VARCHAR(255),                                -- CSV: COUPLE,SOLO,FAMILY,GROUP,SCHOOL,COLLEGE,CORPORATE
    itinerary        MEDIUMTEXT,
    inclusions       TEXT,
    exclusions       TEXT,
    is_published     BOOLEAN        NOT NULL DEFAULT FALSE,
    is_featured      BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE package_images (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id    BIGINT       NOT NULL,
    url           VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(200),
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_pi_pkg FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE package_categories (
    package_id  BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (package_id, category_id),
    CONSTRAINT fk_pc_pkg FOREIGN KEY (package_id)  REFERENCES packages(id)   ON DELETE CASCADE,
    CONSTRAINT fk_pc_cat FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE package_destinations (
    package_id     BIGINT NOT NULL,
    destination_id BIGINT NOT NULL,
    PRIMARY KEY (package_id, destination_id),
    CONSTRAINT fk_pd_pkg FOREIGN KEY (package_id)     REFERENCES packages(id)     ON DELETE CASCADE,
    CONSTRAINT fk_pd_dst FOREIGN KEY (destination_id) REFERENCES destinations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Blog ----------------------------------------------------------------
CREATE TABLE blogs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug            VARCHAR(150) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    excerpt         VARCHAR(500),
    content         LONGTEXT,
    cover_image_url VARCHAR(500),
    author          VARCHAR(120),
    published_at    TIMESTAMP NULL,
    is_published    BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Lead capture --------------------------------------------------------
CREATE TABLE contacts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    email      VARCHAR(180) NOT NULL,
    mobile     VARCHAR(32),
    subject    VARCHAR(200),
    message    TEXT         NOT NULL,
    status     VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE quotes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    email          VARCHAR(180) NOT NULL,
    mobile         VARCHAR(32),
    package_id     BIGINT NULL,
    num_travelers  INT,
    travel_date    DATE,
    message        TEXT,
    status         VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quotes_pkg FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE bookings (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NULL,
    package_id          BIGINT NOT NULL,
    name                VARCHAR(120) NOT NULL,
    email               VARCHAR(180) NOT NULL,
    mobile              VARCHAR(32),
    num_travelers       INT NOT NULL DEFAULT 1,
    travel_date         DATE,
    total_amount_inr    DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency            VARCHAR(8)    NOT NULL DEFAULT 'INR',
    total_amount_local  DECIMAL(12,2) NOT NULL DEFAULT 0,
    status              VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bk_user FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT fk_bk_pkg  FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Currency / settings -------------------------------------------------
CREATE TABLE currency_rates (
    currency_code VARCHAR(8)     PRIMARY KEY,
    rate_to_inr   DECIMAL(18,8)  NOT NULL,         -- 1 currency_code = X INR
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE app_settings (
    setting_key VARCHAR(120) PRIMARY KEY,
    value       TEXT,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Multilingual translations (covers packages, destinations, blogs, etc.)
CREATE TABLE translations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(40)  NOT NULL,             -- PACKAGE | DESTINATION | BLOG | CATEGORY
    entity_id   BIGINT       NOT NULL,
    lang        VARCHAR(8)   NOT NULL,             -- en, hi, ja, fr, de, es
    field       VARCHAR(40)  NOT NULL,             -- title, summary, description, ...
    value       MEDIUMTEXT,
    UNIQUE KEY uq_translation (entity_type, entity_id, lang, field)
) ENGINE=InnoDB;

-- Homepage layout (admin controls what shows where) -------------------
CREATE TABLE homepage_sections (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_key   VARCHAR(80)  NOT NULL UNIQUE,
    title         VARCHAR(150) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    max_items     INT          NOT NULL DEFAULT 6,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE homepage_section_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id    BIGINT      NOT NULL,
    entity_type   VARCHAR(40) NOT NULL,            -- PACKAGE | DESTINATION | BLOG
    entity_id     BIGINT      NOT NULL,
    display_order INT         NOT NULL DEFAULT 0,
    UNIQUE KEY uq_section_item (section_id, entity_type, entity_id),
    CONSTRAINT fk_hsi_section FOREIGN KEY (section_id) REFERENCES homepage_sections(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
-- Seed data
-- =====================================================================
INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

INSERT INTO app_settings (setting_key, value) VALUES
    ('pricing.markup.percent', '10'),
    ('currency.default',       'INR'),
    ('currency.allowed',       'INR,USD,EUR,GBP,JPY,AED,SGD,AUD,CAD'),
    ('language.default',       'en'),
    ('language.allowed',       'en,hi,ja,fr,de,es,zh');

-- Starter rates so the app boots even before the rates job runs.
-- Indicative values only; the cron job will refresh from a live source.
-- Convention: 1 <currency_code> = rate_to_inr INR
INSERT INTO currency_rates (currency_code, rate_to_inr) VALUES
    ('INR', 1.00000000),
    ('USD', 84.00000000),
    ('EUR', 92.00000000),
    ('GBP', 108.00000000),
    ('JPY', 0.55000000),
    ('AED', 22.85000000),
    ('SGD', 62.50000000),
    ('AUD', 55.00000000),
    ('CAD', 61.00000000);

INSERT INTO homepage_sections (section_key, title, display_order, is_active, max_items) VALUES
    ('featured_packages',    'Featured Tours',       1, TRUE, 6),
    ('popular_destinations', 'Popular Destinations', 2, TRUE, 6),
    ('latest_blogs',         'From the Blog',        3, TRUE, 3);
