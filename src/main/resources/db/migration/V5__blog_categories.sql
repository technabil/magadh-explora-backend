-- =====================================================================
-- Blog ↔ Category many-to-many join table
-- =====================================================================

CREATE TABLE blog_categories (
    blog_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (blog_id, category_id),
    CONSTRAINT fk_bc_blog     FOREIGN KEY (blog_id)     REFERENCES blogs(id)      ON DELETE CASCADE,
    CONSTRAINT fk_bc_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bc_category ON blog_categories(category_id);
