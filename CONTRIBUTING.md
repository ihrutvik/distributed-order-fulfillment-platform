# Contributing

Use Java 17, keep changes backward compatible, and include tests for business invariants and failure paths. Run `mvn verify` and `docker compose config --quiet` before opening a pull request.

Database changes require an additive Flyway migration; never edit an applied migration. Kafka consumers must remain idempotent, and state changes that require an event must use the transactional outbox. Do not add order IDs, merchant IDs, message IDs, or correlation IDs as metric labels.

Pull requests should explain the failure mode addressed, transaction boundary, delivery semantics, rollout plan, and operational signal. Never include proprietary employer code, real credentials, customer data, or production endpoints.
