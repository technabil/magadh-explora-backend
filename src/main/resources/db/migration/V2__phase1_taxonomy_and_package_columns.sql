-- =====================================================================
-- Phase 1: extend categories and packages
-- =====================================================================

-- Add a "kind" column so admin can manage multiple taxonomies
-- (THEME = Buddhist/Jain/Heritage, TIER = Essential/Premium/Spiritual,
--  GENERIC = anything else admin invents).
ALTER TABLE categories
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'GENERIC' AFTER slug;

CREATE INDEX idx_categories_kind ON categories(kind);

-- Extend packages with the fields the existing UI displays
ALTER TABLE packages
    ADD COLUMN original_price_inr DECIMAL(12,2) NULL  AFTER price_inr,
    ADD COLUMN rating              DECIMAL(3,2)  NULL  AFTER duration_days,
    ADD COLUMN reviews_count       INT           NOT NULL DEFAULT 0 AFTER rating,
    ADD COLUMN group_size_min      INT           NULL  AFTER reviews_count,
    ADD COLUMN group_size_max      INT           NULL  AFTER group_size_min,
    ADD COLUMN hero_image_url      VARCHAR(500)  NULL  AFTER group_size_max;

CREATE INDEX idx_packages_published ON packages(is_published);
CREATE INDEX idx_packages_featured  ON packages(is_featured);
CREATE INDEX idx_packages_mode      ON packages(mode);

-- Seed default taxonomies so the public site keeps working.
INSERT INTO categories (slug, kind, name, display_order, is_active) VALUES
    ('buddhist', 'THEME', 'Buddhist',  1, TRUE),
    ('jain',     'THEME', 'Jain',      2, TRUE),
    ('heritage', 'THEME', 'Heritage',  3, TRUE),
    ('adventure','THEME', 'Adventure', 4, TRUE),
    ('mixed',    'THEME', 'Mixed',     5, TRUE),
    ('essential','TIER',  'Essential', 1, TRUE),
    ('premium',  'TIER',  'Premium',   2, TRUE),
    ('spiritual','TIER',  'Spiritual', 3, TRUE);

-- Seed the destinations referenced in the existing public site
INSERT INTO destinations (slug, name, region, is_active) VALUES
    ('bodh-gaya',   'Bodh Gaya',   'Gaya',     TRUE),
    ('rajgir',      'Rajgir',      'Nalanda',  TRUE),
    ('nalanda',     'Nalanda',     'Nalanda',  TRUE),
    ('vaishali',    'Vaishali',    'Vaishali', TRUE),
    ('pawapuri',    'Pawapuri',    'Nalanda',  TRUE),
    ('kundalpur',   'Kundalpur',   'Nalanda',  TRUE),
    ('gaya',        'Gaya',        'Gaya',     TRUE),
    ('sasaram',     'Sasaram',     'Rohtas',   TRUE),
    ('patna',       'Patna',       'Patna',    TRUE),
    ('rohtasgarh',  'Rohtasgarh',  'Rohtas',   TRUE),
    ('vikramshila', 'Vikramshila', 'Bhagalpur',TRUE),
    ('kesariya',    'Kesariya',    'East Champaran', TRUE);
