package com.dpolaris.javaapp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

final class ApiClient {
    private final HttpClient client;
    private String host;
    private int port;

    ApiClient(String host, int port) {
        this.client = HttpClient.newBuilder()
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

    private String baseUrl() {
        return "http://" + host + ":" + port;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
