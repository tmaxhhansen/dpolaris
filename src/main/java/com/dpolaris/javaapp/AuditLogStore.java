package com.dpolaris.javaapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AuditLogStore {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final Path logPath;

    AuditLogStore() {
        this(defaultPath());
    }

    AuditLogStore(Path logPath) {
        this.logPath = logPath.toAbsolutePath().normalize();
    }

    Path getLogPath() {
        return logPath;
    }

    synchronized void append(Map<String, Object> entry) throws IOException {
        if (entry == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>(entry);
        if (!payload.containsKey("timestamp")) {
            payload.put("timestamp", LocalDateTime.now().toString());
        }

        Path parent = logPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String line = Json.compact(payload) + System.lineSeparator();
        Files.writeString(
                logPath,
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
        );
    }

    synchronized List<Map<String, Object>> readLatest(int limit) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.exists(logPath)) {
            return out;
        }

        int safeLimit = Math.max(1, limit);
        List<String> lines = Files.readAllLines(logPath, StandardCharsets.UTF_8);
        for (int i = lines.size() - 1; i >= 0 && out.size() < safeLimit; i--) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                Object parsed = Json.parse(line);
                if (parsed instanceof Map<?, ?> mapRaw) {
                    out.add(Json.asObject(mapRaw));
                } else {
                    Map<String, Object> wrapped = new LinkedHashMap<>();
                    wrapped.put("raw", line);
                    wrapped.put("parse_error", "line is not a JSON object");
                    out.add(wrapped);
                }
            } catch (RuntimeException parseError) {
                Map<String, Object> wrapped = new LinkedHashMap<>();
                wrapped.put("raw", line);
                wrapped.put("parse_error", parseError.getMessage());
                out.add(wrapped);
            }
        }
        return out;
    }

    synchronized Path exportSnapshot(Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path out = targetDir.resolve("audit_log_" + LocalDateTime.now().format(TS) + ".jsonl");
        if (!Files.exists(logPath)) {
            Files.writeString(out, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return out;
        }
        Files.copy(logPath, out);
        return out;
    }

    private static Path defaultPath() {
        String override = System.getenv("DPOLARIS_AUDIT_LOG");
        if (override != null && !override.isBlank()) {
            return Path.of(expandHome(override.trim()));
        }
        return Path.of(System.getProperty("user.home"), "dpolaris_data", "audit_log.jsonl");
    }

    private static String expandHome(String value) {
        if (value.equals("~")) {
            return System.getProperty("user.home");
        }
        if (value.startsWith("~/")) {
            return System.getProperty("user.home") + value.substring(1);
        }
        return value;
    }
}
