ALTER TABLE fulfillment_orders ADD COLUMN request_fingerprint VARCHAR(64);
ALTER TABLE fulfillment_orders ADD CONSTRAINT ck_order_request_fingerprint
  CHECK (request_fingerprint IS NULL OR length(request_fingerprint) = 64);

