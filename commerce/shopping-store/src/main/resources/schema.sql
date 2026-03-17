CREATE SCHEMA IF NOT EXISTS shopping_store_schema;

CREATE TABLE IF NOT EXISTS shopping_store_schema.products (
    product_id UUID PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    image_src VARCHAR(500),
    quantity_state VARCHAR(20) NOT NULL,
    product_state VARCHAR(20) NOT NULL,
    product_category VARCHAR(20),
    price DECIMAL(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_products_category ON shopping_store_schema.products(product_category);