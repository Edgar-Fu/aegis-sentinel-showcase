# Recruiter demo guide

## Prepare

On Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

Open `http://localhost:8080`.

## 0:00–1:00 — State the problem

> Object detection alone does not answer whether evidence is reliable enough to act on. Aegis separates perception from governance, refuses bad inputs, routes source conflict to a person, and records evidence that can be verified and replayed.

Point to the seven-stage architecture row.

## 1:00–2:00 — Corroborated evidence

Run **Corroborated evidence**.

Show:

- two independent source IDs;
- passing image-integrity measurements;
- agreeing controlled detections;
- `AGREEMENT` consensus;
- the ordered governance trace;
- `AUTO_EXECUTE`;
- the persisted evidence ID and SHA-256.

## 2:00–3:00 — Independent-source conflict

Run **Source conflict**.

Show:

- the second source reports `spill` and `blocked_exit`;
- source conflict is not collapsed into an average confidence;
- `SOURCE_CONFLICT` remains in the reason list;
- the final route becomes `ESCALATE`.

## 3:00–4:00 — Fail closed before detection

Run **Integrity failure**.

Show:

- `BLUR` in the integrity failures;
- detector mode `SKIPPED_AFTER_INTEGRITY_REJECTION`;
- final route `REJECT`.

Explain that the system does not call perception and then hope governance fixes an invalid input.

## 4:00–5:00 — Replay and tamper evidence

Return to the trusted scenario.

1. Select **Replay stored decision** and show `integrityStatus: PASS` and `replayStatus: MATCH`.
2. Select **Simulate one-byte tamper** and show `TAMPER_DETECTED` plus the changed digest.

## Answering scope questions

**Is this the complete Aegis Sentinel codebase?**  
No. It is a purpose-built public showcase. The private version contains the real-model adapter, broader calibration and reliability system, complete policy and replay implementation, full review workflow, and a much larger validation harness.

**Is the UI faking the result?**  
No. The local Java backend executes each scenario, persists evidence, and performs replay/tamper checks. The detection adapter is controlled and deterministic rather than a hidden model, and the repository says so explicitly.

**Is it production ready?**  
No. It is an engineering showcase of fail-closed routing, evidence integrity, and reproducibility.

**Why not publish the full implementation?**  
The public edition gives reviewers enough code to inspect engineering quality while preserving the option to commercialize or license the broader implementation later.
