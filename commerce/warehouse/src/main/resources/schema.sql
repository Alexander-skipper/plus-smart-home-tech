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