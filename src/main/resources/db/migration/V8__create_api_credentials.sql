CREATE TABLE api_credentials (
  id UUID PRIMARY KEY,
  merchant_id VARCHAR(100) NOT NULL,
  key_hash VARCHAR(64) NOT NULL,
  scopes VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_api_credential_hash UNIQUE(key_hash),
  CONSTRAINT ck_api_credential_hash CHECK (length(key_hash)=64)
);

CREATE INDEX idx_api_credential_merchant ON api_credentials(merchant_id) WHERE active=TRUE;

-- Local demonstration credential only: X-Api-Key demo-secret
INSERT INTO api_credentials(id,merchant_id,key_hash,scopes,active,created_at)
VALUES ('00000000-0000-0000-0000-000000000001','merchant-42','cd577fe2561ebff23505db0bb006300c7cdecbd46bc0e03c449afafaca2c25bf','ORDERS_READ,ORDERS_WRITE',TRUE,now());
