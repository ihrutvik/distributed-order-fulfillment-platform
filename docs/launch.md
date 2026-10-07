# Launch kit

## Repository description

Production-minded Java 17 order-fulfillment saga with Kafka, PostgreSQL, transactional outbox/inbox, idempotent inventory/payment/shipping participants, compensation, OpenTelemetry, Grafana, Kubernetes, and supply-chain security.

## Suggested GitHub topics

`java` · `spring-boot` · `kafka` · `postgresql` · `distributed-systems` · `saga-pattern` · `transactional-outbox` · `microservices` · `opentelemetry` · `kubernetes`

## LinkedIn launch post

I’ve completed a new backend engineering portfolio project: **Distributed Order Fulfillment Platform**.

It models a production-style order journey across inventory, payment, and shipment using an event-driven saga. The difficult part was not the happy path—it was making retries, duplicate messages, timeouts, partial failure, and compensation explicit and safe.

Key engineering decisions:

- Java 17 and Spring Boot with PostgreSQL and Kafka
- Transactional outbox plus durable inbox deduplication
- Payload-aware idempotency and database-enforced concurrency controls
- Independently idempotent inventory, payment, and shipment participants
- Reverse-order compensation and durable saga timeout recovery
- Prometheus, Grafana, OpenTelemetry, Jaeger, correlation IDs, and SLO alerts
- Testcontainers integration tests and k6 correctness/performance gates
- Docker Compose, Kubernetes/EKS manifests, CI/CD, CodeQL, Trivy, SBOMs, and build provenance

The project is intentionally original and provider-neutral. It contains no proprietary employer code or production data.

Repository: https://github.com/ihrutvik/distributed-order-fulfillment-platform

#Java #SpringBoot #Kafka #PostgreSQL #DistributedSystems #Microservices #BackendEngineering #Kubernetes #OpenTelemetry

## Interview summary

“I built an order-fulfillment coordinator that treats inventory, payment, and shipment as independently failing services. PostgreSQL transaction boundaries combine domain changes with outbox writes, Kafka provides at-least-once transport, and every consumer uses durable deduplication. Timeouts retry the original idempotent command before converting forward-stage exhaustion into the same compensation path used by business rejection. The project also includes tenant isolation, operational telemetry, performance gates, deployment manifests, and verifiable release artifacts.”

## Recruiter highlights

- Demonstrates distributed transaction design without relying on two-phase commit.
- Shows concrete handling of duplicate, missing, late, and out-of-order messages.
- Connects application logic to tests, observability, deployment, security, and release operations.
- Uses a stack aligned with senior JVM backend roles: Java, Spring Boot, Kafka, PostgreSQL, AWS patterns, Docker, and Kubernetes.
