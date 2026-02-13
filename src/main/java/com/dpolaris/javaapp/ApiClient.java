package com.dpolaris.javaapp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ApiClient {
    private final HttpClient client;
    private String host;
    private int port;

    ApiClient(String host, int port) {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.host = host;
        this.port = port;
    }

    void configure(String host, int port) {
        this.host = host;
        this.port = port;
    }

    boolean healthCheck() {
        return healthCheck(10);
    }

    boolean healthCheck(int timeoutSeconds) {
        try {
            request("GET", "/health", null, timeoutSeconds);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    Object fetchStatus() throws IOException, InterruptedException {
        return request("GET", "/api/status", null, 15);
    }

    Object fetchModels() throws IOException, InterruptedException {
        return request("GET", "/api/models", null, 20);
    }

    Object fetchMemories(int limit) throws IOException, InterruptedException {
        return request("GET", "/api/memories?limit=" + limit, null, 20);
    }

    Object fetchRuns() throws IOException, InterruptedException {
        return request("GET", "/runs", null, 30);
    }

    Map<String, Object> fetchRun(String runId) throws IOException, InterruptedException {
        Object response = request("GET", "/runs/" + encode(runId), null, 30);
        return Json.asObject(response);
    }

    Object fetchRunArtifacts(String runId) throws IOException, InterruptedException {
        return request("GET", "/runs/" + encode(runId) + "/artifacts", null, 30);
    }

    Object fetchRunArtifact(String runId, String artifactName) throws IOException, InterruptedException {
        return request("GET", "/runs/" + encode(runId) + "/artifact/" + encode(artifactName), null, 30);
    }

    Object fetchUniverse(String universeId) throws IOException, InterruptedException {
        return requestWithFallback(
                "GET",
                List.of(
                        "/scan/universe/" + encode(universeId),
                        "/api/scan/universe/" + encode(universeId),
                        "/scan/universe?name=" + encode(universeId),
                        "/api/scan/universe?name=" + encode(universeId),
                        "/universe/" + encode(universeId),
                        "/api/universe/" + encode(universeId)
                ),
                null,
                45
        );
    }

    Map<String, Object> startScan(Map<String, Object> payload) throws IOException, InterruptedException {
        String body = Json.compact(payload == null ? new LinkedHashMap<String, Object>() : payload);
        Object response = requestWithFallback(
                "POST",
                List.of("/scan/start", "/api/scan/start"),
                body,
                90
        );
        return Json.asObject(response);
    }

    Map<String, Object> fetchScanStatus(String runId) throws IOException, InterruptedException {
        Object response = requestWithFallback(
                "GET",
                List.of("/scan/status/" + encode(runId), "/api/scan/status/" + encode(runId)),
                null,
                8
        );
        return Json.asObject(response);
    }

    Object fetchScanResults(String runId, int page, int pageSize) throws IOException, InterruptedException {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        String query = "?page=" + safePage + "&page_size=" + safePageSize;
        return requestWithFallback(
                "GET",
                List.of(
                        "/scan/results/" + encode(runId) + query,
                        "/api/scan/results/" + encode(runId) + query
                ),
                null,
                90
        );
    }

    Map<String, Object> fetchScanResult(String runId, String ticker) throws IOException, InterruptedException {
        Object response = requestWithFallback(
                "GET",
                List.of(
                        "/scan/result/" + encode(runId) + "/" + encode(ticker),
                        "/api/scan/result/" + encode(runId) + "/" + encode(ticker)
                ),
                null,
                60
        );
        return Json.asObject(response);
    }

    Object fetchScanRuns() throws IOException, InterruptedException {
        return requestWithFallback(
                "GET",
                List.of("/scan/runs", "/api/scan/runs"),
                null,
                30
        );
    }

    boolean supportsScanApi() {
        if (!canReachWithFallback(
                "GET",
                List.of("/scan/runs", "/api/scan/runs"),
                6
        )) {
            return false;
        }
        return canReachWithFallback(
                "GET",
                List.of(
                        "/scan/universe/nasdaq_top_500",
                        "/api/scan/universe/nasdaq_top_500",
                        "/scan/universe?name=nasdaq_top_500",
                        "/api/scan/universe?name=nasdaq_top_500"
                ),
                10
        );
    }

    Map<String, Object> predictSymbol(String symbol) throws IOException, InterruptedException {
        String normalized = symbol.toUpperCase();
        try {
            Object response = request("POST", "/api/deep-learning/predict/" + encode(normalized), "{}", 45);
            return Json.asObject(response);
        } catch (IOException deepError) {
            Object response = request("POST", "/api/predict/" + encode(normalized), "{}", 45);
            return Json.asObject(response);
        }
    }

    Map<String, Object> generateTradeSetup(String symbol, int horizonDays) throws IOException, InterruptedException {
        String normalized = symbol.toUpperCase();
        String path = "/api/signals/" + encode(normalized) + "?horizon_days=" + horizonDays;
        Object response = request("POST", path, "{}", 90);
        return Json.asObject(response);
    }

    Map<String, Object> inspectPrediction(
            String ticker,
            String inspectTime,
            int horizon,
            String runId
    ) throws IOException, InterruptedException {
        StringBuilder query = new StringBuilder();
        query.append("ticker=").append(encode(ticker.toUpperCase()));
        if (inspectTime != null && !inspectTime.isBlank()) {
            query.append("&time=").append(encode(inspectTime.trim()));
        }
        if (horizon > 0) {
            query.append("&horizon=").append(horizon);
        }
        if (runId != null && !runId.isBlank()) {
            query.append("&runId=").append(encode(runId.trim()));
        }

        String path = "/predict/inspect?" + query;
        try {
            Object response = request("GET", path, null, 60);
            return Json.asObject(response);
        } catch (IOException primaryError) {
            String legacyPath = "/api/predict/inspect?" + query;
            Object response = request("GET", legacyPath, null, 60);
            return Json.asObject(response);
        }
    }

    Map<String, Object> trainStable(String symbol) throws IOException, InterruptedException {
        Object response = request("POST", "/api/train/" + encode(symbol), "{}", 240);
        return Json.asObject(response);
    }

    Map<String, Object> enqueueDeepLearningJob(String symbol, String modelType, int epochs)
            throws IOException, InterruptedException {
        String body = "{"
                + "\"symbol\":\"" + Json.escape(symbol.toUpperCase()) + "\","
                + "\"model_type\":\"" + Json.escape(modelType.toLowerCase()) + "\","
                + "\"epochs\":" + epochs
                + "}";
        Object response = request("POST", "/api/jobs/deep-learning/train", body, 60);
        return Json.asObject(response);
    }

    Map<String, Object> fetchJob(String jobId) throws IOException, InterruptedException {
        Object response = request("GET", "/api/jobs/" + encode(jobId), null, 15);
        return Json.asObject(response);
    }

    Map<String, Object> startDaemon() throws IOException, InterruptedException {
        Object response = request("POST", "/api/scheduler/start", "{}", 30);
        return Json.asObject(response);
    }

    Map<String, Object> stopDaemon() throws IOException, InterruptedException {
        Object response = request("POST", "/api/scheduler/stop", "{}", 30);
        return Json.asObject(response);
    }

    Map<String, Object> runSchedulerJob(String jobId) throws IOException, InterruptedException {
        Object response = requestWithFallback(
                "POST",
                List.of(
                        "/api/scheduler/run/" + encode(jobId),
                        "/scheduler/run/" + encode(jobId)
                ),
                "{}",
                120
        );
        return Json.asObject(response);
    }

    Map<String, Object> trainDeepLegacy(String symbol, String modelType, int epochs)
            throws IOException, InterruptedException {
        String path = "/api/deep-learning/train/" + encode(symbol)
                + "?model_type=" + encode(modelType.toLowerCase())
                + "&epochs=" + epochs;
        Object response = request("POST", path, "{}", 1800);
        return Json.asObject(response);
    }

    private Object request(String method, String path, String body, int timeoutSeconds)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(timeoutSeconds));

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            builder.header("Content-Type", "application/json");
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String responseBody = response.body() == null ? "" : response.body();

        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + ": " + responseBody);
        }
        if (responseBody.isBlank()) {
            return new LinkedHashMap<String, Object>();
        }

        try {
            return Json.parse(responseBody);
        } catch (RuntimeException ex) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("raw", responseBody);
            return raw;
        }
    }

    private Object requestWithFallback(
            String method,
            List<String> paths,
            String body,
            int timeoutSeconds
    ) throws IOException, InterruptedException {
        List<Exception> errors = new ArrayList<>();
        for (String path : paths) {
            try {
                return request(method, path, body, timeoutSeconds);
            } catch (IOException | InterruptedException ex) {
                errors.add(ex);
                if (ex instanceof InterruptedException interrupted) {
                    throw interrupted;
                }
                if (isTimeoutException(ex)) {
                    break;
                }
            }
        }

        if (errors.isEmpty()) {
            throw new IOException("No endpoint paths provided.");
        }
        Exception last = errors.get(errors.size() - 1);
        if (last instanceof IOException io) {
            throw io;
        }
        if (last instanceof InterruptedException interrupted) {
            throw interrupted;
        }
        throw new IOException(last.getMessage(), last);
    }

    private boolean isTimeoutException(Throwable error) {
        if (error == null) {
            return false;
        }
        String className = error.getClass().getSimpleName().toLowerCase();
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase();
        return className.contains("timeout")
                || message.contains("timed out")
                || message.contains("timeout");
    }

    private String baseUrl() {
        return "http://" + host + ":" + port;
    }

    private boolean canReachWithFallback(String method, List<String> paths, int timeoutSeconds) {
        try {
            requestWithFallback(method, paths, null, timeoutSeconds);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
