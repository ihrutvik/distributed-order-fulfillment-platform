CREATE TABLE shipment_capacity (
  zone VARCHAR(50) PRIMARY KEY,
  available_slots INTEGER NOT NULL CHECK (available_slots >= 0),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE shipment_reservations (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
  zone VARCHAR(50) NOT NULL REFERENCES shipment_capacity(zone),
  payment_authorization_id VARCHAR(255) NOT NULL,
  reserved_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_shipment_reservation_order UNIQUE(order_id)
);

CREATE TABLE shipment_command_receipts (
  message_id UUID PRIMARY KEY,
  handled_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shipment_reservation_zone ON shipment_reservations(zone,reserved_at);
INSERT INTO shipment_capacity(zone,available_slots) VALUES ('DEFAULT',100);
