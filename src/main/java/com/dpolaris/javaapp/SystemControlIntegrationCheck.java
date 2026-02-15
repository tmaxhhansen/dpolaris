package com.dpolaris.javaapp;

import java.util.Map;

public final class SystemControlIntegrationCheck {
    private SystemControlIntegrationCheck() {
    }

    public static void main(String[] args) throws Exception {
        SystemControlConfig.ConfigValues config = SystemControlConfig.load();
        String host = config.backendHost();
        int port = config.backendPort();

        String hostOverride = System.getenv("DPOLARIS_BACKEND_HOST");
        if (hostOverride != null && !hostOverride.isBlank()) {
            host = hostOverride.trim();
        }
        String portOverride = System.getenv("DPOLARIS_BACKEND_PORT");
        if (portOverride != null && !portOverride.isBlank()) {
            try {
                int parsed = Integer.parseInt(portOverride.trim());
                if (parsed > 0) {
                    port = parsed;
                }
            } catch (NumberFormatException ignored) {
                // Keep configured port.
            }
        }

        ApiClient client = new ApiClient(host, port);
        System.out.println("System Control integration check");
        System.out.println("Target backend: " + host + ":" + port);

        if (!client.healthCheck(2)) {
            System.out.println("SKIP: backend is not reachable at " + host + ":" + port + ".");
            System.out.println("Reason: environment does not currently have a healthy backend to validate.");
            return;
        }

        Map<String, Object> status;
        try {
            status = client.fetchBackendControlStatus();
            System.out.println("Backend status endpoint reachable. Keys=" + status.keySet());
        } catch (Exception ex) {
            System.out.println("FAIL: backend status endpoint call failed: " + ex.getMessage());
            System.exit(1);
            return;
        }

        try {
            Map<String, Object> restart = client.restartBackendControl(false);
            System.out.println("Restart call completed. Keys=" + restart.keySet());
        } catch (Exception ex) {
            System.out.println("FAIL: backend restart call failed: " + ex.getMessage());
            System.exit(1);
            return;
        }

        boolean healthyAfterRestart = false;
        for (int i = 0; i < 20; i++) {
            if (client.healthCheck(2)) {
                healthyAfterRestart = true;
                break;
            }
            Thread.sleep(500);
        }

        if (!healthyAfterRestart) {
            System.out.println("FAIL: /health did not recover after backend restart.");
            System.exit(1);
            return;
        }

        System.out.println("PASS: backend status + restart + /health verification succeeded.");
    }
}
