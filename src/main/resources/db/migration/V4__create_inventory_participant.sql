ALTER TABLE fulfillment_orders ADD COLUMN sku VARCHAR(100);
ALTER TABLE fulfillment_orders ADD COLUMN quantity INTEGER;
UPDATE fulfillment_orders SET sku='DEMO-SKU', quantity=1 WHERE sku IS NULL;
ALTER TABLE fulfillment_orders ALTER COLUMN sku SET NOT NULL;
ALTER TABLE fulfillment_orders ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE fulfillment_orders ADD CONSTRAINT ck_order_quantity CHECK (quantity > 0);

ALTER TABLE outbox_events ADD COLUMN destination VARCHAR(40) NOT NULL DEFAULT 'LIFECYCLE';

CREATE TABLE inventory_stock (
  sku VARCHAR(100) PRIMARY KEY,
  available INTEGER NOT NULL CHECK (available >= 0),
  reserved INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
  version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE inventory_reservations (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
  sku VARCHAR(100) NOT NULL REFERENCES inventory_stock(sku),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  released_at TIMESTAMPTZ,
  CONSTRAINT uk_inventory_reservation_order UNIQUE(order_id)
);

CREATE TABLE inventory_command_receipts (
  message_id UUID PRIMARY KEY,
  handled_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_inventory_reservation_status ON inventory_reservations(status,created_at);
INSERT INTO inventory_stock(sku,available,reserved) VALUES ('DEMO-SKU',100,0);
