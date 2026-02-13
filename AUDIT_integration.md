# AUDIT: dPolaris Integration (dPolaris_ai + dPolaris Java App)

Date: 2026-02-10  
Scope: integration-only audit across:
- Backend: `/Users/darrenwon/my-git/dpolaris_ai`
- Java app: `/Users/darrenwon/my-git/dpolaris`

No implementation changes in this pass.

## Executive Summary

1. Core API surface for runs/inspection is present and wired end-to-end, but one critical correctness gap exists: `runId` in Prediction Inspector is not used to bind model selection.
2. Artifact versioning/backward normalization exists in backend (`training_artifact_version` + legacy normalization), and Java is tolerant to missing/new fields, but this tolerance is heuristic and can silently mis-map fields.
3. Truthfulness controls are partially implemented: leakage status is surfaced and leakage failures are blocked, but other critical guardrail failures still allow “Publish model / Use for trading.”
4. Backtest assumptions are shown in the UI, but “exact config snapshot” is not surfaced as a first-class run detail view; most run details are reconstructed from hints.
5. Performance/robustness risks remain: eager full artifact fetch, string-based artifact downloads (non-streaming), no retry/backoff in HTTP client, and coarse cache invalidation.

---

## 1) API + Artifact Contract Check

### Endpoint compatibility

| Contract endpoint | Backend implementation | Java implementation | Status |
|---|---|---|---|
| `GET /runs` | Implemented in `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1391`; returns `{"runs": [...], "count": ...}` | Called by `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:57`; parsed by `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/RunsService.java:16` | Match |
| `GET /runs/{id}` | Implemented in `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1414`; returns normalized artifact via `load_training_artifact` | Called by `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:61`; consumed in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2077` | Match |
| `GET /runs/{id}/artifact/{name}` | Implemented in `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1438`; file served via `FileResponse` | Called by `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:70` (string body + JSON parse fallback) | Partial mismatch for non-JSON/large artifacts |
| `GET /predict/inspect` | Implemented in `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1640` | Called by `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:92` with fallback to legacy alias `/api/predict/inspect` at `:115` | Match (transport), semantic caveat below |

### Schema versions and backward compatibility

- Artifact semantic version exists: `TRAINING_ARTIFACT_VERSION = "1.0.0"` in `/Users/darrenwon/my-git/dpolaris_ai/ml/training_artifacts.py:30`.
- Top-level schema requires version + section payloads in `/Users/darrenwon/my-git/dpolaris_ai/schemas/training_artifact.schema.json:6`.
- Backend has explicit backward normalization for legacy/camel-case payloads in `/Users/darrenwon/my-git/dpolaris_ai/ml/training_artifacts.py:446`.
- Backward compatibility is tested in `/Users/darrenwon/my-git/dpolaris_ai/tests/test_training_artifact_contract.py:119`.

### Java handling of missing/new fields

What works:
- Java uses wrapper-tolerant extraction (`runs/items/data`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/RunsService.java:18`.
- Missing objects degrade to empty maps (`asObjectOrEmpty`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:4288`.
- Unknown/new keys usually tolerated via recursive key-hint search (`findAnyValue`/`findSectionByHints`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:5429` and `:5470`.

Risk:
- Heuristic key matching can silently map wrong fields when schemas evolve; there is no strict schema adapter/validator in Java before rendering.

---

## 2) Truthfulness Check (Anti-Self-Deception)

### A) Cost/slippage assumptions shown prominently

Verdict: **PASS (UI present), PARTIAL (not hard-gated)**

- Assumptions snapshot has dedicated area in backtest tab: `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:1490`.
- Backtest guardrail checks explicit non-zero costs/slippage in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/GuardrailEngine.java:82`.

### B) Exact config snapshot displayed (not derived)

Verdict: **FAIL**

- Backend generates `config_snapshot.json` in `/Users/darrenwon/my-git/dpolaris_ai/ml/training_artifacts.py:666`.
- Java run-details flow does not have a dedicated config snapshot section; run overview uses hint-based summary rendering (`renderRunDetailsSection`) at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2112`.
- Compare/config views are reconstructed from run/model payloads (`buildCompareConfigMap`) at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:4469`, not guaranteed to be verbatim `config_snapshot.json`.

### C) Leakage status shown + unsafe usage blocked

Verdict: **PARTIAL**

- Leakage status is surfaced in readiness checklist via `mark(report, "leakage_checks")` at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2810`.
- Leakage failure blocks actions (`Publish/Use for trading`) via `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2877` and `:2899`.
- But other critical guardrails (`backtest_no_costs`, `split_not_walk_forward`, `quality_gates_skipped`) do **not** block those actions; only leakage failure blocks.

### D) Prediction Inspector uses causal as-of data

Verdict: **PASS for causality, FAIL for run-specific truthfulness**

- Causal truncation enforced (`<= resolved_ts`) in `/Users/darrenwon/my-git/dpolaris_ai/ml/prediction_inspector.py:64`.
- Endpoint marks `causal_asof: True` in `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1836`.
- Causality test exists in `/Users/darrenwon/my-git/dpolaris_ai/tests/test_prediction_inspector_api.py:40`.
- Critical semantic issue: endpoint accepts `runId` (`/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1645`) but model inference path ignores it and always uses latest load logic via `_predict_symbol_direction(symbol, df)` at `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1685` and `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1568`. `runId` is used for context only (`run_context`) at `:1806`.

---

## 3) Performance & Robustness

### Artifact sizes and UI responsiveness

- Current local run artifacts are small (`/Users/darrenwon/my-git/dpolaris_ai/runs` ~384K), but implementation is not size-adaptive.
- Java loads run details by fetching run metadata + artifact list + **every artifact payload** in one worker loop at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2077`.
- Compare mode loads both run bundles and artifact payloads similarly at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:4435`.

### Streaming downloads for large CSVs

Verdict: **FAIL**

- Backend file endpoint uses `FileResponse` (`/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1449`) which can serve large files.
- Java client always reads full body as `String` (`HttpResponse.BodyHandlers.ofString()`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:174`; no streaming/file sink path.

### Timeouts / retries / backoff

Verdict: **PARTIAL**

- Per-call timeouts exist (`ApiClient.request`, `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:165`).
- No general retry/backoff for transport errors in `ApiClient.request` (`:178` throws immediately).
- Job polling retries are simplistic fixed sleep loops (`Thread.sleep(2000)`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:6885` and `:6895`.

### Caching and invalidation

Verdict: **PARTIAL**

- TTL cache exists (`20_000ms`) in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/RunsService.java:10` via `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/RunsCache.java:10`.
- But no granular invalidation and no visible call site for `runsService.invalidateAll()` (method exists at `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/RunsService.java:42`).

---

## Contract Mismatches Table

| ID | Area | Mismatch | Evidence | Severity |
|---|---|---|---|---|
| INT-01 | Prediction Inspector semantics | `runId` accepted but not used to select model version/run artifact model | Backend accepts `runId` at `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1645`, but prediction path calls `_predict_symbol_direction(symbol, asof_df)` at `:1685` and ignores run binding | High |
| INT-02 | Truthfulness/config transparency | Exact `config_snapshot.json` not rendered as a first-class run details section; UI relies on derived/hinted sections | Backend writes config snapshot `/Users/darrenwon/my-git/dpolaris_ai/ml/training_artifacts.py:666`; Java run details uses hint rendering `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2112` and compare derivation `:4469` | High |
| INT-03 | Artifact transport contract | Generic artifact endpoint returns arbitrary file types, but Java assumes in-memory string + JSON parse fallback | Backend file endpoint `/Users/darrenwon/my-git/dpolaris_ai/api/server.py:1438`; Java body handler ofString `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/ApiClient.java:174` | High |
| INT-04 | Guardrail enforcement | UI only hard-blocks leakage failure, not other critical guardrail failures | Blocking logic `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:2877` and `:2899`; critical findings include no-cost/non-walk-forward in `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/GuardrailEngine.java:90` and `:110` | Medium |
| INT-05 | Schema-driven rendering | Java uses heuristic key matching instead of strict schema adapters; safe from crashes but not from silent mis-maps | Hint search `/Users/darrenwon/my-git/dpolaris/src/main/java/com/dpolaris/javaapp/DPolarisJavaApp.java:5429` and `:5470` | Medium |

---

## Top Integration Failures + Reproduction

### 1) Prediction Inspector does not honor selected `runId` (INT-01)

Reproduce:
1. Train two runs for same ticker with materially different model configs.
2. Open Prediction Inspector in Java app and query same `ticker/time/horizon`, switching only `runId`.
3. Observe output may not change as expected because backend inference chooses latest model loading logic, while `runId` is only attached as context metadata.

Expected:
- Prediction should be produced by the selected run/model artifact.

Actual:
- `runId` affects context block, not inference model binding.

### 2) Exact config snapshot is not directly auditable in run details (INT-02)

Reproduce:
1. Open a run folder and verify `config_snapshot.json` exists.
2. Open same run in Java “Run Details.”
3. Look for dedicated “Config Snapshot (verbatim)” section.

Expected:
- UI should show exact snapshot payload for the run.

Actual:
- UI reconstructs config-like view from hints and section payloads.

### 3) Large/non-JSON artifacts can degrade UX and correctness (INT-03)

Reproduce:
1. Add a large CSV or binary artifact into `runs/<id>/...`.
2. Load run details in Java app.
3. Client fetches all artifacts as strings; non-JSON returns raw payload fallback.

Expected:
- Streamed downloads for large files; typed handling for JSON/CSV/image.

Actual:
- Full in-memory string reads, no streaming path, no typed artifact renderer.

### 4) Unsafe guardrails can still allow “Publish / Use for trading” (INT-04)

Reproduce:
1. Use a run where leakage is “passed” but backtest costs are zero or split is non-walk-forward.
2. Open readiness panel.
3. Click “Publish Model” or “Use For Trading.”

Expected:
- Critical safety failures should block these actions.

Actual:
- Only leakage-failed status blocks actions.

### 5) Runs cache can hide fresh state briefly and has no explicit invalidation path

Reproduce:
1. Load runs list.
2. Complete a training action immediately.
3. Re-open runs view quickly without force refresh.

Expected:
- New run appears immediately or cache invalidates on training completion.

Actual:
- TTL cache (20s) may delay visibility; no explicit `invalidateAll()` call path.

---

## Fix Plan (Correctness First, Then UX)

### P0: Correctness / Audit Integrity

1. Bind Prediction Inspector to `runId` model artifacts.
- Add explicit run-model resolution in backend inspect path.
- Reject/flag if `runId` model is unavailable instead of silently using latest.

2. Make exact `config_snapshot.json` first-class in Java run details.
- Add dedicated config snapshot panel reading artifact by exact name.
- Stop deriving config-only views from heuristic fields when snapshot exists.

3. Harden guardrail enforcement for trading actions.
- Block publish/use-for-trading on any critical guardrail (`no costs`, `non-walk-forward`, `quality gates skipped`, `leakage failed`).

4. Add strict schema adapters in Java for critical sections.
- Keep heuristic fallback only as explicit compatibility mode with warning banners.

### P1: Transport / Reliability

5. Add typed artifact retrieval APIs in Java client.
- JSON endpoint path for JSON artifacts; streaming/file-download path for CSV/binary.
- Avoid full in-memory `String` for large files.

6. Add retry/backoff policy in `ApiClient`.
- Bounded exponential backoff for idempotent GETs and transient network errors.

7. Add request versioning/cancellation in run-details loading.
- Prevent stale async updates when selection changes quickly.

8. Add explicit cache invalidation hook after successful train completion.
- Invalidate runs cache and refresh selected run automatically.

### P2: UX/Polish

9. Add artifact size metadata and lazy loading.
- Show per-artifact size/type before loading payload.

10. Promote assumptions + guardrail status to run header summary.
- Keep details tab for full payload, but show “unsafe” status at top-level immediately.

---

## Final Assessment

- Integration is functional and feature-rich, but **not yet audit-hard**.
- Biggest correctness issue is semantic drift between UI contract and backend behavior for `runId` in Prediction Inspector.
- Biggest transparency gap is missing verbatim config snapshot display in run details.
- Biggest robustness gap is artifact transport strategy (string-only, eager, non-streaming).

