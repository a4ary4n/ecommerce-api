-- =========================================
-- 1. categories
-- =========================================
CREATE TABLE categories (
    id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    slug    VARCHAR(100) NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL
);

-- =========================================
-- 2. brands
-- =========================================
CREATE TABLE brands (
    id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(150) NOT NULL UNIQUE
);

-- =========================================
-- 3. products
-- =========================================
CREATE TABLE products (
    id                      INT UNSIGNED PRIMARY KEY,        -- reuse dummyjson's own product id
    title                   VARCHAR(255) NOT NULL,
    description             TEXT,
    category_id             INT UNSIGNED NOT NULL,
    brand_id                INT UNSIGNED,                    -- nullable: some products have no brand
    sku                     VARCHAR(50) UNIQUE,
    price                   DECIMAL(10,2) NOT NULL,
    discount_percentage     DECIMAL(5,2) DEFAULT 0,
    rating                  DECIMAL(3,2) DEFAULT 0,          -- kept independent of reviews table, see note below
    stock                   INT UNSIGNED DEFAULT 0,
    availability_status     ENUM('IN_STOCK','LOW_STOCK','OUT_OF_STOCK') NOT NULL,
    weight                  DECIMAL(8,2),
    width                   DECIMAL(8,2),
    height                  DECIMAL(8,2),
    depth                   DECIMAL(8,2),
    warranty_information    VARCHAR(255),
    shipping_information    VARCHAR(255),
    return_policy           VARCHAR(255),
    minimum_order_quantity  INT UNSIGNED DEFAULT 1,
    thumbnail               VARCHAR(500),
    barcode                 VARCHAR(50),
    qr_code                 VARCHAR(500),
    created_at              DATETIME,
    updated_at              DATETIME,

    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (brand_id) REFERENCES brands(id),
    INDEX idx_category (category_id),
    INDEX idx_brand (brand_id)
);

-- =========================================
-- 4. reviews
-- =========================================
CREATE TABLE reviews (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id      INT UNSIGNED NOT NULL,
    rating          TINYINT UNSIGNED NOT NULL,
    comment         TEXT,
    reviewer_name   VARCHAR(150),
    reviewer_email  VARCHAR(255),
    review_date     DATETIME,

    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_product (product_id)
);

-- =========================================
-- 5. product_images
-- =========================================
CREATE TABLE product_images (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id      INT UNSIGNED NOT NULL,
    url             VARCHAR(500) NOT NULL,
    sort_order      SMALLINT UNSIGNED DEFAULT 0,

    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_product (product_id)
);

-- =========================================
-- 6. tags
-- =========================================
CREATE TABLE tags (
    id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

-- =========================================
-- 7. product_tags (many-to-many)
-- =========================================
CREATE TABLE product_tags (
    product_id  INT UNSIGNED NOT NULL,
    tag_id      INT UNSIGNED NOT NULL,

    PRIMARY KEY (product_id, tag_id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);