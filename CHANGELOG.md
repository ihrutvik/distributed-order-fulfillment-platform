# Changelog

## 1.0.0 — 2026-10-03

- Event-driven order-fulfillment saga covering inventory, payment, shipment, confirmation, and reverse-order compensation.
- Transactional outbox and durable inbox with Kafka, PostgreSQL locking, optimistic concurrency, and at-least-once delivery safety.
- Payload-aware merchant idempotency, participant command deduplication, durable saga deadlines, retries, and visible compensation stalls.
- Tenant-scoped API-key security, cross-tenant isolation, Prometheus metrics, OpenTelemetry tracing, Jaeger, Grafana, and SLO alerts.
- PostgreSQL Testcontainers coverage, k6 correctness/performance gates, Docker Compose, Kubernetes/EKS manifests, and AWS deployment guidance.
- CI, CodeQL, Trivy, Dependabot, GHCR releases, SPDX SBOMs, checksums, and provenance attestations.
