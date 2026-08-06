# Public showcase architecture

## Purpose

The public repository demonstrates the execution properties that matter most to a technical reviewer while avoiding disclosure of the complete private Aegis Sentinel implementation.

```text
Source observations
  -> byte receipt and SHA-256
  -> image decode
  -> integrity measurements
  -> controlled fixture evidence adapter
  -> source-consensus classification
  -> illustrative public trust score
  -> fail-closed governance route
  -> ordered rule trace
  -> canonical evidence record
  -> replay snapshot + manifest digests
  -> exact public-policy replay / tamper refusal
```

## Components

### `ImageIntegrityService`

Measures brightness, Laplacian sharpness, over/underexposure, and resolution. A failing result prevents any detector evidence from being accepted.

### `FixtureDetectionProvider`

Maps the SHA-256 values of the repository's original synthetic fixtures to controlled detections. The boxes and labels correspond to visible fixture elements. Unknown images receive no detections rather than synthetic claims.

This adapter is a demonstration boundary; it is not presented as a trained model.

### `PublicGovernanceService`

Uses deliberately simplified public thresholds and a small rule set. The source code explicitly states that these values are illustrative and do not represent the private policy.

The route priority is:

```text
REJECT > ESCALATE > AUTO_EXECUTE
```

An integrity failure cannot be weakened by downstream confidence. A source conflict cannot be averaged into automatic execution.

### `EvidenceService`

Writes three files per evidence ID:

```text
runtime/evidence/{evidenceId}/
  evidence.json
  replay.properties
  manifest.properties
```

- `evidence.json` is canonical JSON with sorted map keys.
- `replay.properties` contains only the public governance inputs necessary for deterministic replay.
- `manifest.properties` records separate SHA-256 values plus a package digest.

Files are created through temporary files and a move operation. Existing content under the same deterministic evidence ID must match exactly.

### `AegisHttpServer`

Uses the Java 17 built-in HTTP server, so the public showcase has no package-manager, framework, database, cloud, or API-key dependency. The complete private version remains a broader Spring Boot system.

## Deliberate limitations

- Controlled synthetic detection fixtures, not a public ML benchmark.
- Local filesystem storage, not a multi-tenant evidence service.
- No authentication, authorization, audit retention policy, or public deployment hardening.
- No production calibration or policy configuration.
- No claim that the public trust score is suitable for real-world autonomous decisions.
