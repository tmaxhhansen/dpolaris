package com.dpolaris.javaapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class AnalysisWorkspacePanel extends JPanel {
    private static final Color COLOR_BG = new Color(23, 24, 29);
    private static final Color COLOR_CARD = new Color(34, 36, 43);
    private static final Color COLOR_BORDER = new Color(74, 78, 90);
    private static final Color COLOR_TEXT = new Color(236, 238, 244);
    private static final Color COLOR_MUTED = new Color(173, 179, 193);
    private static final Color COLOR_ACCENT = new Color(54, 130, 247);
    private static final Color COLOR_DANGER = new Color(252, 86, 93);
    private static final Color COLOR_SUCCESS = new Color(45, 194, 117);

    private final ApiClient apiClient;
    private final Font uiFont;
    private final Font monoFont;

    private final JTextField searchField = new JTextField(12);
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel statusLabel = new JLabel("Ready");

    private final AnalysisTableModel tableModel = new AnalysisTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<AnalysisTableModel> sorter = new TableRowSorter<>(tableModel);

    private final JLabel detailHeader = new JLabel("Analysis Detail");
    private final JLabel detailMeta = new JLabel("Select an analysis item.");
    private final JButton openOnDiskButton = new JButton("Open on disk");
    private final JTextArea detailArea = new JTextArea();
    private String detailPath = "";

    AnalysisWorkspacePanel(ApiClient apiClient, Font uiFont, Font monoFont) {
        this.apiClient = apiClient;
        this.uiFont = uiFont;
        this.monoFont = monoFont;

        setLayout(new BorderLayout(8, 8));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(createFilterRow(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        refresh(false);
    }

    private JPanel createFilterRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);

        styleInput(searchField);
        styleInput(fromDateField);
        styleInput(toDateField);
        styleButton(refreshButton);
        styleButton(openOnDiskButton);
        openOnDiskButton.setEnabled(false);

        fromDateField.setToolTipText("From date YYYY-MM-DD (optional)");
        toDateField.setToolTipText("To date YYYY-MM-DD (optional)");

        row.add(label("Ticker"));
        row.add(searchField);
        row.add(label("From"));
        row.add(fromDateField);
        row.add(label("To"));
        row.add(toDateField);
        row.add(refreshButton);
        row.add(statusLabel);

        searchField.getDocument().addDocumentListener(SimpleDocumentListener.of(this::applyFilters));
        fromDateField.getDocument().addDocumentListener(SimpleDocumentListener.of(this::applyFilters));
        toDateField.getDocument().addDocumentListener(SimpleDocumentListener.of(this::applyFilters));
        refreshButton.addActionListener(e -> refresh(true));
        openOnDiskButton.addActionListener(e -> openOnDisk());

        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setFont(uiFont);

        return row;
    }

    private JSplitPane createMainContent() {
        table.setFont(uiFont);
        table.setForeground(COLOR_TEXT);
        table.setBackground(new Color(19, 20, 24));
        table.setGridColor(COLOR_BORDER);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(54, 90, 148));
        table.setSelectionForeground(COLOR_TEXT);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(520);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 1) {
                    openSelectedDetail();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        tableScroll.getViewport().setBackground(new Color(19, 20, 24));

        JPanel detailPanel = new JPanel(new BorderLayout(6, 6));
        detailPanel.setOpaque(false);

        detailHeader.setFont(uiFont.deriveFont(Font.BOLD, 16f));
        detailHeader.setForeground(COLOR_TEXT);
        detailMeta.setFont(uiFont);
        detailMeta.setForeground(COLOR_MUTED);

        JPanel detailTop = new JPanel();
        detailTop.setLayout(new BoxLayout(detailTop, BoxLayout.Y_AXIS));
        detailTop.setOpaque(false);
        detailTop.add(detailHeader);
        detailTop.add(Box.createVerticalStrut(4));
        detailTop.add(detailMeta);
        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        detailActions.setOpaque(false);
        detailActions.add(openOnDiskButton);
        detailTop.add(Box.createVerticalStrut(6));
        detailTop.add(detailActions);

        detailArea.setEditable(false);
        detailArea.setFont(monoFont);
        detailArea.setForeground(COLOR_TEXT);
        detailArea.setBackground(new Color(19, 20, 24));
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        detailScroll.getViewport().setBackground(new Color(19, 20, 24));

        detailPanel.add(detailTop, BorderLayout.NORTH);
        detailPanel.add(detailScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, detailPanel);
        split.setResizeWeight(0.45);
        split.setDividerLocation(260);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    void refresh(boolean force) {
        if (!force && tableModel.getRowCount() > 0) {
            return;
        }

        setStatus("Loading analysis list...", COLOR_MUTED);
        refreshButton.setEnabled(false);

        SwingWorker<List<AnalysisRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AnalysisRow> doInBackground() throws Exception {
                Object payload = apiClient.fetchAnalysisList(200);
                return parseRows(payload);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    List<AnalysisRow> rows = get();
                    tableModel.setRows(rows);
                    applyFilters();
                    setStatus("Loaded " + rows.size() + " analysis reports", COLOR_SUCCESS);
                } catch (Exception ex) {
                    setStatus("Backend unavailable: " + humanize(ex), COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    void openOrGenerateForTicker(String ticker) {
        String symbol = ticker == null ? "" : ticker.trim().toUpperCase();
        if (symbol.isBlank()) {
            return;
        }

        searchField.setText(symbol);
        setStatus("Loading analysis for " + symbol + "...", COLOR_MUTED);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Object bySymbol = apiClient.fetchAnalysisBySymbol(symbol, 1);
                List<AnalysisRow> rows = parseRows(bySymbol);
                if (!rows.isEmpty() && rows.get(0).id != null) {
                    return apiClient.fetchAnalysisArtifact(rows.get(0).id);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> payload = get();
                    if (payload == null || payload.isEmpty()) {
                        renderNoAnalysis(symbol);
                        setStatus("No analysis yet for " + symbol, COLOR_MUTED);
                        return;
                    }
                    renderDetail(payload);
                    setStatus("Analysis loaded for " + symbol, COLOR_SUCCESS);
                } catch (Exception ex) {
                    setStatus("Failed to load analysis: " + humanize(ex), COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void openSelectedDetail() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        AnalysisRow row = tableModel.getRow(modelRow);
        if (row == null || row.id == null || row.id.isBlank()) {
            return;
        }

        setStatus("Loading detail " + row.id + "...", COLOR_MUTED);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return apiClient.fetchAnalysisArtifact(row.id);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> payload = get();
                    renderDetail(payload);
                    setStatus("Detail loaded", COLOR_SUCCESS);
                } catch (Exception ex) {
                    setStatus("Detail load failed: " + humanize(ex), COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void renderDetail(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }

        String ticker = firstNonBlank(asString(payload.get("ticker")), asString(payload.get("symbol"))).toUpperCase();
        String createdAt = firstNonBlank(asString(payload.get("analysis_date")), asString(payload.get("created_at")));
        String model = asString(payload.get("model_type"));
        String device = asString(payload.get("device"));
        detailPath = asString(payload.get("path"));
        openOnDiskButton.setEnabled(!detailPath.isBlank());

        detailHeader.setText(ticker.isBlank() ? "Analysis Detail" : "Analysis Detail - " + ticker);
        detailMeta.setText("Date: " + simplifyDate(createdAt) + "   Model: " + model + "   Device: " + device);

        String report = asString(payload.get("report_text"));
        if (report.isBlank()) {
            Object nested = payload.get("report");
            if (nested instanceof Map<?, ?> map) {
                report = Json.pretty(Json.asObject(map));
            } else {
                report = Json.pretty(payload);
            }
        }
        detailArea.setText(report);
        detailArea.setCaretPosition(0);
    }

    private void renderNoAnalysis(String symbol) {
        detailPath = "";
        openOnDiskButton.setEnabled(false);
        detailHeader.setText("Analysis Detail - " + symbol);
        detailMeta.setText("No analysis artifact found for " + symbol + ".");
        detailArea.setText("No analysis yet for " + symbol + ".\n\nRun a deep-learning analysis job, then refresh.");
        detailArea.setCaretPosition(0);
    }

    private List<AnalysisRow> parseRows(Object payload) {
        List<AnalysisRow> rows = new ArrayList<>();
        List<?> list;
        if (payload instanceof List<?> payloadList) {
            list = payloadList;
        } else if (payload instanceof Map<?, ?> mapRaw) {
            Object data = Json.asObject(mapRaw).get("items");
            if (data instanceof List<?> inner) {
                list = inner;
            } else {
                return rows;
            }
        } else {
            return rows;
        }

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemMapRaw)) {
                continue;
            }
            Map<String, Object> map = Json.asObject(itemMapRaw);
            String id = asString(map.get("id"));
            String createdAt = firstNonBlank(asString(map.get("analysis_date")), asString(map.get("created_at")));
            String ticker = firstNonBlank(asString(map.get("ticker")), asString(map.get("symbol"))).toUpperCase();
            String model = asString(map.get("model_type"));
            String summary = asString(map.get("summary"));
            String device = asString(map.get("device"));
            rows.add(new AnalysisRow(id, createdAt, ticker, model, summary, device));
        }

        rows.sort(Comparator.comparing((AnalysisRow r) -> r.createdAt == null ? "" : r.createdAt).reversed());
        return rows;
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate from = parseDate(fromDateField.getText());
        LocalDate to = parseDate(toDateField.getText());

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends AnalysisTableModel, ? extends Integer> entry) {
                AnalysisRow row = tableModel.getRow(entry.getIdentifier());
                if (row == null) {
                    return false;
                }

                if (!needle.isBlank() && (row.ticker == null || !row.ticker.toLowerCase().contains(needle))) {
                    return false;
                }

                LocalDate rowDate = parseDate(row.createdAt);
                if (rowDate != null && from != null && rowDate.isBefore(from)) {
                    return false;
                }
                if (rowDate != null && to != null && rowDate.isAfter(to)) {
                    return false;
                }
                return true;
            }
        });
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private JLabel label(String text) {
        JLabel out = new JLabel(text);
        out.setFont(uiFont);
        out.setForeground(COLOR_MUTED);
        return out;
    }

    private void styleInput(JTextField field) {
        field.setFont(uiFont);
        field.setBackground(new Color(49, 52, 62));
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(4, 6, 4, 6)
        ));
    }

    private void styleButton(JButton button) {
        button.setFont(uiFont);
        button.setBackground(COLOR_ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    private String simplifyDate(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        try {
            OffsetDateTime ts = OffsetDateTime.parse(value);
            return ts.atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null) {
            return null;
        }
        String raw = text.trim();
        if (raw.isBlank()) {
            return null;
        }

        if (raw.length() >= 10) {
            String datePart = raw.substring(0, 10);
            try {
                return LocalDate.parse(datePart);
            } catch (DateTimeParseException ignored) {
                // fall through
            }
        }

        try {
            return OffsetDateTime.parse(raw).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private void openOnDisk() {
        if (detailPath == null || detailPath.isBlank()) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            setStatus("Open-on-disk unsupported on this platform", COLOR_DANGER);
            return;
        }
        Path path = Path.of(detailPath);
        File target = path.toFile();
        if (!target.exists()) {
            setStatus("Artifact file no longer exists on disk", COLOR_DANGER);
            return;
        }
        try {
            Desktop.getDesktop().open(target.getParentFile() != null ? target.getParentFile() : target);
        } catch (Exception ex) {
            setStatus("Failed to open file path: " + humanize(ex), COLOR_DANGER);
        }
    }

    private String humanize(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return msg;
    }

    private static final class AnalysisTableModel extends AbstractTableModel {
        private static final String[] COLS = {"Date", "Ticker", "Model", "Summary"};
        private List<AnalysisRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AnalysisRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.createdAt == null ? "" : row.createdAt;
                case 1 -> row.ticker;
                case 2 -> row.modelType;
                case 3 -> row.summary;
                default -> "";
            };
        }

        AnalysisRow getRow(int row) {
            if (row < 0 || row >= rows.size()) {
                return null;
            }
            return rows.get(row);
        }

        void setRows(List<AnalysisRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }
    }

    private static final class AnalysisRow {
        private final String id;
        private final String createdAt;
        private final String ticker;
        private final String modelType;
        private final String summary;
        private final String device;

        private AnalysisRow(String id, String createdAt, String ticker, String modelType, String summary, String device) {
            this.id = id;
            this.createdAt = createdAt;
            this.ticker = ticker;
            this.modelType = modelType;
            this.summary = summary;
            this.device = device;
        }
    }

    @FunctionalInterface
    private interface DocRunnable {
        void run();
    }

    private static final class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final DocRunnable runnable;

        private SimpleDocumentListener(DocRunnable runnable) {
            this.runnable = runnable;
        }

        static SimpleDocumentListener of(DocRunnable runnable) {
            return new SimpleDocumentListener(runnable);
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            runnable.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            runnable.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            runnable.run();
        }
    }
}
