# Release process

Releases are immutable and tag-driven. Pushing a `v*` tag runs the full Maven verification suite, packages the JAR, records its SHA-256 checksum, produces an SPDX JSON software bill of materials, publishes a tagged container to GitHub Container Registry, creates a build-provenance attestation, and attaches the release assets to GitHub.

## Maintainer checklist

1. Confirm CI and security workflows pass on `main`.
2. Review database migrations for backward compatibility.
3. Run the k6 acceptance suite against the candidate environment.
4. Update release notes with behavior changes and operational risks.
5. Create an annotated semantic-version tag, for example `git tag -a v1.0.0 -m "v1.0.0"`.
6. Push the tag and verify the JAR checksum, SBOM, container digest, and provenance attestation.
7. Deploy the immutable image digest through the canary sequence in `docs/deployment.md`.

Rollback uses the previous verified image digest. Database changes must follow expand-and-contract migration rules so application rollback does not require a destructive schema rollback.
