# Distributed Order Fulfillment Platform

A production-minded Java 17 and Spring Boot reference platform for coordinating inventory, payment, and shipment through an event-driven saga with explicit compensation.

**Release:** 1.0.0 · **Status:** Complete and GitHub-ready · **License:** MIT

## Why this project matters

An order can cross multiple independently failing systems. A payment may succeed after an inventory timeout, a shipment reservation may fail after authorization, and messages may arrive more than once or out of order. This project demonstrates how to make those failure modes explicit and recoverable.

## Current foundation

- Order aggregate with strict saga state transitions
- Compensation lifecycle and external reservation references
- Merchant-scoped idempotent order creation with canonical SHA-256 request fingerprints
- Explicit `409 IDEMPOTENCY_CONFLICT` response when a key is reused with different order data
- PostgreSQL uniqueness constraints and optimistic locking
- Transactional outbox and Kafka relay
- `FOR UPDATE SKIP LOCKED` leasing for horizontal relay scaling
- Kafka-driven saga coordinator for inventory, payment, and shipment replies
- Transactional inventory participant with pessimistic SKU locking
- Atomic stock reservation, durable command deduplication, and compensating release
- Transactional payment authorization with durable authorization records
- Idempotent payment command handling and compensating authorization voids
- Transactional shipment-capacity reservation with database locking
- Automatic reverse-order compensation when shipment capacity is unavailable
- Destination-aware outbox routing for inventory, payment, shipment, and saga-reply topics
- Durable per-order saga deadlines with exponential-backoff command retries
- Automatic timeout-to-compensation conversion and visible stalled-compensation events
- SHA-256 API-key authentication with constant-time credential verification
- Least-privilege read/write scopes and merchant ownership enforcement
- Prometheus metrics for order requests, saga replies, deadline retries, and outbox publication
- Sanitized correlation IDs propagated in HTTP responses and MDC logs
- Prometheus scraping and actionable saga, availability, and HTTP-error alerts
- OpenTelemetry tracing across inbound HTTP, Kafka publication, and Kafka listeners
- OTLP export to a locally provisioned Jaeger trace backend
- Automatically provisioned Grafana dashboard for saga, API, outbox, and JVM health
- k6 performance suite for sustained traffic and concurrent idempotency races
- Production-oriented Kubernetes/EKS manifests with probes, HPA, disruption budget, and hardened container settings
- CodeQL and Trivy security scanning with automated Maven, Actions, and Docker dependency updates
- Tag-driven GHCR releases with JAR checksums, SPDX SBOM, and build-provenance attestations
- Durable inbox deduplication for at-least-once message delivery
- Reverse-order compensation commands for partial fulfillment failures
- Flyway-managed schema
- Docker Compose for PostgreSQL and Kafka
- OpenAPI, health endpoints, unit tests, PostgreSQL Testcontainers integration tests, and GitHub Actions
- CI verification of Maven tests, Compose configuration, Grafana dashboard JSON, and production image build

## Stack

Java 17 · Spring Boot 3 · PostgreSQL · Kafka · Flyway · JPA · Maven · Docker · JUnit 5 · Mockito

## Run locally

```bash
mvn clean package
docker compose up --build
```

Swagger UI: `http://localhost:8080/docs`

Prometheus: `http://localhost:9090` · Metrics: `http://localhost:8080/actuator/prometheus`

Jaeger trace search: `http://localhost:16686`

Grafana dashboard: `http://localhost:3000` (anonymous viewer enabled locally)

Run the load test after the stack is healthy:

```bash
docker compose --profile load-test run --rm load-test
```

## API example

```bash
curl -i -X POST http://localhost:8080/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Api-Key: demo-secret' \
  -d '{"clientOrderId":"checkout-20260919-001","amount":79.99,"currency":"EUR","sku":"DEMO-SKU","quantity":2}'
```

Repeating the same merchant, client order ID, and logical payload returns the existing order rather than starting a second fulfillment saga. Reusing that key with a different amount or currency returns `409 IDEMPOTENCY_CONFLICT`, preventing a retry bug from silently returning the wrong order.

Amounts and currencies are canonicalized before hashing, so `20`, `20.0`, and `20.00` are equivalent and currency casing does not cause false conflicts. PostgreSQL remains the final concurrency authority through its unique merchant/order constraint.

Additional runnable requests are available in [docs/api-examples.http](docs/api-examples.http).

## API security

API credentials are stored only as SHA-256 hashes and compared in constant time. Authentication establishes the merchant identity server-side, so callers cannot impersonate another tenant by changing a merchant header. `ORDERS_WRITE` protects creation, `ORDERS_READ` protects retrieval, and order lookup includes the authenticated merchant ID to prevent cross-tenant enumeration.

The seeded `demo-secret` credential is for local development only and maps to `merchant-42`. Production deployments should provision independently generated secrets through a secret manager and remove the demonstration row.

## Saga reliability

Participants reply through `order-saga-replies` with a unique `messageId`, `orderId`, outcome type, and optional reference or failure code. The coordinator applies the state transition, writes the next command to the outbox, and records the message in the durable inbox within one transaction. Redelivery therefore has no side effects. Invalid or out-of-order replies fail without being acknowledged and can be retried or routed to a dead-letter topic.

On shipment failure, compensation runs in reverse order: void the payment authorization, then release inventory, then mark the order cancelled. This avoids reporting cancellation before acquired resources have actually been released.

## Inventory consistency

Order acceptance writes a `RESERVE_INVENTORY` command to the transactional outbox. The inventory participant locks the SKU row with `SELECT ... FOR UPDATE`, atomically moves units from available to reserved, persists the reservation, records the command receipt, and writes its saga reply in one PostgreSQL transaction. Insufficient stock produces an `INVENTORY_REJECTED` reply without changing counters.

Compensation uses the persisted reservation as its source of truth. A repeated release command returns a successful reply but cannot increment available stock twice. Command message IDs are retained in a durable receipt table so Kafka redelivery has no side effects.

## Payment consistency

After inventory succeeds, the coordinator writes an `AUTHORIZE_PAYMENT` command to the outbox. The payment participant stores one authorization per order, records the command receipt, and emits `PAYMENT_AUTHORIZED` or `PAYMENT_DECLINED` in the same database transaction. A configurable limit acts as the deterministic local gateway policy, keeping the project runnable without external credentials.

If shipment later fails, `VOID_PAYMENT` locks the authorization row before changing it to `VOIDED`. Repeated commands cannot create a second authorization or void a different order. The authorization record remains available as an audit trail after compensation completes.

## Shipment consistency

After payment authorization, the coordinator emits `RESERVE_SHIPMENT`. The shipment participant pessimistically locks the configured fulfillment-zone capacity, consumes one slot, stores one reservation per order, records the command receipt, and writes the saga reply atomically. Concurrent orders therefore cannot overbook the final slot.

When capacity is exhausted or the zone is unsupported, `SHIPMENT_REJECTED` starts reverse-order compensation: the payment authorization is voided first, inventory is released second, and only then is the order marked cancelled. A successful shipment reservation confirms the order and emits its lifecycle event.

## Timeout recovery

Every in-flight participant command is stored in a durable saga-deadline row together with its logical destination and next-attempt time. A horizontally safe worker leases due deadlines with `FOR UPDATE SKIP LOCKED`, republishes the original idempotent command, and applies capped exponential backoff. Receiving a valid participant reply atomically replaces the deadline with the next saga stage.

After the configured retry budget is exhausted, a forward stage emits a synthetic rejection through the normal reply topic. The coordinator then uses the same tested compensation path as a real business rejection. Exhausted compensation is not treated as success: it emits `COMPENSATION_STALLED` and leaves the order visibly compensating for operator intervention.

## Operational visibility

The platform publishes bounded-cardinality Prometheus counters for accepted, replayed, and conflicting order requests; applied and duplicate saga replies; retried and exhausted deadlines; and outbox publications by logical destination. Every HTTP request receives an `X-Correlation-Id`; safe caller values are preserved, while malformed values are replaced before entering the logging MDC.

Docker Compose provisions Prometheus with alerts for exhausted saga retry budgets, sustained server errors, and an unavailable application target. These signals connect business workflow health to infrastructure health without putting order IDs, merchant IDs, or other unbounded values into metric labels.

Grafana starts with the Prometheus data source and the `Distributed Order Fulfillment` dashboard already provisioned. It shows request outcomes, saga replies, deadline exhaustion, p95 API latency, HTTP 5xx rate, outbox throughput, and JVM heap pressure. No manual dashboard import is required.

Micrometer Tracing bridges Spring observations to OpenTelemetry. HTTP server spans and Kafka producer/consumer spans carry W3C trace context through the asynchronous saga, while the application exports OTLP traces to Jaeger. Logs include trace ID, span ID, and the request correlation ID, allowing an operator to move from an alert to a trace and then to the exact related logs. Local development samples every trace; production can lower `TRACING_SAMPLE_PROBABILITY` without a rebuild.

## Test strategy

Fast unit tests cover aggregate transitions, saga ordering, inventory success and rejection, payment authorization and decline, shipment capacity and replay, timeout retries and exhaustion, compensation, duplicate delivery, fingerprint normalization, metrics, correlation-ID hygiene, tracing, and deployment health configuration. Testcontainers starts PostgreSQL 16 to apply the real Flyway migrations and verify database-enforced order uniqueness and inbox deduplication.

The k6 suite generates a sustained 20 new-order operations per second while immediately replaying each request, plus a separate 20-client race against shared idempotency keys. Release thresholds require fewer than 1% failed HTTP requests, p95 below 500 ms, p99 below 1 second, more than 99% successful logical operations, and zero cases where an identical replay returns a different order ID. These are reproducible portfolio baselines, not claims about results that have not been measured in the target deployment.

## Architecture

See [architecture decisions](docs/architecture.md) for the saga flow, consistency boundaries, compensation strategy, and delivery guarantees.

See the [production deployment guide](docs/deployment.md) for the EKS, RDS, MSK, Secrets Manager, OpenTelemetry, rollout, and rollback model.

See the [release process](docs/releasing.md) and [security policy](SECURITY.md) for artifact integrity, SBOM, provenance, vulnerability reporting, and rollback expectations.

The [launch kit](docs/launch.md) contains the repository description, suggested topics, LinkedIn launch post, interview summary, and recruiter highlights.

## License

MIT
