package com.dpolaris.javaapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backend control panel for one-click start/stop/restart of the dpolaris_ai backend.
 * Provides real-time status, log viewer, and device selection.
 */
public final class BackendControlPanel extends JPanel {
    private static final Color COLOR_BG = new Color(23, 24, 29);
    private static final Color COLOR_CARD = new Color(34, 36, 43);
    private static final Color COLOR_BORDER = new Color(74, 78, 90);
    private static final Color COLOR_TEXT = new Color(236, 238, 244);
    private static final Color COLOR_MUTED = new Color(173, 179, 193);
    private static final Color COLOR_ACCENT = new Color(54, 130, 247);
    private static final Color COLOR_SUCCESS = new Color(45, 194, 117);
    private static final Color COLOR_DANGER = new Color(252, 86, 93);
    private static final Color COLOR_WARNING = new Color(243, 176, 57);
    private static final Color COLOR_LOG_BG = new Color(19, 20, 24);
    private static final Color COLOR_INPUT = new Color(49, 52, 62);

    private final BackendProcessManager processManager;
    private final Font uiFont;
    private final Font monoFont;

    // Configuration fields
    private JTextField aiPathField;
    private JTextField hostField;
    private JTextField portField;
    private JComboBox<String> deviceCombo;

    // Control buttons
    private JButton startButton;
    private JButton stopButton;
    private JButton resetRestartButton;

    // Status display
    private JLabel statusLabel;
    private JLabel pidLabel;
    private JLabel uptimeLabel;

    // Log area
    private JTextArea logArea;
    private JScrollPane logScroll;

    private Timer statusTimer;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);

    public BackendControlPanel(Font uiFont, Font monoFont) {
        this.uiFont = uiFont;
        this.monoFont = monoFont;
        this.processManager = new BackendProcessManager();

        setLayout(new BorderLayout(10, 10));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(createConfigPanel(), BorderLayout.NORTH);
        add(createLogPanel(), BorderLayout.CENTER);

        // Set up log listener
        processManager.addLogListener(this::appendLog);

        // Start status update timer
        statusTimer = new Timer(2000, e -> updateStatus());
        statusTimer.start();

        // Initial status check
        SwingUtilities.invokeLater(this::updateStatus);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // Config row
        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        configRow.setOpaque(false);
        configRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        configRow.setBackground(COLOR_CARD);

        configRow.add(createLabel("AI Path:"));
        aiPathField = createTextField("~/my-git/dpolaris_ai", 20);
        configRow.add(aiPathField);

        configRow.add(createLabel("Host:"));
        hostField = createTextField("127.0.0.1", 10);
        configRow.add(hostField);

        configRow.add(createLabel("Port:"));
        portField = createTextField("8420", 5);
        configRow.add(portField);

        configRow.add(createLabel("Device:"));
        deviceCombo = new JComboBox<>(new String[]{"auto", "cpu", "mps", "cuda"});
        deviceCombo.setFont(uiFont);
        deviceCombo.setPreferredSize(new Dimension(90, 28));
        configRow.add(deviceCombo);

        panel.add(configRow);
        panel.add(Box.createVerticalStrut(10));

        // Control row
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlRow.setOpaque(false);

        startButton = createButton("Start Backend", COLOR_SUCCESS);
        startButton.addActionListener(e -> startBackend());
        controlRow.add(startButton);

        stopButton = createButton("Stop Backend", COLOR_DANGER);
        stopButton.addActionListener(e -> stopBackend());
        controlRow.add(stopButton);

        resetRestartButton = createButton("Reset & Restart", COLOR_WARNING);
        resetRestartButton.addActionListener(e -> resetAndRestart());
        controlRow.add(resetRestartButton);

        controlRow.add(Box.createHorizontalStrut(30));

        // Status indicators
        controlRow.add(createLabel("Status:"));
        statusLabel = new JLabel("Unknown");
        statusLabel.setFont(uiFont.deriveFont(Font.BOLD));
        statusLabel.setForeground(COLOR_MUTED);
        controlRow.add(statusLabel);

        controlRow.add(Box.createHorizontalStrut(15));
        controlRow.add(createLabel("PID:"));
        pidLabel = new JLabel("-");
        pidLabel.setFont(monoFont);
        pidLabel.setForeground(COLOR_MUTED);
        controlRow.add(pidLabel);

        controlRow.add(Box.createHorizontalStrut(15));
        controlRow.add(createLabel("Uptime:"));
        uptimeLabel = new JLabel("-");
        uptimeLabel.setFont(monoFont);
        uptimeLabel.setForeground(COLOR_MUTED);
        controlRow.add(uptimeLabel);

        panel.add(controlRow);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(COLOR_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(5, 10, 10, 10),
                "Backend Log",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                uiFont,
                COLOR_MUTED
            )
        ));

        logArea = new JTextArea();
        logArea.setFont(monoFont);
        logArea.setBackground(COLOR_LOG_BG);
        logArea.setForeground(COLOR_TEXT);
        logArea.setCaretColor(COLOR_TEXT);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        logScroll = new JScrollPane(logArea);
        logScroll.setBackground(COLOR_LOG_BG);
        logScroll.getViewport().setBackground(COLOR_LOG_BG);
        logScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(logScroll, BorderLayout.CENTER);

        // Clear button row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.setOpaque(false);
        JButton clearButton = createButton("Clear Log", COLOR_MUTED);
        clearButton.addActionListener(e -> logArea.setText(""));
        buttonRow.add(clearButton);
        panel.add(buttonRow, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(uiFont);
        label.setForeground(COLOR_TEXT);
        return label;
    }

    private JTextField createTextField(String defaultValue, int columns) {
        JTextField field = new JTextField(defaultValue, columns);
        field.setFont(uiFont);
        field.setBackground(COLOR_INPUT);
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        return field;
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(uiFont);
        button.setBackground(bg);
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 32));
        return button;
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto-scroll to bottom
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void applyConfiguration() {
        String aiPath = aiPathField.getText().trim();
        String host = hostField.getText().trim();
        int port = 8420;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {}

        processManager.configure(aiPath, host, port);
        processManager.setDevicePreference((String) deviceCombo.getSelectedItem());
    }

    private void startBackend() {
        setButtonsEnabled(false);
        applyConfiguration();
        appendLog("[UI] Starting backend...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return processManager.start();
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                updateStatus();
                try {
                    if (get()) {
                        appendLog("[UI] Backend started successfully");
                    } else {
                        appendLog("[UI] Failed to start backend");
                    }
                } catch (Exception e) {
                    appendLog("[UI] Error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void stopBackend() {
        setButtonsEnabled(false);
        appendLog("[UI] Stopping backend...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return processManager.stop();
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                updateStatus();
                try {
                    if (get()) {
                        appendLog("[UI] Backend stopped successfully");
                    } else {
                        appendLog("[UI] Failed to stop backend");
                    }
                } catch (Exception e) {
                    appendLog("[UI] Error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void resetAndRestart() {
        setButtonsEnabled(false);
        applyConfiguration();
        appendLog("[UI] Reset & Restart initiated...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return processManager.resetAndRestart();
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                updateStatus();
                try {
                    if (get()) {
                        appendLog("[UI] Backend reset and restarted successfully");
                    } else {
                        appendLog("[UI] Reset & Restart failed");
                    }
                } catch (Exception e) {
                    appendLog("[UI] Error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        stopButton.setEnabled(enabled);
        resetRestartButton.setEnabled(enabled);
    }

    private void updateStatus() {
        if (isUpdating.getAndSet(true)) return;

        SwingUtilities.invokeLater(() -> {
            try {
                BackendProcessManager.Status status = processManager.getStatus();

                // Update status label
                switch (status.state()) {
                    case RUNNING -> {
                        statusLabel.setText("Running");
                        statusLabel.setForeground(COLOR_SUCCESS);
                    }
                    case STARTING -> {
                        statusLabel.setText("Starting...");
                        statusLabel.setForeground(COLOR_WARNING);
                    }
                    case STOPPING -> {
                        statusLabel.setText("Stopping...");
                        statusLabel.setForeground(COLOR_WARNING);
                    }
                    case STOPPED -> {
                        statusLabel.setText("Stopped");
                        statusLabel.setForeground(COLOR_DANGER);
                    }
                    case ERROR -> {
                        statusLabel.setText("Error");
                        statusLabel.setForeground(COLOR_DANGER);
                    }
                }

                // Update PID
                Long pid = status.pid();
                pidLabel.setText(pid != null ? String.valueOf(pid) : "-");

                // Update uptime
                Instant startedAt = status.startedAt();
                if (startedAt != null && status.state() == BackendProcessManager.State.RUNNING) {
                    Duration uptime = Duration.between(startedAt, Instant.now());
                    uptimeLabel.setText(formatDuration(uptime));
                } else {
                    uptimeLabel.setText("-");
                }

                // Enable/disable buttons based on state
                startButton.setEnabled(status.state() == BackendProcessManager.State.STOPPED ||
                                       status.state() == BackendProcessManager.State.ERROR);
                stopButton.setEnabled(status.state() == BackendProcessManager.State.RUNNING);
                resetRestartButton.setEnabled(true);

            } finally {
                isUpdating.set(false);
            }
        });
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    public BackendProcessManager getProcessManager() {
        return processManager;
    }

    public void shutdown() {
        if (statusTimer != null) {
            statusTimer.stop();
        }
    }
}
