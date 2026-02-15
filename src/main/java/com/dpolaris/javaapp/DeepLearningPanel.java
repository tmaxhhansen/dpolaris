package com.dpolaris.javaapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deep Learning training panel with universe selection, ticker table,
 * one-click training, job monitoring, and log viewer.
 */
@SuppressWarnings("unchecked")
public final class DeepLearningPanel extends JPanel {
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

    private final ApiClient apiClient;
    private final Font uiFont;
    private final Font monoFont;

    // Universe components
    private JComboBox<String> universeCombo;
    private JButton refreshUniverseButton;
    private JLabel universeStatusLabel;

    // Ticker table
    private TickerTableModel tickerTableModel;
    private JTable tickerTable;
    private JTextField tickerFilterField;

    // Training config
    private JComboBox<String> modelCombo;
    private JSpinner epochsSpinner;
    private JButton trainButton;
    private JButton predictButton;

    // Job status
    private JLabel jobStatusLabel;
    private JProgressBar jobProgressBar;
    private JLabel jobIdLabel;

    // Device info
    private JLabel deviceLabel;
    private JComboBox<String> deviceCombo;

    // Log viewer
    private JTextArea logArea;
    private final List<String> logBuffer = new ArrayList<>();
    private static final int MAX_LOG_LINES = 500;

    // State
    private String currentJobId = null;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private javax.swing.Timer pollingTimer;

    public DeepLearningPanel(ApiClient apiClient, Font uiFont, Font monoFont) {
        this.apiClient = apiClient;
        this.uiFont = uiFont;
        this.monoFont = monoFont;

        setLayout(new BorderLayout(10, 10));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(createTopPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        // Initial data load
        SwingUtilities.invokeLater(this::loadUniverseList);
        SwingUtilities.invokeLater(this::loadDeviceInfo);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Universe selection row
        JPanel universeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        universeRow.setOpaque(false);

        JLabel universeLabel = new JLabel("Universe:");
        universeLabel.setFont(uiFont);
        universeLabel.setForeground(COLOR_TEXT);
        universeRow.add(universeLabel);

        universeCombo = new JComboBox<>(new String[]{"nasdaq_top_500", "wsb_favorites", "combined"});
        universeCombo.setFont(uiFont);
        universeCombo.setPreferredSize(new Dimension(180, 28));
        universeCombo.addActionListener(e -> loadSelectedUniverse());
        universeRow.add(universeCombo);

        refreshUniverseButton = createButton("Refresh", COLOR_ACCENT);
        refreshUniverseButton.addActionListener(e -> loadSelectedUniverse());
        universeRow.add(refreshUniverseButton);

        universeStatusLabel = new JLabel("");
        universeStatusLabel.setFont(uiFont);
        universeStatusLabel.setForeground(COLOR_MUTED);
        universeRow.add(universeStatusLabel);

        // Device info row
        JPanel deviceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        deviceRow.setOpaque(false);

        JLabel deviceInfoLabel = new JLabel("Device:");
        deviceInfoLabel.setFont(uiFont);
        deviceInfoLabel.setForeground(COLOR_TEXT);
        deviceRow.add(deviceInfoLabel);

        deviceLabel = new JLabel("Detecting...");
        deviceLabel.setFont(uiFont);
        deviceLabel.setForeground(COLOR_SUCCESS);
        deviceRow.add(deviceLabel);

        deviceRow.add(Box.createHorizontalStrut(20));

        JLabel prefLabel = new JLabel("Preference:");
        prefLabel.setFont(uiFont);
        prefLabel.setForeground(COLOR_TEXT);
        deviceRow.add(prefLabel);

        deviceCombo = new JComboBox<>(new String[]{"auto", "cpu", "mps", "cuda"});
        deviceCombo.setFont(uiFont);
        deviceCombo.setPreferredSize(new Dimension(100, 28));
        deviceRow.add(deviceCombo);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(universeRow);
        topPanel.add(deviceRow);

        panel.add(topPanel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        // Left side: ticker table
        JPanel tickerPanel = createCard("Tickers");
        tickerPanel.setLayout(new BorderLayout(5, 5));

        // Filter
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterRow.setOpaque(false);
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(uiFont);
        filterLabel.setForeground(COLOR_TEXT);
        filterRow.add(filterLabel);

        tickerFilterField = new JTextField(15);
        tickerFilterField.setFont(uiFont);
        tickerFilterField.setBackground(COLOR_INPUT);
        tickerFilterField.setForeground(COLOR_TEXT);
        tickerFilterField.setCaretColor(COLOR_TEXT);
        tickerFilterField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        tickerFilterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTickers(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTickers(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTickers(); }
        });
        filterRow.add(tickerFilterField);
        tickerPanel.add(filterRow, BorderLayout.NORTH);

        // Table
        tickerTableModel = new TickerTableModel();
        tickerTable = new JTable(tickerTableModel);
        tickerTable.setFont(uiFont);
        tickerTable.setBackground(COLOR_CARD);
        tickerTable.setForeground(COLOR_TEXT);
        tickerTable.setGridColor(COLOR_BORDER);
        tickerTable.setSelectionBackground(COLOR_ACCENT);
        tickerTable.setSelectionForeground(COLOR_TEXT);
        tickerTable.setRowHeight(26);
        tickerTable.getTableHeader().setFont(uiFont);
        tickerTable.getTableHeader().setBackground(COLOR_CARD);
        tickerTable.getTableHeader().setForeground(COLOR_MUTED);
        tickerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = new JScrollPane(tickerTable);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        tickerPanel.add(tableScroll, BorderLayout.CENTER);

        // Right side: training config & job status
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        // Training config card
        JPanel configCard = createCard("Training Config");
        configCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel modelLabel = new JLabel("Model:");
        modelLabel.setFont(uiFont);
        modelLabel.setForeground(COLOR_TEXT);
        configCard.add(modelLabel, gbc);

        gbc.gridx = 1;
        modelCombo = new JComboBox<>(new String[]{"lstm", "transformer", "tcn", "wavenet"});
        modelCombo.setFont(uiFont);
        modelCombo.setPreferredSize(new Dimension(150, 28));
        configCard.add(modelCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel epochsLabel = new JLabel("Epochs:");
        epochsLabel.setFont(uiFont);
        epochsLabel.setForeground(COLOR_TEXT);
        configCard.add(epochsLabel, gbc);

        gbc.gridx = 1;
        epochsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 500, 10));
        epochsSpinner.setFont(uiFont);
        epochsSpinner.setPreferredSize(new Dimension(80, 28));
        configCard.add(epochsSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonRow.setOpaque(false);

        trainButton = createButton("Train", COLOR_SUCCESS);
        trainButton.addActionListener(e -> startTraining());
        buttonRow.add(trainButton);

        predictButton = createButton("Predict", COLOR_ACCENT);
        predictButton.addActionListener(e -> runPrediction());
        buttonRow.add(predictButton);

        configCard.add(buttonRow, gbc);
        rightPanel.add(configCard);
        rightPanel.add(Box.createVerticalStrut(10));

        // Job status card
        JPanel statusCard = createCard("Job Status");
        statusCard.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(uiFont);
        statusLabel.setForeground(COLOR_TEXT);
        statusCard.add(statusLabel, gbc);

        gbc.gridx = 1;
        jobStatusLabel = new JLabel("Idle");
        jobStatusLabel.setFont(uiFont);
        jobStatusLabel.setForeground(COLOR_MUTED);
        statusCard.add(jobStatusLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel idLabel = new JLabel("Job ID:");
        idLabel.setFont(uiFont);
        idLabel.setForeground(COLOR_TEXT);
        statusCard.add(idLabel, gbc);

        gbc.gridx = 1;
        jobIdLabel = new JLabel("-");
        jobIdLabel.setFont(monoFont);
        jobIdLabel.setForeground(COLOR_MUTED);
        statusCard.add(jobIdLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        jobProgressBar = new JProgressBar(0, 100);
        jobProgressBar.setStringPainted(true);
        jobProgressBar.setFont(uiFont);
        jobProgressBar.setPreferredSize(new Dimension(250, 22));
        statusCard.add(jobProgressBar, gbc);

        rightPanel.add(statusCard);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tickerPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = createCard("Training Log");
        panel.setLayout(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(0, 200));

        logArea = new JTextArea();
        logArea.setFont(monoFont);
        logArea.setBackground(COLOR_LOG_BG);
        logArea.setForeground(COLOR_TEXT);
        logArea.setCaretColor(COLOR_TEXT);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBackground(COLOR_LOG_BG);
        scrollPane.getViewport().setBackground(COLOR_LOG_BG);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Clear button
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.setOpaque(false);
        JButton clearButton = createButton("Clear", COLOR_MUTED);
        clearButton.addActionListener(e -> {
            logBuffer.clear();
            logArea.setText("");
        });
        buttonRow.add(clearButton);
        panel.add(buttonRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(5, 10, 10, 10),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                uiFont,
                COLOR_MUTED
            )
        ));
        return panel;
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
        return button;
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logBuffer.add(message);
            while (logBuffer.size() > MAX_LOG_LINES) {
                logBuffer.remove(0);
            }
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void loadUniverseList() {
        appendLog("[UI] Loading universe list...");
        // The combo is pre-populated with known universes
        // Could fetch from API if endpoint exists
    }

    private void loadSelectedUniverse() {
        String universe = (String) universeCombo.getSelectedItem();
        if (universe == null || universe.isBlank()) return;

        universeStatusLabel.setText("Loading...");
        appendLog("[UI] Loading universe: " + universe);

        new SwingWorker<List<Map<String, Object>>, Void>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                Object response = apiClient.fetchUniverse(universe);
                if (response instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) response;
                    Object tickers = map.get("tickers");
                    if (tickers == null) tickers = map.get("symbols");
                    if (tickers == null) tickers = map.get("data");
                    if (tickers instanceof List) {
                        return convertToTickerList((List<?>) tickers);
                    }
                }
                if (response instanceof List) {
                    return convertToTickerList((List<?>) response);
                }
                return new ArrayList<>();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> tickers = get();
                    tickerTableModel.setTickers(tickers);
                    universeStatusLabel.setText(tickers.size() + " tickers");
                    appendLog("[UI] Loaded " + tickers.size() + " tickers from " + universe);
                } catch (Exception e) {
                    universeStatusLabel.setText("Error");
                    appendLog("[UI] Error loading universe: " + e.getMessage());
                }
            }
        }.execute();
    }

    private List<Map<String, Object>> convertToTickerList(List<?> raw) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            Map<String, Object> ticker = new LinkedHashMap<>();
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                Object symbol = map.get("symbol");
                if (symbol == null) symbol = map.get("ticker");
                if (symbol == null) symbol = "";
                ticker.put("symbol", String.valueOf(symbol));

                Object name = map.get("name");
                if (name == null) name = map.get("company");
                if (name == null) name = "";
                ticker.put("name", String.valueOf(name));

                Object sector = map.get("sector");
                ticker.put("sector", String.valueOf(sector != null ? sector : ""));

                Object marketCap = map.get("market_cap");
                if (marketCap == null) marketCap = map.get("marketCap");
                ticker.put("market_cap", marketCap != null ? marketCap : "");
            } else if (item instanceof String) {
                ticker.put("symbol", item);
                ticker.put("name", "");
                ticker.put("sector", "");
                ticker.put("market_cap", "");
            }
            if (!ticker.get("symbol").toString().isBlank()) {
                result.add(ticker);
            }
        }
        return result;
    }

    private void loadDeviceInfo() {
        appendLog("[UI] Fetching device info...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    // Try the deep-learning status endpoint
                    Object response = apiClient.request("GET", "/api/deep-learning/status", null, 15);
                    if (response instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) response;
                        Object device = map.get("device");
                        if (device == null) device = map.get("compute_device");
                        if (device != null) return device.toString();
                    }
                } catch (Exception ignored) {}
                // Fallback: detect locally
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) return "mps (Apple Silicon)";
                return "cpu";
            }

            @Override
            protected void done() {
                try {
                    String device = get();
                    deviceLabel.setText(device);
                    appendLog("[UI] Device: " + device);
                } catch (Exception e) {
                    deviceLabel.setText("Unknown");
                }
            }
        }.execute();
    }

    private void filterTickers() {
        String filter = tickerFilterField.getText().trim().toLowerCase();
        tickerTableModel.setFilter(filter);
    }

    private void startTraining() {
        int row = tickerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a ticker first", "No Ticker Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String symbol = (String) tickerTableModel.getValueAt(tickerTable.convertRowIndexToModel(row), 0);
        String model = (String) modelCombo.getSelectedItem();
        int epochs = (Integer) epochsSpinner.getValue();

        appendLog("[Training] Starting " + model + " training for " + symbol + " (" + epochs + " epochs)");
        jobStatusLabel.setText("Starting...");
        jobStatusLabel.setForeground(COLOR_WARNING);
        trainButton.setEnabled(false);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return apiClient.enqueueDeepLearningJob(symbol, model, epochs);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> result = get();
                    String jobId = String.valueOf(result.getOrDefault("job_id", result.getOrDefault("id", "")));
                    if (!jobId.isBlank() && !jobId.equals("null")) {
                        currentJobId = jobId;
                        jobIdLabel.setText(jobId.length() > 20 ? jobId.substring(0, 20) + "..." : jobId);
                        jobStatusLabel.setText("Queued");
                        jobStatusLabel.setForeground(COLOR_ACCENT);
                        appendLog("[Training] Job queued: " + jobId);
                        startPolling();
                    } else {
                        jobStatusLabel.setText("Error");
                        jobStatusLabel.setForeground(COLOR_DANGER);
                        appendLog("[Training] Failed to start job: " + result);
                        trainButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    jobStatusLabel.setText("Error");
                    jobStatusLabel.setForeground(COLOR_DANGER);
                    appendLog("[Training] Error: " + e.getMessage());
                    trainButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void startPolling() {
        if (isPolling.getAndSet(true)) return;

        pollingTimer = new javax.swing.Timer(2000, e -> pollJobStatus());
        pollingTimer.start();
        appendLog("[Polling] Started job status polling");
    }

    private void stopPolling() {
        isPolling.set(false);
        if (pollingTimer != null) {
            pollingTimer.stop();
            pollingTimer = null;
        }
        trainButton.setEnabled(true);
    }

    private void pollJobStatus() {
        if (currentJobId == null || currentJobId.isBlank()) {
            stopPolling();
            return;
        }

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return apiClient.fetchJob(currentJobId);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> job = get();
                    updateJobStatus(job);
                } catch (Exception e) {
                    appendLog("[Polling] Error fetching job status: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void updateJobStatus(Map<String, Object> job) {
        String status = String.valueOf(job.getOrDefault("status", "unknown"));
        int progress = 0;
        Object progressObj = job.get("progress");
        if (progressObj instanceof Number) {
            progress = ((Number) progressObj).intValue();
        }

        jobStatusLabel.setText(status);
        jobProgressBar.setValue(progress);

        // Color based on status
        switch (status.toLowerCase()) {
            case "running", "in_progress" -> {
                jobStatusLabel.setForeground(COLOR_ACCENT);
                // Extract log lines if available
                Object logs = job.get("logs");
                if (logs instanceof List) {
                    List<?> logList = (List<?>) logs;
                    for (Object line : logList) {
                        String logLine = String.valueOf(line);
                        if (!logBuffer.contains(logLine)) {
                            appendLog("[Job] " + logLine);
                        }
                    }
                }
            }
            case "completed", "success" -> {
                jobStatusLabel.setForeground(COLOR_SUCCESS);
                jobProgressBar.setValue(100);
                appendLog("[Training] Job completed successfully!");
                stopPolling();
            }
            case "failed", "error" -> {
                jobStatusLabel.setForeground(COLOR_DANGER);
                String error = String.valueOf(job.getOrDefault("error", job.getOrDefault("message", "Unknown error")));
                appendLog("[Training] Job failed: " + error);
                stopPolling();
            }
            case "queued", "pending" -> jobStatusLabel.setForeground(COLOR_WARNING);
            default -> jobStatusLabel.setForeground(COLOR_MUTED);
        }
    }

    private void runPrediction() {
        int row = tickerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a ticker first", "No Ticker Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String symbol = (String) tickerTableModel.getValueAt(tickerTable.convertRowIndexToModel(row), 0);
        appendLog("[Predict] Running prediction for " + symbol);
        predictButton.setEnabled(false);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return apiClient.predictSymbol(symbol);
            }

            @Override
            protected void done() {
                predictButton.setEnabled(true);
                try {
                    Map<String, Object> result = get();
                    appendLog("[Predict] Result for " + symbol + ":");
                    appendLog("  Prediction: " + result.getOrDefault("prediction", result.getOrDefault("signal", "N/A")));
                    appendLog("  Confidence: " + result.getOrDefault("confidence", "N/A"));
                    appendLog("  Direction: " + result.getOrDefault("direction", result.getOrDefault("bias", "N/A")));

                    // Show dialog
                    StringBuilder sb = new StringBuilder();
                    sb.append("Symbol: ").append(symbol).append("\n\n");
                    for (Map.Entry<String, Object> entry : result.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    JOptionPane.showMessageDialog(DeepLearningPanel.this, sb.toString(),
                        "Prediction Result", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    appendLog("[Predict] Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(DeepLearningPanel.this,
                        "Prediction failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public String getSelectedDevice() {
        return (String) deviceCombo.getSelectedItem();
    }

    // Ticker table model with filtering
    private static class TickerTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Symbol", "Name", "Sector", "Market Cap"};
        private List<Map<String, Object>> allTickers = new ArrayList<>();
        private List<Map<String, Object>> filteredTickers = new ArrayList<>();
        private String filter = "";

        public void setTickers(List<Map<String, Object>> tickers) {
            this.allTickers = new ArrayList<>(tickers);
            applyFilter();
        }

        public void setFilter(String filter) {
            this.filter = filter == null ? "" : filter.toLowerCase();
            applyFilter();
        }

        private void applyFilter() {
            if (filter.isBlank()) {
                filteredTickers = new ArrayList<>(allTickers);
            } else {
                filteredTickers = new ArrayList<>();
                for (Map<String, Object> ticker : allTickers) {
                    String symbol = String.valueOf(ticker.getOrDefault("symbol", "")).toLowerCase();
                    String name = String.valueOf(ticker.getOrDefault("name", "")).toLowerCase();
                    if (symbol.contains(filter) || name.contains(filter)) {
                        filteredTickers.add(ticker);
                    }
                }
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return filteredTickers.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= filteredTickers.size()) return "";
            Map<String, Object> ticker = filteredTickers.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> ticker.getOrDefault("symbol", "");
                case 1 -> ticker.getOrDefault("name", "");
                case 2 -> ticker.getOrDefault("sector", "");
                case 3 -> formatMarketCap(ticker.get("market_cap"));
                default -> "";
            };
        }

        private String formatMarketCap(Object value) {
            if (value == null) return "";
            if (value instanceof Number) {
                double num = ((Number) value).doubleValue();
                if (num >= 1e12) return String.format("%.1fT", num / 1e12);
                if (num >= 1e9) return String.format("%.1fB", num / 1e9);
                if (num >= 1e6) return String.format("%.1fM", num / 1e6);
                return String.format("%.0f", num);
            }
            return String.valueOf(value);
        }
    }
}
