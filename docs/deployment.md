# Production deployment guide

The manifests under `deploy/kubernetes` provide an opinionated EKS starting point. Replace the image tag, RDS endpoint, IAM account ID, Kafka bootstrap address, and secrets before deployment. Never apply `secret.example.yaml` unchanged.

## AWS mapping

- Amazon EKS runs three or more application replicas across availability zones.
- Amazon RDS for PostgreSQL provides Multi-AZ persistence and automated backups.
- Amazon MSK supplies Kafka with TLS and IAM/SASL authentication added per environment.
- AWS Secrets Manager plus the External Secrets Operator should create `order-platform-secrets`.
- AWS Distro for OpenTelemetry can receive OTLP and export traces to X-Ray or another backend.
- An Application Load Balancer Ingress can expose the service with TLS and AWS WAF.

The service account contains an IRSA placeholder. Grant only the exact AWS API permissions required; database and Kafka credentials do not belong in the image or ConfigMap.

## Availability behavior

Rolling updates allow zero unavailable replicas and one surge replica. Startup, liveness, and readiness probes use Spring Boot availability groups. Graceful shutdown gets 30 seconds after a short pre-stop drain, while the pod termination grace period provides the enclosing 45-second budget.

The disruption budget keeps at least two replicas available during voluntary maintenance. The HPA scales from 3 to 12 replicas at 65% average CPU, doubles capacity at most once per minute, and waits five minutes before scaling down to avoid oscillation.

## Release sequence

1. Build and scan an immutable image tagged with the Git commit SHA.
2. Apply Flyway migrations through a controlled deployment or migration job.
3. Deploy one canary replica and verify health, error rate, saga timeouts, and trace export.
4. Roll out remaining replicas with `maxUnavailable: 0`.
5. Run the k6 acceptance suite against the release environment.
6. Promote only when alert, latency, correctness, and idempotency thresholds pass.
