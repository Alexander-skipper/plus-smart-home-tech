CREATE SCHEMA IF NOT EXISTS delivery_schema;

CREATE TABLE IF NOT EXISTS delivery_schema.deliveries (
    delivery_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    from_country VARCHAR(100),
    from_city VARCHAR(100),
    from_street VARCHAR(100),
    from_house VARCHAR(20),
    from_flat VARCHAR(20),
    to_country VARCHAR(100),
    to_city VARCHAR(100),
    to_street VARCHAR(100),
    to_house VARCHAR(20),
    to_flat VARCHAR(20),
    delivery_state VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_deliveries_order_id ON delivery_schema.deliveries(order_id);