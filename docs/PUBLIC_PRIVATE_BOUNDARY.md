# Public/private boundary

## Publication objective

The public edition must satisfy two goals that pull in opposite directions:

1. A technical reviewer should be able to run, inspect, test, and question a coherent system.
2. Publication should not transfer the complete implementation needed to reproduce or commercialize the private Aegis Sentinel system.

The solution is a **complete vertical slice with reduced breadth and deliberately different public policy mechanics**.

## Included publicly

- Dependency-free Java 17 execution layer
- Browser demonstration and HTTP API
- Image receipt, decoding, SHA-256, and integrity measurement
- Three controlled scenarios using original synthetic images
- A fixture adapter whose behavior is fully disclosed
- Simplified source-consensus classification
- Simplified trust score and rule thresholds
- Fail-closed `AUTO_EXECUTE`, `ESCALATE`, and `REJECT` routing
- Ordered governance trace
- Canonical evidence record
- Separate replay snapshot and manifest checksums
- Exact public-policy replay
- One-byte tamper simulation
- Cross-platform scripts and continuous verification
- Eighteen deterministic public checks

## Retained privately

- Real-model loading, model adapter, and model/runtime configuration
- Full detection filtering and calibration implementation
- Calibration tables, labeled fixtures, evaluation summaries, and tuning history
- Complete temporal tracking, scene understanding, and multi-source consensus logic
- Reliability, weather/corruption, drift, degradation, and safety-floor modules
- Complete governance integrations, thresholds, precedence matrices, and exception rules
- Full human-review queue, state machine, persistence, and reviewer audit behavior
- Complete decision evidence, append-only storage, deterministic export, mismatch taxonomy, and replay implementation
- Internal release metadata, verification records, failure logs, engineering journals, patch reports, and development history
- Full regression, boundary, adversarial, corruption, acceptance, real-model, and release-verification corpus
- Experimental and unpublished research/theory components

## Why this is not just a mock

The public system still performs real work:

- it decodes and measures image bytes;
- it enforces a hard integrity boundary;
- it computes consensus and trust using published public rules;
- it creates route explanations;
- it writes evidence files;
- it verifies file digests;
- it reruns the public policy from protected inputs;
- it refuses modified evidence.

The intentionally controlled component is object detection. It is labeled as a fixture adapter and never claims to be a trained model.

## Commercialization value preserved

A future product would still require the private capabilities that create operational depth: real perception integration, production calibration, broader policy behavior, domain configuration, review operations, complete evidence export/replay, security, deployment, observability, customer workflows, and extensive validation.

The public repository reveals the engineering concept and one representative implementation, but it does not provide a production-ready substitute for the private system.
