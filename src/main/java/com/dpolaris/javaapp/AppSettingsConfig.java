package com.dpolaris.javaapp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AppSettingsConfig {
    static final String DEFAULT_HOST = "127.0.0.1";
    static final int DEFAULT_PORT = 8420;
    static final String DEFAULT_LLM_PROVIDER = "none";

    private AppSettingsConfig() {
    }

    static SettingsValues load() {
        Path path = settingsPath();
        if (!Files.isRegularFile(path)) {
            return defaults();
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            Object parsed = Json.parse(text);
            if (parsed instanceof Map<?, ?> mapRaw) {
                return sanitize(fromMap(Json.asObject(mapRaw)));
            }
        } catch (Exception ignored) {
            // Fall back to defaults.
        }
        return defaults();
    }

    static void save(SettingsValues values) throws IOException {
        SettingsValues safe = sanitize(values);
        Path path = settingsPath();
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Invalid settings path: " + path);
        }

        Files.createDirectories(parent);
        secureDirectory(parent);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("backend_host", safe.backendHost());
        payload.put("backend_port", safe.backendPort());
        payload.put("llm_provider", safe.llmProvider());
        payload.put("anthropic_api_key", safe.anthropicApiKey());
        payload.put("openai_api_key", safe.openAiApiKey());
        payload.put("news_api_key", safe.newsApiKey());
        payload.put("slack_webhook_url", safe.slackWebhookUrl());
        payload.put("ai_repo_path", safe.aiRepoPath());
        payload.put("ops_repo_path", safe.opsRepoPath());
        payload.put("data_dir", safe.dataDir());

        Files.writeString(path, Json.pretty(payload), StandardCharsets.UTF_8);
        secureFile(path);
    }

    static SettingsValues sanitize(SettingsValues input) {
        if (input == null) {
            return defaults();
        }
        String host = normalizeHost(input.backendHost());
        int port = normalizePort(input.backendPort());
        String llmProvider = normalizeLlmProvider(input.llmProvider());
        String anthropic = normalizeToken(input.anthropicApiKey());
        String openAi = normalizeToken(input.openAiApiKey());
        String news = normalizeToken(input.newsApiKey());
        String slack = normalizeToken(input.slackWebhookUrl());
        String aiPath = normalizePath(input.aiRepoPath(), detectAiRepoPath());
        String opsPath = normalizePath(input.opsRepoPath(), detectOpsRepoPath());
        String dataDir = normalizePath(input.dataDir(), defaultDataDir());
        return new SettingsValues(host, port, llmProvider, anthropic, openAi, news, slack, aiPath, opsPath, dataDir);
    }

    static SettingsValues defaults() {
        return new SettingsValues(
                DEFAULT_HOST,
                DEFAULT_PORT,
                DEFAULT_LLM_PROVIDER,
                "",
                "",
                "",
                "",
                detectAiRepoPath(),
                detectOpsRepoPath(),
                defaultDataDir()
        );
    }

    static Path settingsPath() {
        return Path.of(System.getProperty("user.home"), ".dpolaris", "settings.json");
    }

    private static SettingsValues fromMap(Map<String, Object> map) {
        String host = asString(map.get("backend_host"));
        int port = asInt(map.get("backend_port"), DEFAULT_PORT);
        String llmProvider = asString(map.get("llm_provider"));
        String anthropic = asString(map.get("anthropic_api_key"));
        String openAi = asString(map.get("openai_api_key"));
        String news = asString(map.get("news_api_key"));
        String slack = asString(map.get("slack_webhook_url"));
        String aiPath = asString(map.get("ai_repo_path"));
        String opsPath = asString(map.get("ops_repo_path"));
        String dataDir = asString(map.get("data_dir"));
        return new SettingsValues(host, port, llmProvider, anthropic, openAi, news, slack, aiPath, opsPath, dataDir);
    }

    private static String detectAiRepoPath() {
        String homeDefault = Path.of(System.getProperty("user.home"), "my-git", "dpolaris_ai").toString();
        for (Path candidate : List.of(
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize().resolveSibling("dpolaris_ai"),
                Path.of(homeDefault)
        )) {
            if (Files.isDirectory(candidate)) {
                return candidate.toString();
            }
        }
        return homeDefault;
    }

    private static String detectOpsRepoPath() {
        String homeDefault = Path.of(System.getProperty("user.home"), "my-git", "dPolaris_ops").toString();
        for (Path candidate : List.of(
                Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize().resolveSibling("dPolaris_ops"),
                Path.of(homeDefault)
        )) {
            if (Files.isDirectory(candidate)) {
                return candidate.toString();
            }
        }
        return homeDefault;
    }

    private static String defaultDataDir() {
        return Path.of(System.getProperty("user.home"), "dpolaris_data").toString();
    }

    private static String normalizeHost(String value) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() ? DEFAULT_HOST : text;
    }

    private static int normalizePort(int value) {
        return value > 0 ? value : DEFAULT_PORT;
    }

    private static String normalizeLlmProvider(String value) {
        String text = value == null ? "" : value.trim().toLowerCase();
        return switch (text) {
            case "none", "anthropic", "openai" -> text;
            default -> DEFAULT_LLM_PROVIDER;
        };
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizePath(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            text = fallback;
        }
        if ("~".equals(text)) {
            return System.getProperty("user.home");
        }
        if (text.startsWith("~/") || text.startsWith("~\\")) {
            return Path.of(System.getProperty("user.home"), text.substring(2)).toString();
        }
        return text;
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void secureDirectory(Path path) {
        securePath(path, true);
    }

    private static void secureFile(Path path) {
        securePath(path, false);
    }

    private static void securePath(Path path, boolean directory) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        if (!isWindows()) {
            try {
                Set<PosixFilePermission> perms = directory
                        ? PosixFilePermissions.fromString("rwx------")
                        : PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(path, perms);
            } catch (Exception ignored) {
                // Fall through to best-effort file API.
            }
        }
        File file = path.toFile();
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
        if (directory) {
            file.setExecutable(true, true);
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    record SettingsValues(
            String backendHost,
            int backendPort,
            String llmProvider,
            String anthropicApiKey,
            String openAiApiKey,
            String newsApiKey,
            String slackWebhookUrl,
            String aiRepoPath,
            String opsRepoPath,
            String dataDir
    ) {
    }
}
