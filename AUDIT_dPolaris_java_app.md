# AUDIT: dPolaris Java App (Control Plane + Training Observability)

Date: 2026-02-10
Scope: full active repo `/Users/darrenwon/my-git/dpolaris` (audit-only)
Build validation: `./gradlew test` -> build succeeds, but `test NO-SOURCE`

## Executive Summary

- The app is functionally usable as a control plane and training-runs viewer, but it is not yet contract-safe or production-robust.
- Core observability features exist (runs list, run details tabs, compare runs, prediction inspector, guardrails/readiness, audit logs), but several are heuristic/text-heavy rather than strict-schema + strongly visual.
- Contract compliance is partial: parsing is flexible by key-hints, not schema-bound; this avoids hard failures but can silently mis-map fields.
- Reliability under backend/network stress is weak (no retries/backoff, full artifact fan-out fetches, no pagination/virtualization, no in-flight request cancellation/versioning).
- Security posture is basic: no auth/TLS support in client, external-port kill behavior is broad, and audit/data logs are plaintext JSONL on disk.

## 1) Repo Coverage

Scanned:
- Build/config/docs: `build.gradle`, `settings.gradle`, `README.md`, run/build/package scripts.
- Active Java code: all files in `src/main/java/com/dpolaris/javaapp/`.
- Archived Swift code exists under `archive/` but is not used by current build.

Active modules/classes:
- `com.dpolaris.javaapp.DPolarisJavaApp` (UI + orchestration; ~8.7k LOC)
- `com.dpolaris.javaapp.ApiClient` (HTTP calls/timeouts)
- `com.dpolaris.javaapp.RunsService` (run/artifact service wrapper)
- `com.dpolaris.javaapp.RunsCache` (TTL cache)
- `com.dpolaris.javaapp.GuardrailEngine` (readiness rules)
- `com.dpolaris.javaapp.AuditLogStore` (JSONL persistence)
- `com.dpolaris.javaapp.Json` (custom JSON parser/serializer)

Dependency inventory (from `build.gradle`):
- Java toolchain: `17`
- Gradle plugin: `application`
- External libraries: **none declared** (JDK-only runtime; HTTP via `java.net.http.HttpClient`, UI via Swing/AWT)

## 2) Architecture Map

UI stack:
- Swing (`JFrame`, `JTabbedPane`, `JTable`, `JDialog`) in `DPolarisJavaApp`.

Networking:
- JDK `HttpClient` in `ApiClient`.
- Direct backend calls to health, train, jobs, runs/artifacts, compare dependencies, scheduler, inspector endpoints.

State management:
- Mutable field-based state inside one class (`DPolarisJavaApp`), plus `volatile` flags and `SwingWorker` background jobs.
- Lightweight in-memory TTL caching through `RunsService` -> `RunsCache`.

Train/Run/Inspect paths:
- Train button path: `startTraining()` -> `runStableTraining()` or `runDeepTraining()` -> `pollDeepTrainingJob()`.
- Dashboard run path: `refreshDashboard()` -> prediction + trade setup API calls.
- Inspector path: `refreshPredictionInspector()` -> `/predict/inspect` + render trace panels.

## 3) Contract Compliance (Schema-Driven UI)

### 3.1 Does UI assume fixed fields or read schema dynamically?

Verdict: **hybrid, mostly heuristic dynamic mapping (not strict schema-driven)**.

What it does:
- Uses normalized key-search and hint-based extraction (`findAnyValue`, `findSectionByHints`, `findArtifactByHints`) to tolerate backend field-name drift.
- Accepts multiple alias keys per semantic value across sections (data/features/model/backtest/etc.).

What it still hardcodes:
- Fixed UI sections/tabs and fixed conceptual metrics per section.
- Fixed table columns in models like `RunsTableModel`, `FeaturesTableModel`, `TradeLogTableModel`.

Risk:
- High tolerance avoids crashes, but can produce silently incorrect mappings when unrelated keys match hints.

Evidence:
- Heuristic key search: `DPolarisJavaApp.findAnyValue`, `findValueByKey`, `findSectionByHints`, `findValueByHint`, `normalizeKey`.
- Fixed section rendering pipeline: `DPolarisJavaApp.loadRunDetails` and `populate*Panel` methods.

### 3.2 Error handling quality

Missing artifacts:
- Handled per-artifact by storing `{"__error": ...}` fallback and rendering section warnings.
- Section render paths show artifact unavailable messages instead of crashing.

Schema mismatch / malformed payload:
- `ApiClient.request` falls back to `{ "raw": "<body>" }` when JSON parse fails.
- UI mostly degrades to text output and placeholder values.

Backend unavailable:
- Health checks before dashboard/inspector fetch; user-facing guidance text shown.
- Runs loading/read details failures reset panel state with visible error text.

Gaps:
- No structured schema validation errors surfaced to user (only generic messages).
- No retry/backoff; transient failures fail immediately.

Evidence:
- Artifact error wrapping: `DPolarisJavaApp.loadRunDetails`, `loadRunBundle`, `isArtifactError`, `extractArtifactError`.
- Backend-unavailable UX: `refreshDashboard`, `refreshPredictionInspector`.
- Parse fallback: `ApiClient.request`.

### 3.3 Caching behavior

Implemented:
- `RunsCache` TTL cache (20 seconds configured by `RunsService`).
- Cached keys:
  - `runs:list`
  - `runs:detail:{id}`
  - `runs:artifacts:{id}`
  - `runs:artifact:{id}:{name}`

Gaps:
- No max-size/eviction policy beyond TTL.
- No in-flight request deduplication.
- No stale-while-revalidate.
- Cache invalidation only global (`invalidateAll`), not granular.

Evidence:
- `RunsService` constructor and `listRuns/getRun/getRunArtifacts/getRunArtifact`.
- `RunsCache.get(...)` implementation.

## 4) Compare Runs + Prediction Inspector Verification

### 4.1 Compare Runs

Exists: **Yes**.

Capabilities verified:
- User selects exactly 2 runs from runs table.
- Generates and displays:
  - config diff
  - data diff
  - feature diff (added/removed/changed + stable diff text)
  - metric diff
  - backtest diff (including equity overlay + trade count)
  - drift diff
  - 5-bullet “best explanation” summary

Evidence:
- Trigger: `DPolarisJavaApp.compareSelectedRuns`.
- Diff generation: `buildCompareConfigMap`, `buildCompareDataMap`, `buildFeatureDiffSummary`, `buildCompareMetricsMap`, `buildBacktestDiffText`, `extractDriftBaselinePayload`, `buildStableJsonDiff`, `buildPerformanceChangeExplanation`.
- UI dialog: `showRunComparisonDialog`.

### 4.2 Prediction Inspector

Exists: **Yes**.

Capabilities verified:
- Inputs: ticker, timestamp/date, horizon, runId.
- Calls backend inspect endpoint with runId support.
- Displays:
  - raw as-of input snapshot (OHLCV, macro, sentiment)
  - computed feature vector (raw + normalized + normalization)
  - regime
  - model output + confidence
  - decision outcome
  - explanation/top features
  - warnings/fallbacks
  - full raw JSON trace

Evidence:
- UI/controls: `createPredictionInspectorPanel`.
- Request path: `refreshPredictionInspector` -> `ApiClient.inspectPrediction`.
- Rendering: `renderPredictionInspectorTrace`.

## 5) Guardrails + Audit Logs Verification

### 5.1 Guardrail warnings

Exists: **Yes** (`GuardrailEngine.evaluate`).

Checks implemented:
- Backtest costs/slippage non-zero.
- Walk-forward split required.
- Data quality gates applied.
- Leakage checks passed.
- Reproducibility checks:
  - snapshots present
  - data hashes resolvable
  - re-executable flag

Evidence:
- `GuardrailEngine.evaluate`.
- Readiness panel rendering: `DPolarisJavaApp.populateModelReadinessPanel`.

### 5.2 Kill-switch / readiness gating

Exists partially:
- “Publish Model” / “Use For Trading” buttons are blocked if leakage failed.
- Warnings shown for non-green checklist.

Limitation:
- Only leakage failure hard-blocks actions; other critical findings do not hard-block actions by default.

Evidence:
- `updateReadinessActionButtons`, `executeReadinessAction`, `GuardrailReport.leakageFailed`.

### 5.3 Audit logging of user training actions

Exists: **Yes**.

Logged fields include:
- `event=train_action`
- `user`
- initiated/completed timestamps
- status
- runId/run_id
- job_id
- config snapshot
- response summary/error

Storage:
- JSONL at default `~/dpolaris_data/audit_log.jsonl` (or env override).
- UI viewer + export snapshot exist.

Evidence:
- `initializeTrainingAudit`, `finalizeTrainingAudit`, `resolveAuditUser`.
- `AuditLogStore.append/readLatest/exportSnapshot`.
- Audit UI: `createAuditLogPanel`, `refreshAuditLogPanel`, `exportAuditLogSnapshot`.

## 6) Feature Completeness Scorecard (0-100)

| Area | Score | Notes |
|---|---:|---|
| Backend control plane (connect/start/stop/daemon) | 86 | Functional and user-visible status; external-stop behavior too broad. |
| Training trigger + job polling + logs | 84 | Works for stable/deep + legacy fallback; no retry policy. |
| Runs list UX (filter/search/sort) | 76 | Good filters/sort; no pagination/virtualization. |
| Run details observability breadth | 79 | All required sections present; some are text-only. |
| Schema contract compliance | 48 | Heuristic mapping, no strict schema enforcement/validation. |
| Artifact browsing/export | 70 | Section-level + key exports; no generic artifact browser/download manager. |
| Compare Runs | 88 | Strong coverage + readable stable diff + explanation bullets. |
| Prediction Inspector | 90 | Complete trace-style inspector with runId and as-of context. |
| Guardrails/readiness gating | 75 | Good checks; hard block only on leakage failure. |
| Auditability (train audit log) | 83 | Strong JSONL trail + UI viewer/export. |
| Testability/CI confidence | 18 | No Java tests in repo (`test NO-SOURCE`). |

Overall weighted audit score: **74/100**

## 7) UX Pain Points

- The app is a single dense desktop surface; many sections rely on raw/prettified JSON rather than purpose-built visual affordances.
- Runs and trade tables lack pagination/virtualization; large histories will feel slow and visually heavy.
- Splits/calibration/diagnostics are mostly text panes; users cannot quickly read reliability trends.
- Run-details loading can momentarily flash stale states due to asynchronous multi-fetch behavior.
- Multiple dense tabs in one panel reduce discoverability for non-technical users.

## 8) Reliability Risks

Network retries/backoff:
- No retry strategy for transient failures in `ApiClient.request`; single-shot HTTP calls.

Timeout strategy:
- Timeouts are hardcoded per call and inconsistent (10s to 1800s), with no adaptive tuning/cancellation contract.

Partial downloads / malformed payloads:
- Parse fallback to `raw` avoids crashes but may hide data integrity issues and lead to partial observability.

Large artifact handling:
- `loadRunDetails` fetches all artifacts and payloads eagerly for a run.
- Compare mode fetches all artifacts for two runs.
- No stream/chunking/compression controls; risk of UI lag and memory pressure.

Concurrency/state:
- No request token to discard stale `loadRunDetails` results when selection changes rapidly.

## 9) Security Concerns

- No auth/token support in `ApiClient`; assumes trusted local backend.
- Plain HTTP (`http://host:port`) with no TLS support.
- Audit logs and run-derived payloads are stored as plaintext JSONL/JSON in user directories.
- External backend stop by port can terminate non-dPolaris process if running on configured port.
- Backend path is user-controlled; process launching uses `ProcessBuilder` argument arrays (good, avoids shell injection), but path trust is still broad.

## 10) Reliability/Correctness Risk Ranking

### High
- Heuristic schema mapping can silently mis-map artifact fields.
  - Class/methods: `DPolarisJavaApp.findAnyValue`, `findSectionByHints`, `findValueByHint`.
- Potential stale UI update race for run-details async loaders.
  - Class/method: `DPolarisJavaApp.loadRunDetails`.
- No Java tests; regressions likely undetected.
  - Build output: `./gradlew test` => `test NO-SOURCE`.

### Medium
- No retries/backoff and no partial-download integrity checks.
  - Class/method: `ApiClient.request`.
- Full artifact fan-out loads can degrade UI and reliability on large runs.
  - Class/methods: `DPolarisJavaApp.loadRunDetails`, `loadRunBundle`.
- External-stop operation may affect wrong process on shared port.
  - Class/methods: `stopExternalBackendByPort`, `findListeningPids`, `stopPid`.

### Low
- Monolithic UI class creates maintenance friction.
  - Class: `DPolarisJavaApp`.

## 11) Prioritized Remediation Tickets (Audit Plan, No Code Changes)

### P0 (Correctness + Safety)

1. Add strict artifact contract layer before rendering.
- Create typed schema adapters + validation errors.
- Target classes: `RunsService`, `DPolarisJavaApp` (`populate*Panel` paths), new `RunArtifactAdapter` package.

2. Add request versioning/cancellation for run-details loaders.
- Discard stale worker results if selected run changes.
- Target class: `DPolarisJavaApp` (`loadRunDetails`, selection handlers).

3. Add retry/backoff and explicit transport error taxonomy.
- Target class: `ApiClient` (`request` and call sites).

4. Harden external backend stop action.
- Verify process identity before kill (command-line signature / pid provenance).
- Target class: `DPolarisJavaApp` (`stopExternalBackendByPort`, `findListeningPids`, `stopPid`).

### P1 (Observability UX + Scale)

5. Add pagination/incremental loading for runs list and trade logs.
- Target class: `DPolarisJavaApp` (runs/trade tables), `ApiClient` and backend endpoint contract.

6. Add dedicated calibration and splits visualization widgets.
- Reliability curve, Brier/ECE trend, walk-forward timeline chart.
- Target class: `DPolarisJavaApp` (calibration/splits tabs).

7. Add generic artifact browser/downloader panel.
- Artifact tree/list with per-artifact open/download and size metadata.
- Target classes: `DPolarisJavaApp`, `RunsService`.

8. Add large-artifact safeguards.
- Size limits, streaming, lazy load, cancel operations.
- Target classes: `ApiClient`, `RunsService`, `DPolarisJavaApp`.

### P2 (Quality + Maintainability)

9. Break `DPolarisJavaApp` into view/controller modules.
- Separate tabs/panels into classes with clear contracts.

10. Add Java tests.
- Unit tests: `GuardrailEngine`, `RunsService` parsing.
- Integration tests: simulated artifact payloads for run details rendering.
- UI smoke tests for critical control paths.

11. Add security controls.
- Optional auth header/token handling, TLS endpoint support, log redaction options.
- Target classes: `ApiClient`, `AuditLogStore`, UI settings.

## 12) Final Verdict

- **Can it act as a training observability console/control plane today?** Yes, for controlled local use.
- **Is it strong on UX correctness and contract integrity?** Not yet.
- **Primary blockers:** schema strictness, resilience under load/failure, and missing automated tests.
