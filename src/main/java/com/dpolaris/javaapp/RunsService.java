package com.dpolaris.javaapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RunsService {
    private final ApiClient apiClient;
    private final RunsCache cache = new RunsCache(20_000L);

    RunsService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    List<Map<String, Object>> listRuns(boolean forceRefresh) throws Exception {
        Object response = cache.get("runs:list", forceRefresh, apiClient::fetchRuns);
        return extractMapList(response, "runs", "items", "data");
    }

    Map<String, Object> getRun(String runId, boolean forceRefresh) throws Exception {
        String key = "runs:detail:" + runId;
        Map<String, Object> response = cache.get(key, forceRefresh, () -> apiClient.fetchRun(runId));
        Object wrapped = firstValue(response, "run", "item", "data");
        if (wrapped instanceof Map<?, ?>) {
            return Json.asObject(wrapped);
        }
        return response;
    }

    List<Map<String, Object>> getRunArtifacts(String runId, boolean forceRefresh) throws Exception {
        String key = "runs:artifacts:" + runId;
        Object response = cache.get(key, forceRefresh, () -> apiClient.fetchRunArtifacts(runId));
        return extractMapList(response, "artifacts", "items", "data");
    }

    Object getRunArtifact(String runId, String artifactName, boolean forceRefresh) throws Exception {
        String key = "runs:artifact:" + runId + ":" + artifactName;
        return cache.get(key, forceRefresh, () -> apiClient.fetchRunArtifact(runId, artifactName));
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
                } else if (item instanceof String name) {
                    Map<String, Object> wrapped = new LinkedHashMap<>();
                    wrapped.put("name", name);
                    out.add(wrapped);
                }
            }
        }
        return out;
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
}
