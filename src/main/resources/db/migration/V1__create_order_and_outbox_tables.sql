create table fulfillment_orders (
  id uuid primary key,
  merchant_id varchar(100) not null,
  client_order_id varchar(100) not null,
  amount numeric(19,2) not null check (amount > 0),
  currency varchar(3) not null,
  status varchar(30) not null,
  inventory_reservation_id varchar(255), payment_authorization_id varchar(255), shipment_reservation_id varchar(255),
  failure_code varchar(255), version bigint not null default 0,
  created_at timestamptz not null, updated_at timestamptz not null,
  constraint uk_order_merchant_request unique (merchant_id, client_order_id)
);
create table outbox_events (
  id uuid primary key, aggregate_id uuid not null, event_type varchar(100) not null,
  payload text not null, created_at timestamptz not null, published_at timestamptz
);
create index idx_outbox_unpublished on outbox_events(created_at) where published_at is null;
