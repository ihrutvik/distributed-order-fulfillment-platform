CREATE TABLE saga_deadlines (
  id UUID PRIMARY KEY,
  order_id UUID NOT NULL REFERENCES fulfillment_orders(id),
  stage VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  destination VARCHAR(40) NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  next_attempt_at TIMESTAMPTZ NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_saga_deadline_order UNIQUE(order_id)
);

CREATE INDEX idx_saga_deadline_due ON saga_deadlines(next_attempt_at) WHERE completed=FALSE;
