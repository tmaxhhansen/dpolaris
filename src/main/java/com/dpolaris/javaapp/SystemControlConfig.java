package com.dpolaris.javaapp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class SystemControlConfig {
    static final String DEFAULT_HOST = "127.0.0.1";
    static final int DEFAULT_PORT = 8420;
    static final String DEFAULT_AI_REPO_PATH = "~/my-git/dpolaris_ai";
    static final String DEFAULT_OPS_REPO_PATH = "~/my-git/dPolaris_ops";
    static final String DEFAULT_DEVICE_PREFERENCE = "auto";

    private static final String KEY_HOST = "backend.host";
    private static final String KEY_PORT = "backend.port";
    private static final String KEY_AI_PATH = "repo.ai";
    private static final String KEY_OPS_PATH = "repo.ops";
    private static final String KEY_DEVICE_PREFERENCE = "deep_learning.device_preference";

    private SystemControlConfig() {
    }

    static ConfigValues load() {
        Path configPath = configPath();
        if (!Files.isRegularFile(configPath)) {
            return defaults();
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        } catch (IOException ignored) {
            return defaults();
        }

        String host = normalizeHost(props.getProperty(KEY_HOST));
        int port = parsePort(props.getProperty(KEY_PORT));
        String aiPath = normalizePath(props.getProperty(KEY_AI_PATH), DEFAULT_AI_REPO_PATH);
        String opsPath = normalizePath(props.getProperty(KEY_OPS_PATH), DEFAULT_OPS_REPO_PATH);
        String devicePreference = normalizeDevicePreference(props.getProperty(KEY_DEVICE_PREFERENCE));
        return new ConfigValues(host, port, aiPath, opsPath, devicePreference);
    }

    static void save(ConfigValues values) throws IOException {
        ConfigValues safe = sanitize(values);
        Path configPath = configPath();
        Files.createDirectories(configPath.getParent());

        Properties props = new Properties();
        props.setProperty(KEY_HOST, safe.backendHost());
        props.setProperty(KEY_PORT, String.valueOf(safe.backendPort()));
        props.setProperty(KEY_AI_PATH, safe.aiRepoPath());
        props.setProperty(KEY_OPS_PATH, safe.opsRepoPath());
        props.setProperty(KEY_DEVICE_PREFERENCE, safe.devicePreference());

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "dPolaris Control Center");
        }
    }

    static ConfigValues sanitize(ConfigValues values) {
        if (values == null) {
            return defaults();
        }
        String host = normalizeHost(values.backendHost());
        int port = values.backendPort() > 0 ? values.backendPort() : DEFAULT_PORT;
        String aiPath = normalizePath(values.aiRepoPath(), DEFAULT_AI_REPO_PATH);
        String opsPath = normalizePath(values.opsRepoPath(), DEFAULT_OPS_REPO_PATH);
        String devicePreference = normalizeDevicePreference(values.devicePreference());
        return new ConfigValues(host, port, aiPath, opsPath, devicePreference);
    }

    static ConfigValues defaults() {
        return new ConfigValues(
                DEFAULT_HOST,
                DEFAULT_PORT,
                DEFAULT_AI_REPO_PATH,
                DEFAULT_OPS_REPO_PATH,
                DEFAULT_DEVICE_PREFERENCE
        );
    }

    private static Path configPath() {
        return Path.of(System.getProperty("user.home"), ".dpolaris", "control-center.properties");
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return DEFAULT_HOST;
        }
        return host.trim();
    }

    private static int parsePort(String portText) {
        if (portText == null || portText.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            int parsed = Integer.parseInt(portText.trim());
            return parsed > 0 ? parsed : DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return DEFAULT_PORT;
        }
    }

    private static String normalizePath(String path, String fallback) {
        if (path == null || path.isBlank()) {
            return fallback;
        }
        return path.trim();
    }

    private static String normalizeDevicePreference(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DEVICE_PREFERENCE;
        }
        String lowered = value.trim().toLowerCase();
        return switch (lowered) {
            case "cuda", "mps", "cpu" -> lowered;
            default -> DEFAULT_DEVICE_PREFERENCE;
        };
    }

    record ConfigValues(
            String backendHost,
            int backendPort,
            String aiRepoPath,
            String opsRepoPath,
            String devicePreference
    ) {
    }
}
