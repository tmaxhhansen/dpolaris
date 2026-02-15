package com.dpolaris.javaapp;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Standalone launcher for the dPolaris Control Center.
 * Provides backend lifecycle control and deep learning training UI.
 *
 * Run with: ./gradlew run -PmainClass=com.dpolaris.javaapp.ControlCenterLauncher
 * Or: java -cp build/classes/java/main com.dpolaris.javaapp.ControlCenterLauncher
 */
public final class ControlCenterLauncher {
    private static final Color COLOR_BG = new Color(23, 24, 29);
    private static final Color COLOR_CARD = new Color(34, 36, 43);
    private static final Color COLOR_BORDER = new Color(74, 78, 90);
    private static final Color COLOR_TEXT = new Color(236, 238, 244);
    private static final Color COLOR_MUTED = new Color(173, 179, 193);
    private static final Color COLOR_ACCENT = new Color(54, 130, 247);

    public static void main(String[] args) {
        // Set system properties for macOS
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.awt.application.name", "dPolaris Control Center");

        SwingUtilities.invokeLater(ControlCenterLauncher::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        try {
            // Use system look and feel on macOS
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to cross-platform look
        }

        // Create fonts
        Font uiFont = loadFont(13f, "SF Pro Text", "Helvetica Neue", "Segoe UI");
        Font monoFont = loadFont(12f, "SF Mono", "Menlo", "Consolas");

        // Create main frame
        JFrame frame = new JFrame("dPolaris Control Center");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(COLOR_BG);

        // Create API client
        ApiClient apiClient = new ApiClient("127.0.0.1", 8420);

        // Create panels
        BackendControlPanel backendPanel = new BackendControlPanel(uiFont, monoFont);
        DeepLearningPanel deepLearningPanel = new DeepLearningPanel(apiClient, uiFont, monoFont);
        JPanel diagnosticsPanel = createDiagnosticsPanel(apiClient, backendPanel, uiFont, monoFont);

        // Create tabbed pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(uiFont);
        tabs.setBackground(COLOR_BG);
        tabs.setForeground(COLOR_TEXT);

        // Style tabs
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                             int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? COLOR_CARD : COLOR_BG);
                g.fillRect(x, y, w, h);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                         int x, int y, int w, int h, boolean isSelected) {
                g.setColor(isSelected ? COLOR_ACCENT : COLOR_BORDER);
                g.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // No content border
            }
        });

        tabs.addTab("Backend Control", backendPanel);
        tabs.addTab("Deep Learning", deepLearningPanel);
        tabs.addTab("Diagnostics", diagnosticsPanel);

        frame.add(tabs);

        // Handle window close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(frame,
                    "Stop backend before closing?",
                    "Close Control Center",
                    JOptionPane.YES_NO_CANCEL_OPTION);

                if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                if (result == JOptionPane.YES_OPTION) {
                    backendPanel.getProcessManager().stop();
                }

                backendPanel.shutdown();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }

    private static JPanel createDiagnosticsPanel(ApiClient apiClient, BackendControlPanel backendPanel,
                                                  Font uiFont, Font monoFont) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Status area
        JTextArea statusArea = new JTextArea();
        statusArea.setFont(monoFont);
        statusArea.setBackground(new Color(19, 20, 24));
        statusArea.setForeground(COLOR_TEXT);
        statusArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            "System Diagnostics",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            uiFont,
            COLOR_MUTED
        ));
        scrollPane.getViewport().setBackground(new Color(19, 20, 24));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setOpaque(false);

        JButton refreshButton = new JButton("Run Diagnostics");
        refreshButton.setFont(uiFont);
        refreshButton.setBackground(COLOR_ACCENT);
        refreshButton.setForeground(COLOR_TEXT);
        refreshButton.addActionListener(e -> {
            refreshButton.setEnabled(false);
            statusArea.setText("Running diagnostics...\n\n");

            new SwingWorker<String, String>() {
                @Override
                protected String doInBackground() throws Exception {
                    StringBuilder sb = new StringBuilder();

                    // Check backend status
                    publish("Checking backend process...");
                    BackendProcessManager.Status status = backendPanel.getProcessManager().getStatus();
                    sb.append("Backend Process\n");
                    sb.append("  State: ").append(status.state()).append("\n");
                    sb.append("  PID: ").append(status.pid() != null ? status.pid() : "N/A").append("\n");
                    sb.append("  Message: ").append(status.message()).append("\n\n");

                    // Check API health
                    publish("Checking API health...");
                    sb.append("API Health Check\n");
                    boolean healthy = apiClient.healthCheck();
                    sb.append("  Status: ").append(healthy ? "HEALTHY" : "UNREACHABLE").append("\n\n");

                    if (healthy) {
                        // Get status
                        publish("Fetching API status...");
                        try {
                            Object apiStatus = apiClient.fetchStatus();
                            sb.append("API Status\n");
                            sb.append("  Response: ").append(apiStatus).append("\n\n");
                        } catch (Exception ex) {
                            sb.append("API Status: Error - ").append(ex.getMessage()).append("\n\n");
                        }

                        // Check deep learning status
                        publish("Checking deep learning status...");
                        try {
                            Object dlStatus = apiClient.request("GET", "/api/deep-learning/status", null, 15);
                            sb.append("Deep Learning Status\n");
                            sb.append("  ").append(dlStatus).append("\n\n");
                        } catch (Exception ex) {
                            sb.append("Deep Learning Status: ").append(ex.getMessage()).append("\n\n");
                        }
                    }

                    // System info
                    publish("Gathering system info...");
                    sb.append("System Information\n");
                    sb.append("  OS: ").append(System.getProperty("os.name")).append(" ")
                      .append(System.getProperty("os.version")).append("\n");
                    sb.append("  Java: ").append(System.getProperty("java.version")).append("\n");
                    sb.append("  User Home: ").append(System.getProperty("user.home")).append("\n");

                    Runtime runtime = Runtime.getRuntime();
                    sb.append("  Memory: ").append(runtime.freeMemory() / 1024 / 1024).append("MB free / ")
                      .append(runtime.maxMemory() / 1024 / 1024).append("MB max\n\n");

                    // Check Python environment
                    publish("Checking Python environment...");
                    sb.append("Python Environment\n");
                    String aiPath = System.getProperty("user.home") + "/my-git/dpolaris_ai";
                    java.io.File venv = new java.io.File(aiPath + "/.venv/bin/python");
                    sb.append("  AI Path: ").append(aiPath).append("\n");
                    sb.append("  Venv Python: ").append(venv.exists() ? "Found" : "NOT FOUND").append("\n");

                    return sb.toString();
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String msg : chunks) {
                        statusArea.append(msg + "\n");
                    }
                }

                @Override
                protected void done() {
                    try {
                        statusArea.setText(get());
                    } catch (Exception ex) {
                        statusArea.append("\nError: " + ex.getMessage());
                    }
                    refreshButton.setEnabled(true);
                }
            }.execute();
        });
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static Font loadFont(float size, String... names) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        java.util.Set<String> availableSet = new java.util.HashSet<>(java.util.Arrays.asList(available));

        for (String name : names) {
            if (availableSet.contains(name)) {
                return new Font(name, Font.PLAIN, (int) size);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }
}
