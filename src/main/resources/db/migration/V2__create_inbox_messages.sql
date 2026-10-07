create table inbox_messages (
  message_id uuid primary key,
  order_id uuid not null references fulfillment_orders(id),
  event_type varchar(100) not null,
  handled_at timestamptz not null
);
create index idx_inbox_order on inbox_messages(order_id, handled_at);
