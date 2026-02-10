package com.dpolaris.javaapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GuardrailEngine {
    private GuardrailEngine() {
    }

    static GuardrailReport evaluate(
            Map<String, Object> runSummary,
            Map<String, Object> dataSummary,
            Map<String, Object> featureSummary,
            Map<String, Object> splitSummary,
            Map<String, Object> backtestSummary,
            Map<String, Object> reproducibilitySummary,
            List<String> artifactFiles
    ) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        List<GuardrailFinding> findings = new ArrayList<>();

        boolean hasConfigSnapshot = hasArtifact(artifactFiles, "config_snapshot.json");
        boolean hasDependencySnapshot = hasArtifact(artifactFiles, "dependency_snapshot.json")
                || hasArtifact(artifactFiles, "dependencies_snapshot.json");
        boolean hasDataHashes = hasArtifact(artifactFiles, "data_hashes.json");
        boolean snapshotsPresent = hasConfigSnapshot && hasDependencySnapshot && hasDataHashes;
        checks.put("snapshots_present", snapshotsPresent);
        if (!snapshotsPresent) {
            findings.add(new GuardrailFinding(
                    "repro_snapshots_missing",
                    Severity.HIGH,
                    "Reproducibility snapshots are incomplete (config/dependencies/data hashes).",
                    false
            ));
        }

        boolean hashesResolvable = asBoolean(
                firstNonNull(
                        reproducibilitySummary.get("data_hashes_resolvable"),
                        reproducibilitySummary.get("hashes_resolvable")
                ),
                false
        );
        if (!hashesResolvable) {
            hashesResolvable = inferResolvableFromHashes(dataSummary, reproducibilitySummary);
        }
        checks.put("data_hashes_resolvable", hashesResolvable);
        if (!hashesResolvable) {
            findings.add(new GuardrailFinding(
                    "repro_hash_unresolvable",
                    Severity.HIGH,
                    "Data hashes are missing or cannot be resolved from run artifacts.",
                    false
            ));
        }

        boolean reExecutable = asBoolean(
                firstNonNull(
                        reproducibilitySummary.get("re_executable_with_same_config"),
                        reproducibilitySummary.get("re_executable")
                ),
                snapshotsPresent && hashesResolvable
        );
        checks.put("re_executable", reExecutable);
        if (!reExecutable) {
            findings.add(new GuardrailFinding(
                    "repro_not_reexecutable",
                    Severity.HIGH,
                    "Run is not marked re-executable with the same config/dependencies.",
                    false
            ));
        }

        Map<String, Object> assumptions = asObject(
                firstNonNull(
                        backtestSummary.get("assumptions"),
                        backtestSummary
                )
        );
        double commission = asDouble(findFirst(assumptions, "commission_bps", "commission", "fee_bps"), 0.0);
        double spread = asDouble(findFirst(assumptions, "spread_bps", "spread"), 0.0);
        double slippage = asDouble(findFirst(assumptions, "slippage_bps", "slippage"), 0.0);
        double transactionCost = asDouble(findFirst(assumptions, "transaction_cost_bps", "transaction_cost"), 0.0);

        boolean hasCosts = (commission > 0.0) || (spread > 0.0) || (slippage > 0.0) || (transactionCost > 0.0);
        checks.put("backtest_costs", hasCosts);
        if (!hasCosts) {
            findings.add(new GuardrailFinding(
                    "backtest_no_costs",
                    Severity.CRITICAL,
                    "Backtest assumptions do not include non-zero costs/slippage.",
                    false
            ));
        }

        String validationMethod = asText(
                firstNonNull(
                        splitSummary.get("validation_method"),
                        findFirst(runSummary, "validation_method", "validation", "method")
                )
        );
        List<Object> walkForwardWindows = asArray(
                firstNonNull(splitSummary.get("walk_forward_windows"), splitSummary.get("walkForwardWindows"))
        );
        boolean walkForward = "walk_forward".equalsIgnoreCase(validationMethod) || !walkForwardWindows.isEmpty();
        checks.put("walk_forward", walkForward);
        if (!walkForward) {
            findings.add(new GuardrailFinding(
                    "split_not_walk_forward",
                    Severity.CRITICAL,
                    "Training split is not walk-forward by default.",
                    false
            ));
        }

        Map<String, Object> qualityGates = asObject(
                firstNonNull(
                        dataSummary.get("quality_gates"),
                        dataSummary.get("qualityGates")
                )
        );
        boolean qualityGatesApplied = areQualityGatesApplied(qualityGates);
        checks.put("quality_gates", qualityGatesApplied);
        if (!qualityGatesApplied) {
            findings.add(new GuardrailFinding(
                    "quality_gates_skipped",
                    Severity.CRITICAL,
                    "Data quality gates were skipped or not reported.",
                    false
            ));
        }

        String leakageStatus = asText(
                firstNonNull(
                        featureSummary.get("leakage_checks_status"),
                        featureSummary.get("leakageChecksStatus")
                )
        );
        boolean leakagePassed = leakageStatus.equalsIgnoreCase("passed");
        boolean leakageFailed = leakageStatus.toLowerCase().contains("fail")
                || leakageStatus.toLowerCase().contains("error");
        checks.put("leakage_checks", leakagePassed);

        if (leakageFailed) {
            findings.add(new GuardrailFinding(
                    "leakage_failed",
                    Severity.BLOCKER,
                    "Leakage checks failed. Publishing/trading must be blocked.",
                    true
            ));
        } else if (!leakagePassed) {
            findings.add(new GuardrailFinding(
                    "leakage_unknown",
                    Severity.HIGH,
                    "Leakage check status is not explicitly passed.",
                    false
            ));
        }

        int reproducibilityScore = computeReproducibilityScore(
                checks.get("snapshots_present"),
                checks.get("data_hashes_resolvable"),
                checks.get("re_executable")
        );
        boolean checklistGreen = checks.values().stream().allMatch(Boolean::booleanValue);
        boolean hasBlocker = findings.stream().anyMatch(GuardrailFinding::blocking);

        return new GuardrailReport(
                reproducibilityScore,
                checks,
                findings,
                checklistGreen,
                hasBlocker
        );
    }

    private static boolean areQualityGatesApplied(Map<String, Object> qualityGates) {
        if (qualityGates.isEmpty()) {
            return false;
        }
        if (qualityGates.containsKey("applied") && !asBoolean(qualityGates.get("applied"), false)) {
            return false;
        }
        for (Map.Entry<String, Object> entry : qualityGates.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if ("applied".equalsIgnoreCase(key)) {
                continue;
            }
            String value = asText(entry.getValue()).toLowerCase();
            if (value.contains("fail") || value.contains("skip") || value.contains("error")) {
                return false;
            }
        }
        return true;
    }

    private static boolean inferResolvableFromHashes(
            Map<String, Object> dataSummary,
            Map<String, Object> reproducibilitySummary
    ) {
        Object hashes = firstNonNull(
                reproducibilitySummary.get("data_hashes"),
                dataSummary.get("data_hashes")
        );
        if (!(hashes instanceof Map<?, ?> mapRaw)) {
            return false;
        }
        Map<String, Object> hashMap = Json.asObject(mapRaw);
        Object entries = firstNonNull(hashMap.get("hashes"), hashMap.get("items"));
        if (!(entries instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemRaw)) {
                return false;
            }
            Map<String, Object> row = Json.asObject(itemRaw);
            if (!asBoolean(row.get("resolvable"), false)) {
                return false;
            }
        }
        return true;
    }

    private static int computeReproducibilityScore(
            Boolean snapshotsPresent,
            Boolean hashesResolvable,
            Boolean reExecutable
    ) {
        int passed = 0;
        if (Boolean.TRUE.equals(snapshotsPresent)) {
            passed++;
        }
        if (Boolean.TRUE.equals(hashesResolvable)) {
            passed++;
        }
        if (Boolean.TRUE.equals(reExecutable)) {
            passed++;
        }
        return (int) Math.round((passed * 100.0) / 3.0);
    }

    private static boolean hasArtifact(List<String> artifacts, String exactName) {
        if (artifacts == null || artifacts.isEmpty() || exactName == null) {
            return false;
        }
        String wanted = exactName.toLowerCase();
        for (String path : artifacts) {
            if (path == null) {
                continue;
            }
            String lowered = path.toLowerCase();
            if (lowered.equals(wanted) || lowered.endsWith("/" + wanted)) {
                return true;
            }
        }
        return false;
    }

    private static Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return Json.asObject(raw);
        }
        return new LinkedHashMap<>();
    }

    private static List<Object> asArray(Object value) {
        if (value instanceof List<?> list) {
            return Json.asArray(list);
        }
        return new ArrayList<>();
    }

    private static Object findFirst(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String asText(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if (text.isBlank()) {
            return fallback;
        }
        return "true".equals(text) || "yes".equals(text) || "1".equals(text) || "passed".equals(text);
    }

    enum Severity {
        HIGH,
        CRITICAL,
        BLOCKER
    }

    record GuardrailFinding(
            String code,
            Severity severity,
            String message,
            boolean blocking
    ) {
    }

    record GuardrailReport(
            int reproducibilityScore,
            Map<String, Boolean> checklist,
            List<GuardrailFinding> findings,
            boolean checklistGreen,
            boolean hasBlocker
    ) {
        boolean leakageFailed() {
            for (GuardrailFinding finding : findings) {
                if ("leakage_failed".equals(finding.code())) {
                    return true;
                }
            }
            return false;
        }
    }
}
