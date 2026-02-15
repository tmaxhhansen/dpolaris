package com.dpolaris.javaapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ApiClient {
    private static final Pattern PID_PATTERN = Pattern.compile("(?i)\\bpid\\s*[:=]\\s*(\\d+)\\b");
    private static final Pattern HEARTBEAT_PATTERN = Pattern.compile("(?i)\\blast[_\\s-]?heartbeat\\s*[:=]\\s*([^\\n\\r]+)");
    private static final Pattern LAST_SCAN_PATTERN = Pattern.compile("(?i)\\blast[_\\s-]?scan[_\\s-]?run\\s*[:=]\\s*([^\\n\\r]+)");
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

    Map<String, Object> runDeepLearning(List<String> tickers) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tickers", tickers);
        payload.put("fetch_data", true);
        payload.put("train_if_missing", true);
        String body = Json.compact(payload);
        Object response = requestWithFallback(
                "POST",
                List.of("/deep-learning/run", "/api/deep-learning/run", "/dl/run"),
                body,
                300  // 5 minutes timeout for training
        );
        return Json.asObject(response);
    }

    /**
     * Fetch stock metadata (sector, market cap, avg volume, change%) for multiple symbols.
     * GET /api/stocks/metadata?symbols=AAPL,MSFT,GOOGL
     */
    Map<String, Object> fetchStocksMetadata(List<String> symbols) throws IOException, InterruptedException {
        if (symbols == null || symbols.isEmpty()) {
            return new LinkedHashMap<>();
        }
        String symbolsParam = String.join(",", symbols);
        Object response = requestWithFallback(
                "GET",
                List.of(
                    "/api/stocks/metadata?symbols=" + encode(symbolsParam),
                    "/stocks/metadata?symbols=" + encode(symbolsParam)
                ),
                null,
                30
        );
        return Json.asObject(response);
    }

    /**
     * Fetch last analysis dates for multiple symbols.
     * GET /api/analysis/last?symbols=AAPL,MSFT,GOOGL
     */
    Map<String, Object> fetchAnalysisLast(List<String> symbols) throws IOException, InterruptedException {
        if (symbols == null || symbols.isEmpty()) {
            return new LinkedHashMap<>();
        }
        String symbolsParam = String.join(",", symbols);
        Object response = requestWithFallback(
                "GET",
                List.of(
                    "/api/analysis/last?symbols=" + encode(symbolsParam),
                    "/analysis/last?symbols=" + encode(symbolsParam)
                ),
                null,
                30
        );
        return Json.asObject(response);
    }

    /**
     * Fetch detailed analysis artifacts for a single symbol.
     * GET /api/analysis/detail/{symbol}
     */
    Map<String, Object> fetchAnalysisDetail(String symbol) throws IOException, InterruptedException {
        String normalized = symbol.toUpperCase();
        Object response = requestWithFallback(
                "GET",
                List.of(
                    "/api/analysis/detail/" + encode(normalized),
                    "/analysis/detail/" + encode(normalized)
                ),
                null,
                45
        );
        return Json.asObject(response);
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

    Map<String, Object> startBackendControl() throws IOException, InterruptedException {
        Object response = request("POST", "/api/control/backend/start", "{}", 45);
        return Json.asObject(response);
    }

    Map<String, Object> stopBackendControl() throws IOException, InterruptedException {
        Object response = request("POST", "/api/control/backend/stop", "{}", 45);
        return Json.asObject(response);
    }

    Map<String, Object> restartBackendControl(boolean clean) throws IOException, InterruptedException {
        String body = clean ? "{\"clean\":true}" : "{}";
        Object response = request("POST", "/api/control/backend/restart", body, 60);
        return Json.asObject(response);
    }

    Map<String, Object> fetchBackendControlStatus() throws IOException, InterruptedException {
        Object response = request("GET", "/api/control/backend/status", null, 30);
        return Json.asObject(response);
    }

    Map<String, Object> startOrchestrator(String opsRepoPath) throws IOException, InterruptedException {
        return runOpsCommand(opsRepoPath, "up", 90);
    }

    Map<String, Object> stopOrchestrator(String opsRepoPath) throws IOException, InterruptedException {
        return runOpsCommand(opsRepoPath, "down", 90);
    }

    Map<String, Object> restartOrchestrator(String opsRepoPath) throws IOException, InterruptedException {
        Map<String, Object> stop = runOpsCommand(opsRepoPath, "down", 90);
        Map<String, Object> start = runOpsCommand(opsRepoPath, "up", 90);

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("action", "restart");
        merged.put("stop", stop);
        merged.put("start", start);
        merged.put("running", start.get("running"));
        merged.put("pid", start.get("pid"));
        merged.put("last_heartbeat", firstNonBlank(
                asString(start.get("last_heartbeat")),
                asString(stop.get("last_heartbeat"))
        ));
        merged.put("last_scan_run", firstNonBlank(
                asString(start.get("last_scan_run")),
                asString(stop.get("last_scan_run"))
        ));
        merged.put("stderr", joinNonBlank(
                asString(stop.get("stderr")),
                asString(start.get("stderr"))
        ));
        merged.put("stdout", joinNonBlank(
                asString(stop.get("stdout")),
                asString(start.get("stdout"))
        ));
        merged.put("exit_code", Json.asInt(start.get("exit_code"), 0));
        merged.put("error_detail", firstNonBlank(
                asString(start.get("error_detail")),
                asString(stop.get("error_detail"))
        ));
        return merged;
    }

    Map<String, Object> fetchOrchestratorStatus(String opsRepoPath) throws IOException, InterruptedException {
        return runOpsCommand(opsRepoPath, "status", 60);
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

    Object request(String method, String path, String body, int timeoutSeconds)
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

    private Map<String, Object> runOpsCommand(String opsRepoPath, String action, int timeoutSeconds)
            throws IOException, InterruptedException {
        String expandedRepoPath = expandUserHome(firstNonBlank(opsRepoPath, "~/my-git/dPolaris_ops"));
        File repoDir = new File(expandedRepoPath);
        // Use the run_ops shell script (macOS) or python -m ops.main (Windows)
        List<String> command = isWindows()
                ? List.of("python", "-m", "ops.main", action)
                : List.of(expandedRepoPath + File.separator + "run_ops", action);

        ProcessOutput output = executeLocalCommand(command, repoDir, timeoutSeconds);
        String stdout = output.stdout() == null ? "" : output.stdout().trim();
        String stderr = output.stderr() == null ? "" : output.stderr().trim();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("command", String.join(" ", command));
        result.put("cwd", expandedRepoPath);
        result.put("exit_code", output.exitCode());
        result.put("stdout", stdout);
        result.put("stderr", stderr);

        Object parsed = parseJsonIfPossible(stdout);
        if (parsed instanceof Map<?, ?> parsedMap) {
            result.putAll(Json.asObject(parsedMap));
        }

        String mapHeartbeat = lookupMapString(result, "last_heartbeat", "heartbeat", "lastHeartbeat");
        String mapLastScan = lookupMapString(result, "last_scan_run", "lastScanRun", "scan_run");
        String mapError = lookupMapString(result, "error_detail", "error", "detail", "stderr");

        if (!mapHeartbeat.isBlank()) {
            result.put("last_heartbeat", mapHeartbeat);
        } else {
            result.put("last_heartbeat", extractFirst(HEARTBEAT_PATTERN, stdout));
        }
        if (!mapLastScan.isBlank()) {
            result.put("last_scan_run", mapLastScan);
        } else {
            result.put("last_scan_run", extractFirst(LAST_SCAN_PATTERN, stdout));
        }

        long pid = resolvePid(result, stdout);
        if (pid > 0) {
            result.put("pid", pid);
        }

        String runningValue = lookupMapString(result, "running", "is_running", "alive", "status");
        Boolean running = resolveRunning(action, runningValue, stdout, stderr, output.exitCode());
        result.put("running", running == null ? "unknown" : running);

        if (output.timedOut()) {
            result.put("error_detail", "Command timed out after " + timeoutSeconds + "s");
        } else if (output.exitCode() != 0) {
            result.put("error_detail", firstNonBlank(stderr, stdout, mapError,
                    "ops.main " + action + " failed (exit=" + output.exitCode() + ")"));
        } else if (!mapError.isBlank()) {
            result.put("error_detail", mapError);
        } else if (!stderr.isBlank()) {
            result.put("error_detail", stderr);
        } else {
            result.put("error_detail", "");
        }

        return result;
    }

    private ProcessOutput executeLocalCommand(List<String> command, File workingDirectory, int timeoutSeconds)
            throws IOException, InterruptedException {
        if (!workingDirectory.isDirectory()) {
            throw new IOException("Ops repo path does not exist: " + workingDirectory.getAbsolutePath());
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory);
        Process process = builder.start();

        StringBuilder stdoutBuffer = new StringBuilder();
        StringBuilder stderrBuffer = new StringBuilder();
        Thread stdoutThread = startStreamPump(process.getInputStream(), stdoutBuffer, "ops-stdout");
        Thread stderrThread = startStreamPump(process.getErrorStream(), stderrBuffer, "ops-stderr");

        boolean finished = process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(3, TimeUnit.SECONDS);
        }

        joinQuietly(stdoutThread);
        joinQuietly(stderrThread);

        int exitCode = finished ? process.exitValue() : -1;
        return new ProcessOutput(exitCode, stdoutBuffer.toString(), stderrBuffer.toString(), !finished);
    }

    private Thread startStreamPump(InputStream stream, StringBuilder buffer, String name) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (!firstLine) {
                        buffer.append('\n');
                    }
                    buffer.append(line);
                    firstLine = false;
                }
            } catch (IOException ignored) {
                // Best-effort stream capture.
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void joinQuietly(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(1200);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private long resolvePid(Map<String, Object> map, String stdout) {
        String mapPid = lookupMapString(map, "pid", "process_id", "processId");
        if (!mapPid.isBlank()) {
            try {
                return Long.parseLong(mapPid.trim());
            } catch (NumberFormatException ignored) {
                // Fall back to text extraction.
            }
        }
        Matcher matcher = PID_PATTERN.matcher(stdout == null ? "" : stdout);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }
        return -1L;
    }

    private Boolean resolveRunning(
            String action,
            String mapValue,
            String stdout,
            String stderr,
            int exitCode
    ) {
        String mapLower = asString(mapValue).toLowerCase();
        if (mapLower.equals("true") || mapLower.equals("running") || mapLower.equals("up") || mapLower.equals("active")) {
            return true;
        }
        if (mapLower.equals("false") || mapLower.equals("stopped") || mapLower.equals("down")
                || mapLower.equals("inactive") || mapLower.equals("not running")) {
            return false;
        }

        String text = (firstNonBlank(stdout, "") + "\n" + firstNonBlank(stderr, "")).toLowerCase();
        if (text.contains("not running") || text.contains("stopped") || text.contains("inactive")
                || text.contains("down")) {
            return false;
        }
        if (text.contains("running") || text.contains("active") || text.contains("up")) {
            return true;
        }

        if ("up".equals(action) && exitCode == 0) {
            return true;
        }
        if ("down".equals(action) && exitCode == 0) {
            return false;
        }
        return null;
    }

    private Object parseJsonIfPossible(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Json.parse(text);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String lookupMapString(Map<String, Object> map, String... keys) {
        if (map == null || map.isEmpty() || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            if (map.containsKey(key)) {
                return asString(map.get(key));
            }
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String normalizedEntryKey = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (normalizedEntryKey.equals(normalizeKey(key))) {
                    return asString(entry.getValue());
                }
            }
        }
        return "";
    }

    private static String normalizeKey(String key) {
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

    private static String asString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String joinNonBlank(String first, String second) {
        String a = first == null ? "" : first.trim();
        String b = second == null ? "" : second.trim();
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + "\n" + b;
    }

    private static String expandUserHome(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.equals("~")) {
            return System.getProperty("user.home");
        }
        if (trimmed.startsWith("~/") || trimmed.startsWith("~\\")) {
            return System.getProperty("user.home") + trimmed.substring(1);
        }
        return trimmed;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String extractFirst(Pattern pattern, String text) {
        if (pattern == null || text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private record ProcessOutput(int exitCode, String stdout, String stderr, boolean timedOut) {
    }
}
