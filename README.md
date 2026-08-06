# Aegis Sentinel — Public Showcase

[View the Aegis Sentinel Engineering Portfolio (PDF)](docs/Aegis_Sentinel_Engineering_Portfolio.pdf)
[![Java 17](https://img.shields.io/badge/Java-17-2f74c0)](https://adoptium.net/)
[![Verification](https://github.com/Edgar-Fu/aegis-sentinel-showcase/actions/workflows/verify.yml/badge.svg)](https://github.com/Edgar-Fu/aegis-sentinel-showcase/actions/workflows/verify.yml)
[![License: Evaluation Only](https://img.shields.io/badge/license-evaluation--only-8b5cf6)](LICENSE)

A recruiter-facing, source-available demonstration of **governed computer vision**: the system checks evidence quality, keeps source conflict distinct from low confidence, selects a fail-closed route, writes canonical decision evidence, and verifies that the stored result can be replayed without concealing tampering.

**Guided overview:** <https://edgar-fu.github.io/aegis-sentinel-showcase/>

> **Public/private boundary:** this repository is a self-contained showcase, not the complete Aegis Sentinel implementation. It uses a deliberately simplified public policy and a controlled synthetic-fixture detection adapter. The private implementation's real-model adapter, calibration corpus, complete policy definitions, advanced reliability modules, full review workflow, and extensive verification harness are intentionally excluded.

## What you can demonstrate

| Scenario | What happens | Final route |
|---|---|---|
| Corroborated evidence | Two synthetic sources pass integrity and agree | `AUTO_EXECUTE` |
| Independent-source conflict | One source introduces safety-critical conflicting evidence | `ESCALATE` |
| Integrity rejection | A heavily blurred frame is stopped before detector evidence is accepted | `REJECT` |
| Evidence replay | Stored inputs are evaluated again under the same public policy | `MATCH` |
| Tamper simulation | A one-byte evidence change alters SHA-256 | `TAMPER_DETECTED` |

The local browser application persists actual evidence files under `runtime/evidence/`; the results are not prerecorded UI screenshots.

## Run it on Windows

Requirements:

- JDK 17 or newer
- PowerShell 5.1 or newer
- No Maven, database, API key, model download, or external service

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

Then open:

```text
http://localhost:8080
```

## Run it on macOS or Linux

```bash
./scripts/verify.sh
./scripts/run.sh
```

Then open `http://localhost:8080`.

## Five-minute recruiter walkthrough

1. Run **Corroborated evidence** and point out two passing source records, `AGREEMENT`, the ordered rule trace, and `AUTO_EXECUTE`.
2. Run **Source conflict** and show that a spill/blocked-exit disagreement becomes `ESCALATE`, not an averaged-away success.
3. Run **Integrity failure** and show that the detector stage is `SKIPPED_AFTER_INTEGRITY_REJECTION`.
4. Use **Replay stored decision** to obtain `MATCH`.
5. Use **Simulate one-byte tamper** to obtain `TAMPER_DETECTED` without modifying the original stored package.

A more detailed script is in [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md).

## Public architecture

```text
Image bytes + source ID
        |
        v
Image decoding and SHA-256 receipt
        |
        v
Integrity gate (sharpness, exposure, brightness, resolution)
        |
        v
Controlled synthetic-fixture evidence adapter
        |
        v
Independent-source consensus
        |
        v
Illustrative public trust gate
        |
        v
PASS / REVIEW / STOP
        |
        v
AUTO_EXECUTE / ESCALATE / REJECT
        |
        v
Canonical evidence + protected replay snapshot + package manifest
        |
        +--> exact-policy replay
        +--> one-byte tamper refusal
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for design details.

## Why the detector is controlled

The complete private version integrates a real object-detection model. That adapter and its surrounding calibration/validation machinery are not published here. Instead, the public repository includes four original synthetic fixtures with deterministic detections that correspond to visible objects in those images.

This choice provides three useful properties:

- a recruiter can run the demo immediately with no model download;
- verification remains deterministic and offline;
- the public repository proves orchestration, safety routing, evidence integrity, and replay without releasing the complete model/calibration implementation.

For an arbitrary uploaded image, Aegis performs real image-integrity analysis but returns no invented object detections. The system therefore escalates instead of pretending that an unavailable public model produced evidence.

## Public versus private scope

| Included here | Retained privately |
|---|---|
| Complete runnable showcase flow | Real-model loading and production adapter |
| Image integrity measurements | Full calibration datasets and calibrated policy tables |
| Controlled multi-source examples | Advanced temporal tracking and consensus machinery |
| Simplified public trust policy | Complete thresholds, precedence rules, and domain policies |
| Ordered governance trace | Full decision-trace/reason taxonomy and replay internals |
| Canonical evidence and SHA-256 checks | Complete append-only evidence/export implementation |
| Exact public-policy replay | Broader human-review workflow and persistence |
| 18 deterministic verification checks | Full regression, boundary, corruption, and release gates |
| Original synthetic sample images | Internal evaluation corpora and experimental modules |

The exact boundary and publication rationale are documented in [docs/PUBLIC_PRIVATE_BOUNDARY.md](docs/PUBLIC_PRIVATE_BOUNDARY.md).

## Verification

The included verification suite compiles the application using only JDK tools and checks:

- corroborated evidence routes to `AUTO_EXECUTE`;
- conflicting evidence routes to `ESCALATE`;
- blurred evidence routes to `REJECT`;
- integrity rejection prevents detector evidence;
- evidence files are persisted;
- protected replay returns `MATCH`;
- a one-byte modification is refused;
- canonical JSON and SHA-256 output are stable.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify.ps1
```

Expected final line:

```text
PUBLIC SHOWCASE VERIFICATION PASS - 18 checks
```

GitHub Actions runs the same verification on every push and pull request.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Public release identity and health |
| `GET` | `/api/scenarios` | List controlled scenarios |
| `POST` | `/api/scenarios/trusted` | Execute corroborated evidence scenario |
| `POST` | `/api/scenarios/conflict` | Execute independent-source conflict |
| `POST` | `/api/scenarios/rejected` | Execute integrity rejection |
| `POST` | `/api/analyze` | Analyze raw PNG/JPEG bytes; use `X-Source-Id` and `X-Filename` headers |
| `POST` | `/api/evidence/{id}/replay` | Verify and replay stored public-policy evidence |
| `POST` | `/api/evidence/{id}/tamper-check` | Simulate a one-byte change and prove refusal |

Example:

```bash
curl -X POST http://localhost:8080/api/scenarios/trusted
```

## Repository layout

```text
.
├── src/main/java/             # Public Java execution layer
├── src/test/java/             # Dependency-free verification suite
├── web/                       # Local interactive browser demo
├── docs/                      # GitHub Pages overview and engineering docs
├── sample_images/             # Original synthetic controlled fixtures
├── scripts/                   # Windows and Unix build/run/verify commands
├── runtime/evidence/          # Generated evidence; ignored except .gitkeep
├── .github/workflows/         # Continuous verification
├── LICENSE                    # Narrow evaluation permission; not open source
└── README.md
```

## Security and data handling

- No credentials, API keys, external accounts, or private datasets are required.
- Uploaded images are processed locally and are not sent to a third party.
- Runtime evidence is written only to `runtime/evidence/` and is excluded by `.gitignore`.
- The server binds to the configured local port and has no authentication; it is a local engineering showcase, not a production service.
- See [SECURITY.md](SECURITY.md) for responsible reporting and deployment limitations.

## License and IP

This repository is **source available for evaluation**, not open source. The included [evaluation license](LICENSE) permits cloning and local execution for employment/recruiting evaluation and personal technical review. It does not grant commercial use, redistribution, hosted-service use, incorporation into another product, or a patent license.

Making a GitHub repository public necessarily allows GitHub's platform functions, including viewing and forking, under GitHub's Terms of Service. The license controls additional use of the code outside those platform rights.

## Author

**Edgar Fu**  
GitHub: <https://github.com/Edgar-Fu>
