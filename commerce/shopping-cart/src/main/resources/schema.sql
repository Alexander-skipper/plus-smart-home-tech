CREATE SCHEMA IF NOT EXISTS shopping_cart_schema;

CREATE TABLE IF NOT EXISTS shopping_cart_schema.shopping_carts (
    shopping_cart_id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS shopping_cart_schema.cart_products (
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    PRIMARY KEY (cart_id, product_id),
    FOREIGN KEY (cart_id) REFERENCES shopping_cart_schema.shopping_carts(shopping_cart_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cart_username ON shopping_cart_schema.shopping_carts(username);