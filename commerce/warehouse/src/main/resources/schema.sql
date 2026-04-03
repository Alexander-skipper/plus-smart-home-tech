CREATE SCHEMA IF NOT EXISTS warehouse_schema;

CREATE TABLE IF NOT EXISTS warehouse_schema.warehouse_items (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE,
    quantity BIGINT NOT NULL DEFAULT 0,
    width DECIMAL(10, 2) NOT NULL,
    height DECIMAL(10, 2) NOT NULL,
    depth DECIMAL(10, 2) NOT NULL,
    weight DECIMAL(10, 2) NOT NULL,
    fragile BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_warehouse_product ON warehouse_schema.warehouse_items(product_id);

CREATE TABLE IF NOT EXISTS warehouse_schema.order_bookings (
    booking_id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    delivery_id UUID,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS warehouse_schema.booking_products (
    booking_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    PRIMARY KEY (booking_id, product_id),
    FOREIGN KEY (booking_id) REFERENCES warehouse_schema.order_bookings(booking_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_bookings_order_id ON warehouse_schema.order_bookings(order_id);