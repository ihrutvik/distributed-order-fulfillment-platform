# Security policy

Please do not open public issues for suspected vulnerabilities. Use GitHub's private vulnerability-reporting feature for this repository and include the affected version, reproduction steps, impact, and any suggested mitigation.

The latest tagged release is supported. Secrets, customer data, and proprietary provider code must never be included in reports, tests, examples, or commits.

Automated controls include CodeQL analysis, Trivy filesystem scanning, Dependabot updates, non-root containers, hashed API credentials, tenant-scoped authorization, bounded observability labels, and signed build provenance for published container images.
