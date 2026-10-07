CREATE TABLE payment_authorizations (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
  amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(20) NOT NULL,
  authorized_at TIMESTAMPTZ NOT NULL,
  voided_at TIMESTAMPTZ,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_payment_authorization_order UNIQUE(order_id),
  CONSTRAINT ck_payment_void_time CHECK ((status='AUTHORIZED' AND voided_at IS NULL) OR (status='VOIDED' AND voided_at IS NOT NULL))
);

CREATE TABLE payment_command_receipts (
  message_id UUID PRIMARY KEY,
  handled_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_authorization_status ON payment_authorizations(status,authorized_at);
