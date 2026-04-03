CREATE SCHEMA IF NOT EXISTS payment_schema;

CREATE TABLE IF NOT EXISTS payment_schema.payments (
    payment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    total_payment DECIMAL(10, 2),
    delivery_total DECIMAL(10, 2),
    fee_total DECIMAL(10, 2),
    state VARCHAR(20) NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payment_schema.payments(order_id);