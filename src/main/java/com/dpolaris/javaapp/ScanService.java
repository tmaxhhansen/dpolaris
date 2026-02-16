package com.dpolaris.javaapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ScanService {
    private final ApiClient apiClient;
    private final RunsCache cache = new RunsCache(12_000L);

    ScanService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    Map<String, Object> getUniverse(String universeId, boolean forceRefresh) throws Exception {
        String key = "scan:universe:" + normalizeKey(universeId);
        Object response = cache.get(key, forceRefresh, () -> apiClient.fetchUniverse(universeId));
        return unwrapObject(response, "universe", "item", "data");
    }

    List<String> listUniverses(boolean forceRefresh) throws Exception {
        Object response = cache.get("scan:universes:list", forceRefresh, apiClient::fetchUniverseList);

        if (response instanceof List<?> rawList) {
            List<String> names = new ArrayList<>();
            for (Object item : rawList) {
                String value = asString(item);
                if (!value.isBlank()) {
                    names.add(value);
                }
            }
            return names;
        }

        if (response instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object universesValue = firstValue(map, "universes", "items", "data");
            if (universesValue instanceof List<?> list) {
                List<String> names = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String text) {
                        String cleaned = text.trim();
                        if (!cleaned.isBlank()) {
                            names.add(cleaned);
                        }
                    } else if (item instanceof Map<?, ?> itemMapRaw) {
                        Map<String, Object> itemMap = Json.asObject(itemMapRaw);
                        String name = asString(firstValue(itemMap, "name", "id", "universe"));
                        if (!name.isBlank()) {
                            names.add(name);
                        }
                    }
                }
                return names;
            }
        }

        return Collections.emptyList();
    }

    Map<String, Object> startScan(Map<String, Object> payload) throws Exception {
        return apiClient.startScan(payload);
    }

    Map<String, Object> getScanStatus(String runId) throws Exception {
        return apiClient.fetchScanStatus(runId);
    }

    ScanResultsPage getScanResults(String runId, int page, int pageSize, boolean forceRefresh) throws Exception {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String key = "scan:results:" + runId + ":" + safePage + ":" + safePageSize;
        Object response = cache.get(
                key,
                forceRefresh,
                () -> apiClient.fetchScanResults(runId, safePage, safePageSize)
        );

        List<Map<String, Object>> rows = extractMapList(response, "results", "items", "tickers", "data");
        Map<String, Object> root = response instanceof Map<?, ?> ? Json.asObject(response) : new LinkedHashMap<>();
        long total = resolveTotal(root, rows.size());
        return new ScanResultsPage(rows, safePage, safePageSize, total, root);
    }

    Map<String, Object> getScanResult(String runId, String ticker, boolean forceRefresh) throws Exception {
        String key = "scan:result:" + runId + ":" + ticker.toUpperCase();
        return cache.get(key, forceRefresh, () -> apiClient.fetchScanResult(runId, ticker));
    }

    Map<String, Object> refreshUniverseNow() throws Exception {
        Map<String, Object> response = apiClient.rebuildUniverse(true);
        cache.invalidateAll();
        return response;
    }

    Map<String, Object> addCustomSymbol(String symbol) throws Exception {
        Map<String, Object> response = apiClient.addCustomUniverseSymbol(symbol);
        cache.invalidateAll();
        return response;
    }

    Map<String, Object> removeCustomSymbol(String symbol) throws Exception {
        Map<String, Object> response = apiClient.removeCustomUniverseSymbol(symbol);
        cache.invalidateAll();
        return response;
    }

    List<Map<String, Object>> listScanRuns(boolean forceRefresh) throws Exception {
        try {
            Object response = cache.get("scan:runs:list", forceRefresh, apiClient::fetchScanRuns);
            List<Map<String, Object>> runs = extractMapList(response, "runs", "items", "data");
            if (!runs.isEmpty()) {
                return runs;
            }
        } catch (Exception ignored) {
            // Fall through to generic /runs endpoint.
        }

        Object response = cache.get("runs:list", forceRefresh, apiClient::fetchRuns);
        List<Map<String, Object>> runs = extractMapList(response, "runs", "items", "data");
        List<Map<String, Object>> scanRuns = new ArrayList<>();
        for (Map<String, Object> run : runs) {
            if (isScanRun(run)) {
                scanRuns.add(run);
            }
        }
        return scanRuns;
    }

    void invalidateAll() {
        cache.invalidateAll();
    }

    private List<Map<String, Object>> extractMapList(Object response, String... wrapperKeys) {
        Object candidate = response;
        if (response instanceof Map<?, ?> map) {
            candidate = firstValue(Json.asObject(map), wrapperKeys);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        if (candidate instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> mapItem) {
                    out.add(Json.asObject(mapItem));
                }
            }
        }
        return out;
    }

    private Map<String, Object> unwrapObject(Object response, String... wrapperKeys) {
        if (response instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object nested = firstValue(map, wrapperKeys);
            if (nested instanceof Map<?, ?> nestedMap) {
                return Json.asObject(nestedMap);
            }
            return map;
        }
        return new LinkedHashMap<>();
    }

    private long resolveTotal(Map<String, Object> root, int fallback) {
        Object direct = firstValue(root, "total", "total_items", "total_results", "count");
        long parsed = asLong(direct);
        if (parsed >= 0) {
            return parsed;
        }
        Map<String, Object> paging = asMap(firstValue(root, "paging", "pagination", "page_info", "meta"));
        parsed = asLong(firstValue(paging, "total", "total_items", "total_results", "count"));
        if (parsed >= 0) {
            return parsed;
        }
        return fallback;
    }

    private boolean isScanRun(Map<String, Object> run) {
        String runMode = asString(firstValue(run, "run_mode", "runMode", "mode", "type", "job_type"));
        if (!runMode.isBlank() && normalizeKey(runMode).contains("scan")) {
            return true;
        }
        String universe = asString(firstValue(run, "universe", "universe_id", "universe_hash"));
        if (!universe.isBlank()) {
            return true;
        }
        Object scanResults = firstValue(run, "scan_results", "scan_summary", "scan");
        return scanResults instanceof Map<?, ?> || scanResults instanceof List<?>;
    }

    private Object firstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = lookup(map, key);
            if (value != null) {
                return value;
            }
        }
        return map;
    }

    private Object lookup(Map<String, Object> map, String wanted) {
        String normWanted = normalizeKey(wanted);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (normalizeKey(entry.getKey()).equals(normWanted)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> mapRaw) {
            return Json.asObject(mapRaw);
        }
        return new LinkedHashMap<>();
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (RuntimeException ignored) {
                return -1L;
            }
        }
        return -1L;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    record ScanResultsPage(
            List<Map<String, Object>> rows,
            int page,
            int pageSize,
            long total,
            Map<String, Object> raw
    ) {
    }
}
