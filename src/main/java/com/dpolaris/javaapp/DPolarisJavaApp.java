package com.dpolaris.javaapp;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

public final class DPolarisJavaApp {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String MODE_STABLE = "Stable (XGBoost)";
    private static final String MODE_DEEP = "Deep Learning";
    private static final Color COLOR_BG = new Color(23, 24, 29);
    private static final Color COLOR_CARD = new Color(34, 36, 43);
    private static final Color COLOR_CARD_ALT = new Color(42, 45, 54);
    private static final Color COLOR_BORDER = new Color(74, 78, 90);
    private static final Color COLOR_TEXT = new Color(236, 238, 244);
    private static final Color COLOR_MUTED = new Color(173, 179, 193);
    private static final Color COLOR_ACCENT = new Color(54, 130, 247);
    private static final Color COLOR_SUCCESS = new Color(45, 194, 117);
    private static final Color COLOR_DANGER = new Color(252, 86, 93);
    private static final Color COLOR_WARNING = new Color(243, 176, 57);
    private static final Color COLOR_LOG_BG = new Color(19, 20, 24);
    private static final Color COLOR_INPUT = new Color(49, 52, 62);
    private static final Color COLOR_MENU_ACTIVE = new Color(64, 86, 133);
    private static final String VIEW_AI_MANAGEMENT = "AI_MANAGEMENT";
    private static final String VIEW_DEEP_LEARNING = "DEEP_LEARNING";
    private static final String VIEW_ANALYSIS = "ANALYSIS";
    private static final String VIEW_DASHBOARD = "DASHBOARD";
    private static final String VIEW_TRAINING_RUNS = "TRAINING_RUNS";
    private static final String VIEW_PREDICTION_INSPECTOR = "PREDICTION_INSPECTOR";
    private static final String VIEW_SETTINGS = "SETTINGS";
    private static final Pattern RUN_ID_PATTERN = Pattern.compile(
            "\"?(?:run_id|runId|run-id)\"?\\s*[:=]\\s*\"?([A-Za-z0-9_.:-]+)\"?"
    );
    private static final Pattern WATCHLIST_TICKER_PATTERN = Pattern.compile("^[A-Z0-9.-]{1,10}$");
    private static final String UNIVERSE_NASDAQ = "nasdaq500";
    private static final String UNIVERSE_WATCHLIST = "watchlist";
    private static final String UNIVERSE_COMBINED = "combined";
    private static final List<String> UNIVERSE_LOAD_ORDER = List.of(
            UNIVERSE_NASDAQ,
            UNIVERSE_WATCHLIST,
            UNIVERSE_COMBINED
    );

    private final ApiClient apiClient = new ApiClient("127.0.0.1", 8420);
    private final RunsService runsService = new RunsService(apiClient);
    private final ScanService scanService = new ScanService(apiClient);
    private final AuditLogStore auditLogStore = new AuditLogStore();
    private final SystemControlConfig.ConfigValues initialControlConfig;
    private AppSettingsConfig.SettingsValues appSettings;
    private final Object trainingAuditLock = new Object();
    private final Font uiFont;
    private final Font titleFont;
    private final Font monoFont;

    private final JFrame frame;
    private final JTextField hostField;
    private final JTextField portField;
    private final JLabel connectionLabel;
    private final JButton checkConnectionButton;
    private JPanel startupBannerPanel;
    private JLabel startupBannerLabel;
    private JButton startupBannerStartButton;

    private JTextField systemConfigHostField;
    private JTextField systemConfigPortField;
    private JTextField systemConfigAiPathField;
    private JTextField systemConfigOpsPathField;
    private JButton systemConfigSaveButton;
    private JButton systemConfigReloadButton;
    private JLabel systemConfigStatusLabel;

    private JButton systemBackendStartButton;
    private JButton systemBackendStopButton;
    private JButton systemBackendRestartButton;
    private JButton systemBackendStatusButton;
    private JButton systemBackendResetButton;
    private JLabel systemBackendActionLabel;
    private JTextArea systemBackendStatusArea;

    private JButton systemOpsStartButton;
    private JButton systemOpsStopButton;
    private JButton systemOpsRestartButton;
    private JButton systemOpsStatusButton;
    private JLabel systemOpsActionLabel;
    private JTextArea systemOpsStatusArea;

    private JButton systemCopyDiagnosticsButton;
    private JLabel systemControlProgressLabel;
    private JTextArea systemControlLogArea;

    private JTextField backendPathField;
    private JButton startBackendButton;
    private JButton stopBackendButton;
    private JButton startDaemonButton;
    private JButton stopDaemonButton;
    private JLabel backendStatusValue;
    private JLabel daemonStatusValue;
    private JTextArea backendLogArea;

    private JTextField symbolField;
    private JComboBox<String> modeCombo;
    private JComboBox<String> deepModelCombo;
    private JSpinner epochsSpinner;
    private JButton trainButton;
    private JButton stopButton;
    private JLabel trainingStatusValue;
    private JLabel trainingGuardrailSummaryLabel;
    private JLabel trainingGuardrailLeakageLabel;
    private JLabel trainingGuardrailCostsLabel;
    private JLabel trainingGuardrailWalkForwardLabel;
    private JLabel trainingGuardrailQualityLabel;

    private JTextArea trainingLogArea;
    private JTextArea dlResultsArea;
    private JTextArea dataArea;
    private JTextArea memoryDistributionArea;
    private JLabel dataStatusValue;
    private JTextField dashboardSymbolField;
    private JSpinner dashboardHorizonSpinner;
    private JButton refreshDashboardButton;
    private JLabel dashboardBiasValue;
    private JLabel dashboardConfidenceValue;
    private JLabel dashboardEntryValue;
    private JLabel dashboardStopValue;
    private JLabel dashboardTargetsValue;
    private JLabel dashboardSizingValue;
    private JLabel dashboardModelValue;
    private JLabel dashboardUpdatedLabel;
    private JTextArea dashboardPlanArea;
    private JTextArea dashboardPredictionArea;
    private JTextArea dashboardInsightsArea;
    private JButton navAiManagementButton;
    private JButton navDeepLearningButton;
    private JButton navAnalysisButton;
    private JButton navDashboardButton;
    private JButton navTrainingRunsButton;
    private JButton navPredictionInspectorButton;
    private JButton navSettingsButton;
    private JPanel contentPanel;
    private CardLayout contentLayout;
    private AnalysisWorkspacePanel analysisWorkspacePanel;

    private JTextField settingsBackendHostField;
    private JTextField settingsBackendPortField;
    private JComboBox<String> settingsLlmProviderCombo;
    private JPasswordField settingsAnthropicKeyField;
    private JPasswordField settingsOpenAiKeyField;
    private JPasswordField settingsNewsApiKeyField;
    private JPasswordField settingsSlackWebhookField;
    private JTextField settingsAiRepoPathField;
    private JTextField settingsOpsRepoPathField;
    private JTextField settingsDataDirField;
    private JButton settingsSaveButton;
    private JButton settingsResetDefaultsButton;
    private JButton settingsTestConnectionButton;
    private JButton settingsCopyExportButton;
    private JLabel settingsStatusLabel;
    private JTextArea settingsLogArea;

    private JTextField inspectorTickerField;
    private JTextField inspectorTimestampField;
    private JSpinner inspectorHorizonSpinner;
    private JTextField inspectorRunIdField;
    private JButton inspectorFetchButton;
    private JLabel inspectorStatusLabel;
    private JLabel inspectorRegimeValue;
    private JLabel inspectorDecisionValue;
    private JLabel inspectorConfidenceValue;
    private JTextArea inspectorInputArea;
    private JTextArea inspectorFeaturesArea;
    private JTextArea inspectorDecisionArea;
    private JTextArea inspectorWarningsArea;
    private JTextArea inspectorRawTraceArea;

    private JTextField runsTickerFilterField;
    private JTextField runsModelFilterField;
    private JTextField runsHorizonFilterField;
    private JTextField runsDateFromField;
    private JTextField runsDateToField;
    private JComboBox<String> runsStatusFilterCombo;
    private JButton runsRefreshButton;
    private JButton runsClearFiltersButton;
    private JButton runsCompareButton;
    private JLabel runsTableStatusLabel;
    private JTable runsTable;
    private RunsTableModel runsTableModel;
    private TableRowSorter<RunsTableModel> runsTableSorter;
    private JLabel runDetailsHeaderLabel;
    private JTextArea runOverviewArea;
    private JTextArea runDataArea;
    private JTable runDataTickerTable;
    private DataTickerStatsTableModel runDataTickerTableModel;
    private JTextArea runDataRawJsonArea;
    private JButton runDataPrettyPrintButton;
    private JButton runDataExportButton;
    private JLabel runDataManifestStatusLabel;
    private boolean runDataPrettyEnabled = true;
    private Object runDataRawPayload = new LinkedHashMap<String, Object>();
    private Object runDataSummaryPayload = new LinkedHashMap<String, Object>();
    private Object runDataHashesPayload = new LinkedHashMap<String, Object>();
    private JTextArea runFeaturesSummaryArea;
    private JTable runFeaturesTable;
    private FeaturesTableModel runFeaturesTableModel;
    private TableRowSorter<FeaturesTableModel> runFeaturesTableSorter;
    private JTextField runFeaturesSearchField;
    private JComboBox<String> runFeaturesCategoryFilterCombo;
    private JComboBox<String> runFeaturesKeptFilterCombo;
    private JButton runFeaturesClearFiltersButton;
    private JButton runFeaturesDetailsButton;
    private JLabel runFeaturesStatusLabel;
    private JTable runTopFeaturesTable;
    private TopFeaturesTableModel runTopFeaturesTableModel;
    private Object runFeaturesRawPayload = new LinkedHashMap<String, Object>();
    private JTextArea runSplitsArea;
    private JTextArea runModelArea;
    private JTextArea runModelHyperparamsArea;
    private JTextArea runModelCurvesArea;
    private JLabel runModelFamilyValue;
    private JLabel runModelCalibrationMethodValue;
    private JLabel runModelCalibrationMetricsValue;
    private JLabel runModelConfidenceTypeValue;
    private JLabel runModelThresholdLogicValue;
    private JLabel runModelStatusLabel;
    private JTable runFailureTable;
    private FailureTableModel runFailureTableModel;
    private JButton runFailureDetailsButton;
    private Object runModelRawPayload = new LinkedHashMap<String, Object>();
    private Object runFailuresRawPayload = new LinkedHashMap<String, Object>();
    private JTextArea runCalibrationArea;
    private JTextArea runMetricsArea;
    private JTextArea runBacktestArea;
    private JTextArea runBacktestAssumptionsArea;
    private JLabel runBacktestStatusLabel;
    private JLabel runBacktestCagrValue;
    private JLabel runBacktestSharpeValue;
    private JLabel runBacktestSortinoValue;
    private JLabel runBacktestMddValue;
    private JLabel runBacktestProfitFactorValue;
    private JLabel runBacktestWinRateValue;
    private JLabel runBacktestAvgWinLossValue;
    private JLabel runBacktestExposureValue;
    private JLabel runBacktestTurnoverValue;
    private LineChartPanel runBacktestEquityChart;
    private LineChartPanel runBacktestDrawdownChart;
    private JTable runTradeLogTable;
    private TradeLogTableModel runTradeLogTableModel;
    private TableRowSorter<TradeLogTableModel> runTradeLogTableSorter;
    private JTextField runTradeLogSearchField;
    private JButton runTradeLogExportButton;
    private JTable runAttributionRegimeTable;
    private JTable runAttributionConfidenceTable;
    private JTable runAttributionTickerTable;
    private AttributionTableModel runAttributionRegimeTableModel;
    private AttributionTableModel runAttributionConfidenceTableModel;
    private AttributionTableModel runAttributionTickerTableModel;
    private Object runBacktestRawPayload = new LinkedHashMap<String, Object>();
    private JTextArea runDiagnosticsArea;
    private JLabel runReadinessBannerLabel;
    private JLabel runReproScoreLabel;
    private JLabel runReadinessStatusLabel;
    private JLabel runReadinessActionStatusLabel;
    private JTextArea runReadinessChecklistArea;
    private JTextArea runReadinessFindingsArea;
    private JButton runPublishModelButton;
    private JButton runUseForTradingButton;
    private JTextArea auditLogViewerArea;
    private JButton auditLogRefreshButton;
    private JButton auditLogExportButton;
    private JLabel auditLogStatusLabel;

    private JTabbedPane universeMainTabs;
    private JTabbedPane universeTabs;
    private JTextField universeSearchField;
    private JComboBox<String> universeSectorFilterCombo;
    private JSpinner universeLiquidityFilterSpinner;
    private JSpinner universeMentionFilterSpinner;
    private JTextField universeAddStockField;
    private JButton universeAddStockButton;
    private JButton universeRemoveStockButton;
    private JButton universeRefreshButton;
    private JButton universeRefreshNowButton;
    private JButton universeSelectAllButton;
    private JButton universeClearSelectionButton;
    private JButton universeExportCsvButton;
    private JButton universeExportJsonButton;
    private JButton universeRunScanButton;
    private JLabel universeStatusLabel;
    private JLabel universeMetaLabel;
    private JLabel universeHashLabel;
    private UniverseTableModel universeNasdaqTableModel;
    private UniverseTableModel universeWatchlistTableModel;
    private UniverseTableModel universeCombinedTableModel;
    private JTable universeNasdaqTable;
    private JTable universeWatchlistTable;
    private JTable universeCombinedTable;
    private TableRowSorter<UniverseTableModel> universeNasdaqSorter;
    private TableRowSorter<UniverseTableModel> universeWatchlistSorter;
    private TableRowSorter<UniverseTableModel> universeCombinedSorter;
    private Map<String, Object> universeNasdaqPayload = new LinkedHashMap<>();
    private Map<String, Object> universeWatchlistPayload = new LinkedHashMap<>();
    private Map<String, Object> universeCombinedPayload = new LinkedHashMap<>();
    private final Object universeAnalysisDateCacheLock = new Object();
    private Map<String, String> universeAnalysisDateCache = new LinkedHashMap<>();
    private long universeAnalysisDateCacheLoadedAtMs = 0L;

    private JTextField scanRunIdField;
    private JButton scanLoadResultsButton;
    private JButton scanRefreshStatusButton;
    private JButton scanPrevPageButton;
    private JButton scanNextPageButton;
    private JSpinner scanPageSpinner;
    private JComboBox<Integer> scanPageSizeCombo;
    private JLabel scanResultsStatusLabel;
    private JProgressBar scanProgressBar;
    private JLabel scanProgressLabel;
    private JLabel scanCurrentTickerLabel;
    private JTable scanResultsTable;
    private ScanResultsTableModel scanResultsTableModel;
    private TableRowSorter<ScanResultsTableModel> scanResultsTableSorter;
    private JTextArea scanWarningsArea;
    private JTextArea scanExecutiveSummaryArea;
    private JTextArea scanMarketContextArea;
    private JTextArea scanPriceVolumeArea;
    private JTextArea scanOptionsArea;
    private JTextArea scanRiskArea;
    private JTextArea scanExplainabilityArea;
    private JTextArea scanArtifactsArea;
    private JButton scanDetailsRetryButton;
    private String activeScanRunId;
    private String expandedScanTicker;
    private long scanResultsTotal = 0L;
    private final Map<String, Map<String, Object>> scanDetailCache = new LinkedHashMap<>();
    private volatile SwingWorker<Void, Map<String, Object>> activeScanStatusWorker;

    private JButton scanRunsRefreshButton;
    private JButton scanRunsOpenButton;
    private JLabel scanRunsStatusLabel;
    private JTable scanRunsTable;
    private ScanRunsTableModel scanRunsTableModel;

    private volatile SwingWorker<Void, String> activeTrainingWorker;
    private volatile Process backendProcess;
    private volatile boolean backendStarting = false;
    private volatile boolean backendExternalConnected = false;
    private volatile boolean backendPortConflictLogged = false;
    private volatile int deliveredLogCount = 0;
    private volatile String loadedRunDetailsId;
    private volatile GuardrailEngine.GuardrailReport currentGuardrailReport;
    private volatile String currentGuardrailRunId;
    private volatile Map<String, Object> activeTrainingAuditConfig = new LinkedHashMap<>();
    private volatile String activeTrainingAuditStartedAt;
    private volatile String activeTrainingJobId;
    private volatile boolean activeTrainingAuditFinalized;
    private volatile String lastScanWarningSignature = "";
    private volatile String lastTickerStatusSignature = "";
    private volatile String lastTickerFailureSignature = "";
    private volatile boolean systemControlActionInFlight = false;
    private volatile String lastBackendHealth = "unknown";
    private volatile String lastBackendRestart = "—";
    private volatile Map<String, Object> lastBackendControlStatus = new LinkedHashMap<>();
    private volatile Map<String, Object> lastOpsControlStatus = new LinkedHashMap<>();
    private volatile javax.swing.Timer systemStatusTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            installLookAndFeel();
            new DPolarisJavaApp().show();
        });
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default.
        }
    }

    public DPolarisJavaApp() {
        uiFont = resolveUiFont(Font.PLAIN, 13);
        titleFont = resolveUiFont(Font.BOLD, 22);
        monoFont = resolveMonoFont(13);
        appSettings = AppSettingsConfig.load();
        initialControlConfig = SystemControlConfig.load();
        apiClient.configure(appSettings.backendHost(), appSettings.backendPort());

        frame = new JFrame("dPolaris Java");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLayout(new BorderLayout(8, 8));
        frame.getContentPane().setBackground(COLOR_BG);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(COLOR_CARD);
        topPanel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        hostField = new JTextField(appSettings.backendHost(), 14);
        portField = new JTextField(String.valueOf(appSettings.backendPort()), 6);
        checkConnectionButton = new JButton("Check Connection");
        connectionLabel = new JLabel("Unknown", JLabel.CENTER);

        styleInputField(hostField);
        styleInputField(portField);
        styleButton(checkConnectionButton, true);
        styleStatusLabel(connectionLabel, "Unknown", COLOR_WARNING);

        JLabel titleLabel = new JLabel("dPolaris Control Center");
        titleLabel.setForeground(COLOR_TEXT);
        titleLabel.setFont(titleFont);

        JPanel connectionControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        connectionControls.setOpaque(false);
        JLabel hostLabel = new JLabel("Host");
        JLabel portLabel = new JLabel("Port");
        hostLabel.setForeground(COLOR_MUTED);
        portLabel.setForeground(COLOR_MUTED);
        hostLabel.setFont(uiFont);
        portLabel.setFont(uiFont);
        connectionControls.add(hostLabel);
        connectionControls.add(hostField);
        connectionControls.add(portLabel);
        connectionControls.add(portField);
        connectionControls.add(checkConnectionButton);
        connectionControls.add(connectionLabel);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(connectionControls, BorderLayout.EAST);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setBackground(COLOR_BG);
        topWrap.setBorder(new EmptyBorder(10, 10, 4, 10));
        topWrap.add(topPanel, BorderLayout.NORTH);
        startupBannerPanel = createStartupBannerPanel();
        topWrap.add(startupBannerPanel, BorderLayout.SOUTH);
        frame.add(topWrap, BorderLayout.NORTH);

        JPanel centerWrap = new JPanel(new BorderLayout(10, 0));
        centerWrap.setBackground(COLOR_BG);
        centerWrap.setBorder(new EmptyBorder(6, 10, 10, 10));

        JPanel sideMenu = createSideMenuPanel();
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(COLOR_BG);
        contentPanel.add(createAiManagementPanel(), VIEW_AI_MANAGEMENT);
        contentPanel.add(createDeepLearningPanel(), VIEW_DEEP_LEARNING);
        analysisWorkspacePanel = new AnalysisWorkspacePanel(apiClient, uiFont, monoFont);
        contentPanel.add(analysisWorkspacePanel, VIEW_ANALYSIS);
        contentPanel.add(createDashboardPanel(), VIEW_DASHBOARD);
        contentPanel.add(createTrainingRunsPanel(), VIEW_TRAINING_RUNS);
        contentPanel.add(createPredictionInspectorPanel(), VIEW_PREDICTION_INSPECTOR);
        contentPanel.add(createSettingsPanel(), VIEW_SETTINGS);

        centerWrap.add(sideMenu, BorderLayout.WEST);
        centerWrap.add(contentPanel, BorderLayout.CENTER);
        frame.add(centerWrap, BorderLayout.CENTER);

        checkConnectionButton.addActionListener(e -> checkConnection());
        navAiManagementButton.addActionListener(e -> showView(VIEW_AI_MANAGEMENT));
        navDeepLearningButton.addActionListener(e -> showView(VIEW_DEEP_LEARNING));
        navAnalysisButton.addActionListener(e -> showView(VIEW_ANALYSIS));
        navDashboardButton.addActionListener(e -> showView(VIEW_DASHBOARD));
        navTrainingRunsButton.addActionListener(e -> showView(VIEW_TRAINING_RUNS));
        navPredictionInspectorButton.addActionListener(e -> showView(VIEW_PREDICTION_INSPECTOR));
        navSettingsButton.addActionListener(e -> showView(VIEW_SETTINGS));
        refreshDashboardButton.addActionListener(e -> refreshDashboard());
        inspectorFetchButton.addActionListener(e -> refreshPredictionInspector());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (systemStatusTimer != null) {
                    systemStatusTimer.stop();
                }
            }
        });

        applySettingsToRuntime(appSettings, true);
        showView(VIEW_AI_MANAGEMENT);
    }

    private JPanel createSideMenuPanel() {
        JPanel menu = createCardPanel();
        menu.setPreferredSize(new Dimension(220, 100));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BorderLayout(0, 12));

        JLabel menuTitle = new JLabel("Workspace");
        menuTitle.setForeground(COLOR_MUTED);
        menuTitle.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        menuTitle.setBorder(new EmptyBorder(2, 2, 4, 2));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);

        navAiManagementButton = new JButton("Management");
        styleNavButton(navAiManagementButton);
        buttonPanel.add(navAiManagementButton, gbc);

        gbc.gridy++;
        navDeepLearningButton = new JButton("Deep Learning");
        styleNavButton(navDeepLearningButton);
        buttonPanel.add(navDeepLearningButton, gbc);

        gbc.gridy++;
        navAnalysisButton = new JButton("Analysis");
        styleNavButton(navAnalysisButton);
        buttonPanel.add(navAnalysisButton, gbc);

        gbc.gridy++;
        navDashboardButton = new JButton("Dashboard");
        styleNavButton(navDashboardButton);
        buttonPanel.add(navDashboardButton, gbc);

        gbc.gridy++;
        navTrainingRunsButton = new JButton("Training Runs");
        styleNavButton(navTrainingRunsButton);
        buttonPanel.add(navTrainingRunsButton, gbc);

        gbc.gridy++;
        navPredictionInspectorButton = new JButton("Prediction Inspector");
        styleNavButton(navPredictionInspectorButton);
        buttonPanel.add(navPredictionInspectorButton, gbc);

        gbc.gridy++;
        navSettingsButton = new JButton("Settings");
        styleNavButton(navSettingsButton);
        buttonPanel.add(navSettingsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        buttonPanel.add(new JPanel(), gbc);

        content.add(menuTitle, BorderLayout.NORTH);
        content.add(buttonPanel, BorderLayout.CENTER);
        menu.add(content, BorderLayout.CENTER);
        return menu;
    }

    private JPanel createAiManagementPanel() {
        JTabbedPane tabs = new JTabbedPane();
        styleTabbedPane(tabs);
        tabs.addTab("System Control", createSystemControlPanel());
        tabs.addTab("Backend Data", createDataPanel());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(COLOR_BG);
        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel createSettingsPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        settingsBackendHostField = new JTextField(16);
        settingsBackendPortField = new JTextField(8);
        settingsLlmProviderCombo = new JComboBox<>(new String[]{"none", "anthropic", "openai"});
        settingsAnthropicKeyField = new JPasswordField(26);
        settingsOpenAiKeyField = new JPasswordField(26);
        settingsNewsApiKeyField = new JPasswordField(26);
        settingsSlackWebhookField = new JPasswordField(40);
        settingsAiRepoPathField = new JTextField(36);
        settingsOpsRepoPathField = new JTextField(36);
        settingsDataDirField = new JTextField(36);
        settingsSaveButton = new JButton("Save");
        settingsResetDefaultsButton = new JButton("Reset to Defaults");
        settingsTestConnectionButton = new JButton("Test Connection");
        settingsCopyExportButton = new JButton("Copy Export Commands");
        settingsStatusLabel = new JLabel();

        styleInputField(settingsBackendHostField);
        styleInputField(settingsBackendPortField);
        styleCombo(settingsLlmProviderCombo);
        styleInputField(settingsAnthropicKeyField);
        styleInputField(settingsOpenAiKeyField);
        styleInputField(settingsNewsApiKeyField);
        styleInputField(settingsSlackWebhookField);
        styleInputField(settingsAiRepoPathField);
        styleInputField(settingsOpsRepoPathField);
        styleInputField(settingsDataDirField);
        styleButton(settingsSaveButton, true);
        styleButton(settingsResetDefaultsButton, false);
        styleButton(settingsTestConnectionButton, false);
        styleButton(settingsCopyExportButton, false);
        styleInlineStatus(settingsStatusLabel, "Stored locally in " + AppSettingsConfig.settingsPath(), COLOR_MUTED);

        JCheckBox showAnthropic = new JCheckBox("Show");
        JCheckBox showOpenAi = new JCheckBox("Show");
        JCheckBox showNews = new JCheckBox("Show");
        JCheckBox showSlack = new JCheckBox("Show");
        configureSecretToggle(settingsAnthropicKeyField, showAnthropic);
        configureSecretToggle(settingsOpenAiKeyField, showOpenAi);
        configureSecretToggle(settingsNewsApiKeyField, showNews);
        configureSecretToggle(settingsSlackWebhookField, showSlack);

        JPanel backendCard = createCardPanel();
        backendCard.add(createSectionHeader("Backend"), BorderLayout.NORTH);
        JPanel backendForm = new JPanel(new GridBagLayout());
        backendForm.setOpaque(false);
        GridBagConstraints b = settingsConstraints();
        addSettingsRow(backendForm, b, "Backend Host", settingsBackendHostField);
        addSettingsRow(backendForm, b, "Backend Port", settingsBackendPortField);
        addSettingsRow(backendForm, b, "LLM Provider", settingsLlmProviderCombo);
        backendCard.add(backendForm, BorderLayout.CENTER);

        JPanel keysCard = createCardPanel();
        keysCard.add(createSectionHeader("API Keys / Tokens"), BorderLayout.NORTH);
        JPanel keysForm = new JPanel(new GridBagLayout());
        keysForm.setOpaque(false);
        GridBagConstraints k = settingsConstraints();
        addSettingsRow(keysForm, k, "Anthropic API Key", secretRow(settingsAnthropicKeyField, showAnthropic));
        addSettingsRow(keysForm, k, "OpenAI API Key", secretRow(settingsOpenAiKeyField, showOpenAi));
        addSettingsRow(keysForm, k, "News API Key", secretRow(settingsNewsApiKeyField, showNews));
        JLabel localOnlyLabel = new JLabel("Stored locally only (masked in UI by default).");
        localOnlyLabel.setForeground(COLOR_MUTED);
        localOnlyLabel.setFont(uiFont.deriveFont(Font.PLAIN, 12f));
        addSettingsRow(keysForm, k, "Note", localOnlyLabel);
        keysCard.add(keysForm, BorderLayout.CENTER);

        JPanel slackCard = createCardPanel();
        slackCard.add(createSectionHeader("Slack"), BorderLayout.NORTH);
        JPanel slackForm = new JPanel(new GridBagLayout());
        slackForm.setOpaque(false);
        GridBagConstraints s = settingsConstraints();
        addSettingsRow(slackForm, s, "Slack Webhook URL", secretRow(settingsSlackWebhookField, showSlack));
        slackCard.add(slackForm, BorderLayout.CENTER);

        JPanel pathsCard = createCardPanel();
        pathsCard.add(createSectionHeader("Paths"), BorderLayout.NORTH);
        JPanel pathsForm = new JPanel(new GridBagLayout());
        pathsForm.setOpaque(false);
        GridBagConstraints p = settingsConstraints();
        addSettingsRow(pathsForm, p, "dpolaris_ai Repo Path", settingsAiRepoPathField);
        addSettingsRow(pathsForm, p, "dPolaris_ops Repo Path", settingsOpsRepoPathField);
        addSettingsRow(pathsForm, p, "dpolaris_data Directory", settingsDataDirField);
        pathsCard.add(pathsForm, BorderLayout.CENTER);

        JPanel actionsCard = createCardPanel();
        actionsCard.add(createSectionHeader("Actions"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(settingsSaveButton);
        actions.add(settingsResetDefaultsButton);
        actions.add(settingsTestConnectionButton);
        actions.add(settingsCopyExportButton);
        actions.add(settingsStatusLabel);
        actionsCard.add(actions, BorderLayout.CENTER);

        JPanel formStack = new JPanel();
        formStack.setOpaque(false);
        formStack.setLayout(new BoxLayout(formStack, BoxLayout.Y_AXIS));
        formStack.add(backendCard);
        formStack.add(Box.createVerticalStrut(8));
        formStack.add(keysCard);
        formStack.add(Box.createVerticalStrut(8));
        formStack.add(slackCard);
        formStack.add(Box.createVerticalStrut(8));
        formStack.add(pathsCard);
        formStack.add(Box.createVerticalStrut(8));
        formStack.add(actionsCard);
        formStack.add(Box.createVerticalGlue());

        JScrollPane formScroll = new JScrollPane(formStack);
        formScroll.getViewport().setBackground(COLOR_BG);
        formScroll.setBackground(COLOR_BG);
        formScroll.setBorder(new LineBorder(COLOR_BORDER, 1, true));

        settingsLogArea = createLogArea();
        settingsLogArea.setRows(8);
        JScrollPane logScroll = createLogScrollPane(settingsLogArea, "Settings Log");
        logScroll.setPreferredSize(new Dimension(400, 190));

        settingsSaveButton.addActionListener(e -> saveSettingsFromWorkspace());
        settingsResetDefaultsButton.addActionListener(e -> resetSettingsToDefaults());
        settingsTestConnectionButton.addActionListener(e -> testSettingsConnection());
        settingsCopyExportButton.addActionListener(e -> copySettingsExportCommands());

        root.add(formScroll, BorderLayout.CENTER);
        root.add(logScroll, BorderLayout.SOUTH);
        populateSettingsForm(appSettings);
        return root;
    }

    private GridBagConstraints settingsConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 4, 4, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        return gbc;
    }

    private void addSettingsRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = createFormLabel(labelText);
        GridBagConstraints left = (GridBagConstraints) gbc.clone();
        left.gridx = 0;
        left.weightx = 0.0;
        left.fill = GridBagConstraints.NONE;
        panel.add(label, left);

        GridBagConstraints right = (GridBagConstraints) gbc.clone();
        right.gridx = 1;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, right);

        gbc.gridy++;
    }

    private JPanel secretRow(JComponent field, JCheckBox showToggle) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(field);
        row.add(showToggle);
        return row;
    }

    private void configureSecretToggle(JPasswordField field, JCheckBox toggle) {
        toggle.setOpaque(false);
        toggle.setForeground(COLOR_TEXT);
        char echoChar = field.getEchoChar();
        toggle.addActionListener(e -> field.setEchoChar(toggle.isSelected() ? (char) 0 : echoChar));
    }

    private JPanel createStartupBannerPanel() {
        JPanel banner = new JPanel(new BorderLayout(8, 0));
        banner.setBackground(new Color(50, 38, 18));
        banner.setBorder(new CompoundBorder(
                new LineBorder(COLOR_WARNING, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        startupBannerLabel = new JLabel("Backend is not healthy. Start it with one click.");
        startupBannerLabel.setForeground(new Color(255, 226, 170));
        startupBannerLabel.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        banner.add(startupBannerLabel, BorderLayout.CENTER);

        startupBannerStartButton = new JButton("Start Backend Now");
        styleButton(startupBannerStartButton, true);
        startupBannerStartButton.addActionListener(e -> startBackendProcess());
        banner.add(startupBannerStartButton, BorderLayout.EAST);

        banner.setVisible(false);
        return banner;
    }

    private JPanel createSystemControlPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));

        JPanel configCard = createCardPanel();
        configCard.add(createSectionHeader("Paths & Ports"), BorderLayout.NORTH);
        systemConfigHostField = new JTextField(appSettings.backendHost(), 14);
        systemConfigPortField = new JTextField(String.valueOf(appSettings.backendPort()), 6);
        systemConfigAiPathField = new JTextField(appSettings.aiRepoPath(), 40);
        systemConfigOpsPathField = new JTextField(appSettings.opsRepoPath(), 40);
        systemConfigSaveButton = new JButton("Save Config");
        systemConfigReloadButton = new JButton("Reload Saved");
        systemConfigStatusLabel = new JLabel();
        styleInputField(systemConfigHostField);
        styleInputField(systemConfigPortField);
        styleInputField(systemConfigAiPathField);
        styleInputField(systemConfigOpsPathField);
        styleButton(systemConfigSaveButton, true);
        styleButton(systemConfigReloadButton, false);
        styleInlineStatus(systemConfigStatusLabel, "Config: loaded", COLOR_MUTED);

        JPanel configRowOne = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        configRowOne.setOpaque(false);
        configRowOne.add(createFormLabel("Backend Host"));
        configRowOne.add(systemConfigHostField);
        configRowOne.add(createFormLabel("Port"));
        configRowOne.add(systemConfigPortField);

        JPanel configRowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        configRowTwo.setOpaque(false);
        configRowTwo.add(createFormLabel("AI Repo"));
        configRowTwo.add(systemConfigAiPathField);

        JPanel configRowThree = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        configRowThree.setOpaque(false);
        configRowThree.add(createFormLabel("Ops Repo"));
        configRowThree.add(systemConfigOpsPathField);

        JPanel configActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        configActions.setOpaque(false);
        configActions.add(systemConfigSaveButton);
        configActions.add(systemConfigReloadButton);
        configActions.add(systemConfigStatusLabel);

        JPanel configBody = new JPanel();
        configBody.setOpaque(false);
        configBody.setLayout(new BoxLayout(configBody, BoxLayout.Y_AXIS));
        configBody.add(configRowOne);
        configBody.add(Box.createVerticalStrut(6));
        configBody.add(configRowTwo);
        configBody.add(Box.createVerticalStrut(6));
        configBody.add(configRowThree);
        configBody.add(Box.createVerticalStrut(6));
        configBody.add(configActions);
        configCard.add(configBody, BorderLayout.CENTER);

        JPanel backendCard = createCardPanel();
        backendCard.add(createSectionHeader("Backend Lifecycle"), BorderLayout.NORTH);
        systemBackendStartButton = new JButton("Start");
        systemBackendStopButton = new JButton("Stop");
        systemBackendRestartButton = new JButton("Restart");
        systemBackendStatusButton = new JButton("Status");
        systemBackendResetButton = new JButton("Reset&Restart (clean)");
        systemBackendActionLabel = new JLabel();
        styleButton(systemBackendStartButton, true);
        styleButton(systemBackendStopButton, false);
        styleButton(systemBackendRestartButton, false);
        styleButton(systemBackendStatusButton, false);
        styleButton(systemBackendResetButton, false);
        styleInlineStatus(systemBackendActionLabel, "Backend action: idle", COLOR_MUTED);

        JPanel backendActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        backendActions.setOpaque(false);
        backendActions.add(systemBackendStartButton);
        backendActions.add(systemBackendStopButton);
        backendActions.add(systemBackendRestartButton);
        backendActions.add(systemBackendStatusButton);
        backendActions.add(systemBackendResetButton);
        backendActions.add(systemBackendActionLabel);

        systemBackendStatusArea = createLogArea();
        systemBackendStatusArea.setRows(7);
        JScrollPane backendStatusScroll = createLogScrollPane(systemBackendStatusArea, "Backend Status");

        JPanel backendBody = new JPanel(new BorderLayout(0, 8));
        backendBody.setOpaque(false);
        backendBody.add(backendActions, BorderLayout.NORTH);
        backendBody.add(backendStatusScroll, BorderLayout.CENTER);
        backendCard.add(backendBody, BorderLayout.CENTER);

        JPanel opsCard = createCardPanel();
        opsCard.add(createSectionHeader("Orchestrator Lifecycle"), BorderLayout.NORTH);
        systemOpsStartButton = new JButton("Start");
        systemOpsStopButton = new JButton("Stop");
        systemOpsRestartButton = new JButton("Restart");
        systemOpsStatusButton = new JButton("Status");
        systemOpsActionLabel = new JLabel();
        styleButton(systemOpsStartButton, true);
        styleButton(systemOpsStopButton, false);
        styleButton(systemOpsRestartButton, false);
        styleButton(systemOpsStatusButton, false);
        styleInlineStatus(systemOpsActionLabel, "Orchestrator action: idle", COLOR_MUTED);

        JPanel opsActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        opsActions.setOpaque(false);
        opsActions.add(systemOpsStartButton);
        opsActions.add(systemOpsStopButton);
        opsActions.add(systemOpsRestartButton);
        opsActions.add(systemOpsStatusButton);
        opsActions.add(systemOpsActionLabel);

        systemOpsStatusArea = createLogArea();
        systemOpsStatusArea.setRows(6);
        JScrollPane opsStatusScroll = createLogScrollPane(systemOpsStatusArea, "Orchestrator Status");

        JPanel opsBody = new JPanel(new BorderLayout(0, 8));
        opsBody.setOpaque(false);
        opsBody.add(opsActions, BorderLayout.NORTH);
        opsBody.add(opsStatusScroll, BorderLayout.CENTER);
        opsCard.add(opsBody, BorderLayout.CENTER);

        JPanel diagnosticsCard = createCardPanel();
        diagnosticsCard.add(createSectionHeader("Diagnostics"), BorderLayout.NORTH);
        systemCopyDiagnosticsButton = new JButton("Copy Diagnostics");
        systemControlProgressLabel = new JLabel();
        styleButton(systemCopyDiagnosticsButton, false);
        styleInlineStatus(systemControlProgressLabel, "Ready", COLOR_MUTED);
        JPanel diagnosticsBody = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        diagnosticsBody.setOpaque(false);
        diagnosticsBody.add(systemCopyDiagnosticsButton);
        diagnosticsBody.add(systemControlProgressLabel);
        diagnosticsCard.add(diagnosticsBody, BorderLayout.CENTER);

        topStack.add(configCard);
        topStack.add(Box.createVerticalStrut(8));
        topStack.add(backendCard);
        topStack.add(Box.createVerticalStrut(8));
        topStack.add(opsCard);
        topStack.add(Box.createVerticalStrut(8));
        topStack.add(diagnosticsCard);

        systemControlLogArea = createLogArea();
        JScrollPane logs = createLogScrollPane(systemControlLogArea, "System Control Activity");

        root.add(topStack, BorderLayout.NORTH);
        root.add(logs, BorderLayout.CENTER);

        applyConfigToUi(toSystemControlConfig(appSettings));
        renderBackendControlStatus(new LinkedHashMap<>(), "No backend status loaded yet.");
        renderOrchestratorStatus(new LinkedHashMap<>(), "No orchestrator status loaded yet.");

        systemConfigSaveButton.addActionListener(e -> saveSystemControlConfig());
        systemConfigReloadButton.addActionListener(e -> reloadSystemControlConfig());
        systemBackendStartButton.addActionListener(e -> startBackendProcess());
        systemBackendStopButton.addActionListener(e -> stopBackendProcess());
        systemBackendRestartButton.addActionListener(e -> { stopBackendProcess(); startBackendProcess(); });
        systemBackendStatusButton.addActionListener(e -> runBackendControlAction("status", false));
        systemBackendResetButton.addActionListener(e -> runBackendControlAction("reset", false));
        systemOpsStartButton.addActionListener(e -> runOrchestratorControlAction("start"));
        systemOpsStopButton.addActionListener(e -> runOrchestratorControlAction("stop"));
        systemOpsRestartButton.addActionListener(e -> runOrchestratorControlAction("restart"));
        systemOpsStatusButton.addActionListener(e -> runOrchestratorControlAction("status"));
        systemCopyDiagnosticsButton.addActionListener(e -> copySystemDiagnostics());

        return root;
    }

    private JPanel createDeepLearningPanel() {
        // Create stock selection panel with tabs
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);

        universeSearchField = new JTextField(14);
        universeSectorFilterCombo = new JComboBox<>(new String[]{"All"});
        universeLiquidityFilterSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1_000_000_000_000.0, 100_000.0));
        universeMentionFilterSpinner = new JSpinner(new SpinnerNumberModel(0L, 0L, 10_000_000L, 1L));
        universeAddStockField = new JTextField(8);
        universeAddStockButton = new JButton("Add Ticker");
        universeRefreshButton = new JButton("Refresh");
        universeSelectAllButton = new JButton("Select All");
        universeClearSelectionButton = new JButton("Clear");
        universeRunScanButton = new JButton("Run Deep Learning");
        universeStatusLabel = new JLabel();
        universeMetaLabel = new JLabel();
        universeHashLabel = new JLabel();

        styleInputField(universeSearchField);
        styleCombo(universeSectorFilterCombo);
        styleInputField(universeAddStockField);
        styleSpinner(universeLiquidityFilterSpinner);
        styleSpinner(universeMentionFilterSpinner);
        styleButton(universeAddStockButton, false);
        styleButton(universeRefreshButton, false);
        styleButton(universeSelectAllButton, false);
        styleButton(universeClearSelectionButton, false);
        styleButton(universeRunScanButton, true);
        universeRunScanButton.setToolTipText("Run deep learning on selected tickers");
        styleInlineStatus(universeStatusLabel, "Select tickers and click Run Deep Learning", COLOR_MUTED);
        styleInlineStatus(universeMetaLabel, "", COLOR_MUTED);
        styleInlineStatus(universeHashLabel, "", COLOR_MUTED);

        universeNasdaqTableModel = new UniverseTableModel();
        universeWatchlistTableModel = new UniverseTableModel();
        universeCombinedTableModel = new UniverseTableModel();
        universeNasdaqTableModel.addTableModelListener(e -> updateUniverseRunButtonState());
        universeWatchlistTableModel.addTableModelListener(e -> updateUniverseRunButtonState());
        universeCombinedTableModel.addTableModelListener(e -> updateUniverseRunButtonState());

        universeNasdaqTable = new JTable(universeNasdaqTableModel);
        universeWatchlistTable = new JTable(universeWatchlistTableModel);
        universeCombinedTable = new JTable(universeCombinedTableModel);
        styleRunsTable(universeNasdaqTable);
        styleRunsTable(universeWatchlistTable);
        styleRunsTable(universeCombinedTable);
        universeNasdaqTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        universeWatchlistTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        universeCombinedTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        universeNasdaqSorter = new TableRowSorter<>(universeNasdaqTableModel);
        universeWatchlistSorter = new TableRowSorter<>(universeWatchlistTableModel);
        universeCombinedSorter = new TableRowSorter<>(universeCombinedTableModel);
        configureUniverseSorter(universeNasdaqSorter);
        configureUniverseSorter(universeWatchlistSorter);
        configureUniverseSorter(universeCombinedSorter);
        universeNasdaqTable.setRowSorter(universeNasdaqSorter);
        universeWatchlistTable.setRowSorter(universeWatchlistSorter);
        universeCombinedTable.setRowSorter(universeCombinedSorter);
        attachUniverseTableAnalysisAction(universeNasdaqTable, universeNasdaqTableModel);
        attachUniverseTableAnalysisAction(universeWatchlistTable, universeWatchlistTableModel);
        attachUniverseTableAnalysisAction(universeCombinedTable, universeCombinedTableModel);
        runUniverseSortSanityCheck();

        universeTabs = new JTabbedPane();
        styleTabbedPane(universeTabs);
        universeTabs.addTab("NASDAQ 500", createUniverseTablePane(universeNasdaqTable, "NASDAQ 500 Stocks"));
        universeTabs.addTab("Watchlist", createUniverseTablePane(universeWatchlistTable, "Watchlist"));
        universeTabs.addTab("Combined", createUniverseTablePane(universeCombinedTable, "Combined Universe"));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(createFormLabel("Search"));
        filters.add(universeSearchField);
        filters.add(createFormLabel("Sector"));
        filters.add(universeSectorFilterCombo);
        filters.add(createFormLabel("Min Market Cap"));
        filters.add(universeLiquidityFilterSpinner);
        filters.add(universeAddStockButton);
        universeRemoveStockButton = new JButton("Remove Selected");
        styleButton(universeRemoveStockButton, false);
        filters.add(universeRemoveStockButton);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsRow.setOpaque(false);
        actionsRow.add(universeRunScanButton);
        actionsRow.add(universeSelectAllButton);
        actionsRow.add(universeClearSelectionButton);
        actionsRow.add(universeRefreshButton);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        metaRow.setOpaque(false);
        metaRow.add(universeStatusLabel);
        metaRow.add(universeMetaLabel);
        metaRow.add(universeHashLabel);

        JPanel top = createCardPanel();
        top.add(createSectionHeader("Deep Learning - Stock Selection"), BorderLayout.NORTH);
        JPanel topBody = new JPanel();
        topBody.setLayout(new BoxLayout(topBody, BoxLayout.Y_AXIS));
        topBody.setOpaque(false);
        topBody.add(filters);
        topBody.add(Box.createVerticalStrut(6));
        topBody.add(actionsRow);
        topBody.add(Box.createVerticalStrut(6));
        topBody.add(metaRow);
        top.add(topBody, BorderLayout.CENTER);

        universeTabs.addChangeListener(e -> {
            updateUniverseMetaDisplay();
            updateUniverseRunButtonState();
            rebuildActiveUniverseSectorOptions();
            applyUniverseFilters();
            ensureDeepLearningTickersLoaded(false);
        });
        universeRefreshButton.addActionListener(e -> ensureDeepLearningTickersLoaded(true));
        universeSelectAllButton.addActionListener(e -> setUniverseSelection(true));
        universeClearSelectionButton.addActionListener(e -> setUniverseSelection(false));
        universeRunScanButton.addActionListener(e -> runDeepLearningOnSelected());
        universeAddStockButton.addActionListener(e -> addWatchlistStockFromUi());
        if (universeRemoveStockButton != null) {
            universeRemoveStockButton.addActionListener(e -> removeSelectedWatchlistStocksFromUi());
        }

        universeSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyUniverseFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyUniverseFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyUniverseFilters(); }
        });
        universeSectorFilterCombo.addActionListener(e -> applyUniverseFilters());
        universeLiquidityFilterSpinner.addChangeListener(e -> applyUniverseFilters());

        // Deep learning results area
        dlResultsArea = createLogArea();
        JScrollPane resultsScroll = createLogScrollPane(dlResultsArea, "Deep Learning Results");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, universeTabs, resultsScroll);
        split.setResizeWeight(0.6);
        split.setDividerLocation(400);
        split.setBorder(null);
        split.setOpaque(false);

        root.add(top, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel createUniverseScanPanel() {
        // Legacy - redirect to deep learning panel
        return createDeepLearningPanel();
    }

    private JScrollPane createUniverseTablePane(JTable table, String title) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(COLOR_LOG_BG);
        scroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        scroll.setColumnHeaderView(createSectionHeader(title));
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));
            table.getColumnModel().getColumn(0).setCellEditor(table.getDefaultEditor(Boolean.class));
            table.getColumnModel().getColumn(0).setPreferredWidth(62);
            table.getColumnModel().getColumn(0).setMaxWidth(72);
        }
        return scroll;
    }

    private JPanel createScanResultsPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);

        scanRunIdField = new JTextField(18);
        scanLoadResultsButton = new JButton("Load Results");
        scanRefreshStatusButton = new JButton("Refresh Status");
        scanPrevPageButton = new JButton("Prev");
        scanNextPageButton = new JButton("Next");
        scanPageSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        scanPageSizeCombo = new JComboBox<>(new Integer[]{50, 100, 200, 500});
        scanResultsStatusLabel = new JLabel();
        scanProgressBar = new JProgressBar(0, 100);
        scanProgressLabel = new JLabel();
        scanCurrentTickerLabel = new JLabel();
        scanWarningsArea = createLogArea();
        scanWarningsArea.setLineWrap(true);
        scanWarningsArea.setWrapStyleWord(true);

        styleInputField(scanRunIdField);
        styleButton(scanLoadResultsButton, true);
        styleButton(scanRefreshStatusButton, false);
        styleButton(scanPrevPageButton, false);
        styleButton(scanNextPageButton, false);
        styleSpinner(scanPageSpinner);
        styleCombo(scanPageSizeCombo);
        styleInlineStatus(scanResultsStatusLabel, "Scan results: idle", COLOR_MUTED);
        styleInlineStatus(scanProgressLabel, "Progress: —", COLOR_MUTED);
        styleInlineStatus(scanCurrentTickerLabel, "Ticker: —", COLOR_MUTED);
        scanProgressBar.setStringPainted(true);
        scanProgressBar.setValue(0);
        scanProgressBar.setForeground(COLOR_ACCENT);
        scanProgressBar.setBackground(COLOR_INPUT);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(createFormLabel("Run ID"));
        controls.add(scanRunIdField);
        controls.add(createFormLabel("Page"));
        controls.add(scanPageSpinner);
        controls.add(createFormLabel("Page Size"));
        controls.add(scanPageSizeCombo);
        controls.add(scanPrevPageButton);
        controls.add(scanNextPageButton);
        controls.add(scanLoadResultsButton);
        controls.add(scanRefreshStatusButton);
        controls.add(scanResultsStatusLabel);

        JPanel progressRow = new JPanel(new BorderLayout(8, 0));
        progressRow.setOpaque(false);
        progressRow.add(scanProgressBar, BorderLayout.CENTER);
        JPanel progressMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        progressMeta.setOpaque(false);
        progressMeta.add(scanProgressLabel);
        progressMeta.add(scanCurrentTickerLabel);
        progressRow.add(progressMeta, BorderLayout.EAST);

        JPanel top = createCardPanel();
        top.add(createSectionHeader("Deep Learning Scan"), BorderLayout.NORTH);
        JPanel topBody = new JPanel(new BorderLayout(0, 8));
        topBody.setOpaque(false);
        topBody.add(controls, BorderLayout.NORTH);
        topBody.add(progressRow, BorderLayout.CENTER);
        top.add(topBody, BorderLayout.CENTER);

        scanResultsTableModel = new ScanResultsTableModel();
        scanResultsTable = new JTable(scanResultsTableModel);
        styleRunsTable(scanResultsTable);
        scanResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scanResultsTableSorter = new TableRowSorter<>(scanResultsTableModel);
        scanResultsTable.setRowSorter(scanResultsTableSorter);
        scanResultsTableSorter.setComparator(3, (a, b) -> Double.compare(parseSortableDouble(a), parseSortableDouble(b)));
        scanResultsTableSorter.setComparator(8, (a, b) -> Double.compare(parseSortableDouble(a), parseSortableDouble(b)));
        scanResultsTableSorter.setComparator(9, (a, b) -> Integer.compare(parseSortableInt(a), parseSortableInt(b)));
        scanResultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = scanResultsTable.rowAtPoint(e.getPoint());
                int col = scanResultsTable.columnAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                if (e.getClickCount() >= 2 || col == 0) {
                    int modelRow = scanResultsTable.convertRowIndexToModel(row);
                    toggleScanDetails(modelRow);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(scanResultsTable);
        tableScroll.getViewport().setBackground(COLOR_LOG_BG);
        tableScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        tableScroll.setColumnHeaderView(createSectionHeader("Scan Results (Expandable Rows)"));

        scanExecutiveSummaryArea = createRunDetailsArea();
        scanMarketContextArea = createRunDetailsArea();
        scanPriceVolumeArea = createRunDetailsArea();
        scanOptionsArea = createRunDetailsArea();
        scanRiskArea = createRunDetailsArea();
        scanExplainabilityArea = createRunDetailsArea();
        scanArtifactsArea = createRunDetailsArea();
        scanDetailsRetryButton = new JButton("Retry Detail Fetch");
        styleButton(scanDetailsRetryButton, false);
        scanDetailsRetryButton.addActionListener(e -> retrySelectedScanDetail());

        JTabbedPane detailsTabs = new JTabbedPane();
        styleTabbedPane(detailsTabs);
        detailsTabs.addTab("Executive Summary", createLogScrollPane(scanExecutiveSummaryArea, "Executive Summary"));
        detailsTabs.addTab("Market Context", createLogScrollPane(scanMarketContextArea, "Market Context"));
        detailsTabs.addTab("Price/Volume Signals", createLogScrollPane(scanPriceVolumeArea, "Price/Volume Signals"));
        detailsTabs.addTab("Option Candidates", createLogScrollPane(scanOptionsArea, "Option Trade Candidates"));
        detailsTabs.addTab("Risk Notes", createLogScrollPane(scanRiskArea, "Risk Notes"));
        detailsTabs.addTab("Why This", createLogScrollPane(scanExplainabilityArea, "Explainability"));
        detailsTabs.addTab("Artifacts", createLogScrollPane(scanArtifactsArea, "Artifacts / Raw JSON"));

        JPanel detailsHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        detailsHeader.setOpaque(false);
        detailsHeader.add(scanDetailsRetryButton);
        detailsHeader.add(createHintLabel("Expand a row to lazy-load full analysis."));

        JPanel detailsCard = createCardPanel();
        detailsCard.add(createSectionHeader("Expanded Analysis"), BorderLayout.NORTH);
        JPanel detailsBody = new JPanel(new BorderLayout(0, 8));
        detailsBody.setOpaque(false);
        detailsBody.add(detailsHeader, BorderLayout.NORTH);
        detailsBody.add(detailsTabs, BorderLayout.CENTER);
        detailsCard.add(detailsBody, BorderLayout.CENTER);

        JScrollPane warningsScroll = createLogScrollPane(scanWarningsArea, "Warnings / Errors");
        JSplitPane lowerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailsCard, warningsScroll);
        lowerSplit.setResizeWeight(0.78);
        lowerSplit.setDividerLocation(850);
        lowerSplit.setBorder(BorderFactory.createEmptyBorder());
        lowerSplit.setBackground(COLOR_BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, lowerSplit);
        mainSplit.setResizeWeight(0.52);
        mainSplit.setDividerLocation(310);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setBackground(COLOR_BG);

        scanLoadResultsButton.addActionListener(e -> loadScanResults(true));
        scanRefreshStatusButton.addActionListener(e -> refreshScanStatusOnce());
        scanPrevPageButton.addActionListener(e -> stepScanPage(-1));
        scanNextPageButton.addActionListener(e -> stepScanPage(1));

        root.add(top, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        clearScanDetails("Select a scan result row to view expanded analysis.");
        return root;
    }

    private JPanel createScanRunsPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);

        scanRunsRefreshButton = new JButton("Refresh");
        scanRunsOpenButton = new JButton("Open Selected Run");
        scanRunsStatusLabel = new JLabel();
        styleButton(scanRunsRefreshButton, true);
        styleButton(scanRunsOpenButton, false);
        styleInlineStatus(scanRunsStatusLabel, "Scan runs: idle", COLOR_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(scanRunsRefreshButton);
        actions.add(scanRunsOpenButton);
        actions.add(scanRunsStatusLabel);

        scanRunsTableModel = new ScanRunsTableModel();
        scanRunsTable = new JTable(scanRunsTableModel);
        styleRunsTable(scanRunsTable);
        scanRunsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scanRunsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    openSelectedScanRun();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(scanRunsTable);
        tableScroll.getViewport().setBackground(COLOR_LOG_BG);
        tableScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        tableScroll.setColumnHeaderView(createSectionHeader("Scan Runs History"));

        scanRunsRefreshButton.addActionListener(e -> loadScanRuns(true));
        scanRunsOpenButton.addActionListener(e -> openSelectedScanRun());

        root.add(actions, BorderLayout.NORTH);
        root.add(tableScroll, BorderLayout.CENTER);
        return root;
    }

    private void ensureUniverseLoaded(boolean forceRefresh) {
        if (universeTabs == null) {
            return;
        }
        if (!forceRefresh && allUniverseTabsLoaded()) {
            updateUniverseMetaDisplay();
            rebuildActiveUniverseSectorOptions();
            applyUniverseFilters();
            return;
        }

        styleInlineStatus(universeStatusLabel, "Universe: loading catalog...", COLOR_WARNING);
        setUniverseControlsEnabled(false);

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return scanService.listUniverses(forceRefresh);
            }

            @Override
            protected void done() {
                try {
                    List<String> names = get();
                    if (names == null || names.isEmpty()) {
                        setUniverseControlsEnabled(false);
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: backend returned empty /api/universe/list",
                                COLOR_WARNING
                        );
                        return;
                    }

                    String nasdaqRequest = resolveUniverseRequestId(names, UNIVERSE_NASDAQ);
                    String watchlistRequest = resolveUniverseRequestId(names, UNIVERSE_WATCHLIST);
                    String combinedRequest = resolveUniverseRequestId(names, UNIVERSE_COMBINED);

                    if (nasdaqRequest == null || watchlistRequest == null || combinedRequest == null) {
                        setUniverseControlsEnabled(false);
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: required universes missing in /api/universe/list",
                                COLOR_WARNING
                        );
                        return;
                    }

                    setUniverseControlsEnabled(true);
                    loadUniverse(UNIVERSE_NASDAQ, nasdaqRequest, forceRefresh);
                    loadUniverse(UNIVERSE_WATCHLIST, watchlistRequest, forceRefresh);
                    loadUniverse(UNIVERSE_COMBINED, combinedRequest, forceRefresh);
                } catch (Exception ex) {
                    String error = humanizeError(ex).toLowerCase();
                    if (error.contains("404") && error.contains("universe") && error.contains("list")) {
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: backend missing /api/universe/list (refresh disabled)",
                                COLOR_DANGER
                        );
                        setUniverseControlsEnabled(false);
                        return;
                    }
                    if (isBackendUnreachableError(ex)) {
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: backend unreachable at " + hostField.getText().trim() + ":" + portField.getText().trim(),
                                COLOR_DANGER
                        );
                        setUniverseControlsEnabled(false);
                        return;
                    }
                    setUniverseControlsEnabled(false);
                    styleInlineStatus(
                            universeStatusLabel,
                            "Universe: failed to load catalog (" + humanizeError(ex) + ")",
                            COLOR_WARNING
                    );
                }
            }
        };
        worker.execute();
    }

    private boolean allUniverseTabsLoaded() {
        return universeNasdaqPayload != null && !universeNasdaqPayload.isEmpty()
                && universeWatchlistPayload != null && !universeWatchlistPayload.isEmpty()
                && universeCombinedPayload != null && !universeCombinedPayload.isEmpty();
    }

    private boolean isBackendUnreachableError(Exception throwable) {
        String text = humanizeError(throwable).toLowerCase();
        return text.contains("connection refused")
                || text.contains("connectexception")
                || text.contains("failed to connect")
                || text.contains("connection reset")
                || text.contains("timed out")
                || text.contains("unknownhost")
                || text.contains("host unreachable");
    }

    private void loadUniverse(String canonicalUniverseId, String requestUniverseId, boolean forceRefresh) {
        if (universeStatusLabel != null) {
            styleInlineStatus(universeStatusLabel, "Universe: loading " + canonicalUniverseId + "...", COLOR_WARNING);
        }

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> bundle = new LinkedHashMap<>();
                bundle.put("payload", scanService.getUniverse(requestUniverseId, forceRefresh));
                bundle.put("analysis_dates", loadAnalysisDatesByTicker(forceRefresh));
                return bundle;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> bundle = get();
                    Map<String, Object> payload = asMap(bundle.get("payload"));
                    Map<String, String> analysisDates = asStringMap(bundle.get("analysis_dates"));
                    List<UniverseRow> rows = parseUniverseRows(payload, analysisDates);
                    switch (canonicalUniverseId) {
                        case UNIVERSE_NASDAQ -> {
                            universeNasdaqPayload = payload;
                            if (universeNasdaqTableModel != null) {
                                universeNasdaqTableModel.setRows(rows);
                            }
                        }
                        case UNIVERSE_WATCHLIST -> {
                            universeWatchlistPayload = payload;
                            if (universeWatchlistTableModel != null) {
                                universeWatchlistTableModel.setRows(rows);
                            }
                        }
                        default -> {
                            universeCombinedPayload = payload;
                            if (universeCombinedTableModel != null) {
                                universeCombinedTableModel.setRows(rows);
                            }
                        }
                    }
                    rebuildActiveUniverseSectorOptions();
                    applyUniverseFilters();
                    refreshUniverseTabTitles();
                    updateUniverseMetaDisplay();
                    updateUniverseRunButtonState();
                    int requested = resolveUniverseRequestedCount(payload);
                    int returned = rows.size();
                    Color loadColor = requested > 0 && returned < requested ? COLOR_WARNING : COLOR_SUCCESS;
                    String loadText = requested > 0
                            ? "Universe: loaded " + returned + "/" + requested + " tickers (" + canonicalUniverseId + ")"
                            : "Universe: loaded " + returned + " tickers (" + canonicalUniverseId + ")";
                    styleInlineStatus(
                            universeStatusLabel,
                            loadText,
                            loadColor
                    );
                    if (rows.isEmpty() && !UNIVERSE_WATCHLIST.equals(canonicalUniverseId)) {
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: backend responded but returned 0 tickers for " + canonicalUniverseId,
                                COLOR_WARNING
                        );
                    }
                } catch (Exception ex) {
                    if (isBackendUnreachableError(ex)) {
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: backend unreachable at " + hostField.getText().trim() + ":" + portField.getText().trim(),
                                COLOR_DANGER
                        );
                    } else {
                        styleInlineStatus(
                                universeStatusLabel,
                                "Universe: load error for " + canonicalUniverseId + " (" + humanizeError(ex) + ")",
                                COLOR_WARNING
                        );
                    }
                    updateUniverseMetaDisplay();
                }
            }
        };
        worker.execute();
    }

    private void ensureDeepLearningTickersLoaded(boolean forceRefresh) {
        // Alias for ensureUniverseLoaded - loads the stock tickers for deep learning
        ensureUniverseLoaded(forceRefresh);
    }

    private void runDeepLearningOnSelected() {
        UniverseTableModel activeModel = activeUniverseModel();
        if (activeModel == null || activeModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "No tickers available. Refresh the list first.",
                    "Run Deep Learning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> selectedTickers = selectedTickersFromModel(activeModel);
        if (selectedTickers.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "No tickers selected. Check the boxes next to tickers you want to analyze.",
                    "Run Deep Learning",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Update UI
        if (dlResultsArea != null) {
            dlResultsArea.setText(ts() + " | Starting deep learning for " + selectedTickers.size() + " tickers...\n");
            dlResultsArea.append(ts() + " | Selected: " + String.join(", ", selectedTickers) + "\n");
        }
        styleInlineStatus(universeStatusLabel, "Running deep learning on " + selectedTickers.size() + " tickers...", COLOR_WARNING);
        universeRunScanButton.setEnabled(false);

        SwingWorker<Map<String, Object>, String> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                publish(ts() + " | Sending request to backend...");
                return apiClient.runDeepLearning(selectedTickers);
            }

            @Override
            protected void process(List<String> chunks) {
                if (dlResultsArea != null) {
                    for (String msg : chunks) {
                        dlResultsArea.append(msg + "\n");
                    }
                    dlResultsArea.setCaretPosition(dlResultsArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                universeRunScanButton.setEnabled(true);
                try {
                    Map<String, Object> result = get();
                    if (dlResultsArea != null) {
                        dlResultsArea.append(ts() + " | Deep learning completed.\n");
                        dlResultsArea.append("─".repeat(60) + "\n");
                        dlResultsArea.append(Json.pretty(result) + "\n");
                        String reportText = extractAnalysisReportText(result);
                        if (reportText != null && !reportText.isBlank()) {
                            dlResultsArea.append("\n# Analysis Report\n");
                            dlResultsArea.append(reportText.trim());
                            dlResultsArea.append("\n");
                        }
                        dlResultsArea.setCaretPosition(dlResultsArea.getDocument().getLength());
                    }
                    styleInlineStatus(universeStatusLabel, "Deep learning completed for " + selectedTickers.size() + " tickers", COLOR_SUCCESS);
                } catch (Exception ex) {
                    String error = humanizeError(ex);
                    if (dlResultsArea != null) {
                        dlResultsArea.append(ts() + " | ERROR: " + error + "\n");
                    }
                    styleInlineStatus(universeStatusLabel, "Deep learning failed: " + error, COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void attachUniverseTableAnalysisAction(JTable table, UniverseTableModel model) {
        if (table == null || model == null) {
            return;
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                UniverseRow row = model.getRow(modelRow);
                if (row == null || row.ticker() == null || row.ticker().isBlank()) {
                    return;
                }
                openAnalysisWorkspaceForTicker(row.ticker());
            }
        });
    }

    private void openAnalysisWorkspaceForTicker(String ticker) {
        String symbol = ticker == null ? "" : ticker.trim().toUpperCase();
        if (symbol.isBlank()) {
            return;
        }
        showView(VIEW_ANALYSIS);
        if (analysisWorkspacePanel != null) {
            analysisWorkspacePanel.openOrGenerateForTicker(symbol);
        }
    }

    private String extractAnalysisReportText(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "";
        }
        Object direct = findAnyValue(payload, "analysis_report", "report_text");
        if (direct instanceof String text && !text.isBlank()) {
            return text;
        }
        Object results = payload.get("results");
        if (results instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Object value : map.values()) {
                if (!(value instanceof Map<?, ?> rowRaw)) {
                    continue;
                }
                Map<String, Object> row = Json.asObject(rowRaw);
                Object textObj = findAnyValue(row, "analysis_report", "report_text");
                if (textObj instanceof String report && !report.isBlank()) {
                    return report;
                }
            }
        }
        return "";
    }

    private void refreshUniverseTabTitles() {
        if (universeTabs == null || universeTabs.getTabCount() < 3) {
            return;
        }
        universeTabs.setTitleAt(
                0,
                buildUniverseTabTitle("Nasdaq 500", universeNasdaqPayload, universeNasdaqTableModel)
        );
        universeTabs.setTitleAt(
                1,
                buildUniverseTabTitle("Watchlist", universeWatchlistPayload, universeWatchlistTableModel)
        );
        universeTabs.setTitleAt(
                2,
                buildUniverseTabTitle("Combined", universeCombinedPayload, universeCombinedTableModel)
        );
    }

    private String buildUniverseTabTitle(
            String base,
            Map<String, Object> payload,
            UniverseTableModel model
    ) {
        int requested = resolveUniverseRequestedCount(payload);
        int returned = resolveUniverseReturnedCount(payload, model);
        if (requested > 0 && returned >= 0) {
            return base + " (" + returned + "/" + requested + ")";
        }
        if (returned >= 0) {
            return base + " (" + returned + ")";
        }
        return base;
    }

    private int resolveUniverseRequestedCount(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return -1;
        }
        long value = asLong(findAnyValue(
                payload,
                "top_n_requested",
                "requested_count",
                "target_count",
                "requested"
        ));
        return value > 0 ? (int) value : -1;
    }

    private int resolveUniverseReturnedCount(Map<String, Object> payload, UniverseTableModel model) {
        if (payload != null && !payload.isEmpty()) {
            long explicit = asLong(findAnyValue(
                    payload,
                    "top_n_returned",
                    "returned_count",
                    "count",
                    "total"
            ));
            if (explicit > 0) {
                return (int) explicit;
            }
            Object listCandidate = findAnyValue(
                    payload,
                    "tickers",
                    "merged",
                    "nasdaq500",
                    "nasdaq300",
                    "watchlist",
                    "wsb100",
                    "combined",
                    "custom",
                    "nasdaq_top_500",
                    "wsb_top_500",
                    "items",
                    "rows",
                    "members",
                    "data"
            );
            if (listCandidate instanceof List<?> list) {
                return list.size();
            }
        }
        if (model != null) {
            return model.getRowCount();
        }
        return -1;
    }

    private void setUniverseControlsEnabled(boolean enabled) {
        if (universeRefreshButton != null) {
            universeRefreshButton.setEnabled(enabled);
        }
        if (universeRefreshNowButton != null) {
            universeRefreshNowButton.setEnabled(enabled);
        }
        if (universeSelectAllButton != null) {
            universeSelectAllButton.setEnabled(enabled);
        }
        if (universeClearSelectionButton != null) {
            universeClearSelectionButton.setEnabled(enabled);
        }
        if (universeAddStockButton != null) {
            universeAddStockButton.setEnabled(enabled);
        }
        if (universeRemoveStockButton != null) {
            universeRemoveStockButton.setEnabled(enabled);
        }
        if (universeAddStockField != null) {
            universeAddStockField.setEnabled(enabled);
        }
    }

    private void rebuildActiveUniverseSectorOptions() {
        if (universeSectorFilterCombo == null) {
            return;
        }
        UniverseTableModel model = activeUniverseModel();
        String prior = universeSectorFilterCombo.getSelectedItem() == null
                ? "All"
                : String.valueOf(universeSectorFilterCombo.getSelectedItem());
        List<String> sectors = new ArrayList<>();
        if (model != null) {
            for (UniverseRow row : model.rows()) {
                if (row == null) {
                    continue;
                }
                String sector = row.sector() == null ? "" : row.sector().trim();
                if (sector.isBlank() || "—".equals(sector)) {
                    continue;
                }
                sectors.add(sector);
            }
        }
        sectors.sort(String.CASE_INSENSITIVE_ORDER);
        universeSectorFilterCombo.removeAllItems();
        universeSectorFilterCombo.addItem("All");
        String last = null;
        for (String sector : sectors) {
            if (last != null && last.equalsIgnoreCase(sector)) {
                continue;
            }
            universeSectorFilterCombo.addItem(sector);
            last = sector;
        }
        boolean restored = false;
        for (int i = 0; i < universeSectorFilterCombo.getItemCount(); i++) {
            String candidate = String.valueOf(universeSectorFilterCombo.getItemAt(i));
            if (candidate.equalsIgnoreCase(prior)) {
                universeSectorFilterCombo.setSelectedIndex(i);
                restored = true;
                break;
            }
        }
        if (!restored) {
            universeSectorFilterCombo.setSelectedIndex(0);
        }
    }

    private void addWatchlistStockFromUi() {
        if (universeAddStockButton == null) {
            return;
        }

        String symbol = JOptionPane.showInputDialog(
                frame,
                "Enter ticker symbol to add to Watchlist:",
                "Add Ticker",
                JOptionPane.PLAIN_MESSAGE
        );
        symbol = symbol == null ? "" : symbol.trim().toUpperCase();
        if (symbol.isBlank()) {
            return;
        }
        if (!WATCHLIST_TICKER_PATTERN.matcher(symbol).matches()) {
            styleInlineStatus(universeStatusLabel, "Watchlist: invalid ticker format (A-Z/0-9/./-, max 10)", COLOR_WARNING);
            return;
        }
        final String symbolFinal = symbol;

        configureClientFromUI();
        universeAddStockButton.setEnabled(false);
        styleInlineStatus(universeStatusLabel, "Watchlist: adding " + symbolFinal + "...", COLOR_WARNING);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return scanService.addWatchlistSymbol(symbolFinal);
            }

            @Override
            protected void done() {
                universeAddStockButton.setEnabled(true);
                try {
                    Map<String, Object> response = get();
                    styleInlineStatus(universeStatusLabel, "Watchlist: added " + symbolFinal, COLOR_SUCCESS);
                    appendBackendLog(ts() + " | Watchlist ticker added: " + symbolFinal + " (" + Json.pretty(response) + ")");
                    ensureDeepLearningTickersLoaded(true);
                } catch (Exception ex) {
                    styleInlineStatus(
                            universeStatusLabel,
                            "Watchlist: add failed (" + humanizeError(ex) + ")",
                            COLOR_DANGER
                    );
                }
            }
        };
        worker.execute();
    }

    private void removeSelectedWatchlistStocksFromUi() {
        if (!UNIVERSE_WATCHLIST.equals(activeUniverseId())) {
            styleInlineStatus(universeStatusLabel, "Watchlist: switch to Watchlist tab to remove tickers", COLOR_WARNING);
            return;
        }
        UniverseTableModel model = universeWatchlistTableModel;
        if (model == null || model.getRowCount() == 0) {
            styleInlineStatus(universeStatusLabel, "Watchlist: no rows to remove", COLOR_WARNING);
            return;
        }

        List<String> selected = selectedTickersFromModel(model);
        if (selected.isEmpty()) {
            styleInlineStatus(universeStatusLabel, "Watchlist: no selected rows", COLOR_WARNING);
            return;
        }

        configureClientFromUI();
        if (universeRemoveStockButton != null) {
            universeRemoveStockButton.setEnabled(false);
        }
        styleInlineStatus(universeStatusLabel, "Watchlist: removing " + selected.size() + " ticker(s)...", COLOR_WARNING);

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                int removed = 0;
                for (String symbol : selected) {
                    scanService.removeWatchlistSymbol(symbol);
                    removed++;
                }
                return removed;
            }

            @Override
            protected void done() {
                if (universeRemoveStockButton != null) {
                    universeRemoveStockButton.setEnabled(true);
                }
                try {
                    int removed = get();
                    styleInlineStatus(universeStatusLabel, "Watchlist: removed " + removed + " ticker(s)", COLOR_SUCCESS);
                    ensureDeepLearningTickersLoaded(true);
                } catch (Exception ex) {
                    styleInlineStatus(universeStatusLabel, "Watchlist: remove failed (" + humanizeError(ex) + ")", COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void configureUniverseSorter(TableRowSorter<UniverseTableModel> sorter) {
        if (sorter == null) {
            return;
        }
        applyUniverseSorterComparators(sorter, javax.swing.SortOrder.ASCENDING);
        sorter.addRowSorterListener(e -> {
            javax.swing.SortOrder order = javax.swing.SortOrder.ASCENDING;
            List<? extends javax.swing.RowSorter.SortKey> keys = sorter.getSortKeys();
            if (!keys.isEmpty()) {
                order = keys.get(0).getSortOrder();
            }
            applyUniverseSorterComparators(sorter, order);
        });
    }

    /**
     * Null handling rule for universe sorting:
     * always keep missing values at the bottom (ascending and descending).
     * Swing reverses comparators for descending, so we flip null priority per direction.
     */
    private void applyUniverseSorterComparators(
            TableRowSorter<UniverseTableModel> sorter,
            javax.swing.SortOrder order
    ) {
        boolean nullsLastInComparator = order != javax.swing.SortOrder.DESCENDING;
        sorter.setComparator(1, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a), String.valueOf(b)));
        sorter.setComparator(2, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a), String.valueOf(b)));
        sorter.setComparator(3, (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(String.valueOf(a), String.valueOf(b)));
        sorter.setComparator(4, (a, b) -> compareNullableDouble(parseDisplayNumeric(a), parseDisplayNumeric(b), nullsLastInComparator));
        sorter.setComparator(5, (a, b) -> compareNullableDouble(parseDisplayNumeric(a), parseDisplayNumeric(b), nullsLastInComparator));
        sorter.setComparator(6, (a, b) -> compareNullableDouble(parseDisplayNumeric(a), parseDisplayNumeric(b), nullsLastInComparator));
        sorter.setComparator(7, (a, b) -> compareNullableLong(parseAnalysisDateEpoch(a), parseAnalysisDateEpoch(b), nullsLastInComparator));
        sorter.setComparator(8, (a, b) -> compareNullableLong(parseDisplayLong(a), parseDisplayLong(b), nullsLastInComparator));
    }

    private void runUniverseSortSanityCheck() {
        boolean numericOk = compareNullableDouble(parseDisplayNumeric("2.0B"), parseDisplayNumeric("900.0M"), true) > 0
                && compareNullableDouble(parseDisplayNumeric("—"), parseDisplayNumeric("1.0M"), true) > 0;
        boolean dateOk = compareNullableLong(
                parseAnalysisDateEpoch("2026-02-14T12:00:00"),
                parseAnalysisDateEpoch("2026-02-13T12:00:00"),
                true
        ) > 0;
        if (!numericOk || !dateOk) {
            appendBackendLog(ts() + " | WARN: Universe sorter sanity check failed.");
        }
    }

    private int compareNullableDouble(Double left, Double right, boolean nullsLast) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return nullsLast ? 1 : -1;
        }
        if (right == null) {
            return nullsLast ? -1 : 1;
        }
        return Double.compare(left, right);
    }

    private int compareNullableLong(Long left, Long right, boolean nullsLast) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return nullsLast ? 1 : -1;
        }
        if (right == null) {
            return nullsLast ? -1 : 1;
        }
        return Long.compare(left, right);
    }

    private Double parseDisplayNumeric(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank() || "—".equals(text)) {
            return null;
        }
        String normalized = text.replace(",", "").replace("%", "").replace("+", "").trim().toUpperCase();
        if (normalized.isBlank()) {
            return null;
        }

        double multiplier = 1.0;
        char suffix = normalized.charAt(normalized.length() - 1);
        if (suffix == 'K' || suffix == 'M' || suffix == 'B' || suffix == 'T') {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
            multiplier = switch (suffix) {
                case 'K' -> 1_000.0;
                case 'M' -> 1_000_000.0;
                case 'B' -> 1_000_000_000.0;
                case 'T' -> 1_000_000_000_000.0;
                default -> 1.0;
            };
        }
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized) * multiplier;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Long parseDisplayLong(Object value) {
        Double parsed = parseDisplayNumeric(value);
        if (parsed == null || !Double.isFinite(parsed)) {
            return null;
        }
        return Math.round(parsed);
    }

    private Long parseAnalysisDateEpoch(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isBlank() || "—".equals(raw)) {
            return null;
        }
        String normalized = raw.replace(' ', 'T');
        try {
            return java.time.Instant.parse(normalized).toEpochMilli();
        } catch (RuntimeException ignored) {
            // Fallback to local datetime parsing.
        }
        try {
            return java.time.LocalDateTime.parse(normalized).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        } catch (RuntimeException ignored) {
            // Continue fallback parsing below.
        }
        try {
            return java.time.LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .toInstant(java.time.ZoneOffset.UTC)
                    .toEpochMilli();
        } catch (RuntimeException ignored) {
            // continue
        }
        try {
            return java.time.LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .toInstant(java.time.ZoneOffset.UTC)
                    .toEpochMilli();
        } catch (RuntimeException ignored) {
            // continue
        }
        try {
            return java.time.LocalDate.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .atStartOfDay()
                    .toInstant(java.time.ZoneOffset.UTC)
                    .toEpochMilli();
        } catch (RuntimeException ignored) {
            // continue
        }
        return null;
    }

    private String resolveUniverseRequestId(List<String> availableNames, String canonicalId) {
        if (availableNames == null || availableNames.isEmpty()) {
            return null;
        }
        Map<String, String> byNormalized = new LinkedHashMap<>();
        for (String name : availableNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            byNormalized.put(normalizeKey(name), name);
        }

        String direct = byNormalized.get(normalizeKey(canonicalId));
        if (direct != null) {
            return direct;
        }

        if (UNIVERSE_NASDAQ.equals(canonicalId)) {
            String alias = byNormalized.get(normalizeKey("nasdaq300"));
            if (alias != null) {
                return alias;
            }
            return byNormalized.get(normalizeKey("nasdaq_top_500"));
        }
        if (UNIVERSE_WATCHLIST.equals(canonicalId)) {
            for (String alias : List.of("watchlist", "custom", "customstocks", "custom_stocks")) {
                String resolved = byNormalized.get(normalizeKey(alias));
                if (resolved != null) {
                    return resolved;
                }
            }
            return null;
        }
        if (UNIVERSE_COMBINED.equals(canonicalId)) {
            String combined400 = byNormalized.get(normalizeKey("combined400"));
            if (combined400 != null) {
                return combined400;
            }
            return byNormalized.get(normalizeKey("combined_1000"));
        }
        return null;
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> mapRaw) {
            return Json.asObject(mapRaw);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, String> asStringMap(Object value) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> raw)) {
            return out;
        }
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            String key = String.valueOf(entry.getKey() == null ? "" : entry.getKey()).trim().toUpperCase();
            String date = stringOrEmpty(entry.getValue());
            if (!key.isBlank() && !date.isBlank()) {
                out.put(key, date);
            }
        }
        return out;
    }

    private Map<String, String> loadAnalysisDatesByTicker(boolean forceRefresh) throws Exception {
        long nowMs = System.currentTimeMillis();
        synchronized (universeAnalysisDateCacheLock) {
            if (!forceRefresh && !universeAnalysisDateCache.isEmpty() && (nowMs - universeAnalysisDateCacheLoadedAtMs) < 60_000L) {
                return new LinkedHashMap<>(universeAnalysisDateCache);
            }
        }

        Object payload = apiClient.fetchAnalysisList(500);
        List<?> items;
        if (payload instanceof List<?> list) {
            items = list;
        } else if (payload instanceof Map<?, ?> mapRaw) {
            Object nested = Json.asObject(mapRaw).get("items");
            if (nested instanceof List<?> nestedList) {
                items = nestedList;
            } else {
                items = List.of();
            }
        } else {
            items = List.of();
        }

        Map<String, String> latestByTicker = new LinkedHashMap<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> mapRaw)) {
                continue;
            }
            Map<String, Object> row = Json.asObject(mapRaw);
            String ticker = firstNonBlank(
                    stringOrEmpty(findAnyValue(row, "ticker", "symbol")),
                    ""
            ).toUpperCase();
            if (ticker.isBlank()) {
                continue;
            }
            String date = firstNonBlank(
                    stringOrEmpty(findAnyValue(row, "analysis_date", "created_at", "last_analysis_at")),
                    ""
            );
            if (date.isBlank()) {
                continue;
            }
            String existing = latestByTicker.get(ticker);
            if (existing == null || date.compareTo(existing) > 0) {
                latestByTicker.put(ticker, date);
            }
        }

        synchronized (universeAnalysisDateCacheLock) {
            universeAnalysisDateCache = new LinkedHashMap<>(latestByTicker);
            universeAnalysisDateCacheLoadedAtMs = nowMs;
        }
        return latestByTicker;
    }

    private void triggerUniverseRefreshNow() {
        configureClientFromUI();
        if (universeRefreshNowButton != null) {
            universeRefreshNowButton.setEnabled(false);
        }
        if (universeRefreshButton != null) {
            universeRefreshButton.setEnabled(false);
        }
        styleInlineStatus(universeStatusLabel, "Universe: rebuilding from sources...", COLOR_WARNING);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return scanService.refreshUniverseNow();
            }

            @Override
            protected void done() {
                if (universeRefreshNowButton != null) {
                    universeRefreshNowButton.setEnabled(true);
                }
                if (universeRefreshButton != null) {
                    universeRefreshButton.setEnabled(true);
                }
                try {
                    Map<String, Object> response = get();
                    styleInlineStatus(universeStatusLabel, "Universe: rebuild complete, reloading...", COLOR_SUCCESS);
                    loadUniverse(UNIVERSE_NASDAQ, UNIVERSE_NASDAQ, true);
                    loadUniverse(UNIVERSE_WATCHLIST, UNIVERSE_WATCHLIST, true);
                    loadUniverse(UNIVERSE_COMBINED, UNIVERSE_COMBINED, true);
                    appendBackendLog(ts() + " | Universe refresh completed: "
                            + firstNonBlank(stringOrEmpty(findAnyValue(response, "status")), "ok"));
                } catch (Exception ex) {
                    styleInlineStatus(universeStatusLabel, "Universe: rebuild failed", COLOR_DANGER);
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to refresh universe now:\n" + humanizeError(ex)
                                    + "\n\nThis requires backend endpoint: POST /api/universe/rebuild",
                            "Universe Refresh Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private List<UniverseRow> parseUniverseRows(Map<String, Object> payload, Map<String, String> analysisDates) {
        Object listCandidate = findAnyValue(payload,
                "tickers",
                "merged",
                "nasdaq500",
                "nasdaq300",
                "watchlist",
                "wsb100",
                "combined",
                "custom",
                "nasdaq_top_500",
                "wsb_top_500",
                "symbols",
                "items",
                "rows",
                "constituents",
                "members",
                "data");
        List<UniverseRow> rows = new ArrayList<>();
        if (listCandidate instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> mapRaw) {
                    Map<String, Object> map = Json.asObject(mapRaw);
                    String ticker = firstNonBlank(
                            stringOrEmpty(findAnyValue(map, "ticker", "symbol", "id")),
                            ""
                    ).toUpperCase();
                    if (ticker.isBlank()) {
                        continue;
                    }
                    String name = firstNonBlank(
                            stringOrEmpty(findAnyValue(map, "name", "company_name", "company", "title")),
                            "—"
                    );
                    String sector = firstNonBlank(
                            stringOrEmpty(findAnyValue(map, "sector", "industry_sector", "gics_sector")),
                            "—"
                    );
                    Double marketCap = asNullableDouble(findAnyValue(map, "market_cap", "marketcap", "mcap"));
                    Double avgVolume7d = asNullableDouble(findAnyValue(map,
                            "avg_volume_7d",
                            "average_volume_7d",
                            "avg_volume",
                            "average_volume",
                            "avg_dollar_volume",
                            "average_dollar_volume",
                            "avg_dollar_vol",
                            "dollar_volume",
                            "liquidity"));
                    Double changePct1d = asNullableDouble(findAnyValue(map,
                            "change_pct_1d",
                            "change_percent_1d",
                            "change_percent",
                            "change_1d"));
                    String analysisDate = firstNonBlank(
                            stringOrEmpty(findAnyValue(map, "last_analysis_date", "analysis_date", "last_analysis_at")),
                            analysisDates == null ? "" : firstNonBlank(analysisDates.get(ticker), "")
                    );
                    Long mentionCount = asLong(findAnyValue(map, "mention_count", "mentions", "count"));
                    rows.add(new UniverseRow(false, ticker, name, sector, marketCap, avgVolume7d, changePct1d, analysisDate, mentionCount, map));
                } else if (item != null) {
                    String ticker = String.valueOf(item).trim().toUpperCase();
                    if (!ticker.isBlank()) {
                        String analysisDate = analysisDates == null ? "" : firstNonBlank(analysisDates.get(ticker), "");
                        Map<String, Object> fallbackRaw = new LinkedHashMap<>();
                        fallbackRaw.put("ticker", ticker);
                        rows.add(new UniverseRow(false, ticker, "—", "—", null, null, null, analysisDate, null,
                                fallbackRaw));
                    }
                }
            }
        }
        rows.sort(Comparator.comparing(UniverseRow::ticker));
        return rows;
    }

    private void updateUniverseMetaDisplay() {
        Map<String, Object> payload = activeUniversePayload();
        String generatedAt = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "generated_at", "generatedAt", "refreshed_at", "updated_at")),
                "—"
        );
        String hash = firstNonBlank(stringOrEmpty(findAnyValue(payload, "universe_hash", "hash")), "—");
        styleInlineStatus(universeMetaLabel, "Last refresh: " + generatedAt, COLOR_MUTED);
        styleInlineStatus(universeHashLabel, "Hash: " + hash, COLOR_MUTED);
    }

    private void updateUniverseRunButtonState() {
        if (universeRunScanButton == null) {
            return;
        }
        UniverseTableModel activeModel = activeUniverseModel();
        String universeId = activeUniverseId();
        int totalRows = activeModel == null ? 0 : activeModel.getRowCount();
        boolean hasRows = totalRows > 0;

        universeRunScanButton.setEnabled(hasRows);

        if (!hasRows) {
            universeRunScanButton.setToolTipText("Load universe data first.");
            return;
        }

        universeRunScanButton.setToolTipText(
                "Run deep learning scan for active universe: " + universeId + " (rows: " + totalRows + ")."
        );
    }

    private void applyUniverseFilters() {
        UniverseTableModel model = activeUniverseModel();
        TableRowSorter<UniverseTableModel> sorter = activeUniverseSorter();
        JTable table = activeUniverseTable();
        if (model == null || sorter == null || table == null) {
            return;
        }

        String search = universeSearchField == null ? "" : universeSearchField.getText().trim().toLowerCase();
        String selectedSector = "";
        if (universeSectorFilterCombo != null && universeSectorFilterCombo.getSelectedItem() != null) {
            selectedSector = String.valueOf(universeSectorFilterCombo.getSelectedItem()).trim();
        }
        final String sectorFilter = selectedSector;
        double minMarketCap = universeLiquidityFilterSpinner == null
                ? 0.0
                : Json.asDouble(universeLiquidityFilterSpinner.getValue(), 0.0);
        long minMentions = universeMentionFilterSpinner == null
                ? 0L
                : Math.max(0L, Json.asInt(universeMentionFilterSpinner.getValue(), 0));

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends UniverseTableModel, ? extends Integer> entry) {
                UniverseRow row = model.getRow(entry.getIdentifier());
                if (row == null) {
                    return false;
                }
                if (!search.isBlank()) {
                    String haystack = (safeLower(row.ticker()) + " "
                            + safeLower(row.name()) + " "
                            + safeLower(row.sector()));
                    if (!haystack.contains(search)) {
                        return false;
                    }
                }
                if (!sectorFilter.isBlank() && !"all".equalsIgnoreCase(sectorFilter)
                        && !safeLower(row.sector()).equals(safeLower(sectorFilter))) {
                    return false;
                }
                if (minMarketCap > 0.0) {
                    Double marketCap = row.marketCap();
                    if (marketCap == null || marketCap < minMarketCap) {
                        return false;
                    }
                }
                if (minMentions > 0L) {
                    Long mentions = row.mentionCount();
                    if (mentions == null || mentions < minMentions) {
                        return false;
                    }
                }
                return true;
            }
        });

        styleInlineStatus(
                universeStatusLabel,
                "Universe: " + table.getRowCount() + " / " + model.getRowCount() + " visible",
                COLOR_MUTED
        );
    }

    private void setUniverseSelection(boolean selected) {
        UniverseTableModel model = activeUniverseModel();
        if (model == null) {
            return;
        }
        if (model.getRowCount() == 0) {
            return;
        }
        for (int modelRow = 0; modelRow < model.getRowCount(); modelRow++) {
            model.setSelected(modelRow, selected);
        }
        styleInlineStatus(
                universeStatusLabel,
                selected
                        ? "Universe: selected all " + model.getRowCount() + " rows in active tab"
                        : "Universe: selection cleared",
                selected ? COLOR_SUCCESS : COLOR_MUTED
        );
        updateUniverseRunButtonState();
    }

    private void exportActiveUniverseCsv() {
        UniverseTableModel model = activeUniverseModel();
        if (model == null || model.getRowCount() == 0) {
            styleInlineStatus(universeStatusLabel, "Universe: nothing to export", COLOR_WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export universe CSV");
        chooser.setSelectedFile(new File(activeUniverseId() + ".csv"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        List<UniverseRow> rows = model.selectedRows();
        if (rows.isEmpty()) {
            rows = model.rows();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ticker,name,sector,market_cap,avg_volume_7d,change_pct_1d,analysis_date,mention_count\n");
        for (UniverseRow row : rows) {
            csv.append(csvCell(row.ticker())).append(",");
            csv.append(csvCell(row.name())).append(",");
            csv.append(csvCell(row.sector())).append(",");
            csv.append(csvCell(row.marketCap() == null ? "" : String.valueOf(row.marketCap()))).append(",");
            csv.append(csvCell(row.avgVolume7d() == null ? "" : String.valueOf(row.avgVolume7d()))).append(",");
            csv.append(csvCell(row.changePct1d() == null ? "" : String.valueOf(row.changePct1d()))).append(",");
            csv.append(csvCell(row.analysisDate())).append(",");
            csv.append(csvCell(row.mentionCount() == null ? "" : String.valueOf(row.mentionCount()))).append("\n");
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            styleInlineStatus(universeStatusLabel, "Universe CSV exported: " + file.getName(), COLOR_SUCCESS);
        } catch (Exception ex) {
            styleInlineStatus(universeStatusLabel, "Universe CSV export failed", COLOR_DANGER);
            JOptionPane.showMessageDialog(
                    frame,
                    "Failed to export CSV:\n" + humanizeError(ex),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void exportActiveUniverseJson() {
        Map<String, Object> payload = activeUniversePayload();
        if (payload == null || payload.isEmpty()) {
            styleInlineStatus(universeStatusLabel, "Universe: no JSON payload to export", COLOR_WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export universe JSON");
        chooser.setSelectedFile(new File(activeUniverseId() + ".json"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), Json.pretty(payload), StandardCharsets.UTF_8);
            styleInlineStatus(universeStatusLabel, "Universe JSON exported: " + file.getName(), COLOR_SUCCESS);
        } catch (Exception ex) {
            styleInlineStatus(universeStatusLabel, "Universe JSON export failed", COLOR_DANGER);
            JOptionPane.showMessageDialog(
                    frame,
                    "Failed to export JSON:\n" + humanizeError(ex),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void openScanConfigDialog(boolean selectedOnly) {
        UniverseTableModel activeModel = activeUniverseModel();
        String universeId = activeUniverseId();
        if (activeModel == null || activeModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Universe is empty. Refresh it first.",
                    "Run Deep Learning Scan",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> selectedTickers = selectedTickersFromModel(activeModel);
        if (selectedOnly && selectedTickers.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "No tickers selected in the active tab. Use the Select All button or check rows first.",
                    "Run Selected Tickers",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int availableTickers = activeModel.getRowCount();
        int selectedCount = selectedTickers.size();
        int maxSelectable = selectedOnly ? Math.max(1, selectedCount) : Math.max(1, availableTickers);
        int defaultMaxTickers = selectedOnly
                ? maxSelectable
                : Math.min(maxSelectable, Math.max(1000, selectedCount));

        // Horizons checkboxes (prediction timeframes)
        JCheckBox horizon1d = new JCheckBox("1d", true);
        JCheckBox horizon3d = new JCheckBox("3d", true);
        JCheckBox horizon5d = new JCheckBox("5d", true);
        JCheckBox horizon10d = new JCheckBox("10d", false);
        JCheckBox horizon20d = new JCheckBox("20d", false);
        JCheckBox[] horizonChecks = {horizon1d, horizon3d, horizon5d, horizon10d, horizon20d};
        JPanel horizonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        horizonsPanel.setOpaque(false);
        for (JCheckBox cb : horizonChecks) {
            cb.setOpaque(false);
            cb.setForeground(COLOR_TEXT);
            horizonsPanel.add(cb);
        }

        // Strategy Set checkboxes
        JCheckBox strategySingleLeg = new JCheckBox("single-leg", true);
        JCheckBox strategyVerticalSpread = new JCheckBox("vertical-spread", true);
        JCheckBox strategyCalendarSpread = new JCheckBox("calendar-spread", false);
        JCheckBox strategyIronCondor = new JCheckBox("iron-condor", false);
        JCheckBox[] strategyChecks = {strategySingleLeg, strategyVerticalSpread, strategyCalendarSpread, strategyIronCondor};
        JPanel strategiesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        strategiesPanel.setOpaque(false);
        for (JCheckBox cb : strategyChecks) {
            cb.setOpaque(false);
            cb.setForeground(COLOR_TEXT);
            strategiesPanel.add(cb);
        }

        JComboBox<String> riskModeCombo = new JComboBox<>(new String[]{"conservative", "standard", "aggressive"});
        JSpinner maxTickersSpinner = new JSpinner(new SpinnerNumberModel(defaultMaxTickers, 1, maxSelectable, 1));
        styleCombo(riskModeCombo);
        styleSpinner(maxTickersSpinner);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_CARD);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 4, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(createFormLabel("Universe"), gbc);
        gbc.gridx = 1;
        panel.add(createFormLabel(universeId + " (" + availableTickers + " rows)"), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createFormLabel("Selection"), gbc);
        gbc.gridx = 1;
        panel.add(createFormLabel(selectedOnly ? (selectedCount + " selected only") : (selectedCount + " selected (or all)")), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createFormLabel("Horizons"), gbc);
        gbc.gridx = 1;
        panel.add(horizonsPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createFormLabel("Strategy Set"), gbc);
        gbc.gridx = 1;
        panel.add(strategiesPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createFormLabel("Risk Mode"), gbc);
        gbc.gridx = 1;
        panel.add(riskModeCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(createFormLabel("Max Tickers (" + maxSelectable + " max)"), gbc);
        gbc.gridx = 1;
        panel.add(maxTickersSpinner, gbc);

        int choice = JOptionPane.showConfirmDialog(
                frame,
                panel,
                selectedOnly ? "Run Deep Learning Scan (Selected)" : "Run Deep Learning Scan",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        int maxTickers = Math.max(1, Json.asInt(maxTickersSpinner.getValue(), defaultMaxTickers));
        List<String> horizons = new ArrayList<>();
        for (JCheckBox cb : horizonChecks) {
            if (cb.isSelected()) horizons.add(cb.getText());
        }
        if (horizons.isEmpty()) horizons.add("1d"); // default fallback
        List<String> strategies = new ArrayList<>();
        for (JCheckBox cb : strategyChecks) {
            if (cb.isSelected()) strategies.add(cb.getText());
        }
        if (strategies.isEmpty()) strategies.add("single-leg"); // default fallback
        String riskMode = String.valueOf(riskModeCombo.getSelectedItem());

        List<String> baseTickers;
        if (selectedOnly) {
            baseTickers = selectedTickers;
        } else if (!selectedTickers.isEmpty()) {
            baseTickers = selectedTickers;
        } else {
            baseTickers = allTickersFromModel(activeModel);
        }
        List<String> tickers = baseTickers.size() > maxTickers
                ? new ArrayList<>(baseTickers.subList(0, maxTickers))
                : new ArrayList<>(baseTickers);
        if (tickers.isEmpty()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "No tickers selected for scan.",
                    "Run Deep Learning Scan",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Map<String, Object> horizonConfig = new LinkedHashMap<>();
        horizonConfig.put("horizons", horizons);
        Map<String, Object> strategyConfig = new LinkedHashMap<>();
        strategyConfig.put("tickers", tickers);
        strategyConfig.put("strategies", strategies);
        strategyConfig.put("max_tickers", maxTickers);
        strategyConfig.put("selection_mode", selectedOnly ? "selected_only" : "selected_or_all");
        strategyConfig.put("selected_count", selectedCount);
        Map<String, Object> riskConfig = new LinkedHashMap<>();
        riskConfig.put("mode", riskMode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("universe", universeId);
        payload.put("runMode", "scan");
        payload.put("horizonConfig", horizonConfig);
        payload.put("strategyUniverseConfig", strategyConfig);
        payload.put("riskConfig", riskConfig);
        startDeepLearningScan(payload);
    }

    private void startDeepLearningScan(Map<String, Object> payload) {
        configureClientFromUI();
        universeRunScanButton.setEnabled(false);
        if (universeMainTabs != null) {
            universeMainTabs.setSelectedIndex(1);
        }
        styleInlineStatus(universeStatusLabel, "Universe: starting scan...", COLOR_WARNING);
        styleInlineStatus(scanResultsStatusLabel, "Scan results: submitting start request...", COLOR_WARNING);
        appendScanWarningLog("Submitting scan request to " + currentHostPort());
        Object strategyConfig = payload == null ? null : payload.get("strategyUniverseConfig");
        int requestedTickers = -1;
        if (strategyConfig instanceof Map<?, ?> strategyMap) {
            Object tickerObj = Json.asObject(strategyMap).get("tickers");
            if (tickerObj instanceof List<?> tickerList) {
                requestedTickers = tickerList.size();
            }
        }
        if (requestedTickers >= 0) {
            appendScanWarningLog("Requested tickers: " + requestedTickers);
        }

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                appendScanWarningLog("Preflight check: /health");
                if (!healthCheckWithRetries(3, 2, 250L)) {
                    throw new IOException(
                            "Backend is not reachable at "
                                    + currentHostPort()
                                    + ". Start AI Backend first."
                    );
                }
                appendScanWarningLog("POST /scan/start");
                return scanService.startScan(payload);
            }

            @Override
            protected void done() {
                universeRunScanButton.setEnabled(true);
                updateUniverseRunButtonState();
                try {
                    Map<String, Object> response = get();
                    String runId = firstNonBlank(
                            stringOrEmpty(findAnyValue(response, "run_id", "runId", "id")),
                            extractRunId(response)
                    );
                    if (runId.isBlank()) {
                        throw new IOException("scan start response did not include runId");
                    }
                    lastScanWarningSignature = "";
                    lastTickerStatusSignature = "";
                    lastTickerFailureSignature = "";
                    activeScanRunId = runId;
                    scanRunIdField.setText(runId);
                    scanDetailCache.clear();
                    expandedScanTicker = null;
                    clearScanDetails("Scan started. Expand a ticker row to load details.");
                    styleInlineStatus(universeStatusLabel, "Universe: scan started (" + shortenRunId(runId) + ")", COLOR_SUCCESS);
                    styleInlineStatus(scanResultsStatusLabel, "Scan results: run " + shortenRunId(runId) + " started", COLOR_WARNING);
                    appendScanWarningLog("Scan run started: " + runId);
                    logScanAudit("started", runId, payload, null);
                    loadScanResults(true);
                    startScanStatusPolling(runId);
                    loadScanRuns(true);
                } catch (Exception ex) {
                    styleInlineStatus(universeStatusLabel, "Universe: scan start failed", COLOR_DANGER);
                    styleInlineStatus(scanResultsStatusLabel, "Scan results: start failed", COLOR_DANGER);
                    appendScanWarningLog("Scan start failed: " + humanizeError(ex));
                    if (isConnectivityIssue(ex)) {
                        styleStatusLabel(connectionLabel, "Connection failed", COLOR_DANGER);
                        backendExternalConnected = false;
                        refreshBackendControls();
                    }
                    logScanAudit("failed", "", payload, humanizeError(ex));
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to start scan:\n" + humanizeError(ex),
                            "Scan Start Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private boolean ensureScanBackendReachableForScan() {
        configureClientFromUI();
        if (healthCheckWithRetries(2, 2, 150L)) {
            styleStatusLabel(connectionLabel, "Connected", COLOR_SUCCESS);
            backendExternalConnected = !backendIsRunning() && !backendStarting;
            refreshBackendControls();
            return true;
        }
        styleStatusLabel(connectionLabel, "Connection failed", COLOR_DANGER);
        backendExternalConnected = false;
        refreshBackendControls();
        appendScanWarningLog("Backend preflight failed at " + currentHostPort());

        styleInlineStatus(
                universeStatusLabel,
                "Universe: backend not reachable at " + currentHostPort(),
                COLOR_DANGER
        );
        JOptionPane.showMessageDialog(
                frame,
                "Cannot start scan because backend is not reachable at " + currentHostPort() + ".\n\n"
                        + "Go to AI Management > Backend Control and click Start AI Backend,\n"
                        + "then click Check Connection.",
                "Backend Not Reachable",
                JOptionPane.ERROR_MESSAGE
        );
        return false;
    }

    private boolean healthCheckWithRetries(int attempts, int timeoutSeconds, long pauseMillis) {
        int tries = Math.max(1, attempts);
        int timeout = Math.max(1, timeoutSeconds);
        for (int i = 0; i < tries; i++) {
            if (apiClient.healthCheck(timeout)) {
                return true;
            }
            if (i + 1 < tries && pauseMillis > 0) {
                try {
                    Thread.sleep(pauseMillis);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }

    private void logScanAudit(String status, String runId, Map<String, Object> config, String error) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("event", "scan_action");
        entry.put("user", resolveAuditUser());
        entry.put("initiated_at", LocalDateTime.now().toString());
        entry.put("status", firstNonBlank(status, "unknown"));
        entry.put("run_id", firstNonBlank(runId, "n/a"));
        entry.put("runId", firstNonBlank(runId, "n/a"));
        entry.put("config", config == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<>(config));
        if (error != null && !error.isBlank()) {
            entry.put("error", error);
        }
        try {
            auditLogStore.append(entry);
            if (auditLogStatusLabel != null) {
                styleInlineStatus(auditLogStatusLabel, "Audit: updated at " + ts(), COLOR_SUCCESS);
            }
        } catch (IOException ex) {
            if (auditLogStatusLabel != null) {
                styleInlineStatus(auditLogStatusLabel, "Audit: write failed", COLOR_DANGER);
            }
        }
    }

    private void stepScanPage(int delta) {
        if (scanPageSpinner == null) {
            return;
        }
        int current = Json.asInt(scanPageSpinner.getValue(), 1);
        int next = Math.max(1, current + delta);
        scanPageSpinner.setValue(next);
        loadScanResults(true);
    }

    private void loadScanResults(boolean forceRefresh) {
        String runId = scanRunIdField == null ? "" : scanRunIdField.getText().trim();
        if (runId.isBlank()) {
            styleInlineStatus(scanResultsStatusLabel, "Scan results: enter runId first", COLOR_WARNING);
            return;
        }
        int page = Math.max(1, Json.asInt(scanPageSpinner.getValue(), 1));
        int pageSize = scanPageSizeCombo == null ? 100 : Math.max(1, (Integer) scanPageSizeCombo.getSelectedItem());
        if (!Objects.equals(activeScanRunId, runId)) {
            activeScanRunId = runId;
            expandedScanTicker = null;
            scanDetailCache.clear();
            clearScanDetails("Run changed. Expand a ticker row to load details.");
        }

        styleInlineStatus(scanResultsStatusLabel, "Scan results: loading...", COLOR_WARNING);
        SwingWorker<ScanService.ScanResultsPage, Void> worker = new SwingWorker<>() {
            @Override
            protected ScanService.ScanResultsPage doInBackground() throws Exception {
                return scanService.getScanResults(runId, page, pageSize, forceRefresh);
            }

            @Override
            protected void done() {
                try {
                    ScanService.ScanResultsPage pageData = get();
                    List<ScanResultRow> rows = buildScanResultRows(pageData.rows());
                    scanResultsTableModel.setRows(rows);
                    scanResultsTableModel.setExpandedTicker(expandedScanTicker);
                    scanResultsTotal = Math.max(pageData.total(), rows.size());
                    scanPrevPageButton.setEnabled(page > 1);
                    scanNextPageButton.setEnabled((long) page * pageSize < scanResultsTotal);
                    String statusText = "Scan results: loaded " + rows.size() + " (page " + page + ", total ~" + scanResultsTotal
                            + ")";
                    styleInlineStatus(scanResultsStatusLabel, statusText, COLOR_SUCCESS);
                    if (rows.isEmpty()) {
                        clearScanDetails("No scan results on this page yet.");
                    }
                } catch (Exception ex) {
                    styleInlineStatus(scanResultsStatusLabel, "Scan results: load failed", COLOR_DANGER);
                    clearScanDetails("Failed to load scan results: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private List<ScanResultRow> buildScanResultRows(List<Map<String, Object>> payloadRows) {
        List<ScanResultRow> rows = new ArrayList<>();
        for (Map<String, Object> payload : payloadRows) {
            String ticker = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "ticker", "symbol", "id")),
                    ""
            ).toUpperCase();
            if (ticker.isBlank()) {
                continue;
            }
            String rowStatus = firstNonBlank(stringOrEmpty(findAnyValue(payload, "status", "state")), "unknown");
            double score = Json.asDouble(findAnyValue(payload, "overall_score", "score", "rank_score"), Double.NaN);
            String regime = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "regime", "regime_label", "market_regime")),
                    "—"
            );
            String trend = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "trend_momentum", "trend", "momentum_label")),
                    "—"
            );
            String volIv = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "volatility_iv", "volatility_context", "iv_context", "iv_label")),
                    "—"
            );
            String topStrategy = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "top_strategy", "recommended_strategy", "strategy")),
                    "—"
            );
            if ("—".equals(topStrategy) && "failed".equalsIgnoreCase(rowStatus)) {
                topStrategy = "FAILED";
            }
            double confidence = Json.asDouble(findAnyValue(payload, "confidence", "probability", "conf"), Double.NaN);
            int warningsCount = Json.asInt(findAnyValue(payload, "warning_count", "warnings_count"), -1);
            if (warningsCount < 0) {
                Object warnings = firstNonNull(findAnyValue(payload, "warnings", "errors"), findAnyValue(payload, "error"));
                if (warnings instanceof List<?> warningList) {
                    warningsCount = warningList.size();
                } else if (warnings == null) {
                    warningsCount = 0;
                } else {
                    warningsCount = 1;
                }
            }
            if (warningsCount == 0 && "failed".equalsIgnoreCase(rowStatus)) {
                warningsCount = 1;
            }
            String updated = firstNonBlank(
                    stringOrEmpty(findAnyValue(payload, "updated_at", "last_updated", "timestamp", "as_of")),
                    "—"
            );
            rows.add(new ScanResultRow(
                    ticker,
                    rowStatus.toUpperCase(),
                    score,
                    regime,
                    trend,
                    volIv,
                    topStrategy,
                    confidence,
                    warningsCount,
                    updated,
                    payload
            ));
        }
        return rows;
    }

    private void refreshScanStatusOnce() {
        String runId = scanRunIdField == null ? "" : scanRunIdField.getText().trim();
        if (runId.isBlank()) {
            styleInlineStatus(scanResultsStatusLabel, "Scan results: enter runId first", COLOR_WARNING);
            return;
        }
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return scanService.getScanStatus(runId);
            }

            @Override
            protected void done() {
                try {
                    updateScanStatusWidgets(get());
                } catch (Exception ex) {
                    styleInlineStatus(scanResultsStatusLabel, "Scan status: fetch failed", COLOR_DANGER);
                    scanWarningsArea.setText(ts() + " | Status fetch failed: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void startScanStatusPolling(String runId) {
        if (runId == null || runId.isBlank()) {
            return;
        }
        if (activeScanStatusWorker != null && !activeScanStatusWorker.isDone()) {
            activeScanStatusWorker.cancel(true);
        }

        activeScanStatusWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int consecutiveErrors = 0;
                appendScanWarningLog("Polling scan status for run " + shortenRunId(runId) + "...");
                while (!isCancelled()) {
                    try {
                        Map<String, Object> status = scanService.getScanStatus(runId);
                        consecutiveErrors = 0;
                        publish(status);
                        String state = safeLower(stringOrEmpty(findAnyValue(status, "status", "state")));
                        if (state.equals("completed") || state.equals("failed") || state.equals("cancelled")) {
                            break;
                        }
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception ex) {
                        consecutiveErrors++;
                        if (consecutiveErrors == 1 || consecutiveErrors % 5 == 0) {
                            Map<String, Object> error = new LinkedHashMap<>();
                            error.put("status", "warning");
                            error.put(
                                    "warning",
                                    "Scan status polling issue (" + consecutiveErrors + "): "
                                            + humanizeError(ex)
                                            + ". Retrying..."
                            );
                            publish(error);
                        }

                        if (consecutiveErrors >= 30) {
                            Map<String, Object> error = new LinkedHashMap<>();
                            error.put("status", "error");
                            error.put(
                                    "error",
                                    "Scan status polling failed repeatedly (" + consecutiveErrors
                                            + "). Stop/restart backend if this persists."
                            );
                            publish(error);
                            break;
                        }

                        try {
                            Thread.sleep(Math.min(5000L, 800L + (long) consecutiveErrors * 200L));
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Map<String, Object>> chunks) {
                if (chunks == null || chunks.isEmpty()) {
                    return;
                }
                updateScanStatusWidgets(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (!isCancelled()) {
                    loadScanResults(true);
                    loadScanRuns(true);
                }
            }
        };
        activeScanStatusWorker.execute();
    }

    private void updateScanStatusWidgets(Map<String, Object> status) {
        if (status == null) {
            return;
        }
        String state = firstNonBlank(stringOrEmpty(findAnyValue(status, "status", "state")), "unknown");
        double progressRaw = Json.asDouble(findAnyValue(status, "progress", "progress_percent", "percent"), Double.NaN);
        int progress = 0;
        if (Double.isFinite(progressRaw)) {
            if (progressRaw <= 1.0) {
                progressRaw *= 100.0;
            }
            progress = (int) Math.max(0, Math.min(100, Math.round(progressRaw)));
        }
        String ticker = firstNonBlank(
                stringOrEmpty(findAnyValue(status, "current_ticker", "ticker", "current_symbol")),
                "—"
        );
        scanProgressBar.setValue(progress);
        scanProgressBar.setString(progress + "%");
        styleInlineStatus(scanProgressLabel, "Progress: " + progress + "% (" + state + ")", COLOR_MUTED);
        styleInlineStatus(scanCurrentTickerLabel, "Ticker: " + ticker, COLOR_MUTED);

        Color statusColor = COLOR_MUTED;
        String lower = safeLower(state);
        if (lower.contains("running") || lower.contains("queued")) {
            statusColor = COLOR_WARNING;
        } else if (lower.contains("completed")) {
            statusColor = COLOR_SUCCESS;
        } else if (lower.contains("failed") || lower.contains("error") || lower.contains("cancelled")) {
            statusColor = COLOR_DANGER;
        }
        styleInlineStatus(scanResultsStatusLabel, "Scan status: " + state, statusColor);

        StringBuilder warningText = new StringBuilder();
        Object warnings = firstNonNull(findAnyValue(status, "warnings", "warning_summary"), findAnyValue(status, "errors", "error"));
        Object errorsSummary = findAnyValue(status, "errorsSummary", "errors_summary");
        if (warnings != null) {
            if (warnings instanceof Map<?, ?> || warnings instanceof List<?>) {
                warningText.append(Json.pretty(warnings));
            } else {
                warningText.append(String.valueOf(warnings));
            }
            String normalized = warningText.toString().trim();
            if (!normalized.isBlank() && !normalized.equals(lastScanWarningSignature)) {
                lastScanWarningSignature = normalized;
                appendScanWarningLog("Backend warning/error update:\n" + normalized);
            }
        } else if (scanWarningsArea != null && scanWarningsArea.getText().isBlank()) {
            scanWarningsArea.setText("No warnings/errors yet.");
            scanWarningsArea.setCaretPosition(0);
        }

        if (errorsSummary instanceof Map<?, ?> errorsMapRaw) {
            Map<String, Object> errorsMap = Json.asObject(errorsMapRaw);
            int failedCount = Json.asInt(findAnyValue(status, "failedTickers", "failed_tickers"), 0);
            int errorCount = Json.asInt(findAnyValue(errorsMap, "count"), 0);
            Object tickersObj = findAnyValue(errorsMap, "tickers", "symbols");
            String tickerSnippet = "";
            if (tickersObj instanceof List<?> list && !list.isEmpty()) {
                List<String> labels = new ArrayList<>();
                for (Object item : list) {
                    if (item == null) {
                        continue;
                    }
                    labels.add(String.valueOf(item));
                    if (labels.size() >= 8) {
                        break;
                    }
                }
                if (!labels.isEmpty()) {
                    tickerSnippet = String.join(", ", labels);
                }
            }
            if ((errorCount > 0 || failedCount > 0) && !tickerSnippet.isBlank()) {
                appendScanWarningLog(
                        "Failure summary: failed_tickers=" + failedCount
                                + ", error_entries=" + errorCount
                                + ", sample=" + tickerSnippet
                );
            } else if (errorCount > 0 || failedCount > 0) {
                appendScanWarningLog(
                        "Failure summary: failed_tickers=" + failedCount
                                + ", error_entries=" + errorCount
                );
            }
        }

        // Per-ticker status logging
        Object tickerStatusObj = findAnyValue(status, "tickerStatus", "ticker_status");
        if (tickerStatusObj instanceof Map<?, ?> tickerStatusMap && !tickerStatusMap.isEmpty()) {
            int running = 0;
            int completed = 0;
            int failed = 0;
            int queued = 0;
            String runningTicker = null;
            String lastCompletedTicker = null;
            String lastFailedTicker = null;
            String lastError = null;

            for (Map.Entry<?, ?> entry : tickerStatusMap.entrySet()) {
                String tickerKey = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> tickerInfo) {
                    String tickerState = safeLower(stringOrEmpty(findAnyValue(Json.asObject(tickerInfo), "status", "state")));
                    switch (tickerState) {
                        case "running" -> {
                            running++;
                            runningTicker = tickerKey;
                        }
                        case "completed" -> {
                            completed++;
                            lastCompletedTicker = tickerKey;
                        }
                        case "failed" -> {
                            failed++;
                            lastFailedTicker = tickerKey;
                            Object err = findAnyValue(Json.asObject(tickerInfo), "error", "message");
                            if (err != null) {
                                lastError = String.valueOf(err);
                            }
                        }
                        default -> queued++;
                    }
                }
            }

            // Build per-ticker status line
            StringBuilder tickerLine = new StringBuilder();
            tickerLine.append("Ticker status: ");
            tickerLine.append("completed=").append(completed);
            tickerLine.append(", running=").append(running);
            tickerLine.append(", queued=").append(queued);
            tickerLine.append(", failed=").append(failed);
            if (runningTicker != null) {
                tickerLine.append(" | processing: ").append(runningTicker);
            }
            if (lastCompletedTicker != null) {
                tickerLine.append(" | last ok: ").append(lastCompletedTicker);
            }

            String tickerSignature = tickerLine.toString();
            if (!tickerSignature.equals(lastTickerStatusSignature)) {
                lastTickerStatusSignature = tickerSignature;
                appendScanWarningLog(tickerLine.toString());
            }

            // Log failures with error details
            if (lastFailedTicker != null && lastError != null) {
                String failSig = lastFailedTicker + ":" + lastError;
                if (!failSig.equals(lastTickerFailureSignature)) {
                    lastTickerFailureSignature = failSig;
                    String shortError = lastError.length() > 100 ? lastError.substring(0, 100) + "..." : lastError;
                    appendScanWarningLog("FAILED " + lastFailedTicker + ": " + shortError);
                }
            }
        }
    }

    private void toggleScanDetails(int modelRow) {
        if (scanResultsTableModel == null) {
            return;
        }
        ScanResultRow row = scanResultsTableModel.getRow(modelRow);
        if (row == null) {
            return;
        }
        String ticker = row.ticker();
        if (ticker.equals(expandedScanTicker)) {
            expandedScanTicker = null;
            scanResultsTableModel.setExpandedTicker(null);
            clearScanDetails("Row collapsed. Expand any row to load details.");
            return;
        }
        expandedScanTicker = ticker;
        scanResultsTableModel.setExpandedTicker(ticker);
        loadScanDetail(false);
    }

    private void retrySelectedScanDetail() {
        if (expandedScanTicker == null || expandedScanTicker.isBlank()) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Expand a ticker row first.",
                    "Retry Detail Fetch",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        loadScanDetail(true);
    }

    private void loadScanDetail(boolean forceRefresh) {
        String runId = activeScanRunId == null ? "" : activeScanRunId.trim();
        String ticker = expandedScanTicker == null ? "" : expandedScanTicker.trim();
        if (runId.isBlank() || ticker.isBlank()) {
            clearScanDetails("Select a ticker row to load details.");
            return;
        }
        String cacheKey = runId + "::" + ticker;
        if (!forceRefresh && scanDetailCache.containsKey(cacheKey)) {
            renderScanDetail(runId, ticker, scanDetailCache.get(cacheKey));
            return;
        }

        clearScanDetails("Loading detail for " + ticker + "...");
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return scanService.getScanResult(runId, ticker, forceRefresh);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> payload = get();
                    scanDetailCache.put(cacheKey, payload);
                    renderScanDetail(runId, ticker, payload);
                } catch (Exception ex) {
                    clearScanDetails("Failed to load detail for " + ticker + ": " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderScanDetail(String runId, String ticker, Map<String, Object> payload) {
        Object executive = findAnyValue(payload, "executive_summary", "summary", "overview");
        Object market = findAnyValue(payload, "market_context", "regime");
        Object priceVolume = findAnyValue(payload, "price_volume_signals", "price_volume_analysis", "signals");
        Object options = findAnyValue(payload, "option_candidates", "options", "strategies");
        Object risk = findAnyValue(payload, "risk_notes", "risk_summary", "risk");
        Object explain = findAnyValue(payload, "why_this", "traceability", "explainability");

        scanExecutiveSummaryArea.setText(formatScanSection("Executive Summary", executive));
        scanMarketContextArea.setText(formatScanSection("Market Context", market));
        scanPriceVolumeArea.setText(formatScanSection("Price/Volume Signals", priceVolume));
        scanOptionsArea.setText(formatScanSection("Option Trade Candidates", options));
        scanRiskArea.setText(formatScanSection("Risk Notes", risk));
        scanExplainabilityArea.setText(formatScanSection("Why This", explain));

        StringBuilder artifacts = new StringBuilder();
        artifacts.append("Run ID: ").append(runId).append("\n");
        artifacts.append("Ticker: ").append(ticker).append("\n\n");
        artifacts.append("Raw Payload:\n");
        artifacts.append(Json.pretty(payload));
        scanArtifactsArea.setText(artifacts.toString());
        scanArtifactsArea.setCaretPosition(0);
        scanExecutiveSummaryArea.setCaretPosition(0);
        scanMarketContextArea.setCaretPosition(0);
        scanPriceVolumeArea.setCaretPosition(0);
        scanOptionsArea.setCaretPosition(0);
        scanRiskArea.setCaretPosition(0);
        scanExplainabilityArea.setCaretPosition(0);

        Object warnings = firstNonNull(findAnyValue(payload, "warnings"), findAnyValue(payload, "errors"));
        if (warnings != null) {
            scanWarningsArea.setText(formatScanSection("Warnings", warnings));
            scanWarningsArea.setCaretPosition(0);
        }
    }

    private String formatScanSection(String title, Object payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(":\n");
        if (payload == null) {
            sb.append("- section data is not available in the backend payload.");
            return sb.toString();
        }
        if (payload instanceof Map<?, ?> || payload instanceof List<?>) {
            sb.append(Json.pretty(payload));
            return sb.toString();
        }
        sb.append(String.valueOf(payload));
        return sb.toString();
    }

    private void clearScanDetails(String message) {
        String text = message == null || message.isBlank() ? "No expanded row selected." : message;
        if (scanExecutiveSummaryArea != null) {
            scanExecutiveSummaryArea.setText(formatScanPlaceholder("Executive Summary", text));
            scanExecutiveSummaryArea.setCaretPosition(0);
        }
        if (scanMarketContextArea != null) {
            scanMarketContextArea.setText(formatScanPlaceholder("Market Context", text));
            scanMarketContextArea.setCaretPosition(0);
        }
        if (scanPriceVolumeArea != null) {
            scanPriceVolumeArea.setText(formatScanPlaceholder("Price/Volume Signals", text));
            scanPriceVolumeArea.setCaretPosition(0);
        }
        if (scanOptionsArea != null) {
            scanOptionsArea.setText(formatScanPlaceholder("Option Trade Candidates", text));
            scanOptionsArea.setCaretPosition(0);
        }
        if (scanRiskArea != null) {
            scanRiskArea.setText(formatScanPlaceholder("Risk Notes", text));
            scanRiskArea.setCaretPosition(0);
        }
        if (scanExplainabilityArea != null) {
            scanExplainabilityArea.setText(formatScanPlaceholder("Why This", text));
            scanExplainabilityArea.setCaretPosition(0);
        }
        if (scanArtifactsArea != null) {
            scanArtifactsArea.setText(formatScanPlaceholder("Artifacts", text));
            scanArtifactsArea.setCaretPosition(0);
        }
    }

    private String formatScanPlaceholder(String section, String message) {
        String status = message == null || message.isBlank() ? "No expanded row selected." : message;
        return section + ":\n" + status;
    }

    private void appendScanWarningLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        Runnable update = () -> {
            if (scanWarningsArea == null) {
                return;
            }
            String existing = scanWarningsArea.getText();
            if (existing == null) {
                existing = "";
            }
            if (existing.equalsIgnoreCase("No warnings/errors.")
                    || existing.equalsIgnoreCase("No warnings/errors yet.")) {
                existing = "";
            }

            String line = ts() + " | " + message;
            String next = existing.isBlank() ? line : existing + "\n" + line;
            int maxChars = 30_000;
            if (next.length() > maxChars) {
                next = next.substring(next.length() - maxChars);
            }
            scanWarningsArea.setText(next);
            scanWarningsArea.setCaretPosition(scanWarningsArea.getDocument().getLength());
        };

        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    private void loadScanRuns(boolean forceRefresh) {
        configureClientFromUI();
        if (!apiClient.healthCheck(2)) {
            styleInlineStatus(
                    scanRunsStatusLabel,
                    "Scan runs: backend offline at " + currentHostPort(),
                    COLOR_DANGER
            );
            if (scanRunsTableModel != null && scanRunsTableModel.getRowCount() == 0) {
                scanRunsTableModel.setRows(new ArrayList<>());
            }
            return;
        }

        styleInlineStatus(scanRunsStatusLabel, "Scan runs: loading...", COLOR_WARNING);
        SwingWorker<List<ScanRunRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ScanRunRow> doInBackground() throws Exception {
                List<Map<String, Object>> runs = scanService.listScanRuns(forceRefresh);
                List<ScanRunRow> rows = new ArrayList<>();
                for (Map<String, Object> run : runs) {
                    String runId = firstNonBlank(stringOrEmpty(findAnyValue(run, "run_id", "runId", "id")), "");
                    if (runId.isBlank()) {
                        continue;
                    }
                    String startedAt = firstNonBlank(
                            stringOrEmpty(findAnyValue(run, "created_at", "started_at", "start_time", "timestamp")),
                            "—"
                    );
                    String universeHash = firstNonBlank(
                            stringOrEmpty(findAnyValue(run, "universe_hash", "hash", "universeHash")),
                            "—"
                    );
                    String configSummary = firstNonBlank(
                            stringOrEmpty(findAnyValue(run, "config_summary", "horizon", "horizons")),
                            "—"
                    );
                    String status = firstNonBlank(stringOrEmpty(findAnyValue(run, "status", "state")), "unknown");
                    String topMetric = asMetricString(findAnyValue(run, "primary_score", "score", "sharpe", "f1"));
                    rows.add(new ScanRunRow(runId, startedAt, universeHash, configSummary, topMetric, status, run));
                }
                rows.sort((a, b) -> b.startedAt().compareTo(a.startedAt()));
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<ScanRunRow> rows = get();
                    scanRunsTableModel.setRows(rows);
                    styleInlineStatus(scanRunsStatusLabel, "Scan runs: loaded " + rows.size(), COLOR_SUCCESS);
                } catch (Exception ex) {
                    if (isConnectivityIssue(ex)) {
                        styleInlineStatus(
                                scanRunsStatusLabel,
                                "Scan runs: backend offline at " + currentHostPort(),
                                COLOR_DANGER
                        );
                        if (scanRunsTableModel != null && scanRunsTableModel.getRowCount() == 0) {
                            scanRunsTableModel.setRows(new ArrayList<>());
                        }
                        return;
                    }

                    styleInlineStatus(scanRunsStatusLabel, "Scan runs: load failed", COLOR_DANGER);
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to load scan runs:\n" + humanizeError(ex),
                            "Scan Runs",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private boolean isConnectivityIssue(Throwable error) {
        if (error == null) {
            return false;
        }
        Throwable cause = error;
        if (error instanceof Exception ex && ex.getCause() != null) {
            cause = ex.getCause();
        }
        String className = cause.getClass().getSimpleName();
        String message = cause.getMessage() == null ? "" : cause.getMessage();
        String full = (className + " " + message).toLowerCase();
        return full.contains("connectexception")
                || full.contains("connection refused")
                || full.contains("failed to connect")
                || full.contains("couldn't connect")
                || full.contains("timed out")
                || full.contains("no route to host")
                || full.contains("host is down");
    }

    private void openSelectedScanRun() {
        if (scanRunsTable == null || scanRunsTableModel == null) {
            return;
        }
        int viewRow = scanRunsTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Select a run first.",
                    "Open Scan Run",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        int modelRow = scanRunsTable.convertRowIndexToModel(viewRow);
        ScanRunRow row = scanRunsTableModel.getRow(modelRow);
        if (row == null) {
            return;
        }
        activeScanRunId = row.runId();
        scanRunIdField.setText(row.runId());
        if (universeMainTabs != null) {
            universeMainTabs.setSelectedIndex(1);
        }
        loadScanResults(true);
        startScanStatusPolling(row.runId());
    }

    private List<String> selectedTickersFromModel(UniverseTableModel model) {
        LinkedHashSet<String> tickers = new LinkedHashSet<>();
        if (model == null) {
            return new ArrayList<>();
        }
        for (UniverseRow row : model.selectedRows()) {
            if (row != null && row.ticker() != null) {
                String ticker = row.ticker().trim().toUpperCase();
                if (!ticker.isBlank()) {
                    tickers.add(ticker);
                }
            }
        }
        return new ArrayList<>(tickers);
    }

    private List<String> allTickersFromModel(UniverseTableModel model) {
        LinkedHashSet<String> tickers = new LinkedHashSet<>();
        if (model == null) {
            return new ArrayList<>();
        }
        for (UniverseRow row : model.rows()) {
            if (row != null && row.ticker() != null) {
                String ticker = row.ticker().trim().toUpperCase();
                if (!ticker.isBlank()) {
                    tickers.add(ticker);
                }
            }
        }
        return new ArrayList<>(tickers);
    }

    private List<String> parseCsvTokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }
        for (String part : text.split(",")) {
            String token = part.trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String activeUniverseId() {
        if (universeTabs == null) {
            return UNIVERSE_NASDAQ;
        }
        return switch (universeTabs.getSelectedIndex()) {
            case 1 -> UNIVERSE_WATCHLIST;
            case 2 -> UNIVERSE_COMBINED;
            default -> UNIVERSE_NASDAQ;
        };
    }

    private UniverseTableModel activeUniverseModel() {
        if (universeTabs == null) {
            return universeNasdaqTableModel;
        }
        return switch (universeTabs.getSelectedIndex()) {
            case 1 -> universeWatchlistTableModel;
            case 2 -> universeCombinedTableModel;
            default -> universeNasdaqTableModel;
        };
    }

    private JTable activeUniverseTable() {
        if (universeTabs == null) {
            return universeNasdaqTable;
        }
        return switch (universeTabs.getSelectedIndex()) {
            case 1 -> universeWatchlistTable;
            case 2 -> universeCombinedTable;
            default -> universeNasdaqTable;
        };
    }

    private TableRowSorter<UniverseTableModel> activeUniverseSorter() {
        if (universeTabs == null) {
            return universeNasdaqSorter;
        }
        return switch (universeTabs.getSelectedIndex()) {
            case 1 -> universeWatchlistSorter;
            case 2 -> universeCombinedSorter;
            default -> universeNasdaqSorter;
        };
    }

    private Map<String, Object> activeUniversePayload() {
        if (universeTabs == null) {
            return universeNasdaqPayload;
        }
        return switch (universeTabs.getSelectedIndex()) {
            case 1 -> universeWatchlistPayload;
            case 2 -> universeCombinedPayload;
            default -> universeNasdaqPayload;
        };
    }

    private Double asNullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        double parsed = Json.asDouble(value, Double.NaN);
        if (!Double.isFinite(parsed)) {
            return null;
        }
        return parsed;
    }

    private double parseSortableDouble(Object value) {
        if (value == null) {
            return Double.NaN;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = String.valueOf(value).replace(",", "").trim();
        if (text.isEmpty() || "—".equals(text)) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text);
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private int parseSortableInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "—".equals(text)) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(text);
        } catch (RuntimeException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private JPanel createDashboardPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        dashboardSymbolField = new JTextField("SPY", 8);
        dashboardHorizonSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        refreshDashboardButton = new JButton("Get Prediction + Setup");
        dashboardUpdatedLabel = new JLabel("Status: idle");
        styleInputField(dashboardSymbolField);
        styleSpinner(dashboardHorizonSpinner);
        styleButton(refreshDashboardButton, true);
        styleDashboardStatus(dashboardUpdatedLabel, "Status: idle", COLOR_MUTED);

        JLabel symbolLabel = new JLabel("Symbol");
        JLabel horizonLabel = new JLabel("Horizon (days)");
        symbolLabel.setForeground(COLOR_MUTED);
        horizonLabel.setForeground(COLOR_MUTED);
        symbolLabel.setFont(uiFont);
        horizonLabel.setFont(uiFont);

        controls.add(symbolLabel);
        controls.add(dashboardSymbolField);
        controls.add(horizonLabel);
        controls.add(dashboardHorizonSpinner);
        controls.add(refreshDashboardButton);
        controls.add(dashboardUpdatedLabel);

        JPanel controlsCard = createCardPanel();
        controlsCard.add(controls, BorderLayout.CENTER);

        JPanel summaryTiles = new JPanel(new GridBagLayout());
        summaryTiles.setOpaque(false);

        dashboardBiasValue = createMetricValueLabel();
        dashboardConfidenceValue = createMetricValueLabel();
        dashboardEntryValue = createMetricValueLabel();
        dashboardStopValue = createMetricValueLabel();
        dashboardTargetsValue = createMetricValueLabel();
        dashboardSizingValue = createMetricValueLabel();
        dashboardModelValue = createMetricValueLabel();

        addMetricTile(summaryTiles, 0, 0, "Bias", dashboardBiasValue);
        addMetricTile(summaryTiles, 0, 1, "Confidence", dashboardConfidenceValue);
        addMetricTile(summaryTiles, 1, 0, "Entry Zone", dashboardEntryValue);
        addMetricTile(summaryTiles, 1, 1, "Stop Loss", dashboardStopValue);
        addMetricTile(summaryTiles, 2, 0, "Targets", dashboardTargetsValue);
        addMetricTile(summaryTiles, 2, 1, "Suggested Size", dashboardSizingValue);
        addMetricTile(summaryTiles, 3, 0, "Model", dashboardModelValue);

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 1;
        spacer.gridy = 3;
        spacer.weightx = 1.0;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.BOTH;
        summaryTiles.add(new JPanel(), spacer);

        JPanel summaryCard = createCardPanel();
        summaryCard.add(createSectionHeader("Trade Summary"), BorderLayout.NORTH);
        summaryCard.add(summaryTiles, BorderLayout.CENTER);

        dashboardPlanArea = createDashboardInfoArea();
        dashboardPredictionArea = createDashboardInfoArea();
        dashboardInsightsArea = createDashboardInfoArea();

        JSplitPane topSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createLogScrollPane(dashboardPredictionArea, "Market Read"),
                createLogScrollPane(dashboardPlanArea, "Execution Checklist")
        );
        topSplit.setResizeWeight(0.45);
        topSplit.setDividerLocation(430);
        topSplit.setBackground(COLOR_BG);
        topSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                topSplit,
                createLogScrollPane(dashboardInsightsArea, "Why It Matters")
        );
        mainSplit.setResizeWeight(0.62);
        mainSplit.setDividerLocation(380);
        mainSplit.setBackground(COLOR_BG);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.add(summaryCard, BorderLayout.NORTH);
        bottom.add(mainSplit, BorderLayout.CENTER);

        root.add(controlsCard, BorderLayout.NORTH);
        root.add(bottom, BorderLayout.CENTER);

        resetDashboardState();
        return root;
    }

    private JPanel createPredictionInspectorPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        inspectorTickerField = new JTextField("SPY", 8);
        inspectorTimestampField = new JTextField(22);
        inspectorHorizonSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        inspectorRunIdField = new JTextField(20);
        inspectorFetchButton = new JButton("Inspect Prediction");
        inspectorStatusLabel = new JLabel("Status: idle");
        inspectorRegimeValue = createMetricValueLabel();
        inspectorDecisionValue = createMetricValueLabel();
        inspectorConfidenceValue = createMetricValueLabel();

        styleInputField(inspectorTickerField);
        styleInputField(inspectorTimestampField);
        styleSpinner(inspectorHorizonSpinner);
        styleInputField(inspectorRunIdField);
        styleButton(inspectorFetchButton, true);
        styleDashboardStatus(inspectorStatusLabel, "Status: idle", COLOR_MUTED);

        inspectorTimestampField.setToolTipText(
                "Optional timestamp. Use ISO format, e.g. 2026-02-09T14:30:00Z. Leave blank for latest.");
        inspectorRunIdField.setToolTipText("Optional run ID. Leave blank to use latest model.");

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(createFormLabel("Ticker"));
        controls.add(inspectorTickerField);
        controls.add(createFormLabel("Timestamp"));
        controls.add(inspectorTimestampField);
        controls.add(createFormLabel("Horizon"));
        controls.add(inspectorHorizonSpinner);
        controls.add(createFormLabel("Run ID"));
        controls.add(inspectorRunIdField);
        controls.add(inspectorFetchButton);
        controls.add(inspectorStatusLabel);

        JPanel controlsCard = createCardPanel();
        controlsCard.add(createSectionHeader("Prediction Inspector Controls"), BorderLayout.NORTH);
        controlsCard.add(controls, BorderLayout.CENTER);

        JPanel summaryTiles = new JPanel(new GridBagLayout());
        summaryTiles.setOpaque(false);
        addMetricTile(summaryTiles, 0, 0, "Regime", inspectorRegimeValue);
        addMetricTile(summaryTiles, 0, 1, "Decision", inspectorDecisionValue);
        addMetricTile(summaryTiles, 1, 0, "Confidence", inspectorConfidenceValue);
        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 1;
        spacer.gridy = 1;
        spacer.weightx = 1.0;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.BOTH;
        summaryTiles.add(new JPanel(), spacer);

        JPanel summaryCard = createCardPanel();
        summaryCard.add(createSectionHeader("As-Of Decision Snapshot"), BorderLayout.NORTH);
        summaryCard.add(summaryTiles, BorderLayout.CENTER);

        inspectorInputArea = createDashboardInfoArea();
        inspectorFeaturesArea = createDashboardInfoArea();
        inspectorDecisionArea = createDashboardInfoArea();
        inspectorWarningsArea = createDashboardInfoArea();
        inspectorRawTraceArea = createDashboardInfoArea();

        JSplitPane topSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createLogScrollPane(inspectorInputArea, "Raw Input Snapshot"),
                createLogScrollPane(inspectorFeaturesArea, "Computed Feature Vector")
        );
        topSplit.setResizeWeight(0.5);
        topSplit.setDividerLocation(520);
        topSplit.setBackground(COLOR_BG);
        topSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane middleSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createLogScrollPane(inspectorDecisionArea, "Model Output + Decision"),
                createLogScrollPane(inspectorWarningsArea, "Warnings + Fallbacks")
        );
        middleSplit.setResizeWeight(0.65);
        middleSplit.setDividerLocation(680);
        middleSplit.setBackground(COLOR_BG);
        middleSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane lowerSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                middleSplit,
                createLogScrollPane(inspectorRawTraceArea, "Full Trace (Raw JSON)")
        );
        lowerSplit.setResizeWeight(0.5);
        lowerSplit.setDividerLocation(280);
        lowerSplit.setBackground(COLOR_BG);
        lowerSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, lowerSplit);
        mainSplit.setResizeWeight(0.48);
        mainSplit.setDividerLocation(330);
        mainSplit.setBackground(COLOR_BG);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);
        center.add(summaryCard, BorderLayout.NORTH);
        center.add(mainSplit, BorderLayout.CENTER);

        root.add(controlsCard, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        resetPredictionInspectorState();
        return root;
    }

    private JPanel createTrainingRunsPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel filtersBody = new JPanel(new GridBagLayout());
        filtersBody.setOpaque(false);

        runsTickerFilterField = new JTextField(12);
        runsModelFilterField = new JTextField(10);
        runsHorizonFilterField = new JTextField(6);
        runsDateFromField = new JTextField(10);
        runsDateToField = new JTextField(10);
        runsStatusFilterCombo = new JComboBox<>(new String[]{
                "All", "completed", "failed", "running", "queued", "cancelled"
        });
        runsRefreshButton = new JButton("Refresh Runs");
        runsClearFiltersButton = new JButton("Clear Filters");
        runsCompareButton = new JButton("Compare Selected");
        runsTableStatusLabel = new JLabel();

        styleInputField(runsTickerFilterField);
        styleInputField(runsModelFilterField);
        styleInputField(runsHorizonFilterField);
        styleInputField(runsDateFromField);
        styleInputField(runsDateToField);
        styleCombo(runsStatusFilterCombo);
        styleButton(runsRefreshButton, true);
        styleButton(runsClearFiltersButton, false);
        styleButton(runsCompareButton, false);
        styleInlineStatus(runsTableStatusLabel, "Runs: idle", COLOR_MUTED);

        runsTickerFilterField.setToolTipText("Ticker filter. Supports comma-separated values, e.g. SPY,QQQ");
        runsDateFromField.setToolTipText("From date (YYYY-MM-DD)");
        runsDateToField.setToolTipText("To date (YYYY-MM-DD)");

        GridBagConstraints f = new GridBagConstraints();
        f.insets = new Insets(0, 0, 8, 8);
        f.anchor = GridBagConstraints.WEST;
        f.fill = GridBagConstraints.HORIZONTAL;

        int col = 0;
        f.gridx = col++;
        f.gridy = 0;
        filtersBody.add(createFormLabel("Tickers"), f);
        f.gridx = col++;
        filtersBody.add(runsTickerFilterField, f);
        f.gridx = col++;
        filtersBody.add(createFormLabel("Model"), f);
        f.gridx = col++;
        filtersBody.add(runsModelFilterField, f);
        f.gridx = col++;
        filtersBody.add(createFormLabel("Horizon"), f);
        f.gridx = col++;
        filtersBody.add(runsHorizonFilterField, f);
        f.gridx = col++;
        filtersBody.add(createFormLabel("Status"), f);
        f.gridx = col++;
        filtersBody.add(runsStatusFilterCombo, f);

        col = 0;
        f.gridx = col++;
        f.gridy = 1;
        filtersBody.add(createFormLabel("Date From"), f);
        f.gridx = col++;
        filtersBody.add(runsDateFromField, f);
        f.gridx = col++;
        filtersBody.add(createFormLabel("Date To"), f);
        f.gridx = col++;
        filtersBody.add(runsDateToField, f);
        f.gridx = col++;
        filtersBody.add(runsRefreshButton, f);
        f.gridx = col++;
        filtersBody.add(runsClearFiltersButton, f);
        f.gridx = col++;
        filtersBody.add(runsCompareButton, f);
        f.gridx = col++;
        f.gridwidth = 2;
        f.weightx = 1.0;
        filtersBody.add(runsTableStatusLabel, f);

        JPanel filtersCard = createCardPanel();
        filtersCard.add(createSectionHeader("Training Runs Filters"), BorderLayout.NORTH);
        filtersCard.add(filtersBody, BorderLayout.CENTER);

        runsTableModel = new RunsTableModel();
        runsTable = new JTable(runsTableModel);
        styleRunsTable(runsTable);
        runsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        runsTableSorter = new TableRowSorter<>(runsTableModel);
        runsTable.setRowSorter(runsTableSorter);
        runsTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            onRunSelectionChanged();
        });

        JScrollPane runsTableScroll = new JScrollPane(runsTable);
        runsTableScroll.getViewport().setBackground(COLOR_LOG_BG);
        runsTableScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        runsTableScroll.setColumnHeaderView(createSectionHeader("Runs"));

        JPanel detailsPanel = createRunDetailsPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, runsTableScroll, detailsPanel);
        split.setResizeWeight(0.46);
        split.setDividerLocation(560);
        split.setBackground(COLOR_BG);
        split.setBorder(BorderFactory.createEmptyBorder());

        runsRefreshButton.addActionListener(e -> loadRuns(true));
        runsClearFiltersButton.addActionListener(e -> clearRunsFilters());
        runsCompareButton.addActionListener(e -> compareSelectedRuns());
        runsStatusFilterCombo.addActionListener(e -> applyRunsFilters());
        attachRunsFilterListener(runsTickerFilterField);
        attachRunsFilterListener(runsModelFilterField);
        attachRunsFilterListener(runsHorizonFilterField);
        attachRunsFilterListener(runsDateFromField);
        attachRunsFilterListener(runsDateToField);

        root.add(filtersCard, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        resetRunDetailsState("Select a training run from the left list.");
        return root;
    }

    private JPanel createRunDetailsPanel() {
        JPanel wrap = createCardPanel();
        runDetailsHeaderLabel = new JLabel("Run Details");
        runDetailsHeaderLabel.setForeground(COLOR_TEXT);
        runDetailsHeaderLabel.setFont(uiFont.deriveFont(Font.BOLD, 14f));
        runDetailsHeaderLabel.setBorder(new EmptyBorder(2, 2, 8, 2));
        runReadinessBannerLabel = new JLabel();
        styleInlineStatus(runReadinessBannerLabel, "Guardrails: idle", COLOR_MUTED);

        JTabbedPane tabs = new JTabbedPane();
        styleTabbedPane(tabs);

        runOverviewArea = createRunDetailsArea();
        JPanel dataTransparencyPanel = createDataTransparencyPanel();
        JPanel featureTransparencyPanel = createFeatureTransparencyPanel();
        JPanel modelTransparencyPanel = createModelTransparencyPanel();
        JPanel backtestTransparencyPanel = createBacktestTransparencyPanel();
        runSplitsArea = createRunDetailsArea();
        runCalibrationArea = createRunDetailsArea();
        runMetricsArea = createRunDetailsArea();
        runDiagnosticsArea = createRunDetailsArea();
        JPanel readinessPanel = createModelReadinessPanel();
        JPanel auditPanel = createAuditLogPanel();

        tabs.addTab("Overview", createLogScrollPane(runOverviewArea, "Overview"));
        tabs.addTab("Data", dataTransparencyPanel);
        tabs.addTab("Features", featureTransparencyPanel);
        tabs.addTab("Splits", createLogScrollPane(runSplitsArea, "Splits"));
        tabs.addTab("Model", modelTransparencyPanel);
        tabs.addTab("Calibration", createLogScrollPane(runCalibrationArea, "Calibration"));
        tabs.addTab("Metrics", createLogScrollPane(runMetricsArea, "Metrics"));
        tabs.addTab("Backtest", backtestTransparencyPanel);
        tabs.addTab("Diagnostics", createLogScrollPane(runDiagnosticsArea, "Diagnostics"));
        tabs.addTab("Readiness", readinessPanel);
        tabs.addTab("Audit Logs", auditPanel);

        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        title.add(runDetailsHeaderLabel, BorderLayout.WEST);
        title.add(runReadinessBannerLabel, BorderLayout.EAST);
        wrap.add(title, BorderLayout.NORTH);
        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    private JTextArea createRunDetailsArea() {
        JTextArea area = createLogArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JPanel createModelReadinessPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        runReproScoreLabel = new JLabel();
        runReadinessStatusLabel = new JLabel();
        runReadinessActionStatusLabel = new JLabel();
        styleInlineStatus(runReproScoreLabel, "Reproducibility Score: —", COLOR_MUTED);
        styleInlineStatus(runReadinessStatusLabel, "Checklist: idle", COLOR_MUTED);
        styleInlineStatus(runReadinessActionStatusLabel, "Actions: idle", COLOR_MUTED);

        runPublishModelButton = new JButton("Publish Model");
        runUseForTradingButton = new JButton("Use For Trading");
        styleButton(runPublishModelButton, false);
        styleButton(runUseForTradingButton, false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(runPublishModelButton);
        actions.add(runUseForTradingButton);
        actions.add(runReadinessActionStatusLabel);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        statusRow.add(runReproScoreLabel);
        statusRow.add(runReadinessStatusLabel);

        JPanel header = createCardPanel();
        header.add(createSectionHeader("Model Readiness Checklist"), BorderLayout.NORTH);
        JPanel headerBody = new JPanel(new BorderLayout(0, 8));
        headerBody.setOpaque(false);
        headerBody.add(statusRow, BorderLayout.NORTH);
        headerBody.add(actions, BorderLayout.CENTER);
        header.add(headerBody, BorderLayout.CENTER);

        runReadinessChecklistArea = createRunDetailsArea();
        runReadinessFindingsArea = createRunDetailsArea();
        JScrollPane checklistScroll = createLogScrollPane(runReadinessChecklistArea, "Checklist");
        JScrollPane findingsScroll = createLogScrollPane(runReadinessFindingsArea, "Guardrail Findings");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, checklistScroll, findingsScroll);
        split.setResizeWeight(0.5);
        split.setDividerLocation(250);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(COLOR_BG);

        runPublishModelButton.addActionListener(e -> executeReadinessAction("Publish model"));
        runUseForTradingButton.addActionListener(e -> executeReadinessAction("Use for trading"));
        updateReadinessActionButtons();

        root.add(header, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel createAuditLogPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        auditLogRefreshButton = new JButton("Refresh");
        auditLogExportButton = new JButton("Export JSONL");
        auditLogStatusLabel = new JLabel();
        styleButton(auditLogRefreshButton, false);
        styleButton(auditLogExportButton, false);
        styleInlineStatus(auditLogStatusLabel, "Audit: idle", COLOR_MUTED);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.add(auditLogRefreshButton);
        actionRow.add(auditLogExportButton);
        actionRow.add(auditLogStatusLabel);

        auditLogViewerArea = createLogArea();
        auditLogViewerArea.setLineWrap(false);
        JScrollPane logScroll = createLogScrollPane(auditLogViewerArea, "audit_log.jsonl");

        auditLogRefreshButton.addActionListener(e -> refreshAuditLogPanel());
        auditLogExportButton.addActionListener(e -> exportAuditLogSnapshot());

        root.add(actionRow, BorderLayout.NORTH);
        root.add(logScroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel createDataTransparencyPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        runDataPrettyPrintButton = new JButton("Pretty JSON: ON");
        runDataExportButton = new JButton("Download Dataset Manifest");
        runDataManifestStatusLabel = new JLabel();
        styleButton(runDataPrettyPrintButton, false);
        styleButton(runDataExportButton, false);
        styleInlineStatus(runDataManifestStatusLabel, "Manifest: idle", COLOR_MUTED);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.add(runDataPrettyPrintButton);
        actionRow.add(runDataExportButton);
        actionRow.add(runDataManifestStatusLabel);

        runDataArea = createRunDetailsArea();
        runDataTickerTableModel = new DataTickerStatsTableModel();
        runDataTickerTable = new JTable(runDataTickerTableModel);
        styleRunsTable(runDataTickerTable);

        runDataRawJsonArea = createLogArea();
        runDataRawJsonArea.setLineWrap(true);
        runDataRawJsonArea.setWrapStyleWord(true);

        JScrollPane summaryScroll = createLogScrollPane(runDataArea, "Data Transparency Summary");
        JScrollPane tickerScroll = new JScrollPane(runDataTickerTable);
        tickerScroll.getViewport().setBackground(COLOR_LOG_BG);
        tickerScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        tickerScroll.setColumnHeaderView(createSectionHeader("Per-Ticker Stats"));

        JScrollPane rawJsonScroll = createLogScrollPane(runDataRawJsonArea, "Raw JSON Viewer");

        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summaryScroll, tickerScroll);
        topSplit.setResizeWeight(0.55);
        topSplit.setDividerLocation(260);
        topSplit.setBorder(BorderFactory.createEmptyBorder());
        topSplit.setBackground(COLOR_BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, rawJsonScroll);
        mainSplit.setResizeWeight(0.65);
        mainSplit.setDividerLocation(460);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setBackground(COLOR_BG);

        runDataPrettyPrintButton.addActionListener(e -> toggleDataRawPrettyPrint());
        runDataExportButton.addActionListener(e -> exportDatasetManifest());

        root.add(actionRow, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        return root;
    }

    private JPanel createFeatureTransparencyPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        runFeaturesSearchField = new JTextField(16);
        runFeaturesCategoryFilterCombo = new JComboBox<>(new String[]{
                "All", "technical", "macro", "fundamental", "sentiment", "regime", "other"
        });
        runFeaturesKeptFilterCombo = new JComboBox<>(new String[]{"All", "kept", "dropped"});
        runFeaturesClearFiltersButton = new JButton("Clear Filters");
        runFeaturesDetailsButton = new JButton("Feature Details");
        runFeaturesStatusLabel = new JLabel();
        styleInputField(runFeaturesSearchField);
        styleCombo(runFeaturesCategoryFilterCombo);
        styleCombo(runFeaturesKeptFilterCombo);
        styleButton(runFeaturesClearFiltersButton, false);
        styleButton(runFeaturesDetailsButton, false);
        styleInlineStatus(runFeaturesStatusLabel, "Features: idle", COLOR_MUTED);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(createFormLabel("Search"));
        filters.add(runFeaturesSearchField);
        filters.add(createFormLabel("Category"));
        filters.add(runFeaturesCategoryFilterCombo);
        filters.add(createFormLabel("Status"));
        filters.add(runFeaturesKeptFilterCombo);
        filters.add(runFeaturesClearFiltersButton);
        filters.add(runFeaturesDetailsButton);
        filters.add(runFeaturesStatusLabel);

        runFeaturesTableModel = new FeaturesTableModel();
        runFeaturesTable = new JTable(runFeaturesTableModel);
        styleRunsTable(runFeaturesTable);
        runFeaturesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        runFeaturesTableSorter = new TableRowSorter<>(runFeaturesTableModel);
        runFeaturesTable.setRowSorter(runFeaturesTableSorter);

        runTopFeaturesTableModel = new TopFeaturesTableModel();
        runTopFeaturesTable = new JTable(runTopFeaturesTableModel);
        styleRunsTable(runTopFeaturesTable);
        runTopFeaturesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        runFeaturesSummaryArea = createRunDetailsArea();
        runFeaturesSummaryArea.setText("Feature transparency will load with run details.");

        JScrollPane featureTableScroll = new JScrollPane(runFeaturesTable);
        featureTableScroll.getViewport().setBackground(COLOR_LOG_BG);
        featureTableScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        featureTableScroll.setColumnHeaderView(createSectionHeader("Feature Library"));

        JScrollPane topFeaturesScroll = new JScrollPane(runTopFeaturesTable);
        topFeaturesScroll.getViewport().setBackground(COLOR_LOG_BG);
        topFeaturesScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        topFeaturesScroll.setColumnHeaderView(createSectionHeader("Top Features (Importance + Stability)"));

        JScrollPane summaryScroll = createLogScrollPane(runFeaturesSummaryArea, "Feature Quality Summary");

        JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topFeaturesScroll, summaryScroll);
        lowerSplit.setResizeWeight(0.55);
        lowerSplit.setDividerLocation(220);
        lowerSplit.setBorder(BorderFactory.createEmptyBorder());
        lowerSplit.setBackground(COLOR_BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, featureTableScroll, lowerSplit);
        mainSplit.setResizeWeight(0.56);
        mainSplit.setDividerLocation(300);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setBackground(COLOR_BG);

        runFeaturesCategoryFilterCombo.addActionListener(e -> applyFeatureFilters());
        runFeaturesKeptFilterCombo.addActionListener(e -> applyFeatureFilters());
        runFeaturesClearFiltersButton.addActionListener(e -> clearFeatureFilters());
        runFeaturesDetailsButton.addActionListener(e -> openSelectedFeatureDetailsModal());
        runFeaturesSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFeatureFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFeatureFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFeatureFilters();
            }
        });
        runFeaturesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    openSelectedFeatureDetailsModal();
                }
            }
        });

        root.add(filters, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        return root;
    }

    private void clearFeatureFilters() {
        if (runFeaturesSearchField == null) {
            return;
        }
        runFeaturesSearchField.setText("");
        runFeaturesCategoryFilterCombo.setSelectedItem("All");
        runFeaturesKeptFilterCombo.setSelectedItem("All");
        applyFeatureFilters();
    }

    private void applyFeatureFilters() {
        if (runFeaturesTableSorter == null || runFeaturesTableModel == null) {
            return;
        }
        String search = runFeaturesSearchField.getText().trim().toLowerCase();
        String category = String.valueOf(runFeaturesCategoryFilterCombo.getSelectedItem()).toLowerCase();
        String keptFilter = String.valueOf(runFeaturesKeptFilterCombo.getSelectedItem()).toLowerCase();

        runFeaturesTableSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends FeaturesTableModel, ? extends Integer> entry) {
                FeatureRow row = runFeaturesTableModel.getRow(entry.getIdentifier());
                if (row == null) {
                    return false;
                }
                if (!search.isBlank()) {
                    String haystack = (
                            safeLower(row.featureName()) + " "
                                    + safeLower(row.category()) + " "
                                    + safeLower(row.parameters()) + " "
                                    + safeLower(row.timeframe()) + " "
                                    + safeLower(row.scaling()) + " "
                                    + safeLower(row.statusReason()) + " "
                                    + safeLower(row.dependencies())
                    );
                    if (!haystack.contains(search)) {
                        return false;
                    }
                }
                if (!"all".equals(category) && !safeLower(row.category()).contains(category)) {
                    return false;
                }
                if (!"all".equals(keptFilter)) {
                    boolean kept = row.kept();
                    if ("kept".equals(keptFilter) && !kept) {
                        return false;
                    }
                    if ("dropped".equals(keptFilter) && kept) {
                        return false;
                    }
                }
                return true;
            }
        });

        int shown = runFeaturesTable.getRowCount();
        int total = runFeaturesTableModel.getRowCount();
        styleInlineStatus(runFeaturesStatusLabel, "Features: " + shown + " / " + total + " visible", COLOR_MUTED);
    }

    private void openSelectedFeatureDetailsModal() {
        if (runFeaturesTable == null || runFeaturesTableModel == null) {
            return;
        }
        int viewRow = runFeaturesTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Select a feature row first.",
                    "Feature Details",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        int modelRow = runFeaturesTable.convertRowIndexToModel(viewRow);
        FeatureRow row = runFeaturesTableModel.getRow(modelRow);
        if (row == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Feature: ").append(row.featureName()).append("\n");
        sb.append("Category: ").append(row.category()).append("\n");
        sb.append("Parameters: ").append(row.parameters()).append("\n");
        sb.append("Timeframe: ").append(row.timeframe()).append("\n");
        sb.append("Scaling: ").append(row.scaling()).append("\n");
        sb.append("Missing %: ").append(row.missingPct()).append("\n");
        sb.append("Zero variance: ").append(row.zeroVariance()).append("\n");
        sb.append("Correlation cluster: ").append(row.correlationCluster()).append("\n");
        sb.append("Status: ").append(row.kept() ? "kept" : "dropped").append("\n");
        sb.append("Reason: ").append(row.statusReason()).append("\n");
        sb.append("As-of join lag: ").append(row.joinLag()).append("\n");
        sb.append("\nLineage Graph\n");
        sb.append(buildFeatureLineageGraph(row));
        if (row.raw() != null) {
            sb.append("\nRaw payload:\n").append(Json.pretty(row.raw()));
        }

        JTextArea detailsArea = new JTextArea(sb.toString());
        detailsArea.setEditable(false);
        detailsArea.setFont(monoFont);
        detailsArea.setBackground(COLOR_LOG_BG);
        detailsArea.setForeground(COLOR_TEXT);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(detailsArea);
        scroll.setPreferredSize(new Dimension(760, 560));

        JOptionPane.showMessageDialog(frame, scroll, "Feature Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildFeatureLineageGraph(FeatureRow row) {
        StringBuilder graph = new StringBuilder();
        String feature = row.featureName().isBlank() ? "(feature)" : row.featureName();
        graph.append(feature).append("\n");

        List<String> deps = splitDependencies(row.dependencies());
        if (deps.isEmpty()) {
            graph.append("  +-- upstream: not reported\n");
        } else {
            for (String dep : deps) {
                graph.append("  +-- ").append(dep).append("\n");
            }
        }
        if (!row.joinLag().isBlank() && !"—".equals(row.joinLag())) {
            graph.append("  +-- as_of_join_lag: ").append(row.joinLag()).append("\n");
        }
        return graph.toString();
    }

    private List<String> splitDependencies(String depsText) {
        List<String> deps = new ArrayList<>();
        if (depsText == null || depsText.isBlank() || "—".equals(depsText)) {
            return deps;
        }
        String normalized = depsText.replace(";", ",").replace("|", ",");
        for (String token : normalized.split(",")) {
            String dep = token.trim();
            if (!dep.isBlank()) {
                deps.add(dep);
            }
        }
        return deps;
    }

    private JPanel createModelTransparencyPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        runModelFamilyValue = createModelValueLabel();
        runModelCalibrationMethodValue = createModelValueLabel();
        runModelCalibrationMetricsValue = createModelValueLabel();
        runModelConfidenceTypeValue = createModelValueLabel();
        runModelThresholdLogicValue = createModelValueLabel();
        runModelStatusLabel = new JLabel();
        styleInlineStatus(runModelStatusLabel, "Model: idle", COLOR_MUTED);

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setOpaque(false);
        addModelMetricRow(stats, 0, "Model Family", runModelFamilyValue);
        addModelMetricRow(stats, 1, "Calibration Method", runModelCalibrationMethodValue);
        addModelMetricRow(stats, 2, "Calibration Metrics", runModelCalibrationMetricsValue);
        addModelMetricRow(stats, 3, "Confidence Output", runModelConfidenceTypeValue);
        addModelMetricRow(stats, 4, "Trade Threshold Logic", runModelThresholdLogicValue);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        statusRow.add(runModelStatusLabel);

        JPanel header = createCardPanel();
        header.add(createSectionHeader("Model Snapshot"), BorderLayout.NORTH);
        JPanel headerBody = new JPanel(new BorderLayout(0, 8));
        headerBody.setOpaque(false);
        headerBody.add(stats, BorderLayout.CENTER);
        headerBody.add(statusRow, BorderLayout.SOUTH);
        header.add(headerBody, BorderLayout.CENTER);

        runModelArea = createRunDetailsArea();
        runModelHyperparamsArea = createLogArea();
        runModelHyperparamsArea.setLineWrap(true);
        runModelHyperparamsArea.setWrapStyleWord(true);
        runModelCurvesArea = createLogArea();
        runModelCurvesArea.setLineWrap(true);
        runModelCurvesArea.setWrapStyleWord(true);

        JScrollPane summaryScroll = createLogScrollPane(runModelArea, "Model Overview");
        JScrollPane hyperparamsScroll = createLogScrollPane(runModelHyperparamsArea, "Hyperparameters");
        JScrollPane curvesScroll = createLogScrollPane(runModelCurvesArea, "Training Curves (loss vs epoch)");

        JSplitPane paramCurveSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hyperparamsScroll, curvesScroll);
        paramCurveSplit.setResizeWeight(0.5);
        paramCurveSplit.setDividerLocation(350);
        paramCurveSplit.setBorder(BorderFactory.createEmptyBorder());
        paramCurveSplit.setBackground(COLOR_BG);

        JSplitPane modelTopSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summaryScroll, paramCurveSplit);
        modelTopSplit.setResizeWeight(0.44);
        modelTopSplit.setDividerLocation(220);
        modelTopSplit.setBorder(BorderFactory.createEmptyBorder());
        modelTopSplit.setBackground(COLOR_BG);

        runFailureTableModel = new FailureTableModel();
        runFailureTable = new JTable(runFailureTableModel);
        styleRunsTable(runFailureTable);
        runFailureTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        runFailureDetailsButton = new JButton("Failure Details");
        styleButton(runFailureDetailsButton, false);
        runFailureDetailsButton.addActionListener(e -> openSelectedFailureDetailsModal());
        runFailureTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    openSelectedFailureDetailsModal();
                }
            }
        });

        JPanel failureActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        failureActionRow.setOpaque(false);
        failureActionRow.add(runFailureDetailsButton);
        failureActionRow.add(createHintLabel("Top 20 worst predictions by loss/error."));

        JScrollPane failureScroll = new JScrollPane(runFailureTable);
        failureScroll.getViewport().setBackground(COLOR_LOG_BG);
        failureScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        failureScroll.setColumnHeaderView(createSectionHeader("Failure Mode Explorer"));

        JPanel failurePanel = createCardPanel();
        failurePanel.add(failureActionRow, BorderLayout.NORTH);
        failurePanel.add(failureScroll, BorderLayout.CENTER);

        JSplitPane bodySplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, modelTopSplit, failurePanel);
        bodySplit.setResizeWeight(0.56);
        bodySplit.setDividerLocation(430);
        bodySplit.setBorder(BorderFactory.createEmptyBorder());
        bodySplit.setBackground(COLOR_BG);

        root.add(header, BorderLayout.NORTH);
        root.add(bodySplit, BorderLayout.CENTER);
        return root;
    }

    private JLabel createModelValueLabel() {
        JLabel value = new JLabel("—");
        value.setForeground(COLOR_TEXT);
        value.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        return value;
    }

    private void addModelMetricRow(JPanel panel, int row, String title, JLabel valueLabel) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(0, 2, 6, 10);
        panel.add(createFormLabel(title), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(0, 0, 6, 0);
        panel.add(valueLabel, right);
    }

    private void openSelectedFailureDetailsModal() {
        if (runFailureTable == null || runFailureTableModel == null) {
            return;
        }
        int viewRow = runFailureTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Select a failure row first.",
                    "Failure Details",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        int modelRow = runFailureTable.convertRowIndexToModel(viewRow);
        FailureRow row = runFailureTableModel.getRow(modelRow);
        if (row == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp: ").append(row.timestamp()).append("\n");
        sb.append("Ticker: ").append(row.ticker()).append("\n");
        sb.append("Predicted: ").append(row.predictedValue()).append("\n");
        sb.append("Confidence: ").append(row.confidence()).append("\n");
        sb.append("Actual: ").append(row.actualOutcome()).append("\n");
        sb.append("Regime: ").append(row.regime()).append("\n");
        sb.append("Error/Loss: ").append(row.errorOrLoss()).append("\n");
        sb.append("\nTop contributing features:\n").append(row.topContributions()).append("\n");
        if (row.raw() != null) {
            sb.append("\nRaw failure payload:\n").append(Json.pretty(row.raw()));
        }

        JTextArea detailsArea = new JTextArea(sb.toString());
        detailsArea.setEditable(false);
        detailsArea.setFont(monoFont);
        detailsArea.setBackground(COLOR_LOG_BG);
        detailsArea.setForeground(COLOR_TEXT);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(detailsArea);
        scroll.setPreferredSize(new Dimension(780, 560));
        JOptionPane.showMessageDialog(frame, scroll, "Failure Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createBacktestTransparencyPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        runBacktestStatusLabel = new JLabel();
        styleInlineStatus(runBacktestStatusLabel, "Backtest: idle", COLOR_MUTED);

        runBacktestCagrValue = createModelValueLabel();
        runBacktestSharpeValue = createModelValueLabel();
        runBacktestSortinoValue = createModelValueLabel();
        runBacktestMddValue = createModelValueLabel();
        runBacktestProfitFactorValue = createModelValueLabel();
        runBacktestWinRateValue = createModelValueLabel();
        runBacktestAvgWinLossValue = createModelValueLabel();
        runBacktestExposureValue = createModelValueLabel();
        runBacktestTurnoverValue = createModelValueLabel();

        JPanel metricsGrid = new JPanel(new GridBagLayout());
        metricsGrid.setOpaque(false);
        addModelMetricRow(metricsGrid, 0, "CAGR", runBacktestCagrValue);
        addModelMetricRow(metricsGrid, 1, "Sharpe", runBacktestSharpeValue);
        addModelMetricRow(metricsGrid, 2, "Sortino", runBacktestSortinoValue);
        addModelMetricRow(metricsGrid, 3, "Max Drawdown", runBacktestMddValue);
        addModelMetricRow(metricsGrid, 4, "Profit Factor", runBacktestProfitFactorValue);
        addModelMetricRow(metricsGrid, 5, "Win Rate", runBacktestWinRateValue);
        addModelMetricRow(metricsGrid, 6, "Avg Win/Loss", runBacktestAvgWinLossValue);
        addModelMetricRow(metricsGrid, 7, "Exposure", runBacktestExposureValue);
        addModelMetricRow(metricsGrid, 8, "Turnover", runBacktestTurnoverValue);

        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusWrap.setOpaque(false);
        statusWrap.add(runBacktestStatusLabel);

        JPanel metricsCard = createCardPanel();
        metricsCard.add(createSectionHeader("Backtest Summary Stats"), BorderLayout.NORTH);
        JPanel metricsBody = new JPanel(new BorderLayout(0, 8));
        metricsBody.setOpaque(false);
        metricsBody.add(metricsGrid, BorderLayout.CENTER);
        metricsBody.add(statusWrap, BorderLayout.SOUTH);
        metricsCard.add(metricsBody, BorderLayout.CENTER);

        runBacktestEquityChart = new LineChartPanel("Equity Curve", COLOR_SUCCESS, false);
        runBacktestDrawdownChart = new LineChartPanel("Drawdown Curve", COLOR_DANGER, true);
        JPanel equityChartWrap = createCardPanel();
        equityChartWrap.add(createSectionHeader("Equity Curve"), BorderLayout.NORTH);
        equityChartWrap.add(runBacktestEquityChart, BorderLayout.CENTER);
        JPanel drawdownChartWrap = createCardPanel();
        drawdownChartWrap.add(createSectionHeader("Drawdown Curve"), BorderLayout.NORTH);
        drawdownChartWrap.add(runBacktestDrawdownChart, BorderLayout.CENTER);

        JSplitPane chartSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, equityChartWrap, drawdownChartWrap);
        chartSplit.setResizeWeight(0.5);
        chartSplit.setDividerLocation(420);
        chartSplit.setBorder(BorderFactory.createEmptyBorder());
        chartSplit.setBackground(COLOR_BG);

        runBacktestAssumptionsArea = createLogArea();
        runBacktestAssumptionsArea.setLineWrap(true);
        runBacktestAssumptionsArea.setWrapStyleWord(true);
        JScrollPane assumptionsScroll = createLogScrollPane(runBacktestAssumptionsArea, "Assumptions Snapshot");

        runBacktestArea = createRunDetailsArea();
        JScrollPane overviewScroll = createLogScrollPane(runBacktestArea, "Backtest Overview");

        JSplitPane topDetailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartSplit, assumptionsScroll);
        topDetailSplit.setResizeWeight(0.52);
        topDetailSplit.setDividerLocation(260);
        topDetailSplit.setBorder(BorderFactory.createEmptyBorder());
        topDetailSplit.setBackground(COLOR_BG);

        JSplitPane topSectionSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overviewScroll, topDetailSplit);
        topSectionSplit.setResizeWeight(0.26);
        topSectionSplit.setDividerLocation(140);
        topSectionSplit.setBorder(BorderFactory.createEmptyBorder());
        topSectionSplit.setBackground(COLOR_BG);

        runTradeLogSearchField = new JTextField(24);
        runTradeLogExportButton = new JButton("Export Trade Log CSV");
        styleInputField(runTradeLogSearchField);
        styleButton(runTradeLogExportButton, false);
        runTradeLogExportButton.addActionListener(e -> exportTradeLogCsv());

        runTradeLogTableModel = new TradeLogTableModel();
        runTradeLogTable = new JTable(runTradeLogTableModel);
        styleRunsTable(runTradeLogTable);
        runTradeLogTableSorter = new TableRowSorter<>(runTradeLogTableModel);
        runTradeLogTable.setRowSorter(runTradeLogTableSorter);
        runTradeLogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        runTradeLogSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyTradeLogFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyTradeLogFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyTradeLogFilter();
            }
        });

        JPanel tradeActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tradeActionRow.setOpaque(false);
        tradeActionRow.add(createFormLabel("Search Trades"));
        tradeActionRow.add(runTradeLogSearchField);
        tradeActionRow.add(runTradeLogExportButton);

        JScrollPane tradeLogScroll = new JScrollPane(runTradeLogTable);
        tradeLogScroll.getViewport().setBackground(COLOR_LOG_BG);
        tradeLogScroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        tradeLogScroll.setColumnHeaderView(createSectionHeader("Trade Log Explorer"));

        JPanel tradeLogPanel = createCardPanel();
        tradeLogPanel.add(tradeActionRow, BorderLayout.NORTH);
        tradeLogPanel.add(tradeLogScroll, BorderLayout.CENTER);

        runAttributionRegimeTableModel = new AttributionTableModel();
        runAttributionConfidenceTableModel = new AttributionTableModel();
        runAttributionTickerTableModel = new AttributionTableModel();

        runAttributionRegimeTable = new JTable(runAttributionRegimeTableModel);
        runAttributionConfidenceTable = new JTable(runAttributionConfidenceTableModel);
        runAttributionTickerTable = new JTable(runAttributionTickerTableModel);
        styleRunsTable(runAttributionRegimeTable);
        styleRunsTable(runAttributionConfidenceTable);
        styleRunsTable(runAttributionTickerTable);

        JTabbedPane attributionTabs = new JTabbedPane();
        styleTabbedPane(attributionTabs);
        attributionTabs.addTab("By Regime", buildAttributionTableTab(runAttributionRegimeTable, "Performance by Regime"));
        attributionTabs.addTab("By Confidence", buildAttributionTableTab(
                runAttributionConfidenceTable, "Performance by Confidence Bucket"));
        attributionTabs.addTab("By Ticker/Sector", buildAttributionTableTab(
                runAttributionTickerTable, "Performance by Ticker/Sector"));

        JPanel attributionPanel = createCardPanel();
        attributionPanel.add(createSectionHeader("Attribution"), BorderLayout.NORTH);
        attributionPanel.add(attributionTabs, BorderLayout.CENTER);

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tradeLogPanel, attributionPanel);
        bottomSplit.setResizeWeight(0.58);
        bottomSplit.setDividerLocation(260);
        bottomSplit.setBorder(BorderFactory.createEmptyBorder());
        bottomSplit.setBackground(COLOR_BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSectionSplit, bottomSplit);
        mainSplit.setResizeWeight(0.56);
        mainSplit.setDividerLocation(430);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setBackground(COLOR_BG);

        root.add(metricsCard, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildAttributionTableTab(JTable table, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(COLOR_LOG_BG);
        scroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));
        scroll.setColumnHeaderView(createSectionHeader(title));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleRunsTable(JTable table) {
        table.setRowHeight(28);
        table.setBackground(COLOR_LOG_BG);
        table.setForeground(COLOR_TEXT);
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(COLOR_BORDER);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFont(uiFont.deriveFont(Font.PLAIN, 12f));
        table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setBackground(COLOR_CARD_ALT);
        table.getTableHeader().setForeground(COLOR_TEXT);
        table.getTableHeader().setBorder(new LineBorder(COLOR_BORDER, 1, true));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 8, 0, 8));
        renderer.setOpaque(true);
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void attachRunsFilterListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyRunsFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyRunsFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyRunsFilters();
            }
        });
    }

    private void clearRunsFilters() {
        runsTickerFilterField.setText("");
        runsModelFilterField.setText("");
        runsHorizonFilterField.setText("");
        runsDateFromField.setText("");
        runsDateToField.setText("");
        runsStatusFilterCombo.setSelectedItem("All");
        applyRunsFilters();
    }

    private void applyRunsFilters() {
        if (runsTableSorter == null || runsTableModel == null) {
            return;
        }
        String tickerFilter = runsTickerFilterField.getText().trim().toLowerCase();
        String modelFilter = runsModelFilterField.getText().trim().toLowerCase();
        String horizonFilter = runsHorizonFilterField.getText().trim().toLowerCase();
        String statusFilter = String.valueOf(runsStatusFilterCombo.getSelectedItem()).toLowerCase();
        LocalDate fromDate = parseDateFilter(runsDateFromField.getText().trim());
        LocalDate toDate = parseDateFilter(runsDateToField.getText().trim());

        List<String> tickerTokens = new ArrayList<>();
        if (!tickerFilter.isBlank()) {
            for (String token : tickerFilter.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    tickerTokens.add(trimmed);
                }
            }
        }

        runsTableSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends RunsTableModel, ? extends Integer> entry) {
                RunsTableRow row = runsTableModel.getRow(entry.getIdentifier());
                if (row == null) {
                    return false;
                }

                if (!tickerTokens.isEmpty()) {
                    String rowTickers = safeLower(row.tickers());
                    boolean matched = false;
                    for (String token : tickerTokens) {
                        if (rowTickers.contains(token)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        return false;
                    }
                }

                if (!modelFilter.isBlank() && !safeLower(row.modelType()).contains(modelFilter)) {
                    return false;
                }

                if (!horizonFilter.isBlank()) {
                    String horizonText = safeLower(row.horizon()) + " " + safeLower(row.targetHorizon());
                    if (!horizonText.contains(horizonFilter)) {
                        return false;
                    }
                }

                if (!"all".equals(statusFilter) && !safeLower(row.status()).contains(statusFilter)) {
                    return false;
                }

                if (fromDate != null || toDate != null) {
                    LocalDate rowDate = row.date();
                    if (rowDate == null) {
                        return false;
                    }
                    if (fromDate != null && rowDate.isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && rowDate.isAfter(toDate)) {
                        return false;
                    }
                }

                return true;
            }
        });

        int shown = runsTable.getRowCount();
        int total = runsTableModel.getRowCount();
        styleInlineStatus(runsTableStatusLabel, "Runs: " + shown + " / " + total + " visible", COLOR_MUTED);
    }

    private void loadRuns(boolean forceRefresh) {
        configureClientFromUI();
        runsRefreshButton.setEnabled(false);
        styleInlineStatus(runsTableStatusLabel, "Runs: loading...", COLOR_WARNING);
        String selectedRunId = getSelectedRunId();

        SwingWorker<List<RunsTableRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<RunsTableRow> doInBackground() throws Exception {
                List<Map<String, Object>> runs = runsService.listRuns(forceRefresh);
                List<RunsTableRow> rows = new ArrayList<>(runs.size());
                for (Map<String, Object> run : runs) {
                    rows.add(buildRunsTableRow(run));
                }
                rows.sort(Comparator.comparing(
                        RunsTableRow::dateTimeSortKey,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ));
                return rows;
            }

            @Override
            protected void done() {
                runsRefreshButton.setEnabled(true);
                try {
                    List<RunsTableRow> rows = get();
                    runsTableModel.setRows(rows);
                    applyRunsFilters();

                    if (selectedRunId != null) {
                        selectRunById(selectedRunId);
                    }
                    if (runsTable.getSelectedRow() < 0 && runsTable.getRowCount() > 0) {
                        runsTable.setRowSelectionInterval(0, 0);
                    }
                    styleInlineStatus(runsTableStatusLabel, "Runs: loaded " + rows.size() + " at " + ts(),
                            COLOR_SUCCESS);
                } catch (Exception ex) {
                    styleInlineStatus(runsTableStatusLabel, "Runs: load failed", COLOR_DANGER);
                    resetRunDetailsState("Failed to load runs: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void onRunSelectionChanged() {
        int[] selected = runsTable.getSelectedRows();
        if (selected.length == 0) {
            loadedRunDetailsId = null;
            resetRunDetailsState("Select a training run from the left list.");
            return;
        }
        if (selected.length > 1) {
            runDetailsHeaderLabel.setText("Run Details (" + selected.length + " selected)");
            runOverviewArea.setText("Select one run to inspect details, or click Compare Selected.");
            runDataArea.setText("");
            if (runDataTickerTableModel != null) {
                runDataTickerTableModel.setRows(new ArrayList<>());
            }
            if (runDataRawJsonArea != null) {
                runDataRawJsonArea.setText("");
            }
            if (runFeaturesSummaryArea != null) {
                runFeaturesSummaryArea.setText("Select one run to inspect details.");
            }
            if (runFeaturesTableModel != null) {
                runFeaturesTableModel.setRows(new ArrayList<>());
            }
            if (runTopFeaturesTableModel != null) {
                runTopFeaturesTableModel.setRows(new ArrayList<>());
            }
            if (runFeaturesStatusLabel != null) {
                styleInlineStatus(runFeaturesStatusLabel, "Features: select one run", COLOR_MUTED);
            }
            runSplitsArea.setText("");
            runModelArea.setText("");
            if (runModelHyperparamsArea != null) {
                runModelHyperparamsArea.setText("");
            }
            if (runModelCurvesArea != null) {
                runModelCurvesArea.setText("");
            }
            if (runModelFamilyValue != null) {
                runModelFamilyValue.setText("—");
            }
            if (runModelCalibrationMethodValue != null) {
                runModelCalibrationMethodValue.setText("—");
            }
            if (runModelCalibrationMetricsValue != null) {
                runModelCalibrationMetricsValue.setText("—");
            }
            if (runModelConfidenceTypeValue != null) {
                runModelConfidenceTypeValue.setText("—");
            }
            if (runModelThresholdLogicValue != null) {
                runModelThresholdLogicValue.setText("—");
            }
            if (runFailureTableModel != null) {
                runFailureTableModel.setRows(new ArrayList<>());
            }
            if (runModelStatusLabel != null) {
                styleInlineStatus(runModelStatusLabel, "Model: select one run", COLOR_MUTED);
            }
            runCalibrationArea.setText("");
            runMetricsArea.setText("");
            if (runBacktestArea != null) {
                runBacktestArea.setText("");
            }
            if (runBacktestAssumptionsArea != null) {
                runBacktestAssumptionsArea.setText("");
            }
            if (runBacktestCagrValue != null) {
                runBacktestCagrValue.setText("—");
            }
            if (runBacktestSharpeValue != null) {
                runBacktestSharpeValue.setText("—");
            }
            if (runBacktestSortinoValue != null) {
                runBacktestSortinoValue.setText("—");
            }
            if (runBacktestMddValue != null) {
                runBacktestMddValue.setText("—");
            }
            if (runBacktestProfitFactorValue != null) {
                runBacktestProfitFactorValue.setText("—");
            }
            if (runBacktestWinRateValue != null) {
                runBacktestWinRateValue.setText("—");
            }
            if (runBacktestAvgWinLossValue != null) {
                runBacktestAvgWinLossValue.setText("—");
            }
            if (runBacktestExposureValue != null) {
                runBacktestExposureValue.setText("—");
            }
            if (runBacktestTurnoverValue != null) {
                runBacktestTurnoverValue.setText("—");
            }
            if (runTradeLogTableModel != null) {
                runTradeLogTableModel.setRows(new ArrayList<>());
            }
            if (runAttributionRegimeTableModel != null) {
                runAttributionRegimeTableModel.setRows(new ArrayList<>());
            }
            if (runAttributionConfidenceTableModel != null) {
                runAttributionConfidenceTableModel.setRows(new ArrayList<>());
            }
            if (runAttributionTickerTableModel != null) {
                runAttributionTickerTableModel.setRows(new ArrayList<>());
            }
            if (runBacktestEquityChart != null) {
                runBacktestEquityChart.setSeries(new ArrayList<>(), false);
            }
            if (runBacktestDrawdownChart != null) {
                runBacktestDrawdownChart.setSeries(new ArrayList<>(), true);
            }
            if (runBacktestStatusLabel != null) {
                styleInlineStatus(runBacktestStatusLabel, "Backtest: select one run", COLOR_MUTED);
            }
            runDiagnosticsArea.setText("");
            if (runDataManifestStatusLabel != null) {
                styleInlineStatus(runDataManifestStatusLabel, "Manifest: select one run", COLOR_MUTED);
            }
            currentGuardrailReport = null;
            currentGuardrailRunId = null;
            if (runReadinessChecklistArea != null) {
                runReadinessChecklistArea.setText("Select one run to evaluate readiness.");
            }
            if (runReadinessFindingsArea != null) {
                runReadinessFindingsArea.setText("");
            }
            if (runReproScoreLabel != null) {
                styleInlineStatus(runReproScoreLabel, "Reproducibility Score: —", COLOR_MUTED);
            }
            if (runReadinessStatusLabel != null) {
                styleInlineStatus(runReadinessStatusLabel, "Checklist: select one run", COLOR_MUTED);
            }
            if (runReadinessActionStatusLabel != null) {
                styleInlineStatus(runReadinessActionStatusLabel, "Actions: select one run", COLOR_MUTED);
            }
            if (runReadinessBannerLabel != null) {
                styleInlineStatus(runReadinessBannerLabel, "Guardrails: select one run", COLOR_MUTED);
            }
            setTrainingGuardrailIdle();
            updateReadinessActionButtons();
            return;
        }

        int modelRow = runsTable.convertRowIndexToModel(selected[0]);
        RunsTableRow row = runsTableModel.getRow(modelRow);
        if (row == null || row.runId() == null || row.runId().isBlank()) {
            resetRunDetailsState("Selected row does not include a run id.");
            return;
        }
        if (Objects.equals(loadedRunDetailsId, row.runId())) {
            return;
        }
        loadRunDetails(row.runId(), false);
    }

    private void loadRunDetails(String runId, boolean forceRefresh) {
        configureClientFromUI();
        runDetailsHeaderLabel.setText("Run Details: " + shortenRunId(runId));
        runOverviewArea.setText("Loading run details...");
        runDataArea.setText("Loading...");
        if (runDataTickerTableModel != null) {
            runDataTickerTableModel.setRows(new ArrayList<>());
        }
        if (runDataRawJsonArea != null) {
            runDataRawJsonArea.setText("Loading...");
        }
        if (runDataManifestStatusLabel != null) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: loading...", COLOR_WARNING);
        }
        if (runFeaturesSummaryArea != null) {
            runFeaturesSummaryArea.setText("Loading...");
        }
        if (runFeaturesTableModel != null) {
            runFeaturesTableModel.setRows(new ArrayList<>());
        }
        if (runTopFeaturesTableModel != null) {
            runTopFeaturesTableModel.setRows(new ArrayList<>());
        }
        if (runFeaturesStatusLabel != null) {
            styleInlineStatus(runFeaturesStatusLabel, "Features: loading...", COLOR_WARNING);
        }
        runSplitsArea.setText("Loading...");
        runModelArea.setText("Loading...");
        if (runModelHyperparamsArea != null) {
            runModelHyperparamsArea.setText("Loading...");
        }
        if (runModelCurvesArea != null) {
            runModelCurvesArea.setText("Loading...");
        }
        if (runModelFamilyValue != null) {
            runModelFamilyValue.setText("...");
        }
        if (runModelCalibrationMethodValue != null) {
            runModelCalibrationMethodValue.setText("...");
        }
        if (runModelCalibrationMetricsValue != null) {
            runModelCalibrationMetricsValue.setText("...");
        }
        if (runModelConfidenceTypeValue != null) {
            runModelConfidenceTypeValue.setText("...");
        }
        if (runModelThresholdLogicValue != null) {
            runModelThresholdLogicValue.setText("...");
        }
        if (runFailureTableModel != null) {
            runFailureTableModel.setRows(new ArrayList<>());
        }
        if (runModelStatusLabel != null) {
            styleInlineStatus(runModelStatusLabel, "Model: loading...", COLOR_WARNING);
        }
        runCalibrationArea.setText("Loading...");
        runMetricsArea.setText("Loading...");
        if (runBacktestArea != null) {
            runBacktestArea.setText("Loading...");
        }
        if (runBacktestAssumptionsArea != null) {
            runBacktestAssumptionsArea.setText("Loading...");
        }
        if (runBacktestCagrValue != null) {
            runBacktestCagrValue.setText("...");
        }
        if (runBacktestSharpeValue != null) {
            runBacktestSharpeValue.setText("...");
        }
        if (runBacktestSortinoValue != null) {
            runBacktestSortinoValue.setText("...");
        }
        if (runBacktestMddValue != null) {
            runBacktestMddValue.setText("...");
        }
        if (runBacktestProfitFactorValue != null) {
            runBacktestProfitFactorValue.setText("...");
        }
        if (runBacktestWinRateValue != null) {
            runBacktestWinRateValue.setText("...");
        }
        if (runBacktestAvgWinLossValue != null) {
            runBacktestAvgWinLossValue.setText("...");
        }
        if (runBacktestExposureValue != null) {
            runBacktestExposureValue.setText("...");
        }
        if (runBacktestTurnoverValue != null) {
            runBacktestTurnoverValue.setText("...");
        }
        if (runTradeLogTableModel != null) {
            runTradeLogTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionRegimeTableModel != null) {
            runAttributionRegimeTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionConfidenceTableModel != null) {
            runAttributionConfidenceTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionTickerTableModel != null) {
            runAttributionTickerTableModel.setRows(new ArrayList<>());
        }
        if (runBacktestEquityChart != null) {
            runBacktestEquityChart.setSeries(new ArrayList<>(), false);
        }
        if (runBacktestDrawdownChart != null) {
            runBacktestDrawdownChart.setSeries(new ArrayList<>(), true);
        }
        if (runBacktestStatusLabel != null) {
            styleInlineStatus(runBacktestStatusLabel, "Backtest: loading...", COLOR_WARNING);
        }
        runDiagnosticsArea.setText("Loading...");
        if (runReadinessChecklistArea != null) {
            runReadinessChecklistArea.setText("Loading...");
        }
        if (runReadinessFindingsArea != null) {
            runReadinessFindingsArea.setText("Loading...");
        }
        if (runReproScoreLabel != null) {
            styleInlineStatus(runReproScoreLabel, "Reproducibility Score: ...", COLOR_WARNING);
        }
        if (runReadinessStatusLabel != null) {
            styleInlineStatus(runReadinessStatusLabel, "Checklist: loading...", COLOR_WARNING);
        }
        if (runReadinessActionStatusLabel != null) {
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: waiting for run details", COLOR_MUTED);
        }
        if (runReadinessBannerLabel != null) {
            styleInlineStatus(runReadinessBannerLabel, "Guardrails: loading...", COLOR_WARNING);
        }
        setTrainingGuardrailLoading(runId);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> payload = new LinkedHashMap<>();
                Map<String, Object> run = runsService.getRun(runId, forceRefresh);
                payload.put("run", run);
                List<Map<String, Object>> artifacts = runsService.getRunArtifacts(runId, forceRefresh);
                payload.put("artifacts", artifacts);
                Map<String, Object> artifactPayloads = new LinkedHashMap<>();
                for (Map<String, Object> artifactMeta : artifacts) {
                    String artifactName = extractArtifactName(artifactMeta);
                    if (artifactName == null || artifactName.isBlank()) {
                        continue;
                    }
                    try {
                        Object artifactPayload = runsService.getRunArtifact(runId, artifactName, forceRefresh);
                        artifactPayloads.put(artifactName, artifactPayload);
                    } catch (Exception artifactError) {
                        Map<String, Object> error = new LinkedHashMap<>();
                        error.put("__error", humanizeError(artifactError));
                        artifactPayloads.put(artifactName, error);
                    }
                }
                payload.put("artifact_payloads", artifactPayloads);
                return payload;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> payload = get();
                    Map<String, Object> run = safeObject(payload.get("run"));
                    List<Map<String, Object>> artifacts = asMapList(payload.get("artifacts"));
                    Map<String, String> artifactIndex = indexArtifacts(artifacts);
                    Map<String, Object> artifactPayloads = safeObject(payload.get("artifact_payloads"));

                    loadedRunDetailsId = runId;
                    runDetailsHeaderLabel.setText("Run Details: " + shortenRunId(runId));

                    renderRunDetailsSection(run, artifactIndex, artifactPayloads, runOverviewArea,
                            "overview", "runsummary", "summary", "metadata");
                    populateDataTransparencyPanel(run, artifactIndex, artifactPayloads);
                    populateFeatureTransparencyPanel(run, artifactIndex, artifactPayloads);
                    renderRunDetailsSection(run, artifactIndex, artifactPayloads, runSplitsArea,
                            "split", "splitsummary", "walkforward", "window");
                    populateModelTransparencyPanel(run, artifactIndex, artifactPayloads);
                    renderRunDetailsSection(run, artifactIndex, artifactPayloads, runCalibrationArea,
                            "calibration", "reliability", "brier");
                    renderRunDetailsSection(run, artifactIndex, artifactPayloads, runMetricsArea,
                            "metric", "metricssummary", "classification", "regression", "trading");
                    populateBacktestTransparencyPanel(run, artifactIndex, artifactPayloads);
                    renderRunDetailsSection(run, artifactIndex, artifactPayloads, runDiagnosticsArea,
                            "diagnostic", "drift", "regime", "error");
                    populateModelReadinessPanel(run, artifactIndex, artifactPayloads);
                    refreshAuditLogPanel();
                } catch (Exception ex) {
                    loadedRunDetailsId = null;
                    resetRunDetailsState("Failed to load run details: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderRunDetailsSection(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads,
            JTextArea targetArea,
            String... hints
    ) {
        Object inlineSection = findSectionByHints(run, hints);
        StringBuilder sb = new StringBuilder();
        if (inlineSection != null) {
            sb.append(Json.pretty(inlineSection));
        } else {
            sb.append("No inline section found in run payload for hints: ")
                    .append(String.join(", ", hints))
                    .append("\n");
        }

        String artifactName = findArtifactByHints(artifactIndex, hints);
        if (artifactName != null) {
            Object artifact = findArtifactPayload(artifactPayloads, artifactName);
            if (isArtifactError(artifact)) {
                sb.append("\n\nArtifact ").append(artifactName)
                        .append(" unavailable: ")
                        .append(extractArtifactError(artifact));
            } else if (artifact != null) {
                sb.append("\n\nArtifact: ").append(artifactName).append("\n");
                sb.append(Json.pretty(artifact));
            } else {
                sb.append("\n\nArtifact ").append(artifactName)
                        .append(" unavailable: missing payload.");
            }
        } else {
            sb.append("\n\nArtifact unavailable for this section (missing in /runs/{id}/artifacts).");
        }
        targetArea.setText(sb.toString());
        targetArea.setCaretPosition(0);
    }

    private void populateDataTransparencyPanel(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
        Object inlineData = findSectionByHints(run, "data", "datasummary", "dataset", "quality");
        String dataSummaryArtifact = findArtifactByHints(artifactIndex,
                "data_summary", "datasummary", "dataset_manifest", "data", "quality");
        Object artifactData = dataSummaryArtifact == null ? null : findArtifactPayload(artifactPayloads, dataSummaryArtifact);
        Object dataSummary = selectDataSummaryPayload(inlineData, artifactData);

        Object inlineHashes = findSectionByHints(run, "datahashes", "datahash", "datasethash", "hash");
        String dataHashesArtifact = findArtifactByHints(artifactIndex,
                "data_hashes", "datahash", "dataset_hash", "hash");
        Object artifactHashes = dataHashesArtifact == null ? null : findArtifactPayload(artifactPayloads, dataHashesArtifact);
        Object dataHashes = selectDataHashesPayload(inlineHashes, artifactHashes);

        runDataSummaryPayload = dataSummary == null ? new LinkedHashMap<String, Object>() : dataSummary;
        runDataHashesPayload = dataHashes == null ? new LinkedHashMap<String, Object>() : dataHashes;

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("data_summary", runDataSummaryPayload);
        raw.put("data_hashes", runDataHashesPayload);
        if (dataSummaryArtifact != null) {
            raw.put("data_summary_artifact", dataSummaryArtifact);
        }
        if (dataHashesArtifact != null) {
            raw.put("data_hashes_artifact", dataHashesArtifact);
        }
        runDataRawPayload = raw;
        refreshDataRawJsonViewer();

        List<DataTickerStatsRow> tickerRows = extractTickerStats(runDataSummaryPayload);
        runDataTickerTableModel.setRows(tickerRows);
        runDataArea.setText(buildDataTransparencySummary(run, runDataSummaryPayload, runDataHashesPayload, tickerRows));
        runDataArea.setCaretPosition(0);
        styleInlineStatus(runDataManifestStatusLabel, "Manifest: ready", COLOR_SUCCESS);
    }

    private void populateFeatureTransparencyPanel(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
        Object inlineFeatures = findSectionByHints(run, "feature", "featuresummary", "featurelist", "featureregistry");
        String featureArtifactName = findArtifactByHints(artifactIndex,
                "feature_summary", "feature_registry", "features", "featurelist");
        Object featureArtifact = featureArtifactName == null ? null : findArtifactPayload(artifactPayloads, featureArtifactName);
        Object featurePayload = featureArtifact != null && !isArtifactError(featureArtifact) ? featureArtifact : inlineFeatures;

        String importanceArtifactName = findArtifactByHints(artifactIndex,
                "feature_importance", "importance", "top_features", "model_summary");
        Object importanceArtifact = importanceArtifactName == null ? null : findArtifactPayload(artifactPayloads, importanceArtifactName);
        Object importancePayload = importanceArtifact != null && !isArtifactError(importanceArtifact)
                ? importanceArtifact
                : findSectionByHints(run, "importance", "topfeatures", "featureimportance");

        List<FeatureRow> features = extractFeatureRows(featurePayload);
        List<TopFeatureRow> topRows = extractTopFeatureRows(importancePayload, features);

        runFeaturesTableModel.setRows(features);
        runTopFeaturesTableModel.setRows(topRows);
        applyFeatureFilters();

        runFeaturesRawPayload = new LinkedHashMap<>();
        Map<String, Object> raw = asObjectOrEmpty(runFeaturesRawPayload);
        raw.put("feature_payload", featurePayload == null ? new LinkedHashMap<String, Object>() : featurePayload);
        raw.put("importance_payload", importancePayload == null ? new LinkedHashMap<String, Object>() : importancePayload);
        if (featureArtifactName != null) {
            raw.put("feature_artifact", featureArtifactName);
        }
        if (importanceArtifactName != null) {
            raw.put("importance_artifact", importanceArtifactName);
        }
        runFeaturesRawPayload = raw;

        runFeaturesSummaryArea.setText(buildFeatureSummary(features, topRows));
        runFeaturesSummaryArea.setCaretPosition(0);
        styleInlineStatus(runFeaturesStatusLabel, "Features: loaded " + features.size(), COLOR_SUCCESS);
    }

    private List<FeatureRow> extractFeatureRows(Object featurePayload) {
        List<FeatureRow> rows = new ArrayList<>();
        Object candidate = featurePayload;
        if (featurePayload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object nested = findAnyValue(map,
                    "features",
                    "feature_list",
                    "feature_registry",
                    "feature_summary",
                    "items");
            if (nested != null) {
                candidate = nested;
            }
        }

        if (candidate instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }
                Map<String, Object> m = Json.asObject(itemMapRaw);
                rows.add(toFeatureRow(m));
            }
        } else if (candidate instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> valueMapRaw) {
                    Map<String, Object> m = Json.asObject(valueMapRaw);
                    if (findAnyValue(m, "feature_name", "name") == null) {
                        m.put("feature_name", key);
                    }
                    rows.add(toFeatureRow(m));
                } else if (value != null) {
                    Map<String, Object> wrapped = new LinkedHashMap<>();
                    wrapped.put("feature_name", key);
                    wrapped.put("value", value);
                    rows.add(toFeatureRow(wrapped));
                }
            }
        }
        return rows;
    }

    private FeatureRow toFeatureRow(Map<String, Object> m) {
        String featureName = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "feature_name", "name", "feature", "id")),
                "—"
        );
        String category = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "category", "feature_category", "group", "type")),
                "other"
        );
        String parameters = formatParameters(findAnyValue(m, "parameters", "params", "config"));
        String timeframe = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "timeframe", "interval", "resolution")),
                "—"
        );
        String scaling = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "normalization", "scaling", "scaler", "normalizer")),
                "—"
        );
        String missingPct = formatPercentageFlexible(findAnyValue(m, "missing_pct", "missing_percent", "missingness"));
        String zeroVariance = formatBoolean(findAnyValue(m, "zero_variance", "zero_variance_flag", "is_zero_variance"));
        String correlationCluster = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "correlation_cluster_id", "cluster_id", "corr_cluster")),
                "—"
        );
        boolean kept = asBoolean(findAnyValue(m, "kept", "is_kept", "selected"), true);
        String reason = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "reason", "drop_reason", "status_reason", "selection_reason")),
                kept ? "kept" : "dropped"
        );
        String dependencies = formatDependencies(findAnyValue(m, "dependencies", "upstream", "lineage", "inputs"));
        String joinLag = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "join_lag", "asof_join_lag", "as_of_join_lag", "lag")),
                "—"
        );
        Double importance = asDoubleObject(findAnyValue(m, "importance", "feature_importance", "gain", "weight"));
        String stability = firstNonBlank(
                stringOrEmpty(findAnyValue(m, "importance_stability", "fold_stability")),
                "—"
        );
        return new FeatureRow(
                featureName,
                category,
                parameters,
                timeframe,
                scaling,
                missingPct,
                zeroVariance,
                correlationCluster,
                kept,
                reason,
                dependencies,
                joinLag,
                importance,
                stability,
                m
        );
    }

    private List<TopFeatureRow> extractTopFeatureRows(Object importancePayload, List<FeatureRow> features) {
        List<TopFeatureRow> rows = new ArrayList<>();
        Object candidate = importancePayload;
        if (importancePayload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object nested = findAnyValue(map,
                    "top_features",
                    "feature_importance",
                    "importance",
                    "importances",
                    "items");
            if (nested != null) {
                candidate = nested;
            }
        }

        if (candidate instanceof List<?> list) {
            int rank = 1;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }
                Map<String, Object> m = Json.asObject(itemMapRaw);
                String name = firstNonBlank(
                        stringOrEmpty(findAnyValue(m, "feature_name", "name", "feature", "id")),
                        "—"
                );
                double importance = Json.asDouble(findAnyValue(m, "importance", "score", "value", "gain"), Double.NaN);
                String stability = firstNonBlank(
                        computeStabilityFromFolds(findAnyValue(m,
                                "importance_folds",
                                "fold_importance",
                                "importance_by_fold",
                                "fold_scores")),
                        stringOrEmpty(findAnyValue(m, "importance_stability", "stability", "stability_score"))
                );
                String category = firstNonBlank(
                        stringOrEmpty(findAnyValue(m, "category", "group")),
                        findCategoryForFeature(features, name)
                );
                rows.add(new TopFeatureRow(rank++, name, importance, stability.isBlank() ? "—" : stability, category));
            }
        } else if (candidate instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            List<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
            entries.sort((a, b) -> Double.compare(Json.asDouble(b.getValue(), 0.0), Json.asDouble(a.getValue(), 0.0)));
            int rank = 1;
            for (Map.Entry<String, Object> e : entries) {
                rows.add(new TopFeatureRow(
                        rank++,
                        e.getKey(),
                        Json.asDouble(e.getValue(), Double.NaN),
                        "—",
                        findCategoryForFeature(features, e.getKey())
                ));
            }
        }

        if (rows.isEmpty() && !features.isEmpty()) {
            List<FeatureRow> withImportance = new ArrayList<>();
            for (FeatureRow feature : features) {
                if (feature.importance() != null) {
                    withImportance.add(feature);
                }
            }
            withImportance.sort((a, b) -> Double.compare(
                    b.importance() == null ? Double.NEGATIVE_INFINITY : b.importance(),
                    a.importance() == null ? Double.NEGATIVE_INFINITY : a.importance()
            ));
            int rank = 1;
            for (FeatureRow feature : withImportance) {
                rows.add(new TopFeatureRow(
                        rank++,
                        feature.featureName(),
                        feature.importance() == null ? Double.NaN : feature.importance(),
                        feature.stability(),
                        feature.category()
                ));
            }
        }
        return rows;
    }

    private String findCategoryForFeature(List<FeatureRow> features, String featureName) {
        String needle = safeLower(featureName);
        for (FeatureRow row : features) {
            if (safeLower(row.featureName()).equals(needle)) {
                return row.category();
            }
        }
        return "—";
    }

    private String buildFeatureSummary(List<FeatureRow> features, List<TopFeatureRow> topRows) {
        int kept = 0;
        int dropped = 0;
        Map<String, Integer> categories = new LinkedHashMap<>();
        for (FeatureRow row : features) {
            if (row.kept()) {
                kept++;
            } else {
                dropped++;
            }
            String category = row.category().isBlank() ? "other" : row.category();
            categories.put(category, categories.getOrDefault(category, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Feature Transparency\n\n");
        sb.append("Total features: ").append(features.size()).append("\n");
        sb.append("Kept: ").append(kept).append(", Dropped: ").append(dropped).append("\n");
        sb.append("Categories: ");
        if (categories.isEmpty()) {
            sb.append("not reported");
        } else {
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : categories.entrySet()) {
                parts.add(e.getKey() + "=" + e.getValue());
            }
            sb.append(String.join(", ", parts));
        }
        sb.append("\n");

        if (!topRows.isEmpty()) {
            sb.append("\nTop features:\n");
            int n = Math.min(10, topRows.size());
            for (int i = 0; i < n; i++) {
                TopFeatureRow row = topRows.get(i);
                sb.append(i + 1).append(". ")
                        .append(row.featureName())
                        .append(" | importance=")
                        .append(formatImportance(row.importance()))
                        .append(" | stability=")
                        .append(row.stability())
                        .append("\n");
            }
        } else {
            sb.append("\nTop features: no importance payload reported.\n");
        }

        return sb.toString();
    }

    private void populateModelTransparencyPanel(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
        Object inlineModel = findSectionByHints(run,
                "model",
                "modelsummary",
                "hyperparameter",
                "trainingcurve",
                "calibration",
                "threshold");
        String modelArtifactName = findArtifactByHints(artifactIndex,
                "model_summary",
                "model",
                "training_curve",
                "calibration",
                "hyperparameter");
        Object modelArtifact = modelArtifactName == null ? null : findArtifactPayload(artifactPayloads, modelArtifactName);
        Object modelPayload = modelArtifact != null && !isArtifactError(modelArtifact) ? modelArtifact : inlineModel;
        Map<String, Object> modelMap = asObjectOrEmpty(modelPayload);

        runModelFamilyValue.setText(firstNonBlank(
                stringOrDash(findAnyValue(modelMap, "model_family", "family", "algorithm", "model_type", "model")),
                "—"
        ));
        runModelCalibrationMethodValue.setText(formatCalibrationMethod(modelMap));
        runModelCalibrationMetricsValue.setText(formatCalibrationMetrics(modelMap));
        runModelConfidenceTypeValue.setText(formatConfidenceOutputType(run, modelMap));
        runModelThresholdLogicValue.setText(formatThresholdLogic(modelMap));

        Object hyperparams = findAnyValue(modelMap, "hyperparameters", "params", "config", "best_params");
        if (hyperparams == null) {
            runModelHyperparamsArea.setText("Hyperparameters not reported.");
        } else {
            runModelHyperparamsArea.setText(Json.pretty(hyperparams));
        }
        runModelHyperparamsArea.setCaretPosition(0);

        Object trainingCurves = findAnyValue(modelMap,
                "training_curves",
                "loss_curve",
                "history",
                "learning_curve",
                "loss_history",
                "curves");
        runModelCurvesArea.setText(formatTrainingCurves(trainingCurves));
        runModelCurvesArea.setCaretPosition(0);

        Object inlineFailures = findSectionByHints(run,
                "failure",
                "worstprediction",
                "erroranalysis",
                "largesterror",
                "misprediction");
        String failureArtifactName = findArtifactByHints(artifactIndex,
                "failure",
                "worst_prediction",
                "largest_error",
                "error_analysis",
                "misprediction");
        Object failureArtifact = failureArtifactName == null ? null : findArtifactPayload(artifactPayloads, failureArtifactName);
        Object failurePayload = failureArtifact != null && !isArtifactError(failureArtifact) ? failureArtifact : inlineFailures;
        List<FailureRow> failures = extractFailureRows(failurePayload);
        runFailureTableModel.setRows(failures);

        runModelRawPayload = modelPayload == null ? new LinkedHashMap<String, Object>() : modelPayload;
        runFailuresRawPayload = failurePayload == null ? new LinkedHashMap<String, Object>() : failurePayload;

        runModelArea.setText(buildModelSummaryText(modelMap, failures, modelArtifactName, failureArtifactName));
        runModelArea.setCaretPosition(0);
        styleInlineStatus(runModelStatusLabel, "Model: loaded (" + failures.size() + " failures)", COLOR_SUCCESS);
    }

    private String buildModelSummaryText(
            Map<String, Object> modelMap,
            List<FailureRow> failures,
            String modelArtifactName,
            String failureArtifactName
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model Transparency\n\n");
        sb.append("Model family: ").append(runModelFamilyValue.getText()).append("\n");
        sb.append("Calibration method: ").append(runModelCalibrationMethodValue.getText()).append("\n");
        sb.append("Calibration metrics: ").append(runModelCalibrationMetricsValue.getText()).append("\n");
        sb.append("Confidence output: ").append(runModelConfidenceTypeValue.getText()).append("\n");
        sb.append("Threshold logic: ").append(runModelThresholdLogicValue.getText()).append("\n");
        sb.append("Failures loaded: ").append(failures.size()).append("\n");
        if (modelArtifactName != null) {
            sb.append("Model artifact: ").append(modelArtifactName).append("\n");
        }
        if (failureArtifactName != null) {
            sb.append("Failure artifact: ").append(failureArtifactName).append("\n");
        }
        if (modelMap.isEmpty()) {
            sb.append("\nModel payload fields not reported.");
        }
        return sb.toString();
    }

    private void populateBacktestTransparencyPanel(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
        Object inlineBacktest = findSectionByHints(run,
                "backtest",
                "backtestsummary",
                "equity",
                "drawdown",
                "trade",
                "attribution");
        String backtestArtifactName = findArtifactByHints(artifactIndex,
                "backtest_summary",
                "backtest",
                "equity_curve",
                "drawdown_curve",
                "trade_log",
                "attribution");
        Object backtestArtifact = backtestArtifactName == null ? null : findArtifactPayload(artifactPayloads, backtestArtifactName);
        Object backtestPayload = backtestArtifact != null && !isArtifactError(backtestArtifact)
                ? backtestArtifact
                : inlineBacktest;

        String tradeArtifactName = findArtifactByHints(artifactIndex, "trade_log", "trade_list", "trades");
        Object tradeArtifact = tradeArtifactName == null ? null : findArtifactPayload(artifactPayloads, tradeArtifactName);
        Object tradePayload = tradeArtifact != null && !isArtifactError(tradeArtifact) ? tradeArtifact : null;

        String equityArtifactName = findArtifactByHints(artifactIndex, "equity_curve", "equity");
        Object equityArtifact = equityArtifactName == null ? null : findArtifactPayload(artifactPayloads, equityArtifactName);
        Object equityPayload = equityArtifact != null && !isArtifactError(equityArtifact) ? equityArtifact : null;

        String drawdownArtifactName = findArtifactByHints(artifactIndex, "drawdown_curve", "drawdown");
        Object drawdownArtifact = drawdownArtifactName == null ? null : findArtifactPayload(artifactPayloads, drawdownArtifactName);
        Object drawdownPayload = drawdownArtifact != null && !isArtifactError(drawdownArtifact) ? drawdownArtifact : null;

        String assumptionsArtifactName = findArtifactByHints(artifactIndex,
                "assumptions",
                "backtest_assumptions",
                "assumption_snapshot");
        Object assumptionsArtifact = assumptionsArtifactName == null ? null : findArtifactPayload(artifactPayloads, assumptionsArtifactName);
        Object assumptionsPayload = assumptionsArtifact != null && !isArtifactError(assumptionsArtifact) ? assumptionsArtifact : null;

        String attributionArtifactName = findArtifactByHints(artifactIndex, "attribution", "performance_by");
        Object attributionArtifact = attributionArtifactName == null ? null : findArtifactPayload(artifactPayloads, attributionArtifactName);
        Object attributionPayload = attributionArtifact != null && !isArtifactError(attributionArtifact) ? attributionArtifact : null;

        Map<String, Object> backtestMap = asObjectOrEmpty(backtestPayload);
        Map<String, Object> summaryMap = selectBacktestSummary(backtestMap);
        Object assumptions = assumptionsPayload != null ? assumptionsPayload : selectBacktestAssumptions(backtestMap);

        List<SeriesPoint> equitySeries = extractSeriesPoints(
                firstNonNull(equityPayload, backtestPayload),
                "equity");
        List<SeriesPoint> drawdownSeries = extractSeriesPoints(
                firstNonNull(drawdownPayload, backtestPayload),
                "drawdown");

        List<TradeLogRow> tradeRows = extractTradeLogRows(
                firstNonNull(tradePayload, backtestPayload));
        List<AttributionRow> regimeRows = extractAttributionRows(
                firstNonNull(attributionPayload, backtestPayload),
                "regime");
        List<AttributionRow> confidenceRows = extractAttributionRows(
                firstNonNull(attributionPayload, backtestPayload),
                "confidence");
        List<AttributionRow> tickerRows = extractAttributionRows(
                firstNonNull(attributionPayload, backtestPayload),
                "ticker");

        runBacktestRawPayload = new LinkedHashMap<>();
        Map<String, Object> raw = asObjectOrEmpty(runBacktestRawPayload);
        raw.put("backtest_payload", backtestPayload == null ? new LinkedHashMap<String, Object>() : backtestPayload);
        raw.put("summary", summaryMap);
        raw.put("assumptions", assumptions == null ? new LinkedHashMap<String, Object>() : assumptions);
        raw.put("trades", tradeRows.size());
        raw.put("equity_points", equitySeries.size());
        raw.put("drawdown_points", drawdownSeries.size());
        raw.put("attribution_regime", regimeRows.size());
        raw.put("attribution_confidence", confidenceRows.size());
        raw.put("attribution_ticker", tickerRows.size());
        if (backtestArtifactName != null) {
            raw.put("backtest_artifact", backtestArtifactName);
        }
        if (assumptionsArtifactName != null) {
            raw.put("assumptions_artifact", assumptionsArtifactName);
        }
        if (tradeArtifactName != null) {
            raw.put("trade_artifact", tradeArtifactName);
        }
        if (equityArtifactName != null) {
            raw.put("equity_artifact", equityArtifactName);
        }
        if (drawdownArtifactName != null) {
            raw.put("drawdown_artifact", drawdownArtifactName);
        }
        if (attributionArtifactName != null) {
            raw.put("attribution_artifact", attributionArtifactName);
        }
        runBacktestRawPayload = raw;

        updateBacktestMetricLabels(summaryMap, backtestMap);
        runBacktestEquityChart.setSeries(equitySeries, false);
        runBacktestDrawdownChart.setSeries(drawdownSeries, true);

        runTradeLogTableModel.setRows(tradeRows);
        applyTradeLogFilter();
        runAttributionRegimeTableModel.setRows(regimeRows);
        runAttributionConfidenceTableModel.setRows(confidenceRows);
        runAttributionTickerTableModel.setRows(tickerRows);

        if (runBacktestAssumptionsArea != null) {
            runBacktestAssumptionsArea.setText(formatBacktestAssumptions(assumptions));
            runBacktestAssumptionsArea.setCaretPosition(0);
        }
        if (runBacktestArea != null) {
            runBacktestArea.setText(buildBacktestOverviewText(
                    summaryMap,
                    assumptions,
                    tradeRows,
                    regimeRows,
                    confidenceRows,
                    tickerRows));
            runBacktestArea.setCaretPosition(0);
        }

        if (backtestPayload == null) {
            styleInlineStatus(runBacktestStatusLabel, "Backtest: no payload reported", COLOR_WARNING);
        } else {
            styleInlineStatus(
                    runBacktestStatusLabel,
                    "Backtest: loaded (trades=" + tradeRows.size() + ", equity=" + equitySeries.size() + " pts)",
                    COLOR_SUCCESS
            );
        }
    }

    private void populateModelReadinessPanel(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
        Map<String, Object> runSummary = asObjectOrEmpty(firstNonNull(
                findSectionByHints(run, "run_summary", "runsummary"),
                run
        ));
        Map<String, Object> dataSummary = resolveSectionMap(
                run,
                artifactIndex,
                artifactPayloads,
                new String[]{"data_summary", "datasummary", "data", "dataset", "quality"},
                new String[]{"data_summary", "datasummary", "dataset_manifest", "quality"}
        );
        Map<String, Object> featureSummary = resolveSectionMap(
                run,
                artifactIndex,
                artifactPayloads,
                new String[]{"feature_summary", "featuresummary", "feature"},
                new String[]{"feature_summary", "featuresummary", "feature"}
        );
        Map<String, Object> splitSummary = resolveSectionMap(
                run,
                artifactIndex,
                artifactPayloads,
                new String[]{"split_summary", "splitsummary", "split", "walkforward"},
                new String[]{"split_summary", "splitsummary", "split", "walkforward"}
        );
        Map<String, Object> backtestSummary = resolveSectionMap(
                run,
                artifactIndex,
                artifactPayloads,
                new String[]{"backtest_summary", "backtestsummary", "backtest"},
                new String[]{"backtest_summary", "backtest", "assumptions"}
        );
        Map<String, Object> reproducibilitySummary = resolveSectionMap(
                run,
                artifactIndex,
                artifactPayloads,
                new String[]{"reproducibility_summary", "reproducibility", "repro"},
                new String[]{"reproducibility_summary", "reproducibility"}
        );
        List<String> artifactFiles = new ArrayList<>(artifactIndex.values());

        GuardrailEngine.GuardrailReport report = GuardrailEngine.evaluate(
                runSummary,
                dataSummary,
                featureSummary,
                splitSummary,
                backtestSummary,
                reproducibilitySummary,
                artifactFiles
        );
        currentGuardrailReport = report;
        currentGuardrailRunId = firstNonBlank(
                stringOrEmpty(findAnyValue(runSummary, "run_id", "runId", "id")),
                loadedRunDetailsId
        );

        StringBuilder checklist = new StringBuilder();
        checklist.append("Reproducibility\n");
        checklist.append("- ").append(mark(report, "snapshots_present")).append(" all snapshots present\n");
        checklist.append("- ").append(mark(report, "data_hashes_resolvable")).append(" data hashes resolvable\n");
        checklist.append("- ").append(mark(report, "re_executable")).append(" run re-executable with same config\n");
        checklist.append("\nGuardrails\n");
        checklist.append("- ").append(mark(report, "backtest_costs")).append(" backtest includes costs/slippage\n");
        checklist.append("- ").append(mark(report, "walk_forward")).append(" training uses walk-forward split\n");
        checklist.append("- ").append(mark(report, "quality_gates")).append(" data quality gates applied\n");
        checklist.append("- ").append(mark(report, "leakage_checks")).append(" leakage checks passed\n");
        runReadinessChecklistArea.setText(checklist.toString());
        runReadinessChecklistArea.setCaretPosition(0);

        StringBuilder findings = new StringBuilder();
        if (report.findings().isEmpty()) {
            findings.append("No guardrail findings. This run is ready.");
        } else {
            for (GuardrailEngine.GuardrailFinding finding : report.findings()) {
                findings.append("[")
                        .append(finding.severity())
                        .append("] ")
                        .append(finding.message())
                        .append(" (")
                        .append(finding.code())
                        .append(")\n");
            }
        }
        runReadinessFindingsArea.setText(findings.toString());
        runReadinessFindingsArea.setCaretPosition(0);

        int score = report.reproducibilityScore();
        Color scoreColor = score >= 90 ? COLOR_SUCCESS : (score >= 70 ? COLOR_WARNING : COLOR_DANGER);
        styleInlineStatus(runReproScoreLabel, "Reproducibility Score: " + score + "/100", scoreColor);

        if (report.checklistGreen()) {
            styleInlineStatus(runReadinessStatusLabel, "Checklist: GREEN", COLOR_SUCCESS);
            styleInlineStatus(runReadinessBannerLabel, "Guardrails: all checks passed", COLOR_SUCCESS);
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: available", COLOR_SUCCESS);
        } else if (report.hasBlocker()) {
            styleInlineStatus(runReadinessStatusLabel, "Checklist: BLOCKED", COLOR_DANGER);
            styleInlineStatus(runReadinessBannerLabel, "Guardrails: BLOCKER found - fix before trading", COLOR_DANGER);
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: blocked", COLOR_DANGER);
        } else {
            styleInlineStatus(runReadinessStatusLabel, "Checklist: ACTION REQUIRED", COLOR_WARNING);
            styleInlineStatus(runReadinessBannerLabel, "Guardrails: warnings present", COLOR_WARNING);
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: available with warnings", COLOR_WARNING);
        }
        updateTrainingGuardrailBanners(report, currentGuardrailRunId);
        updateReadinessActionButtons();
    }

    private Map<String, Object> resolveSectionMap(
            Map<String, Object> run,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads,
            String[] inlineHints,
            String[] artifactHints
    ) {
        Object inlinePayload = findSectionByHints(run, inlineHints);
        String artifactName = findArtifactByHints(artifactIndex, artifactHints);
        Object artifactPayload = artifactName == null ? null : findArtifactPayload(artifactPayloads, artifactName);
        if (artifactPayload != null && !isArtifactError(artifactPayload)) {
            return asObjectOrEmpty(artifactPayload);
        }
        return asObjectOrEmpty(inlinePayload);
    }

    private String mark(GuardrailEngine.GuardrailReport report, String key) {
        boolean ok = report.checklist().getOrDefault(key, false);
        return ok ? "PASS" : "FAIL";
    }

    private void updateReadinessActionButtons() {
        if (runPublishModelButton == null || runUseForTradingButton == null) {
            return;
        }
        boolean hasRun = currentGuardrailRunId != null && !currentGuardrailRunId.isBlank();
        boolean leakageFailed = currentGuardrailReport != null && currentGuardrailReport.leakageFailed();
        boolean enabled = hasRun && !leakageFailed;

        runPublishModelButton.setEnabled(enabled);
        runUseForTradingButton.setEnabled(enabled);
        if (enabled) {
            runPublishModelButton.setToolTipText("Publish selected run");
            runUseForTradingButton.setToolTipText("Enable selected run for trading");
        } else if (leakageFailed) {
            runPublishModelButton.setToolTipText("Blocked: leakage checks failed.");
            runUseForTradingButton.setToolTipText("Blocked: leakage checks failed.");
        } else {
            runPublishModelButton.setToolTipText("Select a run first.");
            runUseForTradingButton.setToolTipText("Select a run first.");
        }
    }

    private void executeReadinessAction(String action) {
        if (currentGuardrailReport == null || currentGuardrailRunId == null || currentGuardrailRunId.isBlank()) {
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: select one run", COLOR_WARNING);
            return;
        }
        if (currentGuardrailReport.leakageFailed()) {
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: blocked by leakage failure", COLOR_DANGER);
            JOptionPane.showMessageDialog(
                    frame,
                    action + " blocked. Leakage checks failed for this run.",
                    "Guardrail Block",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        String message = action + " allowed for run " + shortenRunId(currentGuardrailRunId) + ".";
        if (!currentGuardrailReport.checklistGreen()) {
            message += "\nGuardrail warnings exist. Review Readiness findings first.";
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: allowed with warnings", COLOR_WARNING);
        } else {
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: allowed", COLOR_SUCCESS);
        }
        JOptionPane.showMessageDialog(frame, message, action, JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAuditLogPanel() {
        if (auditLogViewerArea == null || auditLogStatusLabel == null) {
            return;
        }

        styleInlineStatus(auditLogStatusLabel, "Audit: loading...", COLOR_WARNING);
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() throws Exception {
                return auditLogStore.readLatest(300);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> entries = get();
                    StringBuilder sb = new StringBuilder();
                    if (entries.isEmpty()) {
                        sb.append("No audit entries yet.");
                    } else {
                        for (Map<String, Object> entry : entries) {
                            sb.append(Json.compact(entry)).append("\n");
                        }
                    }
                    auditLogViewerArea.setText(sb.toString());
                    auditLogViewerArea.setCaretPosition(0);
                    styleInlineStatus(auditLogStatusLabel, "Audit: loaded " + entries.size() + " entries", COLOR_SUCCESS);
                } catch (Exception ex) {
                    auditLogViewerArea.setText("Failed to load audit logs: " + humanizeError(ex));
                    styleInlineStatus(auditLogStatusLabel, "Audit: load failed", COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void exportAuditLogSnapshot() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export audit log snapshot");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(new File(System.getProperty("user.home")));
        int result = chooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            styleInlineStatus(auditLogStatusLabel, "Audit: export cancelled", COLOR_MUTED);
            return;
        }

        Path targetDir = chooser.getSelectedFile().toPath();
        try {
            Path out = auditLogStore.exportSnapshot(targetDir);
            styleInlineStatus(auditLogStatusLabel, "Audit: exported to " + out, COLOR_SUCCESS);
        } catch (IOException ex) {
            styleInlineStatus(auditLogStatusLabel, "Audit: export failed", COLOR_DANGER);
            appendTrainingLog(ts() + " | Audit export failed: " + ex.getMessage());
        }
    }

    private Map<String, Object> selectBacktestSummary(Map<String, Object> backtestMap) {
        Object summary = findAnyValue(backtestMap,
                "backtest_summary",
                "summary",
                "summary_stats",
                "metrics",
                "statistics",
                "performance");
        if (summary instanceof Map<?, ?> mapRaw) {
            return Json.asObject(mapRaw);
        }
        return backtestMap;
    }

    private Object selectBacktestAssumptions(Map<String, Object> backtestMap) {
        return findAnyValue(backtestMap,
                "assumptions",
                "backtest_assumptions",
                "assumption_snapshot",
                "execution_model",
                "execution_assumptions",
                "config",
                "settings");
    }

    private void updateBacktestMetricLabels(Map<String, Object> summaryMap, Map<String, Object> backtestMap) {
        Object cagr = firstNonNull(
                findAnyValue(summaryMap, "cagr", "annual_return", "annualized_return"),
                findAnyValue(backtestMap, "cagr", "annual_return", "annualized_return"));
        Object sharpe = firstNonNull(
                findAnyValue(summaryMap, "sharpe", "sharpe_ratio"),
                findAnyValue(backtestMap, "sharpe", "sharpe_ratio"));
        Object sortino = firstNonNull(
                findAnyValue(summaryMap, "sortino", "sortino_ratio"),
                findAnyValue(backtestMap, "sortino", "sortino_ratio"));
        Object mdd = firstNonNull(
                findAnyValue(summaryMap, "max_drawdown", "max_dd", "mdd", "drawdown"),
                findAnyValue(backtestMap, "max_drawdown", "max_dd", "mdd", "drawdown"));
        Object profitFactor = firstNonNull(
                findAnyValue(summaryMap, "profit_factor"),
                findAnyValue(backtestMap, "profit_factor"));
        Object winRate = firstNonNull(
                findAnyValue(summaryMap, "win_rate", "hit_rate", "winrate"),
                findAnyValue(backtestMap, "win_rate", "hit_rate", "winrate"));
        Object avgWinLoss = firstNonNull(
                findAnyValue(summaryMap, "avg_win_loss", "avg_win_loss_ratio", "average_win_loss_ratio"),
                findAnyValue(backtestMap, "avg_win_loss", "avg_win_loss_ratio", "average_win_loss_ratio"));
        Object exposure = firstNonNull(
                findAnyValue(summaryMap, "exposure", "avg_exposure", "market_exposure"),
                findAnyValue(backtestMap, "exposure", "avg_exposure", "market_exposure"));
        Object turnover = firstNonNull(
                findAnyValue(summaryMap, "turnover", "portfolio_turnover"),
                findAnyValue(backtestMap, "turnover", "portfolio_turnover"));

        runBacktestCagrValue.setText(formatBacktestMetric(cagr, true));
        runBacktestSharpeValue.setText(formatBacktestMetric(sharpe, false));
        runBacktestSortinoValue.setText(formatBacktestMetric(sortino, false));
        runBacktestMddValue.setText(formatBacktestMetric(mdd, true));
        runBacktestProfitFactorValue.setText(formatBacktestMetric(profitFactor, false));
        runBacktestWinRateValue.setText(formatBacktestMetric(winRate, true));
        runBacktestAvgWinLossValue.setText(formatBacktestAvgWinLoss(avgWinLoss, summaryMap, backtestMap));
        runBacktestExposureValue.setText(formatBacktestMetric(exposure, true));
        runBacktestTurnoverValue.setText(formatBacktestMetric(turnover, false));
    }

    private String formatBacktestMetric(Object value, boolean preferPercent) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Number number) {
            double d = number.doubleValue();
            if (preferPercent) {
                if (Math.abs(d) <= 1.5) {
                    d *= 100.0;
                }
                return String.format("%.2f%%", d);
            }
            return String.format("%.4f", d);
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return "—";
        }
        if (preferPercent && !text.contains("%")) {
            double numeric = Json.asDouble(text, Double.NaN);
            if (!Double.isNaN(numeric)) {
                if (Math.abs(numeric) <= 1.5) {
                    numeric *= 100.0;
                }
                return String.format("%.2f%%", numeric);
            }
        }
        return text;
    }

    private String formatBacktestAvgWinLoss(
            Object explicitMetric,
            Map<String, Object> summaryMap,
            Map<String, Object> backtestMap
    ) {
        if (explicitMetric != null) {
            return formatBacktestMetric(explicitMetric, false);
        }

        Object avgWin = firstNonNull(
                findAnyValue(summaryMap, "avg_win", "average_win"),
                findAnyValue(backtestMap, "avg_win", "average_win"));
        Object avgLoss = firstNonNull(
                findAnyValue(summaryMap, "avg_loss", "average_loss"),
                findAnyValue(backtestMap, "avg_loss", "average_loss"));
        if (avgWin == null && avgLoss == null) {
            return "—";
        }
        String winText = formatBacktestMetric(avgWin, false);
        String lossText = formatBacktestMetric(avgLoss, false);
        return winText + " / " + lossText;
    }

    private String formatBacktestAssumptions(Object assumptionsPayload) {
        if (assumptionsPayload == null) {
            return "Assumptions snapshot not reported.";
        }
        if (assumptionsPayload instanceof Map<?, ?> || assumptionsPayload instanceof List<?>) {
            return Json.pretty(assumptionsPayload);
        }
        String text = String.valueOf(assumptionsPayload).trim();
        return text.isBlank() ? "Assumptions snapshot not reported." : text;
    }

    private String buildBacktestOverviewText(
            Map<String, Object> summaryMap,
            Object assumptionsPayload,
            List<TradeLogRow> tradeRows,
            List<AttributionRow> regimeRows,
            List<AttributionRow> confidenceRows,
            List<AttributionRow> tickerRows
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Backtest Reality Check\n\n");
        sb.append("Summary stats loaded:\n");
        sb.append("- CAGR: ").append(runBacktestCagrValue.getText()).append("\n");
        sb.append("- Sharpe: ").append(runBacktestSharpeValue.getText()).append("\n");
        sb.append("- Sortino: ").append(runBacktestSortinoValue.getText()).append("\n");
        sb.append("- Max drawdown: ").append(runBacktestMddValue.getText()).append("\n");
        sb.append("- Profit factor: ").append(runBacktestProfitFactorValue.getText()).append("\n");
        sb.append("- Win rate: ").append(runBacktestWinRateValue.getText()).append("\n");
        sb.append("- Avg win/loss: ").append(runBacktestAvgWinLossValue.getText()).append("\n");
        sb.append("- Exposure: ").append(runBacktestExposureValue.getText()).append("\n");
        sb.append("- Turnover: ").append(runBacktestTurnoverValue.getText()).append("\n");

        sb.append("\nCoverage:\n");
        sb.append("- Trades: ").append(tradeRows.size()).append("\n");
        sb.append("- Attribution rows (regime/confidence/ticker): ")
                .append(regimeRows.size()).append(" / ")
                .append(confidenceRows.size()).append(" / ")
                .append(tickerRows.size()).append("\n");
        sb.append("- Assumptions snapshot: ").append(assumptionsPayload == null ? "missing" : "present").append("\n");

        if (summaryMap.isEmpty()) {
            sb.append("\nBacktest summary fields were not found in the run payload.");
        }
        return sb.toString();
    }

    private List<SeriesPoint> extractSeriesPoints(Object payload, String kind) {
        if (payload == null) {
            return new ArrayList<>();
        }
        Object candidate = payload;
        if (payload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            if ("equity".equals(kind)) {
                candidate = firstNonNull(
                        findAnyValue(map, "equity_curve", "equity_series", "equity", "portfolio_value_curve"),
                        map);
            } else {
                candidate = firstNonNull(
                        findAnyValue(map, "drawdown_curve", "drawdown_series", "drawdown", "drawdowns"),
                        map);
            }
        }

        List<SeriesPoint> points = parseSeriesCandidate(candidate, kind);
        if (points.isEmpty() && payload instanceof Map<?, ?> mapRaw) {
            points = parseSeriesCandidate(Json.asObject(mapRaw), kind);
        }
        return points;
    }

    private List<SeriesPoint> parseSeriesCandidate(Object candidate, String kind) {
        List<SeriesPoint> points = new ArrayList<>();
        if (candidate == null) {
            return points;
        }
        if (candidate instanceof List<?> list) {
            int idx = 1;
            for (Object item : list) {
                if (item instanceof Number num) {
                    points.add(new SeriesPoint(String.valueOf(idx++), num.doubleValue()));
                    continue;
                }
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }
                Map<String, Object> m = Json.asObject(itemMapRaw);
                Object valueObj = firstNonNull(
                        findAnyValue(m, kind, "value", "y", "amount", "equity", "drawdown"),
                        findAnyValue(m, "metric", "score"));
                Double value = asDoubleObject(valueObj);
                if (value == null) {
                    continue;
                }
                String label = firstNonBlank(
                        stringOrEmpty(findAnyValue(m, "timestamp", "time", "date", "x", "label", "index")),
                        String.valueOf(idx)
                );
                points.add(new SeriesPoint(label, value));
                idx++;
            }
            return points;
        }
        if (candidate instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object yValues = findAnyValue(map, "values", "y", "series", kind, "equity", "drawdown");
            Object xValues = findAnyValue(map, "timestamps", "times", "x", "labels", "dates", "index");
            if (yValues instanceof List<?> yList) {
                List<?> xList = xValues instanceof List<?> xl ? xl : new ArrayList<>();
                for (int i = 0; i < yList.size(); i++) {
                    Double value = asDoubleObject(yList.get(i));
                    if (value == null) {
                        continue;
                    }
                    String label = i < xList.size() ? String.valueOf(xList.get(i)) : String.valueOf(i + 1);
                    points.add(new SeriesPoint(label, value));
                }
                return points;
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Double value = asDoubleObject(entry.getValue());
                if (value != null) {
                    points.add(new SeriesPoint(entry.getKey(), value));
                }
            }
        }
        return points;
    }

    private List<TradeLogRow> extractTradeLogRows(Object payload) {
        List<TradeLogRow> rows = new ArrayList<>();
        if (payload == null) {
            return rows;
        }
        Object candidate = payload;
        if (payload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object nested = findAnyValue(map,
                    "trade_log",
                    "trade_list",
                    "trades",
                    "executions",
                    "rows",
                    "items");
            if (nested != null) {
                candidate = nested;
            }
        }
        if (!(candidate instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemMapRaw)) {
                continue;
            }
            Map<String, Object> m = Json.asObject(itemMapRaw);
            String ticker = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "ticker", "symbol", "asset")),
                    "—"
            );
            String entryTime = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "entry_time", "entry_timestamp", "entered_at", "open_time")),
                    "—"
            );
            String exitTime = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "exit_time", "exit_timestamp", "exited_at", "close_time")),
                    "—"
            );
            String size = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "size", "position_size", "quantity", "shares")),
                    "—"
            );
            String entryPrice = formatPredictionValue(findAnyValue(m, "entry_price", "open_price", "buy_price"));
            String exitPrice = formatPredictionValue(findAnyValue(m, "exit_price", "close_price", "sell_price"));
            String pnl = formatPredictionValue(findAnyValue(m, "pnl", "net_pnl", "profit_loss", "return"));
            String mae = formatPredictionValue(findAnyValue(m, "mae", "max_adverse_excursion"));
            String mfe = formatPredictionValue(findAnyValue(m, "mfe", "max_favorable_excursion"));
            String reason = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "reason_code", "reason", "entry_reason", "exit_reason")),
                    "—"
            );
            String regime = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "regime", "regime_label", "regime_at_entry", "entry_regime")),
                    "—"
            );
            String confidence = formatPredictionValue(findAnyValue(m, "confidence", "probability", "signal_confidence"));
            rows.add(new TradeLogRow(
                    ticker,
                    entryTime,
                    exitTime,
                    size,
                    entryPrice,
                    exitPrice,
                    pnl,
                    mae,
                    mfe,
                    reason,
                    regime,
                    confidence,
                    m
            ));
        }
        return rows;
    }

    private List<AttributionRow> extractAttributionRows(Object payload, String mode) {
        List<AttributionRow> rows = new ArrayList<>();
        if (payload == null) {
            return rows;
        }
        Object candidate = payload;
        if (payload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            if ("regime".equals(mode)) {
                candidate = firstNonNull(
                        findAnyValue(map, "performance_by_regime", "attribution_by_regime", "regime_performance", "by_regime"),
                        map);
            } else if ("confidence".equals(mode)) {
                candidate = firstNonNull(
                        findAnyValue(map, "performance_by_confidence", "attribution_by_confidence", "confidence_buckets", "by_confidence"),
                        map);
            } else {
                candidate = firstNonNull(
                        findAnyValue(map, "performance_by_ticker", "performance_by_sector", "attribution_by_ticker", "by_ticker", "by_sector"),
                        map);
            }
        }

        if (candidate instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }
                Map<String, Object> m = Json.asObject(itemMapRaw);
                rows.add(toAttributionRow(m, mode));
            }
            return rows;
        }
        if (candidate instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> valueMapRaw) {
                    Map<String, Object> m = Json.asObject(valueMapRaw);
                    if (findAnyValue(m, "bucket", "label", "name", "regime", "ticker", "sector") == null) {
                        m.put("bucket", entry.getKey());
                    }
                    rows.add(toAttributionRow(m, mode));
                } else {
                    Double value = asDoubleObject(entry.getValue());
                    rows.add(new AttributionRow(
                            entry.getKey(),
                            "—",
                            value == null ? "—" : String.format("%.6f", value),
                            "—",
                            "—",
                            "—",
                            new LinkedHashMap<>()
                    ));
                }
            }
        }
        return rows;
    }

    private AttributionRow toAttributionRow(Map<String, Object> row, String mode) {
        String bucket;
        if ("regime".equals(mode)) {
            bucket = firstNonBlank(
                    stringOrEmpty(findAnyValue(row, "regime", "regime_label", "bucket", "label", "name")),
                    "—"
            );
        } else if ("confidence".equals(mode)) {
            bucket = firstNonBlank(
                    stringOrEmpty(findAnyValue(row, "confidence_bucket", "bucket", "label", "name")),
                    "—"
            );
        } else {
            String ticker = stringOrEmpty(findAnyValue(row, "ticker", "symbol", "asset", "bucket", "label"));
            String sector = stringOrEmpty(findAnyValue(row, "sector", "group"));
            bucket = ticker;
            if (!sector.isBlank()) {
                bucket = (bucket.isBlank() ? "—" : bucket) + " / " + sector;
            }
            if (bucket.isBlank()) {
                bucket = "—";
            }
        }

        String trades = firstNonBlank(
                stringOrEmpty(findAnyValue(row, "trades", "trade_count", "count", "n_trades")),
                "—"
        );
        String pnl = formatPredictionValue(findAnyValue(row, "pnl", "net_pnl", "profit", "return"));
        String winRate = formatBacktestMetric(findAnyValue(row, "win_rate", "hit_rate", "winrate"), true);
        String sharpe = formatBacktestMetric(findAnyValue(row, "sharpe", "sharpe_ratio"), false);
        String avgReturn = formatBacktestMetric(findAnyValue(row, "avg_return", "average_return", "mean_return"), true);
        return new AttributionRow(bucket, trades, pnl, winRate, sharpe, avgReturn, row);
    }

    private void applyTradeLogFilter() {
        if (runTradeLogTableSorter == null || runTradeLogTableModel == null || runTradeLogSearchField == null) {
            return;
        }
        String query = runTradeLogSearchField.getText().trim().toLowerCase();
        if (query.isBlank()) {
            runTradeLogTableSorter.setRowFilter(null);
            return;
        }

        runTradeLogTableSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TradeLogTableModel, ? extends Integer> entry) {
                TradeLogRow row = runTradeLogTableModel.getRow(entry.getIdentifier());
                if (row == null) {
                    return false;
                }
                String haystack = (
                        safeLower(row.ticker()) + " "
                                + safeLower(row.entryTime()) + " "
                                + safeLower(row.exitTime()) + " "
                                + safeLower(row.size()) + " "
                                + safeLower(row.entryPrice()) + " "
                                + safeLower(row.exitPrice()) + " "
                                + safeLower(row.pnl()) + " "
                                + safeLower(row.mae()) + " "
                                + safeLower(row.mfe()) + " "
                                + safeLower(row.reason()) + " "
                                + safeLower(row.regime()) + " "
                                + safeLower(row.confidence())
                );
                return haystack.contains(query);
            }
        });
    }

    private void exportTradeLogCsv() {
        if (runTradeLogTableModel == null || runTradeLogTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                    frame,
                    "No trade log rows available to export.",
                    "Export Trade Log",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export trade log CSV");
        String defaultName = loadedRunDetailsId == null || loadedRunDetailsId.isBlank()
                ? "trade_log.csv"
                : "trade_log_" + shortenRunId(loadedRunDetailsId) + ".csv";
        chooser.setSelectedFile(new File(defaultName));
        int result = chooser.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        try {
            Path out = file.toPath();
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }

            StringBuilder csv = new StringBuilder();
            csv.append("ticker,entry_time,exit_time,size,entry_price,exit_price,pnl,mae,mfe,reason,regime,confidence\n");
            for (TradeLogRow row : runTradeLogTableModel.rows()) {
                csv.append(csvCell(row.ticker())).append(",");
                csv.append(csvCell(row.entryTime())).append(",");
                csv.append(csvCell(row.exitTime())).append(",");
                csv.append(csvCell(row.size())).append(",");
                csv.append(csvCell(row.entryPrice())).append(",");
                csv.append(csvCell(row.exitPrice())).append(",");
                csv.append(csvCell(row.pnl())).append(",");
                csv.append(csvCell(row.mae())).append(",");
                csv.append(csvCell(row.mfe())).append(",");
                csv.append(csvCell(row.reason())).append(",");
                csv.append(csvCell(row.regime())).append(",");
                csv.append(csvCell(row.confidence())).append("\n");
            }
            Files.writeString(out, csv.toString(), StandardCharsets.UTF_8);
            styleInlineStatus(runBacktestStatusLabel, "Backtest: trade log exported to " + out, COLOR_SUCCESS);
        } catch (Exception ex) {
            styleInlineStatus(runBacktestStatusLabel, "Backtest: trade log export failed", COLOR_DANGER);
            JOptionPane.showMessageDialog(
                    frame,
                    "Failed to export trade log CSV:\n" + humanizeError(ex),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        boolean mustQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        if (text.contains("\"")) {
            text = text.replace("\"", "\"\"");
        }
        if (mustQuote) {
            return "\"" + text + "\"";
        }
        return text;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String formatCalibrationMethod(Map<String, Object> modelMap) {
        Object method = findAnyValue(modelMap, "calibration_method", "calibration", "probability_calibration");
        if (method instanceof Map<?, ?> methodMapRaw) {
            Map<String, Object> methodMap = Json.asObject(methodMapRaw);
            String nestedMethod = stringOrEmpty(findAnyValue(methodMap, "method", "name", "type"));
            if (!nestedMethod.isBlank()) {
                return nestedMethod;
            }
        }
        String text = stringOrEmpty(method);
        return text.isBlank() ? "none" : text;
    }

    private String formatCalibrationMetrics(Map<String, Object> modelMap) {
        Object calibrationMetrics = findAnyValue(modelMap, "calibration_metrics", "metrics");
        if (calibrationMetrics instanceof Map<?, ?> cmRaw) {
            Map<String, Object> cm = Json.asObject(cmRaw);
            String brier = formatMetric(findAnyValue(cm, "brier", "brier_score"));
            String ece = formatMetric(findAnyValue(cm, "ece", "expected_calibration_error"));
            if (!brier.equals("—") || !ece.equals("—")) {
                return "Brier=" + brier + ", ECE=" + ece;
            }
        }
        String brier = formatMetric(findAnyValue(modelMap, "brier", "brier_score"));
        String ece = formatMetric(findAnyValue(modelMap, "ece", "expected_calibration_error"));
        if (!brier.equals("—") || !ece.equals("—")) {
            return "Brier=" + brier + ", ECE=" + ece;
        }
        return "not reported";
    }

    private String formatMetric(Object value) {
        if (value == null) {
            return "—";
        }
        double v = Json.asDouble(value, Double.NaN);
        if (Double.isNaN(v)) {
            String text = String.valueOf(value).trim();
            return text.isBlank() ? "—" : text;
        }
        return String.format("%.6f", v);
    }

    private String formatConfidenceOutputType(Map<String, Object> run, Map<String, Object> modelMap) {
        String explicit = stringOrEmpty(findAnyValue(modelMap,
                "confidence_output_type",
                "confidence_type",
                "output_confidence_type",
                "prediction_output_type"));
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (findAnyValue(modelMap, "prediction_interval", "quantiles", "std_dev_proxy", "prediction_std") != null) {
            return "prediction interval";
        }
        String taskType = stringOrEmpty(findAnyValue(run, "task_type", "prediction_type", "objective"));
        if (safeLower(taskType).contains("regression")) {
            return "prediction interval";
        }
        return "probability";
    }

    private String formatThresholdLogic(Map<String, Object> modelMap) {
        Object thresholds = findAnyValue(modelMap,
                "threshold_logic",
                "trade_threshold_logic",
                "probability_threshold",
                "decision_thresholds",
                "signal_threshold");
        if (thresholds == null) {
            return "none / not reported";
        }
        if (thresholds instanceof Map<?, ?> || thresholds instanceof List<?>) {
            return Json.compact(thresholds);
        }
        String text = String.valueOf(thresholds).trim();
        return text.isBlank() ? "none / not reported" : text;
    }

    private String formatTrainingCurves(Object trainingCurves) {
        if (trainingCurves == null) {
            return "Training curves not reported.";
        }
        if (trainingCurves instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object trainLoss = findAnyValue(map, "train_loss", "loss", "training_loss");
            Object valLoss = findAnyValue(map, "val_loss", "validation_loss", "test_loss");
            Object epochsObj = findAnyValue(map, "epochs", "epoch");
            if (trainLoss instanceof List<?> trainList) {
                List<?> valList = valLoss instanceof List<?> l ? l : new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                sb.append("Epoch | Train Loss | Val Loss\n");
                int max = trainList.size();
                for (int i = 0; i < max; i++) {
                    String epochLabel = epochsObj instanceof List<?> epList && i < epList.size()
                            ? String.valueOf(epList.get(i))
                            : String.valueOf(i + 1);
                    String train = String.valueOf(trainList.get(i));
                    String val = i < valList.size() ? String.valueOf(valList.get(i)) : "—";
                    sb.append(String.format("%5s | %10s | %8s%n", epochLabel, train, val));
                }
                return sb.toString();
            }
        }
        if (trainingCurves instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("Epoch | Loss\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(String.format("%5d | %s%n", i + 1, String.valueOf(list.get(i))));
            }
            return sb.toString();
        }
        return Json.pretty(trainingCurves);
    }

    private List<FailureRow> extractFailureRows(Object failurePayload) {
        List<FailureRow> rows = new ArrayList<>();
        Object candidate = failurePayload;
        if (failurePayload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            Object nested = findAnyValue(map,
                    "worst_predictions",
                    "top_failures",
                    "largest_errors",
                    "errors",
                    "failures",
                    "rows",
                    "samples");
            if (nested != null) {
                candidate = nested;
            }
        }
        if (!(candidate instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> itemMapRaw)) {
                continue;
            }
            Map<String, Object> m = Json.asObject(itemMapRaw);
            String timestamp = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "timestamp", "time", "datetime", "date")),
                    "—"
            );
            String ticker = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "ticker", "symbol", "asset")),
                    "—"
            );
            String predicted = formatPredictionValue(findAnyValue(m, "predicted", "prediction", "predicted_value"));
            String confidence = formatPredictionValue(findAnyValue(m, "confidence", "probability", "predicted_confidence"));
            String actual = formatPredictionValue(findAnyValue(m, "actual", "actual_outcome", "actual_value", "label"));
            String regime = firstNonBlank(
                    stringOrEmpty(findAnyValue(m, "regime", "regime_label", "market_regime")),
                    "—"
            );
            Double errorValue = asDoubleObject(findAnyValue(m, "loss", "error", "abs_error", "pnl_loss"));
            if (errorValue == null) {
                Double predNum = asDoubleObject(findAnyValue(m, "predicted", "prediction", "predicted_value"));
                Double actualNum = asDoubleObject(findAnyValue(m, "actual", "actual_outcome", "actual_value"));
                if (predNum != null && actualNum != null) {
                    errorValue = Math.abs(predNum - actualNum);
                }
            }
            String contributions = formatTopContributions(findAnyValue(m,
                    "top_contributing_features",
                    "top_features",
                    "shap",
                    "feature_contributions",
                    "contributions"));
            rows.add(new FailureRow(
                    timestamp,
                    ticker,
                    predicted,
                    confidence,
                    actual,
                    regime,
                    errorValue == null ? "—" : String.format("%.6f", errorValue),
                    contributions,
                    errorValue,
                    m
            ));
        }

        rows.sort((a, b) -> Double.compare(
                b.errorScore() == null ? Double.NEGATIVE_INFINITY : b.errorScore(),
                a.errorScore() == null ? Double.NEGATIVE_INFINITY : a.errorScore()
        ));
        if (rows.size() > 20) {
            return new ArrayList<>(rows.subList(0, 20));
        }
        return rows;
    }

    private String formatPredictionValue(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Number n) {
            return String.format("%.6f", n.doubleValue());
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "—" : text;
    }

    private String formatTopContributions(Object value) {
        if (value == null) {
            return "not reported";
        }
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            int limit = Math.min(8, list.size());
            for (int i = 0; i < limit; i++) {
                Object item = list.get(i);
                if (item instanceof Map<?, ?> itemMapRaw) {
                    Map<String, Object> m = Json.asObject(itemMapRaw);
                    String name = firstNonBlank(
                            stringOrEmpty(findAnyValue(m, "feature", "feature_name", "name")),
                            "feature"
                    );
                    String contribution = formatPredictionValue(findAnyValue(m, "contribution", "value", "shap"));
                    parts.add(name + ":" + contribution);
                } else if (item != null) {
                    parts.add(String.valueOf(item));
                }
            }
            return parts.isEmpty() ? "not reported" : String.join(", ", parts);
        }
        if (value instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                parts.add(e.getKey() + ":" + formatPredictionValue(e.getValue()));
            }
            return parts.isEmpty() ? "not reported" : String.join(", ", parts);
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "not reported" : text;
    }

    private String formatImportance(double value) {
        if (Double.isNaN(value)) {
            return "—";
        }
        return String.format("%.6f", value);
    }

    private String computeStabilityFromFolds(Object foldPayload) {
        if (!(foldPayload instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        List<Double> values = new ArrayList<>();
        for (Object v : list) {
            double d = Json.asDouble(v, Double.NaN);
            if (!Double.isNaN(d)) {
                values.add(d);
            }
        }
        if (values.size() < 2) {
            return "";
        }
        double mean = 0.0;
        for (double v : values) {
            mean += v;
        }
        mean /= values.size();
        double var = 0.0;
        for (double v : values) {
            double diff = v - mean;
            var += diff * diff;
        }
        var /= values.size();
        double std = Math.sqrt(var);
        return String.format("std=%.6f", std);
    }

    private String formatParameters(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return Json.compact(value);
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "—" : text;
    }

    private String formatDependencies(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof List<?> list) {
            List<String> deps = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    deps.add(String.valueOf(item));
                }
            }
            return deps.isEmpty() ? "—" : String.join(", ", deps);
        }
        if (value instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            List<String> deps = new ArrayList<>();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                deps.add(e.getKey() + ":" + String.valueOf(e.getValue()));
            }
            return deps.isEmpty() ? "—" : String.join(", ", deps);
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "—" : text;
    }

    private String formatPercentageFlexible(Object value) {
        if (value == null) {
            return "—";
        }
        double number = Json.asDouble(value, Double.NaN);
        if (Double.isNaN(number)) {
            String text = String.valueOf(value).trim();
            return text.isBlank() ? "—" : text;
        }
        if (number <= 1.0) {
            number *= 100.0;
        }
        return String.format("%.2f%%", number);
    }

    private String formatBoolean(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return "—";
        }
        return text;
    }

    private boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        if (text.isBlank()) {
            return fallback;
        }
        if ("true".equals(text) || "yes".equals(text) || "1".equals(text) || "kept".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "no".equals(text) || "0".equals(text) || "dropped".equals(text)) {
            return false;
        }
        return fallback;
    }

    private Double asDoubleObject(Object value) {
        if (value == null) {
            return null;
        }
        double d = Json.asDouble(value, Double.NaN);
        return Double.isNaN(d) ? null : d;
    }

    private Object selectDataSummaryPayload(Object inlineData, Object artifactData) {
        if (artifactData != null && !isArtifactError(artifactData)) {
            if (artifactData instanceof Map<?, ?> artifactMapRaw) {
                Map<String, Object> artifactMap = Json.asObject(artifactMapRaw);
                Object nested = findAnyValue(artifactMap, "data_summary", "datasummary", "summary", "data");
                if (nested != null && !(nested instanceof String)) {
                    return nested;
                }
            }
            return artifactData;
        }
        return inlineData;
    }

    private Object selectDataHashesPayload(Object inlineHashes, Object artifactHashes) {
        if (artifactHashes != null && !isArtifactError(artifactHashes)) {
            if (artifactHashes instanceof Map<?, ?> artifactMapRaw) {
                Map<String, Object> artifactMap = Json.asObject(artifactMapRaw);
                Object nested = findAnyValue(artifactMap, "data_hashes", "hashes", "dataset_hashes", "hash");
                if (nested != null && !(nested instanceof String)) {
                    return nested;
                }
            }
            return artifactHashes;
        }
        return inlineHashes;
    }

    private String buildDataTransparencySummary(
            Map<String, Object> run,
            Object dataSummaryPayload,
            Object dataHashesPayload,
            List<DataTickerStatsRow> tickerRows
    ) {
        Map<String, Object> dataSummary = asObjectOrEmpty(dataSummaryPayload);
        Map<String, Object> dataHashes = asObjectOrEmpty(dataHashesPayload);
        StringBuilder sb = new StringBuilder();

        sb.append("Data Transparency\n\n");
        sb.append("Data sources: ").append(formatDataSources(dataSummary)).append("\n");
        sb.append("Source refresh timestamps: ").append(formatSourceRefreshTimes(dataSummary)).append("\n");
        sb.append("Time range: ").append(formatDataRange(run, dataSummary)).append("\n");
        sb.append("Timeframe: ").append(formatTimeframe(run, dataSummary)).append("\n");
        sb.append("Corporate actions: ").append(formatCorporateActions(dataSummary)).append("\n");
        sb.append("Outlier detection: ").append(formatOutlierReport(dataSummary)).append("\n");
        sb.append("Class balance: ").append(formatClassBalance(dataSummary)).append("\n");
        sb.append("Target definition: ").append(formatTargetDefinition(run, dataSummary)).append("\n");
        sb.append("Dataset hash IDs: ").append(formatDatasetHashes(dataHashes)).append("\n");

        sb.append("\nPer-ticker bar quality:\n");
        if (tickerRows.isEmpty()) {
            sb.append("- No per-ticker stats found in run payload.\n");
        } else {
            for (DataTickerStatsRow row : tickerRows) {
                sb.append("- ").append(row.ticker())
                        .append(": bars=").append(formatCount(row.bars()))
                        .append(", missing=").append(formatCount(row.missingBars()))
                        .append(", repaired=").append(formatCount(row.repairedBars()))
                        .append(", dropped=").append(formatCount(row.droppedBars()))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    private List<DataTickerStatsRow> extractTickerStats(Object dataSummaryPayload) {
        List<DataTickerStatsRow> rows = new ArrayList<>();
        Map<String, Object> summary = asObjectOrEmpty(dataSummaryPayload);
        Object candidate = findAnyValue(summary,
                "per_ticker_stats",
                "ticker_stats",
                "symbol_stats",
                "per_symbol_stats",
                "tickers");

        if (candidate instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> itemMapRaw)) {
                    continue;
                }
                Map<String, Object> itemMap = Json.asObject(itemMapRaw);
                rows.add(new DataTickerStatsRow(
                        firstNonBlank(
                                stringOrEmpty(findAnyValue(itemMap, "ticker", "symbol", "name")),
                                "—"
                        ),
                        asLong(findAnyValue(itemMap, "bars", "bar_count", "bars_count", "num_bars")),
                        asLong(findAnyValue(itemMap, "missing_bars", "missing", "missing_count")),
                        asLong(findAnyValue(itemMap, "repaired_bars", "repaired", "imputed_bars")),
                        asLong(findAnyValue(itemMap, "dropped_bars", "dropped", "removed_bars"))
                ));
            }
        } else if (candidate instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String ticker = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> valueMapRaw) {
                    Map<String, Object> valueMap = Json.asObject(valueMapRaw);
                    rows.add(new DataTickerStatsRow(
                            ticker,
                            asLong(findAnyValue(valueMap, "bars", "bar_count", "bars_count", "num_bars")),
                            asLong(findAnyValue(valueMap, "missing_bars", "missing", "missing_count")),
                            asLong(findAnyValue(valueMap, "repaired_bars", "repaired", "imputed_bars")),
                            asLong(findAnyValue(valueMap, "dropped_bars", "dropped", "removed_bars"))
                    ));
                } else {
                    rows.add(new DataTickerStatsRow(ticker, asLong(value), null, null, null));
                }
            }
        }
        return rows;
    }

    private String formatDataSources(Map<String, Object> dataSummary) {
        Object sources = findAnyValue(dataSummary, "sources", "data_sources", "source_list", "inputs");
        if (sources instanceof List<?> list && !list.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> mapRaw) {
                    Map<String, Object> map = Json.asObject(mapRaw);
                    String type = stringOrEmpty(findAnyValue(map, "type", "category", "kind"));
                    String name = stringOrEmpty(findAnyValue(map, "name", "source", "provider"));
                    String label = firstNonBlank(name, type);
                    if (!label.isBlank()) {
                        names.add(label);
                    }
                } else if (item != null) {
                    names.add(String.valueOf(item));
                }
            }
            if (!names.isEmpty()) {
                return String.join(", ", names);
            }
        }
        return "not reported";
    }

    private String formatSourceRefreshTimes(Map<String, Object> dataSummary) {
        Object sources = findAnyValue(dataSummary, "sources", "data_sources", "source_list");
        if (sources instanceof List<?> list && !list.isEmpty()) {
            List<String> stamps = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> mapRaw)) {
                    continue;
                }
                Map<String, Object> map = Json.asObject(mapRaw);
                String name = firstNonBlank(
                        stringOrEmpty(findAnyValue(map, "name", "source", "provider")),
                        stringOrEmpty(findAnyValue(map, "type", "category"))
                );
                String tsValue = stringOrEmpty(findAnyValue(map,
                        "refresh_timestamp",
                        "updated_at",
                        "last_refresh",
                        "as_of"));
                if (!name.isBlank() || !tsValue.isBlank()) {
                    stamps.add((name.isBlank() ? "source" : name) + "=" + (tsValue.isBlank() ? "n/a" : tsValue));
                }
            }
            if (!stamps.isEmpty()) {
                return String.join(", ", stamps);
            }
        }
        return "not reported";
    }

    private String formatDataRange(Map<String, Object> run, Map<String, Object> dataSummary) {
        String start = firstNonBlank(
                stringOrEmpty(findAnyValue(dataSummary, "start", "start_time", "data_start", "from")),
                stringOrEmpty(findAnyValue(run, "data_start", "dataset_start", "start_date", "from"))
        );
        String end = firstNonBlank(
                stringOrEmpty(findAnyValue(dataSummary, "end", "end_time", "data_end", "to")),
                stringOrEmpty(findAnyValue(run, "data_end", "dataset_end", "end_date", "to"))
        );
        if (start.isBlank() && end.isBlank()) {
            String inline = stringOrEmpty(findAnyValue(dataSummary, "range", "date_range", "dataset_range"));
            return inline.isBlank() ? "not reported" : inline;
        }
        return (start.isBlank() ? "?" : start) + " -> " + (end.isBlank() ? "?" : end);
    }

    private String formatTimeframe(Map<String, Object> run, Map<String, Object> dataSummary) {
        String timeframe = firstNonBlank(
                stringOrEmpty(findAnyValue(dataSummary, "timeframe", "interval", "bar_size", "resolution")),
                stringOrEmpty(findAnyValue(run, "timeframe", "interval", "resolution"))
        );
        return timeframe.isBlank() ? "not reported" : timeframe;
    }

    private String formatCorporateActions(Map<String, Object> dataSummary) {
        String splitAdj = stringOrEmpty(findAnyValue(dataSummary, "split_adjusted", "split_adjustments", "splits_applied"));
        String divAdj = stringOrEmpty(findAnyValue(dataSummary,
                "dividend_adjusted",
                "dividend_adjustments",
                "dividends_applied"));
        String closeMode = stringOrEmpty(findAnyValue(dataSummary,
                "close_mode",
                "adjusted_close_used",
                "price_adjustment_mode",
                "adjusted_vs_unadjusted"));
        List<String> parts = new ArrayList<>();
        if (!splitAdj.isBlank()) {
            parts.add("splits=" + splitAdj);
        }
        if (!divAdj.isBlank()) {
            parts.add("dividends=" + divAdj);
        }
        if (!closeMode.isBlank()) {
            parts.add("close=" + closeMode);
        }
        if (parts.isEmpty()) {
            return "not reported";
        }
        return String.join(", ", parts);
    }

    private String formatOutlierReport(Map<String, Object> dataSummary) {
        Object flagged = findAnyValue(dataSummary, "outliers_flagged", "outlier_count", "num_outliers");
        String rules = stringOrEmpty(findAnyValue(dataSummary, "outlier_rules", "rules", "detection_rules"));
        String actions = stringOrEmpty(findAnyValue(dataSummary, "outlier_actions", "actions_taken", "outlier_action"));
        List<String> parts = new ArrayList<>();
        if (flagged != null) {
            parts.add("flagged=" + formatCount(asLong(flagged)));
        }
        if (!rules.isBlank()) {
            parts.add("rules=" + rules);
        }
        if (!actions.isBlank()) {
            parts.add("actions=" + actions);
        }
        if (parts.isEmpty()) {
            return "not reported";
        }
        return String.join(", ", parts);
    }

    private String formatClassBalance(Map<String, Object> dataSummary) {
        String up = asPercent(findAnyValue(dataSummary, "up_pct", "pct_up", "class_up_pct"));
        String down = asPercent(findAnyValue(dataSummary, "down_pct", "pct_down", "class_down_pct"));
        String flat = asPercent(findAnyValue(dataSummary, "flat_pct", "pct_flat", "class_flat_pct"));
        String threshold = stringOrEmpty(findAnyValue(dataSummary,
                "class_threshold",
                "label_threshold",
                "threshold_definition"));
        if ("—".equals(up) && "—".equals(down) && "—".equals(flat) && threshold.isBlank()) {
            Object distribution = findAnyValue(dataSummary, "class_balance", "label_distribution");
            if (distribution != null) {
                return Json.pretty(distribution);
            }
            return "not reported";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("up=").append(up).append(", down=").append(down).append(", flat=").append(flat);
        if (!threshold.isBlank()) {
            sb.append(", threshold=").append(threshold);
        }
        return sb.toString();
    }

    private String formatTargetDefinition(Map<String, Object> run, Map<String, Object> dataSummary) {
        String targetMath = firstNonBlank(
                stringOrEmpty(findAnyValue(dataSummary, "target_definition", "label_math", "target_math")),
                stringOrEmpty(findAnyValue(run, "target_definition", "label_definition", "target"))
        );
        String horizon = firstNonBlank(
                stringOrEmpty(findAnyValue(dataSummary, "horizon", "horizon_bars", "target_horizon")),
                stringOrEmpty(findAnyValue(run, "horizon", "horizon_days", "target_horizon"))
        );
        String threshold = stringOrEmpty(findAnyValue(dataSummary, "threshold", "label_threshold", "target_threshold"));
        if (targetMath.isBlank() && horizon.isBlank() && threshold.isBlank()) {
            return "not reported";
        }
        List<String> parts = new ArrayList<>();
        if (!targetMath.isBlank()) {
            parts.add(targetMath);
        }
        if (!horizon.isBlank()) {
            parts.add("horizon=" + horizon);
        }
        if (!threshold.isBlank()) {
            parts.add("threshold=" + threshold);
        }
        return String.join(", ", parts);
    }

    private String formatDatasetHashes(Map<String, Object> dataHashes) {
        if (dataHashes.isEmpty()) {
            return "not reported";
        }
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dataHashes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof String text) {
                ids.add(key + "=" + text);
            } else {
                ids.add(key + "=" + String.valueOf(value));
            }
        }
        if (ids.isEmpty()) {
            return "not reported";
        }
        return String.join(", ", ids);
    }

    private void toggleDataRawPrettyPrint() {
        runDataPrettyEnabled = !runDataPrettyEnabled;
        runDataPrettyPrintButton.setText(runDataPrettyEnabled ? "Pretty JSON: ON" : "Pretty JSON: OFF");
        refreshDataRawJsonViewer();
    }

    private void refreshDataRawJsonViewer() {
        if (runDataRawJsonArea == null) {
            return;
        }
        if (runDataRawPayload == null) {
            runDataRawJsonArea.setText("");
            return;
        }
        String text = runDataPrettyEnabled ? Json.pretty(runDataRawPayload) : Json.compact(runDataRawPayload);
        runDataRawJsonArea.setText(text);
        runDataRawJsonArea.setCaretPosition(0);
    }

    private void exportDatasetManifest() {
        Map<String, Object> summary = asObjectOrWrapped(runDataSummaryPayload, "data_summary");
        Map<String, Object> hashes = asObjectOrWrapped(runDataHashesPayload, "data_hashes");
        if (summary.isEmpty() && hashes.isEmpty()) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: no data to export", COLOR_WARNING);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select export folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int choice = chooser.showSaveDialog(frame);
        if (choice != JFileChooser.APPROVE_OPTION) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: export cancelled", COLOR_MUTED);
            return;
        }

        File dir = chooser.getSelectedFile();
        if (dir == null) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: export cancelled", COLOR_MUTED);
            return;
        }
        try {
            Path folder = dir.toPath();
            Files.createDirectories(folder);
            Path summaryPath = folder.resolve("data_summary.json");
            Path hashesPath = folder.resolve("data_hashes.json");
            Files.writeString(summaryPath, Json.pretty(summary), StandardCharsets.UTF_8);
            Files.writeString(hashesPath, Json.pretty(hashes), StandardCharsets.UTF_8);
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: exported to " + folder, COLOR_SUCCESS);
        } catch (Exception ex) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: export failed", COLOR_DANGER);
            JOptionPane.showMessageDialog(
                    frame,
                    "Failed to export manifest files:\n" + humanizeError(ex),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Object findArtifactPayload(Map<String, Object> artifactPayloads, String artifactName) {
        if (artifactPayloads == null || artifactName == null) {
            return null;
        }
        if (artifactPayloads.containsKey(artifactName)) {
            return artifactPayloads.get(artifactName);
        }
        String target = normalizeKey(artifactName);
        for (Map.Entry<String, Object> entry : artifactPayloads.entrySet()) {
            if (normalizeKey(entry.getKey()).equals(target)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isArtifactError(Object value) {
        if (!(value instanceof Map<?, ?> mapRaw)) {
            return false;
        }
        Map<String, Object> map = Json.asObject(mapRaw);
        return findAnyValue(map, "__error", "error") != null;
    }

    private String extractArtifactError(Object value) {
        if (!(value instanceof Map<?, ?> mapRaw)) {
            return "unknown error";
        }
        Map<String, Object> map = Json.asObject(mapRaw);
        Object error = findAnyValue(map, "__error", "error");
        return error == null ? "unknown error" : String.valueOf(error);
    }

    private Map<String, Object> asObjectOrEmpty(Object value) {
        if (value instanceof Map<?, ?> mapRaw) {
            return Json.asObject(mapRaw);
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> asObjectOrWrapped(Object value, String key) {
        if (value instanceof Map<?, ?> mapRaw) {
            return Json.asObject(mapRaw);
        }
        Map<String, Object> wrapped = new LinkedHashMap<>();
        if (value != null) {
            wrapped.put(key, value);
        }
        return wrapped;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String text) {
            try {
                if (text.contains(".")) {
                    return (long) Double.parseDouble(text);
                }
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String formatCount(Long value) {
        if (value == null) {
            return "—";
        }
        return String.valueOf(value);
    }

    private void compareSelectedRuns() {
        int[] selected = runsTable.getSelectedRows();
        if (selected.length != 2) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Select exactly two runs in the table to compare.",
                    "Compare Runs",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        RunsTableRow left = runsTableModel.getRow(runsTable.convertRowIndexToModel(selected[0]));
        RunsTableRow right = runsTableModel.getRow(runsTable.convertRowIndexToModel(selected[1]));
        if (left == null || right == null) {
            return;
        }

        runsCompareButton.setEnabled(false);
        styleInlineStatus(runsTableStatusLabel, "Compare: loading artifacts...", COLOR_WARNING);

        SwingWorker<RunComparisonModel, Void> worker = new SwingWorker<>() {
            @Override
            protected RunComparisonModel doInBackground() throws Exception {
                RunBundle leftBundle = loadRunBundle(left.runId(), false);
                RunBundle rightBundle = loadRunBundle(right.runId(), false);

                Map<String, Object> leftConfig = buildCompareConfigMap(leftBundle);
                Map<String, Object> rightConfig = buildCompareConfigMap(rightBundle);
                String configDiff = buildStableJsonDiff(leftConfig, rightConfig);

                Map<String, Object> leftData = buildCompareDataMap(leftBundle);
                Map<String, Object> rightData = buildCompareDataMap(rightBundle);
                String dataDiff = buildStableJsonDiff(leftData, rightData);

                Map<String, Map<String, Object>> leftFeatures = buildFeatureSnapshot(leftBundle);
                Map<String, Map<String, Object>> rightFeatures = buildFeatureSnapshot(rightBundle);
                FeatureDiffSummary featureDiff = buildFeatureDiffSummary(leftFeatures, rightFeatures);

                Map<String, Object> leftMetrics = buildCompareMetricsMap(leftBundle);
                Map<String, Object> rightMetrics = buildCompareMetricsMap(rightBundle);
                String metricsDiff = buildStableJsonDiff(leftMetrics, rightMetrics);

                BacktestCompare leftBacktest = extractBacktestCompare(leftBundle);
                BacktestCompare rightBacktest = extractBacktestCompare(rightBundle);
                String backtestDiff = buildBacktestDiffText(leftBacktest, rightBacktest);

                Object leftDrift = extractDriftBaselinePayload(leftBundle);
                Object rightDrift = extractDriftBaselinePayload(rightBundle);
                String driftDiff = buildStableJsonDiff(leftDrift, rightDrift);

                String explanation = buildPerformanceChangeExplanation(
                        left,
                        right,
                        leftConfig,
                        rightConfig,
                        leftData,
                        rightData,
                        leftMetrics,
                        rightMetrics,
                        featureDiff,
                        leftBacktest,
                        rightBacktest,
                        driftDiff
                );

                return new RunComparisonModel(
                        left,
                        right,
                        configDiff,
                        dataDiff,
                        featureDiff.text(),
                        metricsDiff,
                        backtestDiff,
                        driftDiff,
                        leftBacktest.equityCurve(),
                        rightBacktest.equityCurve(),
                        explanation
                );
            }

            @Override
            protected void done() {
                runsCompareButton.setEnabled(true);
                try {
                    RunComparisonModel model = get();
                    styleInlineStatus(runsTableStatusLabel, "Compare: ready at " + ts(), COLOR_SUCCESS);
                    showRunComparisonDialog(model);
                } catch (Exception ex) {
                    styleInlineStatus(runsTableStatusLabel, "Compare: failed", COLOR_DANGER);
                    JOptionPane.showMessageDialog(
                            frame,
                            "Failed to compare selected runs:\n" + humanizeError(ex),
                            "Compare Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private RunBundle loadRunBundle(String runId, boolean forceRefresh) throws Exception {
        Map<String, Object> run = runsService.getRun(runId, forceRefresh);
        List<Map<String, Object>> artifacts = runsService.getRunArtifacts(runId, forceRefresh);
        Map<String, String> artifactIndex = indexArtifacts(artifacts);
        Map<String, Object> artifactPayloads = new LinkedHashMap<>();
        for (Map<String, Object> artifactMeta : artifacts) {
            String artifactName = extractArtifactName(artifactMeta);
            if (artifactName == null || artifactName.isBlank()) {
                continue;
            }
            try {
                Object payload = runsService.getRunArtifact(runId, artifactName, forceRefresh);
                artifactPayloads.put(artifactName, payload);
            } catch (Exception artifactError) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("__error", humanizeError(artifactError));
                artifactPayloads.put(artifactName, error);
            }
        }
        return new RunBundle(runId, run, artifactIndex, artifactPayloads);
    }

    private Object resolveSectionPayload(RunBundle bundle, String[] runHints, String[] artifactHints) {
        Object inline = findSectionByHints(bundle.runPayload(), runHints);
        String artifactName = findArtifactByHints(bundle.artifactIndex(), artifactHints);
        if (artifactName != null) {
            Object artifact = findArtifactPayload(bundle.artifactPayloads(), artifactName);
            if (artifact != null && !isArtifactError(artifact)) {
                return artifact;
            }
        }
        return inline;
    }

    private Map<String, Object> buildCompareConfigMap(RunBundle bundle) {
        Map<String, Object> run = bundle.runPayload();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("run_id", bundle.runId());
        out.put("target", firstNonBlank(
                stringOrEmpty(findAnyValue(run, "target", "target_name", "prediction_target")),
                "—"
        ));
        out.put("horizon", firstNonBlank(
                stringOrEmpty(findAnyValue(run, "horizon", "target_horizon", "horizon_days", "forecast_horizon")),
                "—"
        ));
        out.put("model_type", firstNonBlank(
                stringOrEmpty(findAnyValue(run, "model_type", "algorithm", "model_name", "model")),
                "—"
        ));

        Object modelPayload = resolveSectionPayload(
                bundle,
                new String[]{"model", "modelsummary", "hyperparameter", "threshold"},
                new String[]{"model_summary", "model", "hyperparameter", "training_curve"}
        );
        Map<String, Object> modelMap = asObjectOrEmpty(modelPayload);
        out.put("model_family", firstNonBlank(
                stringOrEmpty(findAnyValue(modelMap, "model_family", "family", "algorithm", "model_type", "model")),
                "—"
        ));
        out.put("model_params", firstNonNull(
                findAnyValue(modelMap, "hyperparameters", "params", "config", "best_params"),
                new LinkedHashMap<String, Object>()
        ));
        out.put("threshold_logic", firstNonNull(
                findAnyValue(modelMap,
                        "threshold_logic",
                        "trade_threshold_logic",
                        "probability_threshold",
                        "decision_thresholds"),
                "none / not reported"
        ));

        Map<String, Map<String, Object>> snapshot = buildFeatureSnapshot(bundle);
        List<String> enabledFeatures = new ArrayList<>();
        List<String> names = new ArrayList<>(snapshot.keySet());
        names.sort(String::compareTo);
        for (String name : names) {
            Map<String, Object> feature = snapshot.get(name);
            if (feature == null) {
                continue;
            }
            if (asBoolean(feature.get("kept"), true)) {
                enabledFeatures.add(name);
            }
        }
        out.put("enabled_feature_count", enabledFeatures.size());
        out.put("enabled_features", enabledFeatures);
        return out;
    }

    private Map<String, Object> buildCompareDataMap(RunBundle bundle) {
        Map<String, Object> run = bundle.runPayload();
        Object dataPayload = resolveSectionPayload(
                bundle,
                new String[]{"data", "datasummary", "dataset", "quality"},
                new String[]{"data_summary", "datasummary", "dataset_manifest", "data", "quality"}
        );

        Map<String, Object> dataMap = asObjectOrEmpty(dataPayload);
        Object nestedSummary = firstNonNull(
                findAnyValue(dataMap, "data_summary", "datasummary", "summary", "data"),
                dataMap
        );
        Map<String, Object> summary = asObjectOrEmpty(nestedSummary);

        List<DataTickerStatsRow> tickerRows = extractTickerStats(summary);
        long barsTotal = 0L;
        long missingTotal = 0L;
        long repairedTotal = 0L;
        long droppedTotal = 0L;
        Map<String, Object> perTicker = new LinkedHashMap<>();
        for (DataTickerStatsRow row : tickerRows) {
            barsTotal += row.bars() == null ? 0L : row.bars();
            missingTotal += row.missingBars() == null ? 0L : row.missingBars();
            repairedTotal += row.repairedBars() == null ? 0L : row.repairedBars();
            droppedTotal += row.droppedBars() == null ? 0L : row.droppedBars();

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("bars", row.bars() == null ? "—" : row.bars());
            stats.put("missing", row.missingBars() == null ? "—" : row.missingBars());
            stats.put("repaired", row.repairedBars() == null ? "—" : row.repairedBars());
            stats.put("dropped", row.droppedBars() == null ? "—" : row.droppedBars());
            perTicker.put(row.ticker(), stats);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date_range", formatDataRange(run, summary));
        out.put("timeframe", formatTimeframe(run, summary));
        out.put("tickers", extractTickers(run));
        out.put("bars_total", barsTotal);
        out.put("missing_total", missingTotal);
        out.put("repaired_total", repairedTotal);
        out.put("dropped_total", droppedTotal);
        out.put("missingness_report", firstNonNull(
                findAnyValue(summary, "missingness", "missingness_report", "missing_data", "missing_pct"),
                "not reported"
        ));
        out.put("data_sources", formatDataSources(summary));
        out.put("source_refresh", formatSourceRefreshTimes(summary));
        out.put("corporate_actions", formatCorporateActions(summary));
        out.put("outlier_report", formatOutlierReport(summary));
        out.put("per_ticker", perTicker);
        return out;
    }

    private Map<String, Map<String, Object>> buildFeatureSnapshot(RunBundle bundle) {
        Object featurePayload = resolveSectionPayload(
                bundle,
                new String[]{"feature", "featuresummary", "featurelist", "featureregistry"},
                new String[]{"feature_summary", "feature_registry", "features", "featurelist"}
        );
        List<FeatureRow> rows = extractFeatureRows(featurePayload);
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        for (FeatureRow row : rows) {
            String key = row.featureName();
            if (key == null || key.isBlank() || "—".equals(key)) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("category", row.category());
            entry.put("parameters", row.parameters());
            entry.put("timeframe", row.timeframe());
            entry.put("scaling", row.scaling());
            entry.put("missing_pct", row.missingPct());
            entry.put("kept", row.kept());
            entry.put("reason", row.statusReason());
            entry.put("dependencies", row.dependencies());
            entry.put("join_lag", row.joinLag());
            snapshot.put(key, entry);
        }
        return snapshot;
    }

    private FeatureDiffSummary buildFeatureDiffSummary(
            Map<String, Map<String, Object>> leftFeatures,
            Map<String, Map<String, Object>> rightFeatures
    ) {
        List<String> names = new ArrayList<>();
        names.addAll(leftFeatures.keySet());
        for (String name : rightFeatures.keySet()) {
            if (!leftFeatures.containsKey(name)) {
                names.add(name);
            }
        }
        names.sort(String::compareTo);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (String name : names) {
            Map<String, Object> left = leftFeatures.get(name);
            Map<String, Object> right = rightFeatures.get(name);
            if (left == null && right != null) {
                added.add(name);
                continue;
            }
            if (left != null && right == null) {
                removed.add(name);
                continue;
            }
            if (left == null || right == null) {
                continue;
            }

            List<String> deltas = new ArrayList<>();
            appendFieldDelta(deltas, "parameters", left.get("parameters"), right.get("parameters"));
            appendFieldDelta(deltas, "timeframe", left.get("timeframe"), right.get("timeframe"));
            appendFieldDelta(deltas, "scaling", left.get("scaling"), right.get("scaling"));
            appendFieldDelta(deltas, "kept", left.get("kept"), right.get("kept"));
            if (!deltas.isEmpty()) {
                changed.add(name + " | " + String.join("; ", deltas));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Feature Diff\n\n");
        sb.append("Added: ").append(added.size())
                .append(", Removed: ").append(removed.size())
                .append(", Changed parameters: ").append(changed.size()).append("\n\n");

        sb.append("Added features:\n");
        if (added.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (String item : added) {
                sb.append("- ").append(item).append("\n");
            }
        }

        sb.append("\nRemoved features:\n");
        if (removed.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (String item : removed) {
                sb.append("- ").append(item).append("\n");
            }
        }

        sb.append("\nChanged feature parameters:\n");
        if (changed.isEmpty()) {
            sb.append("- none\n");
        } else {
            for (String item : changed) {
                sb.append("- ").append(item).append("\n");
            }
        }

        sb.append("\nSnapshot diff:\n");
        sb.append(limitLines(buildStableJsonDiff(leftFeatures, rightFeatures), 260));

        return new FeatureDiffSummary(sb.toString(), added.size(), removed.size(), changed.size());
    }

    private void appendFieldDelta(List<String> deltas, String field, Object left, Object right) {
        String leftText = left == null ? "null" : String.valueOf(left);
        String rightText = right == null ? "null" : String.valueOf(right);
        if (!Objects.equals(leftText, rightText)) {
            deltas.add(field + ": " + leftText + " -> " + rightText);
        }
    }

    private Map<String, Object> buildCompareMetricsMap(RunBundle bundle) {
        Map<String, Object> run = bundle.runPayload();
        Object metricsPayload = resolveSectionPayload(
                bundle,
                new String[]{"metric", "metricssummary", "classification", "regression", "trading"},
                new String[]{"metrics_summary", "metric", "classification", "regression", "trading"}
        );
        Map<String, Object> metricsMap = asObjectOrEmpty(metricsPayload);

        Object modelPayload = resolveSectionPayload(
                bundle,
                new String[]{"model", "modelsummary", "calibration"},
                new String[]{"model_summary", "model", "calibration"}
        );
        Map<String, Object> modelMap = asObjectOrEmpty(modelPayload);

        Object backtestPayload = resolveSectionPayload(
                bundle,
                new String[]{"backtest", "backtestsummary", "equity", "drawdown", "trade"},
                new String[]{"backtest_summary", "backtest", "equity_curve", "drawdown_curve", "trade_log"}
        );
        Map<String, Object> backtestMap = asObjectOrEmpty(backtestPayload);
        Map<String, Object> backtestSummary = selectBacktestSummary(backtestMap);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("primary_score", firstNonNull(
                findAnyValue(run, "primary_score", "score", "objective_score", "best_score"),
                "—"
        ));
        out.put("sharpe", firstNonNull(
                findAnyValue(backtestSummary, "sharpe", "sharpe_ratio"),
                findAnyValue(backtestMap, "sharpe", "sharpe_ratio")
        ));
        out.put("max_drawdown", firstNonNull(
                findAnyValue(backtestSummary, "max_drawdown", "max_dd", "mdd"),
                findAnyValue(backtestMap, "max_drawdown", "max_dd", "mdd")
        ));
        out.put("f1", firstNonNull(
                findAnyValue(metricsMap, "f1", "f1_score", "classification_f1"),
                findAnyValue(run, "f1", "f1_score")
        ));
        out.put("brier", firstNonNull(
                findAnyValue(modelMap, "brier", "brier_score"),
                findAnyValue(metricsMap, "brier", "brier_score")
        ));
        out.put("ece", firstNonNull(
                findAnyValue(modelMap, "ece", "expected_calibration_error"),
                findAnyValue(metricsMap, "ece", "expected_calibration_error")
        ));
        out.put("win_rate", firstNonNull(
                findAnyValue(backtestSummary, "win_rate", "hit_rate", "winrate"),
                findAnyValue(backtestMap, "win_rate", "hit_rate", "winrate")
        ));
        return out;
    }

    private BacktestCompare extractBacktestCompare(RunBundle bundle) {
        Object payload = resolveSectionPayload(
                bundle,
                new String[]{"backtest", "backtestsummary", "equity", "drawdown", "trade", "assumption"},
                new String[]{"backtest_summary", "backtest", "equity_curve", "drawdown_curve", "trade_log", "assumptions"}
        );
        Map<String, Object> backtestMap = asObjectOrEmpty(payload);
        Map<String, Object> summary = selectBacktestSummary(backtestMap);
        Object assumptions = selectBacktestAssumptions(backtestMap);

        Object tradePayload = resolveSectionPayload(
                bundle,
                new String[]{"trade", "trade_log", "trade_list"},
                new String[]{"trade_log", "trade_list", "trades"}
        );
        Object equityPayload = resolveSectionPayload(
                bundle,
                new String[]{"equity", "equity_curve"},
                new String[]{"equity_curve", "equity"}
        );

        List<TradeLogRow> trades = extractTradeLogRows(firstNonNull(tradePayload, payload));
        List<SeriesPoint> equityCurve = extractSeriesPoints(firstNonNull(equityPayload, payload), "equity");
        return new BacktestCompare(equityCurve, trades.size(), summary, assumptions);
    }

    private String buildBacktestDiffText(BacktestCompare left, BacktestCompare right) {
        double leftFinal = left.equityCurve().isEmpty()
                ? Double.NaN
                : left.equityCurve().get(left.equityCurve().size() - 1).value();
        double rightFinal = right.equityCurve().isEmpty()
                ? Double.NaN
                : right.equityCurve().get(right.equityCurve().size() - 1).value();

        StringBuilder sb = new StringBuilder();
        sb.append("Backtest Diff\n\n");
        sb.append("Trade count:\n");
        sb.append("- Run A: ").append(left.tradeCount()).append("\n");
        sb.append("- Run B: ").append(right.tradeCount()).append("\n");
        sb.append("- Delta: ").append(right.tradeCount() - left.tradeCount()).append("\n\n");

        sb.append("Equity curve points:\n");
        sb.append("- Run A: ").append(left.equityCurve().size()).append("\n");
        sb.append("- Run B: ").append(right.equityCurve().size()).append("\n\n");

        sb.append("Final equity:\n");
        sb.append("- Run A: ").append(Double.isNaN(leftFinal) ? "—" : String.format("%.6f", leftFinal)).append("\n");
        sb.append("- Run B: ").append(Double.isNaN(rightFinal) ? "—" : String.format("%.6f", rightFinal)).append("\n");
        if (!Double.isNaN(leftFinal) && !Double.isNaN(rightFinal)) {
            sb.append("- Delta: ").append(String.format("%.6f", rightFinal - leftFinal)).append("\n");
        }

        sb.append("\nAssumptions snapshot diff:\n");
        sb.append(limitLines(buildStableJsonDiff(left.assumptions(), right.assumptions()), 180));
        return sb.toString();
    }

    private Object extractDriftBaselinePayload(RunBundle bundle) {
        Object diagnosticsPayload = resolveSectionPayload(
                bundle,
                new String[]{"diagnostic", "drift", "regime", "error"},
                new String[]{"diagnostic", "drift", "diagnostics_summary", "error_analysis"}
        );
        Map<String, Object> diagnostics = asObjectOrEmpty(diagnosticsPayload);
        return firstNonNull(
                findAnyValue(diagnostics,
                        "drift_baseline",
                        "drift",
                        "drift_stats",
                        "feature_drift",
                        "baseline"),
                diagnosticsPayload
        );
    }

    private String buildStableJsonDiff(Object left, Object right) {
        Map<String, String> leftFlat = new LinkedHashMap<>();
        Map<String, String> rightFlat = new LinkedHashMap<>();
        flattenForDiff("$", left, leftFlat);
        flattenForDiff("$", right, rightFlat);

        List<String> paths = new ArrayList<>();
        paths.addAll(leftFlat.keySet());
        for (String path : rightFlat.keySet()) {
            if (!leftFlat.containsKey(path)) {
                paths.add(path);
            }
        }
        paths.sort(String::compareTo);

        int added = 0;
        int removed = 0;
        int changed = 0;
        List<String> lines = new ArrayList<>();
        for (String path : paths) {
            String leftValue = leftFlat.get(path);
            String rightValue = rightFlat.get(path);
            if (leftValue == null) {
                added++;
                lines.add("+ " + path + " = " + truncateDiffValue(rightValue));
            } else if (rightValue == null) {
                removed++;
                lines.add("- " + path + " = " + truncateDiffValue(leftValue));
            } else if (!Objects.equals(leftValue, rightValue)) {
                changed++;
                lines.add("~ " + path + ": " + truncateDiffValue(leftValue) + " -> " + truncateDiffValue(rightValue));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Stable JSON Diff\n");
        sb.append("Added=").append(added)
                .append(", Removed=").append(removed)
                .append(", Changed=").append(changed)
                .append("\n\n");
        if (lines.isEmpty()) {
            sb.append("No differences detected.");
        } else {
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void flattenForDiff(String path, Object value, Map<String, String> out) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            out.put(path, renderDiffValue(value));
            return;
        }

        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.put(path, "[]");
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                flattenForDiff(path + "[" + i + "]", list.get(i), out);
            }
            return;
        }

        if (value instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            if (map.isEmpty()) {
                out.put(path, "{}");
                return;
            }
            List<String> keys = new ArrayList<>(map.keySet());
            keys.sort(String::compareTo);
            for (String key : keys) {
                String next = path + "." + escapeDiffPathKey(key);
                flattenForDiff(next, map.get(key), out);
            }
            return;
        }

        out.put(path, renderDiffValue(value));
    }

    private String escapeDiffPathKey(String key) {
        if (key == null || key.isBlank()) {
            return "\"\"";
        }
        boolean safe = true;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) {
                safe = false;
                break;
            }
        }
        if (safe) {
            return key;
        }
        return "\"" + key.replace("\"", "\\\"") + "\"";
    }

    private String renderDiffValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + text + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return Json.compact(value);
        }
        return String.valueOf(value);
    }

    private String truncateDiffValue(String value) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= 220) {
            return value;
        }
        return value.substring(0, 220) + "...";
    }

    private String limitLines(String text, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\\R");
        if (lines.length <= maxLines) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("... (").append(lines.length - maxLines).append(" more lines)");
        return sb.toString();
    }

    private String buildPerformanceChangeExplanation(
            RunsTableRow left,
            RunsTableRow right,
            Map<String, Object> leftConfig,
            Map<String, Object> rightConfig,
            Map<String, Object> leftData,
            Map<String, Object> rightData,
            Map<String, Object> leftMetrics,
            Map<String, Object> rightMetrics,
            FeatureDiffSummary featureDiff,
            BacktestCompare leftBacktest,
            BacktestCompare rightBacktest,
            String driftDiff
    ) {
        List<String> reasons = new ArrayList<>();

        Double leftPrimary = asDoubleObject(leftMetrics.get("primary_score"));
        Double rightPrimary = asDoubleObject(rightMetrics.get("primary_score"));
        if (leftPrimary != null && rightPrimary != null && Math.abs(rightPrimary - leftPrimary) > 1e-6) {
            reasons.add("Primary score changed from " + String.format("%.6f", leftPrimary)
                    + " to " + String.format("%.6f", rightPrimary) + ".");
        }

        Double leftSharpe = asDoubleObject(leftMetrics.get("sharpe"));
        Double rightSharpe = asDoubleObject(rightMetrics.get("sharpe"));
        if (leftSharpe != null && rightSharpe != null && Math.abs(rightSharpe - leftSharpe) > 1e-6) {
            reasons.add("Risk-adjusted return moved (Sharpe " + String.format("%.4f", leftSharpe)
                    + " -> " + String.format("%.4f", rightSharpe) + ").");
        }

        Double leftDd = asDoubleObject(leftMetrics.get("max_drawdown"));
        Double rightDd = asDoubleObject(rightMetrics.get("max_drawdown"));
        if (leftDd != null && rightDd != null && Math.abs(rightDd - leftDd) > 1e-6) {
            reasons.add("Drawdown profile changed (" + String.format("%.4f", leftDd)
                    + " -> " + String.format("%.4f", rightDd) + ").");
        }

        if (featureDiff.addedCount() > 0 || featureDiff.removedCount() > 0 || featureDiff.changedCount() > 0) {
            reasons.add("Feature set changed (added=" + featureDiff.addedCount()
                    + ", removed=" + featureDiff.removedCount()
                    + ", param changes=" + featureDiff.changedCount() + ").");
        }

        Long leftBars = asLong(leftData.get("bars_total"));
        Long rightBars = asLong(rightData.get("bars_total"));
        if (leftBars != null && rightBars != null && !Objects.equals(leftBars, rightBars)) {
            reasons.add("Training data volume changed (" + leftBars + " bars -> " + rightBars + " bars).");
        }

        if (leftBacktest.tradeCount() != rightBacktest.tradeCount()) {
            reasons.add("Trade frequency shifted (" + leftBacktest.tradeCount()
                    + " -> " + rightBacktest.tradeCount() + " trades).");
        }

        if (driftDiff != null && !driftDiff.contains("No differences detected.")) {
            reasons.add("Drift baseline changed, indicating a different feature distribution regime.");
        }

        String leftModel = String.valueOf(firstNonNull(leftConfig.get("model_family"), leftConfig.get("model_type")));
        String rightModel = String.valueOf(firstNonNull(rightConfig.get("model_family"), rightConfig.get("model_type")));
        if (!Objects.equals(leftModel, rightModel)) {
            reasons.add("Model family changed (" + leftModel + " -> " + rightModel + ").");
        }

        while (reasons.size() < 5) {
            reasons.add("Combined config/data/feature differences likely shifted signal quality and execution outcomes.");
        }
        if (reasons.size() > 5) {
            reasons = new ArrayList<>(reasons.subList(0, 5));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Best Explanation (Top 5 likely reasons)\n\n");
        for (String reason : reasons) {
            sb.append("- ").append(reason).append("\n");
        }
        sb.append("\nRun A: ").append(left.runId()).append("\n");
        sb.append("Run B: ").append(right.runId()).append("\n");
        return sb.toString();
    }

    private void showRunComparisonDialog(RunComparisonModel model) {
        JDialog dialog = new JDialog(frame, "Compare Runs", false);
        dialog.setSize(1180, 800);
        dialog.setMinimumSize(new Dimension(980, 680));
        dialog.setLocationRelativeTo(frame);
        dialog.getContentPane().setBackground(COLOR_BG);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel header = new JLabel("Run A: " + model.left().runId() + "    |    Run B: " + model.right().runId());
        header.setForeground(COLOR_TEXT);
        header.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.setBorder(new EmptyBorder(8, 10, 0, 10));
        headerWrap.add(header, BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        styleTabbedPane(tabs);
        tabs.addTab("Config Diff", createLogScrollPane(createCompareTextArea(model.configDiff()), "Config Diff"));
        tabs.addTab("Data Diff", createLogScrollPane(createCompareTextArea(model.dataDiff()), "Data Diff"));
        tabs.addTab("Feature Diff", createLogScrollPane(createCompareTextArea(model.featureDiff()), "Feature Diff"));
        tabs.addTab("Metric Diff", createLogScrollPane(createCompareTextArea(model.metricsDiff()), "Metric Diff"));
        tabs.addTab("Drift Diff", createLogScrollPane(createCompareTextArea(model.driftDiff()), "Drift Baseline Diff"));
        tabs.addTab("Explanation", createLogScrollPane(createCompareTextArea(model.explanation()), "Likely Performance Drivers"));

        CompareLineChartPanel overlayChart = new CompareLineChartPanel(
                "Equity Curve Overlay",
                COLOR_SUCCESS,
                COLOR_ACCENT
        );
        overlayChart.setSeries(
                model.equityA(),
                model.equityB(),
                "Run A (" + shortenRunId(model.left().runId()) + ")",
                "Run B (" + shortenRunId(model.right().runId()) + ")"
        );

        JPanel chartCard = createCardPanel();
        chartCard.add(createSectionHeader("Equity Overlay"), BorderLayout.NORTH);
        chartCard.add(overlayChart, BorderLayout.CENTER);

        JScrollPane backtestScroll = createLogScrollPane(createCompareTextArea(model.backtestDiff()), "Backtest Diff");
        JSplitPane backtestSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartCard, backtestScroll);
        backtestSplit.setResizeWeight(0.55);
        backtestSplit.setDividerLocation(340);
        backtestSplit.setBorder(BorderFactory.createEmptyBorder());
        backtestSplit.setBackground(COLOR_BG);
        tabs.addTab("Backtest Diff", backtestSplit);

        dialog.add(headerWrap, BorderLayout.NORTH);
        dialog.add(tabs, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JTextArea createCompareTextArea(String text) {
        JTextArea area = createRunDetailsArea();
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
        return area;
    }

    private RunsTableRow buildRunsTableRow(Map<String, Object> run) {
        String runId = stringOrEmpty(findAnyValue(run, "runId", "run_id", "id"));
        if (runId.isBlank()) {
            runId = "(missing-id)";
        }
        String dateTime = firstNonBlank(
                stringOrEmpty(findAnyValue(run, "created_at", "start_time", "started_at", "finished_at", "timestamp")),
                stringOrEmpty(findAnyValue(run, "time", "run_time", "date"))
        );
        if (dateTime.isBlank()) {
            dateTime = "—";
        }

        String target = stringOrEmpty(findAnyValue(run, "target", "target_name", "prediction_target"));
        String horizon = stringOrEmpty(findAnyValue(run, "horizon", "horizon_days", "target_horizon", "forecast_horizon"));
        String targetHorizon = target.isBlank() ? "—" : target;
        if (!horizon.isBlank()) {
            targetHorizon = (targetHorizon.equals("—") ? "" : targetHorizon + " / ") + horizon;
        }
        if (targetHorizon.isBlank()) {
            targetHorizon = "—";
        }

        String dataStart = stringOrEmpty(findAnyValue(run,
                "data_start", "dataset_start", "train_start", "start_date", "data_from"));
        String dataEnd = stringOrEmpty(findAnyValue(run,
                "data_end", "dataset_end", "test_end", "end_date", "data_to"));
        String datasetRange = "—";
        if (!dataStart.isBlank() || !dataEnd.isBlank()) {
            datasetRange = (dataStart.isBlank() ? "?" : dataStart) + " -> " + (dataEnd.isBlank() ? "?" : dataEnd);
        } else {
            String inlineRange = stringOrEmpty(findAnyValue(run, "dataset_range", "data_range"));
            if (!inlineRange.isBlank()) {
                datasetRange = inlineRange;
            }
        }

        String modelType = firstNonBlank(
                stringOrEmpty(findAnyValue(run, "model_type", "algorithm", "model_name", "model")),
                "—"
        );

        String primaryScore = asMetricString(findAnyValue(run,
                "primary_score", "score", "objective_score", "best_score", "sharpe", "f1"));
        String status = firstNonBlank(
                stringOrEmpty(findAnyValue(run, "status", "state", "run_status")),
                "unknown"
        );

        String tickers = extractTickers(run);
        LocalDate date = extractDateFromText(dateTime);
        return new RunsTableRow(
                runId,
                shortenRunId(runId),
                dateTime,
                targetHorizon,
                datasetRange,
                modelType,
                primaryScore,
                status,
                tickers,
                horizon,
                date,
                run
        );
    }

    private String extractTickers(Map<String, Object> run) {
        Object value = findAnyValue(run, "ticker", "symbol", "tickers", "symbols");
        if (value instanceof List<?> list) {
            List<String> parts = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    parts.add(String.valueOf(item));
                }
            }
            return String.join(",", parts);
        }
        if (value != null) {
            return String.valueOf(value);
        }
        return "";
    }

    private void selectRunById(String runId) {
        if (runId == null || runId.isBlank() || runsTable == null || runsTableModel == null) {
            return;
        }
        for (int modelRow = 0; modelRow < runsTableModel.getRowCount(); modelRow++) {
            RunsTableRow row = runsTableModel.getRow(modelRow);
            if (row != null && runId.equals(row.runId())) {
                int viewRow = runsTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    runsTable.setRowSelectionInterval(viewRow, viewRow);
                    runsTable.scrollRectToVisible(runsTable.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    private String getSelectedRunId() {
        if (runsTable == null || runsTableModel == null) {
            return null;
        }
        int viewRow = runsTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = runsTable.convertRowIndexToModel(viewRow);
        RunsTableRow row = runsTableModel.getRow(modelRow);
        return row == null ? null : row.runId();
    }

    private void resetRunDetailsState(String message) {
        if (runDetailsHeaderLabel != null) {
            runDetailsHeaderLabel.setText("Run Details");
        }
        String text = message == null || message.isBlank() ? "No run selected." : message;
        if (runOverviewArea != null) {
            runOverviewArea.setText(text);
        }
        if (runDataArea != null) {
            runDataArea.setText("");
        }
        if (runDataTickerTableModel != null) {
            runDataTickerTableModel.setRows(new ArrayList<>());
        }
        if (runDataRawJsonArea != null) {
            runDataRawJsonArea.setText("");
        }
        runDataRawPayload = new LinkedHashMap<String, Object>();
        runDataSummaryPayload = new LinkedHashMap<String, Object>();
        runDataHashesPayload = new LinkedHashMap<String, Object>();
        if (runDataManifestStatusLabel != null) {
            styleInlineStatus(runDataManifestStatusLabel, "Manifest: idle", COLOR_MUTED);
        }
        if (runFeaturesSummaryArea != null) {
            runFeaturesSummaryArea.setText("");
        }
        if (runFeaturesTableModel != null) {
            runFeaturesTableModel.setRows(new ArrayList<>());
        }
        if (runTopFeaturesTableModel != null) {
            runTopFeaturesTableModel.setRows(new ArrayList<>());
        }
        runFeaturesRawPayload = new LinkedHashMap<String, Object>();
        if (runFeaturesStatusLabel != null) {
            styleInlineStatus(runFeaturesStatusLabel, "Features: idle", COLOR_MUTED);
        }
        if (runSplitsArea != null) {
            runSplitsArea.setText("");
        }
        if (runModelArea != null) {
            runModelArea.setText("");
        }
        if (runModelHyperparamsArea != null) {
            runModelHyperparamsArea.setText("");
        }
        if (runModelCurvesArea != null) {
            runModelCurvesArea.setText("");
        }
        if (runModelFamilyValue != null) {
            runModelFamilyValue.setText("—");
        }
        if (runModelCalibrationMethodValue != null) {
            runModelCalibrationMethodValue.setText("—");
        }
        if (runModelCalibrationMetricsValue != null) {
            runModelCalibrationMetricsValue.setText("—");
        }
        if (runModelConfidenceTypeValue != null) {
            runModelConfidenceTypeValue.setText("—");
        }
        if (runModelThresholdLogicValue != null) {
            runModelThresholdLogicValue.setText("—");
        }
        if (runFailureTableModel != null) {
            runFailureTableModel.setRows(new ArrayList<>());
        }
        runModelRawPayload = new LinkedHashMap<String, Object>();
        runFailuresRawPayload = new LinkedHashMap<String, Object>();
        if (runModelStatusLabel != null) {
            styleInlineStatus(runModelStatusLabel, "Model: idle", COLOR_MUTED);
        }
        if (runCalibrationArea != null) {
            runCalibrationArea.setText("");
        }
        if (runMetricsArea != null) {
            runMetricsArea.setText("");
        }
        if (runBacktestArea != null) {
            runBacktestArea.setText("");
        }
        if (runBacktestAssumptionsArea != null) {
            runBacktestAssumptionsArea.setText("");
        }
        if (runBacktestCagrValue != null) {
            runBacktestCagrValue.setText("—");
        }
        if (runBacktestSharpeValue != null) {
            runBacktestSharpeValue.setText("—");
        }
        if (runBacktestSortinoValue != null) {
            runBacktestSortinoValue.setText("—");
        }
        if (runBacktestMddValue != null) {
            runBacktestMddValue.setText("—");
        }
        if (runBacktestProfitFactorValue != null) {
            runBacktestProfitFactorValue.setText("—");
        }
        if (runBacktestWinRateValue != null) {
            runBacktestWinRateValue.setText("—");
        }
        if (runBacktestAvgWinLossValue != null) {
            runBacktestAvgWinLossValue.setText("—");
        }
        if (runBacktestExposureValue != null) {
            runBacktestExposureValue.setText("—");
        }
        if (runBacktestTurnoverValue != null) {
            runBacktestTurnoverValue.setText("—");
        }
        if (runTradeLogSearchField != null) {
            runTradeLogSearchField.setText("");
        }
        if (runTradeLogTableModel != null) {
            runTradeLogTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionRegimeTableModel != null) {
            runAttributionRegimeTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionConfidenceTableModel != null) {
            runAttributionConfidenceTableModel.setRows(new ArrayList<>());
        }
        if (runAttributionTickerTableModel != null) {
            runAttributionTickerTableModel.setRows(new ArrayList<>());
        }
        if (runBacktestEquityChart != null) {
            runBacktestEquityChart.setSeries(new ArrayList<>(), false);
        }
        if (runBacktestDrawdownChart != null) {
            runBacktestDrawdownChart.setSeries(new ArrayList<>(), true);
        }
        runBacktestRawPayload = new LinkedHashMap<String, Object>();
        if (runBacktestStatusLabel != null) {
            styleInlineStatus(runBacktestStatusLabel, "Backtest: idle", COLOR_MUTED);
        }
        if (runDiagnosticsArea != null) {
            runDiagnosticsArea.setText("");
        }
        currentGuardrailReport = null;
        currentGuardrailRunId = null;
        if (runReadinessChecklistArea != null) {
            runReadinessChecklistArea.setText("");
        }
        if (runReadinessFindingsArea != null) {
            runReadinessFindingsArea.setText("");
        }
        if (runReproScoreLabel != null) {
            styleInlineStatus(runReproScoreLabel, "Reproducibility Score: —", COLOR_MUTED);
        }
        if (runReadinessStatusLabel != null) {
            styleInlineStatus(runReadinessStatusLabel, "Checklist: idle", COLOR_MUTED);
        }
        if (runReadinessActionStatusLabel != null) {
            styleInlineStatus(runReadinessActionStatusLabel, "Actions: idle", COLOR_MUTED);
        }
        if (runReadinessBannerLabel != null) {
            styleInlineStatus(runReadinessBannerLabel, "Guardrails: idle", COLOR_MUTED);
        }
        setTrainingGuardrailIdle();
        updateReadinessActionButtons();
    }

    private Map<String, String> indexArtifacts(List<Map<String, Object>> artifacts) {
        Map<String, String> index = new LinkedHashMap<>();
        for (Map<String, Object> artifact : artifacts) {
            String name = extractArtifactName(artifact);
            if (name == null || name.isBlank()) {
                continue;
            }
            index.put(normalizeKey(name), name);
        }
        return index;
    }

    private String extractArtifactName(Map<String, Object> artifact) {
        String[] keys = {"name", "artifact", "file", "filename", "path"};
        for (String key : keys) {
            Object value = findAnyValue(artifact, key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private String findArtifactByHints(Map<String, String> artifactIndex, String... hints) {
        for (Map.Entry<String, String> entry : artifactIndex.entrySet()) {
            String normalized = entry.getKey();
            for (String hint : hints) {
                if (normalized.contains(normalizeKey(hint))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Object findSectionByHints(Map<String, Object> root, String... hints) {
        for (String hint : hints) {
            Object direct = findValueByHint(root, normalizeKey(hint), 0);
            if (direct != null) {
                return direct;
            }
        }
        return null;
    }

    private Object findValueByHint(Object node, String hint, int depth) {
        if (node == null || depth > 8) {
            return null;
        }
        if (node instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String normalizedKey = normalizeKey(entry.getKey());
                if (normalizedKey.contains(hint) && entry.getValue() != null) {
                    return entry.getValue();
                }
            }
            for (Object value : map.values()) {
                Object nested = findValueByHint(value, hint, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                Object nested = findValueByHint(item, hint, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Object findAnyValue(Map<String, Object> root, String... candidateKeys) {
        for (String key : candidateKeys) {
            String normalized = normalizeKey(key);
            Object value = findValueByKey(root, normalized, 0);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object findValueByKey(Object node, String normalizedKey, int depth) {
        if (node == null || depth > 8) {
            return null;
        }
        if (node instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (normalizeKey(entry.getKey()).equals(normalizedKey)) {
                    return entry.getValue();
                }
            }
            for (Object value : map.values()) {
                Object nested = findValueByKey(value, normalizedKey, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                Object nested = findValueByKey(item, normalizedKey, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
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

    private LocalDate parseDateFilter(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDate extractDateFromText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.length() >= 10) {
            String prefix = text.substring(0, 10);
            try {
                return LocalDate.parse(prefix);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asMetricString(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof Number number) {
            return String.format("%.4f", number.doubleValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return "—";
        }
        return text;
    }

    private String shortenRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return "—";
        }
        return runId.length() <= 8 ? runId : runId.substring(0, 8);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private JLabel createMetricValueLabel() {
        JLabel value = new JLabel("—");
        value.setForeground(COLOR_TEXT);
        value.setFont(uiFont.deriveFont(Font.BOLD, 15f));
        return value;
    }

    private void addMetricTile(JPanel grid, int row, int col, String title, JLabel valueLabel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        grid.add(createMetricTile(title, valueLabel), gbc);
    }

    private JPanel createMetricTile(String title, JLabel valueLabel) {
        JPanel tile = new JPanel(new BorderLayout(0, 6));
        tile.setBackground(COLOR_CARD_ALT);
        tile.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(COLOR_MUTED);
        titleLabel.setFont(uiFont.deriveFont(Font.BOLD, 11f));

        valueLabel.setHorizontalAlignment(JLabel.LEFT);
        tile.add(titleLabel, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private void showView(String viewId) {
        if (contentLayout == null || contentPanel == null) {
            return;
        }
        contentLayout.show(contentPanel, viewId);
        styleNavButtonState(navAiManagementButton, VIEW_AI_MANAGEMENT.equals(viewId));
        styleNavButtonState(navDeepLearningButton, VIEW_DEEP_LEARNING.equals(viewId));
        styleNavButtonState(navAnalysisButton, VIEW_ANALYSIS.equals(viewId));
        styleNavButtonState(navDashboardButton, VIEW_DASHBOARD.equals(viewId));
        styleNavButtonState(navTrainingRunsButton, VIEW_TRAINING_RUNS.equals(viewId));
        styleNavButtonState(navPredictionInspectorButton, VIEW_PREDICTION_INSPECTOR.equals(viewId));
        styleNavButtonState(navSettingsButton, VIEW_SETTINGS.equals(viewId));
        if (VIEW_DEEP_LEARNING.equals(viewId)) {
            ensureDeepLearningTickersLoaded(false);
        }
        if (VIEW_ANALYSIS.equals(viewId) && analysisWorkspacePanel != null) {
            analysisWorkspacePanel.refresh(false);
        }
        if (VIEW_TRAINING_RUNS.equals(viewId) && runsTableModel != null && runsTableModel.getRowCount() == 0) {
            loadRuns(false);
        }
    }

    private JPanel createBackendControlPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        backendPathField = new JTextField(defaultBackendPath(), 44);
        startBackendButton = new JButton("Start AI Backend");
        stopBackendButton = new JButton("Stop AI Backend");
        startDaemonButton = new JButton("Start Daemon");
        stopDaemonButton = new JButton("Stop Daemon");
        JButton clearLogsButton = new JButton("Clear Logs");
        backendStatusValue = new JLabel();
        daemonStatusValue = new JLabel();

        styleInputField(backendPathField);
        styleButton(startBackendButton, true);
        styleButton(stopBackendButton, false);
        styleButton(startDaemonButton, false);
        styleButton(stopDaemonButton, false);
        styleButton(clearLogsButton, false);
        styleInlineStatus(backendStatusValue, "Backend: stopped", COLOR_MUTED);
        styleInlineStatus(daemonStatusValue, "Scheduler: unknown", COLOR_MUTED);

        clearLogsButton.addActionListener(e -> backendLogArea.setText(""));

        startBackendButton.addActionListener(e -> startBackendProcess());
        stopBackendButton.addActionListener(e -> stopBackendProcess());
        startDaemonButton.addActionListener(e -> startDaemon());
        stopDaemonButton.addActionListener(e -> stopDaemon());

        JLabel pathLabel = createFormLabel("AI Backend Path");
        JLabel noteLabel = createHintLabel(
                "Use Start AI Backend if no server is running. If one is already on port 8420, the app will reuse it."
        );

        JPanel runtimeActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        runtimeActions.setOpaque(false);
        runtimeActions.add(startBackendButton);
        runtimeActions.add(stopBackendButton);
        runtimeActions.add(backendStatusValue);

        JPanel daemonActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        daemonActions.setOpaque(false);
        daemonActions.add(startDaemonButton);
        daemonActions.add(stopDaemonButton);
        daemonActions.add(clearLogsButton);
        daemonActions.add(daemonStatusValue);

        JPanel controlsBody = new JPanel(new GridBagLayout());
        controlsBody.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 0);
        controlsBody.add(pathLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 10, 0);
        controlsBody.add(backendPathField, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        controlsBody.add(runtimeActions, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        controlsBody.add(daemonActions, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 0, 0, 0);
        controlsBody.add(noteLabel, gbc);

        JPanel controlsCard = createCardPanel();
        controlsCard.add(createSectionHeader("Runtime Controls"), BorderLayout.NORTH);
        controlsCard.add(controlsBody, BorderLayout.CENTER);

        backendLogArea = createLogArea();
        JScrollPane logs = createLogScrollPane(backendLogArea, "Backend Activity (select + copy supported)");

        root.add(controlsCard, BorderLayout.NORTH);
        root.add(logs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createTrainingPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        symbolField = new JTextField("SPY", 8);
        modeCombo = new JComboBox<>(new String[]{MODE_STABLE, MODE_DEEP});
        deepModelCombo = new JComboBox<>(new String[]{"lstm", "transformer"});
        epochsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 5000, 1));
        trainButton = new JButton("Start Training");
        stopButton = new JButton("Stop Polling");
        trainingStatusValue = new JLabel();
        stopButton.setEnabled(false);

        styleInputField(symbolField);
        styleCombo(modeCombo);
        styleCombo(deepModelCombo);
        styleSpinner(epochsSpinner);
        styleButton(trainButton, true);
        styleButton(stopButton, false);
        styleInlineStatus(trainingStatusValue, "Training: idle", COLOR_MUTED);
        modeCombo.setToolTipText(buildTrainingModeTooltip());
        deepModelCombo.addActionListener(e -> updateDeepModelTooltip());
        deepModelCombo.setToolTipText(buildDeepModelTooltip("lstm"));

        trainingGuardrailSummaryLabel = new JLabel();
        trainingGuardrailLeakageLabel = new JLabel();
        trainingGuardrailCostsLabel = new JLabel();
        trainingGuardrailWalkForwardLabel = new JLabel();
        trainingGuardrailQualityLabel = new JLabel();

        JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowOne.setOpaque(false);
        rowOne.add(createFormLabel("Symbol"));
        rowOne.add(symbolField);
        JLabel modeLabel = createFormLabel("Mode");
        modeLabel.setToolTipText(buildTrainingModeTooltip());
        rowOne.add(modeLabel);
        rowOne.add(modeCombo);
        JLabel deepModelLabel = createFormLabel("Deep Model");
        deepModelLabel.setToolTipText(buildDeepModelTooltip(null));
        rowOne.add(deepModelLabel);
        rowOne.add(deepModelCombo);

        JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowTwo.setOpaque(false);
        rowTwo.add(createFormLabel("Epochs"));
        rowTwo.add(epochsSpinner);
        rowTwo.add(trainButton);
        rowTwo.add(stopButton);
        rowTwo.add(trainingStatusValue);

        JLabel noteLabel = createHintLabel(
                "Stable mode is quick. Deep Learning mode can take longer depending on symbol history and model type."
        );

        JPanel controlsBody = new JPanel(new GridBagLayout());
        controlsBody.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);
        controlsBody.add(rowOne, gbc);

        gbc.gridy++;
        controlsBody.add(rowTwo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 0, 0, 0);
        controlsBody.add(noteLabel, gbc);

        JPanel controlsCard = createCardPanel();
        controlsCard.add(createSectionHeader("Training Controls"), BorderLayout.NORTH);
        controlsCard.add(controlsBody, BorderLayout.CENTER);

        JPanel guardrailRowOne = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        guardrailRowOne.setOpaque(false);
        guardrailRowOne.add(trainingGuardrailSummaryLabel);
        guardrailRowOne.add(trainingGuardrailLeakageLabel);

        JPanel guardrailRowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        guardrailRowTwo.setOpaque(false);
        guardrailRowTwo.add(trainingGuardrailCostsLabel);
        guardrailRowTwo.add(trainingGuardrailWalkForwardLabel);
        guardrailRowTwo.add(trainingGuardrailQualityLabel);

        JPanel guardrailBody = new JPanel(new GridBagLayout());
        guardrailBody.setOpaque(false);
        GridBagConstraints guardrailGbc = new GridBagConstraints();
        guardrailGbc.gridx = 0;
        guardrailGbc.gridy = 0;
        guardrailGbc.weightx = 1.0;
        guardrailGbc.fill = GridBagConstraints.HORIZONTAL;
        guardrailGbc.insets = new Insets(0, 0, 6, 0);
        guardrailBody.add(guardrailRowOne, guardrailGbc);
        guardrailGbc.gridy++;
        guardrailGbc.insets = new Insets(0, 0, 0, 0);
        guardrailBody.add(guardrailRowTwo, guardrailGbc);

        JPanel guardrailCard = createCardPanel();
        guardrailCard.add(createSectionHeader("Guardrails (Loaded Run)"), BorderLayout.NORTH);
        guardrailCard.add(guardrailBody, BorderLayout.CENTER);

        trainingLogArea = createLogArea();
        JScrollPane logScroll = createLogScrollPane(trainingLogArea, "Training Timeline (select + copy supported)");

        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        controlsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        guardrailCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        topStack.add(controlsCard);
        topStack.add(Box.createVerticalStrut(8));
        topStack.add(guardrailCard);

        root.add(topStack, BorderLayout.NORTH);
        root.add(logScroll, BorderLayout.CENTER);
        updateDeepModelTooltip();
        setTrainingGuardrailIdle();
        return root;
    }

    private JPanel createDataPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(COLOR_BG);
        root.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);
        JButton statusBtn = new JButton("Fetch /api/status");
        JButton modelsBtn = new JButton("Fetch /api/models");
        JButton memoriesBtn = new JButton("Fetch /api/memories");
        dataStatusValue = new JLabel();
        styleButton(statusBtn, false);
        styleButton(modelsBtn, false);
        styleButton(memoriesBtn, false);
        styleInlineStatus(dataStatusValue, "Data: idle", COLOR_MUTED);
        actions.add(statusBtn);
        actions.add(modelsBtn);
        actions.add(memoriesBtn);
        actions.add(dataStatusValue);

        JPanel actionsCard = createCardPanel();
        actionsCard.add(createSectionHeader("Backend Data Endpoints"), BorderLayout.NORTH);
        actionsCard.add(actions, BorderLayout.CENTER);

        dataArea = createLogArea();
        memoryDistributionArea = createLogArea();

        JScrollPane left = createLogScrollPane(dataArea, "Endpoint Response");
        JScrollPane right = createLogScrollPane(memoryDistributionArea, "Memory Distribution Snapshot");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(760);
        split.setResizeWeight(0.7);
        split.setBackground(COLOR_BG);
        split.setBorder(BorderFactory.createEmptyBorder());

        statusBtn.addActionListener(e -> fetchStatus());
        modelsBtn.addActionListener(e -> fetchModels());
        memoriesBtn.addActionListener(e -> fetchMemories());

        root.add(actionsCard, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JTextArea createLogArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(monoFont);
        area.setBackground(COLOR_LOG_BG);
        area.setForeground(COLOR_TEXT);
        area.setCaretColor(COLOR_TEXT);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        return area;
    }

    private JTextArea createDashboardInfoArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(uiFont.deriveFont(Font.PLAIN, 14f));
        area.setBackground(COLOR_LOG_BG);
        area.setForeground(COLOR_TEXT);
        area.setCaretColor(COLOR_TEXT);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));
        return area;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CARD);
        panel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private JScrollPane createLogScrollPane(JTextArea area, String title) {
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(COLOR_LOG_BG);
        scroll.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        scroll.setColumnHeaderView(createSectionHeader(title));
        return scroll;
    }

    private JComponent createSectionHeader(String title) {
        JLabel label = new JLabel(title);
        label.setForeground(COLOR_MUTED);
        label.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        label.setBorder(new EmptyBorder(0, 2, 6, 2));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(label, BorderLayout.WEST);
        return wrapper;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_MUTED);
        label.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        return label;
    }

    private JLabel createHintLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_MUTED);
        label.setFont(uiFont.deriveFont(Font.PLAIN, 12f));
        return label;
    }

    private void styleTabbedPane(JTabbedPane tabs) {
        tabs.setFont(uiFont.deriveFont(Font.BOLD, 14f));
        tabs.setBackground(COLOR_BG);
        tabs.setForeground(COLOR_TEXT);
        tabs.setOpaque(true);
        tabs.setBorder(new EmptyBorder(2, 0, 0, 0));
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(8, 10, 6, 10);
                tabInsets = new Insets(8, 16, 8, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(8, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(
                    Graphics g,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int w,
                    int h,
                    boolean isSelected
            ) {
                g.setColor(isSelected ? COLOR_ACCENT : COLOR_CARD_ALT);
                g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 10, 10);
            }

            @Override
            protected void paintTabBorder(
                    Graphics g,
                    int tabPlacement,
                    int tabIndex,
                    int x,
                    int y,
                    int w,
                    int h,
                    boolean isSelected
            ) {
                g.setColor(isSelected ? COLOR_ACCENT : COLOR_BORDER);
                g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 10, 10);
            }

            @Override
            protected void paintText(
                    Graphics g,
                    int tabPlacement,
                    Font font,
                    FontMetrics metrics,
                    int tabIndex,
                    String title,
                    Rectangle textRect,
                    boolean isSelected
            ) {
                g.setFont(font);
                g.setColor(isSelected ? Color.WHITE : COLOR_TEXT);
                int y = textRect.y + metrics.getAscent();
                g.drawString(title, textRect.x, y);
            }

            @Override
            protected void paintFocusIndicator(
                    Graphics g,
                    int tabPlacement,
                    Rectangle[] rects,
                    int tabIndex,
                    Rectangle iconRect,
                    Rectangle textRect,
                    boolean isSelected
            ) {
                // Keep tabs visually clean in dark theme.
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                int x = 0;
                int y = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                int w = tabs.getWidth() - 1;
                int h = tabs.getHeight() - y - 1;
                g.setColor(COLOR_BORDER);
                g.drawRect(x, y, w, h);
            }
        });
    }

    private void styleInputField(JTextField field) {
        field.setFont(uiFont);
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setBackground(COLOR_INPUT);
        field.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(6, 8, 6, 8)
        ));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(uiFont);
        combo.setOpaque(true);
        combo.setForeground(COLOR_TEXT);
        combo.setBackground(COLOR_INPUT);
        combo.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        combo.setMaximumRowCount(12);
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new BasicArrowButton(
                        BasicArrowButton.SOUTH,
                        COLOR_CARD_ALT,
                        COLOR_CARD_ALT,
                        COLOR_TEXT,
                        COLOR_TEXT
                );
                arrow.setBorder(new LineBorder(COLOR_BORDER, 1, true));
                arrow.setOpaque(true);
                arrow.setBackground(COLOR_CARD_ALT);
                return arrow;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = new BasicComboPopup(comboBox);
                popup.setBorder(new LineBorder(COLOR_BORDER, 1, true));
                popup.getList().setBackground(COLOR_CARD_ALT);
                popup.getList().setForeground(COLOR_TEXT);
                popup.getList().setSelectionBackground(COLOR_ACCENT);
                popup.getList().setSelectionForeground(Color.WHITE);
                popup.getList().setFont(uiFont);
                return popup;
            }
        });
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );
                label.setOpaque(true);
                label.setFont(uiFont);
                label.setBorder(new EmptyBorder(4, 8, 4, 8));
                list.setSelectionBackground(COLOR_ACCENT);
                list.setSelectionForeground(Color.WHITE);

                if (!combo.isEnabled()) {
                    label.setBackground(COLOR_CARD_ALT);
                    label.setForeground(COLOR_MUTED);
                } else if (index == -1) {
                    // Collapsed (selected value) view inside combo box.
                    label.setBackground(COLOR_INPUT);
                    label.setForeground(COLOR_TEXT);
                } else if (isSelected) {
                    label.setBackground(COLOR_ACCENT);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(COLOR_CARD_ALT);
                    label.setForeground(COLOR_TEXT);
                }
                return label;
            }
        });
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(uiFont);
        spinner.setBorder(new LineBorder(COLOR_BORDER, 1, true));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField textField = defaultEditor.getTextField();
            styleInputField(textField);
        }
    }

    private void styleButton(JButton button, boolean primary) {
        button.setFont(uiFont.deriveFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(
                new LineBorder(primary ? COLOR_ACCENT : COLOR_BORDER, 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        if (primary) {
            button.setBackground(COLOR_ACCENT);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(COLOR_CARD_ALT);
            button.setForeground(COLOR_TEXT);
        }
    }

    private void styleNavButton(JButton button) {
        button.setFont(uiFont.deriveFont(Font.BOLD, 14f));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        styleNavButtonState(button, false);
    }

    private void styleNavButtonState(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            button.setBackground(COLOR_MENU_ACTIVE);
            button.setForeground(Color.WHITE);
            button.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_ACCENT, 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));
        } else {
            button.setBackground(COLOR_CARD_ALT);
            button.setForeground(COLOR_TEXT);
            button.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_BORDER, 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));
        }
    }

    private void styleStatusLabel(JLabel label, String text, Color accentColor) {
        label.setText(text);
        label.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        label.setForeground(accentColor);
        label.setOpaque(true);
        label.setBackground(COLOR_CARD_ALT);
        label.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
    }

    private void styleInlineStatus(JLabel label, String text, Color color) {
        if (label == null) {
            return;
        }
        label.setText(text);
        label.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        label.setForeground(color);
        label.setOpaque(true);
        label.setBackground(COLOR_CARD_ALT);
        label.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleDashboardStatus(JLabel label, String text, Color color) {
        label.setText(text);
        label.setFont(uiFont.deriveFont(Font.BOLD, 12f));
        label.setForeground(color);
    }

    private Font resolveUiFont(int style, int size) {
        String[] preferred = {"SF Pro Text", ".SF NS Text", "Helvetica Neue", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String candidate : preferred) {
            for (String family : available) {
                if (candidate.equalsIgnoreCase(family)) {
                    return new Font(family, style, size);
                }
            }
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    private Font resolveMonoFont(int size) {
        String[] preferred = {"Menlo", "SF Mono", "Monaco", "Consolas"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String candidate : preferred) {
            for (String family : available) {
                if (candidate.equalsIgnoreCase(family)) {
                    return new Font(family, Font.PLAIN, size);
                }
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    private void show() {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        checkConnection();
        refreshSystemStatuses(true);
        startSystemStatusTimer();
    }

    private void configureClientFromUI() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }
        int port = 8420;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            portField.setText("8420");
        }
        apiClient.configure(host, port);
    }

    private void checkConnection() {
        configureClientFromUI();
        checkConnectionButton.setEnabled(false);
        styleStatusLabel(connectionLabel, "Checking...", COLOR_WARNING);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return apiClient.healthCheck();
            }

            @Override
            protected void done() {
                checkConnectionButton.setEnabled(true);
                boolean connected = false;
                try {
                    connected = get();
                } catch (Exception ignored) {
                    connected = false;
                }
                if (connected) {
                    styleStatusLabel(connectionLabel, "Connected", COLOR_SUCCESS);
                    backendExternalConnected = !backendIsRunning() && !backendStarting;
                    lastBackendHealth = "healthy at " + ts();
                } else {
                    styleStatusLabel(connectionLabel, "Connection failed", COLOR_DANGER);
                    backendExternalConnected = false;
                    lastBackendHealth = "unhealthy at " + ts();
                }
                refreshBackendControls();
                refreshStartupBanner(connected);
            }
        };
        worker.execute();
    }

    private void startSystemStatusTimer() {
        if (systemStatusTimer != null) {
            systemStatusTimer.stop();
        }
        systemStatusTimer = new javax.swing.Timer(15000, e -> refreshSystemStatuses(false));
        systemStatusTimer.setRepeats(true);
        systemStatusTimer.start();
    }

    private void populateSettingsForm(AppSettingsConfig.SettingsValues values) {
        AppSettingsConfig.SettingsValues safe = AppSettingsConfig.sanitize(values);
        if (settingsBackendHostField != null) {
            settingsBackendHostField.setText(safe.backendHost());
        }
        if (settingsBackendPortField != null) {
            settingsBackendPortField.setText(String.valueOf(safe.backendPort()));
        }
        if (settingsLlmProviderCombo != null) {
            settingsLlmProviderCombo.setSelectedItem(safe.llmProvider());
        }
        if (settingsAnthropicKeyField != null) {
            settingsAnthropicKeyField.setText(safe.anthropicApiKey());
        }
        if (settingsOpenAiKeyField != null) {
            settingsOpenAiKeyField.setText(safe.openAiApiKey());
        }
        if (settingsNewsApiKeyField != null) {
            settingsNewsApiKeyField.setText(safe.newsApiKey());
        }
        if (settingsSlackWebhookField != null) {
            settingsSlackWebhookField.setText(safe.slackWebhookUrl());
        }
        if (settingsAiRepoPathField != null) {
            settingsAiRepoPathField.setText(safe.aiRepoPath());
        }
        if (settingsOpsRepoPathField != null) {
            settingsOpsRepoPathField.setText(safe.opsRepoPath());
        }
        if (settingsDataDirField != null) {
            settingsDataDirField.setText(safe.dataDir());
        }
    }

    private AppSettingsConfig.SettingsValues readSettingsFromForm() {
        String host = settingsBackendHostField == null ? "" : settingsBackendHostField.getText().trim();
        String portText = settingsBackendPortField == null ? "" : settingsBackendPortField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port <= 0) {
                throw new NumberFormatException("port must be positive");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Backend port must be a positive integer.");
        }

        String llmProvider = settingsLlmProviderCombo == null || settingsLlmProviderCombo.getSelectedItem() == null
                ? AppSettingsConfig.DEFAULT_LLM_PROVIDER
                : String.valueOf(settingsLlmProviderCombo.getSelectedItem()).trim();

        String anthropic = settingsAnthropicKeyField == null ? "" : new String(settingsAnthropicKeyField.getPassword()).trim();
        String openAi = settingsOpenAiKeyField == null ? "" : new String(settingsOpenAiKeyField.getPassword()).trim();
        String news = settingsNewsApiKeyField == null ? "" : new String(settingsNewsApiKeyField.getPassword()).trim();
        String slack = settingsSlackWebhookField == null ? "" : new String(settingsSlackWebhookField.getPassword()).trim();
        String aiPath = settingsAiRepoPathField == null ? "" : settingsAiRepoPathField.getText().trim();
        String opsPath = settingsOpsRepoPathField == null ? "" : settingsOpsRepoPathField.getText().trim();
        String dataDir = settingsDataDirField == null ? "" : settingsDataDirField.getText().trim();

        return AppSettingsConfig.sanitize(new AppSettingsConfig.SettingsValues(
                host,
                port,
                llmProvider,
                anthropic,
                openAi,
                news,
                slack,
                aiPath,
                opsPath,
                dataDir
        ));
    }

    private void saveSettingsFromWorkspace() {
        AppSettingsConfig.SettingsValues values;
        try {
            values = readSettingsFromForm();
        } catch (IllegalArgumentException ex) {
            styleInlineStatus(settingsStatusLabel, "Settings: " + ex.getMessage(), COLOR_DANGER);
            appendSettingsLog(ts() + " | Save failed: " + ex.getMessage());
            return;
        }

        try {
            AppSettingsConfig.save(values);
            applySettingsToRuntime(values, true);
            try {
                SystemControlConfig.save(toSystemControlConfig(values));
            } catch (Exception ignored) {
                // Keep legacy sync best-effort only.
            }
            styleInlineStatus(settingsStatusLabel, "Settings: saved", COLOR_SUCCESS);
            appendSettingsLog(ts() + " | Saved settings to " + AppSettingsConfig.settingsPath());
            checkConnection();
        } catch (IOException ex) {
            styleInlineStatus(settingsStatusLabel, "Settings: save failed", COLOR_DANGER);
            appendSettingsLog(ts() + " | Save failed: " + ex.getMessage());
        }
    }

    private void resetSettingsToDefaults() {
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Reset all settings to defaults and overwrite local settings.json?",
                "Reset Settings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        AppSettingsConfig.SettingsValues defaults = AppSettingsConfig.defaults();
        try {
            AppSettingsConfig.save(defaults);
            applySettingsToRuntime(defaults, true);
            try {
                SystemControlConfig.save(toSystemControlConfig(defaults));
            } catch (Exception ignored) {
                // Best-effort legacy sync.
            }
            styleInlineStatus(settingsStatusLabel, "Settings: reset to defaults", COLOR_SUCCESS);
            appendSettingsLog(ts() + " | Reset settings to defaults.");
            checkConnection();
        } catch (IOException ex) {
            styleInlineStatus(settingsStatusLabel, "Settings: reset failed", COLOR_DANGER);
            appendSettingsLog(ts() + " | Reset failed: " + ex.getMessage());
        }
    }

    private void testSettingsConnection() {
        AppSettingsConfig.SettingsValues values;
        try {
            values = readSettingsFromForm();
        } catch (IllegalArgumentException ex) {
            styleInlineStatus(settingsStatusLabel, "Settings: " + ex.getMessage(), COLOR_DANGER);
            appendSettingsLog(ts() + " | Test failed: " + ex.getMessage());
            return;
        }

        if (settingsTestConnectionButton != null) {
            settingsTestConnectionButton.setEnabled(false);
        }
        styleInlineStatus(settingsStatusLabel,
                "Settings: testing " + values.backendHost() + ":" + values.backendPort() + "...",
                COLOR_WARNING);
        appendSettingsLog(ts() + " | Testing backend at " + values.backendHost() + ":" + values.backendPort());

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> result = new LinkedHashMap<>();
                apiClient.configure(values.backendHost(), values.backendPort());
                boolean healthy = apiClient.healthCheck(8);
                result.put("healthy", healthy);
                if (healthy) {
                    try {
                        Object status = apiClient.fetchStatus();
                        result.put("status", status);
                    } catch (Exception ex) {
                        result.put("status_error", humanizeError(ex));
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                if (settingsTestConnectionButton != null) {
                    settingsTestConnectionButton.setEnabled(true);
                }
                configureClientFromUI();
                try {
                    Map<String, Object> result = get();
                    boolean healthy = Boolean.TRUE.equals(result.get("healthy"));
                    if (!healthy) {
                        styleInlineStatus(settingsStatusLabel, "Settings: FAIL /health", COLOR_DANGER);
                        appendSettingsLog(ts() + " | FAIL /health");
                        return;
                    }
                    Object statusErr = result.get("status_error");
                    if (statusErr != null) {
                        styleInlineStatus(settingsStatusLabel, "Settings: FAIL /api/status", COLOR_DANGER);
                        appendSettingsLog(ts() + " | PASS /health");
                        appendSettingsLog(ts() + " | FAIL /api/status: " + statusErr);
                        return;
                    }
                    styleInlineStatus(settingsStatusLabel, "Settings: PASS /health + /api/status", COLOR_SUCCESS);
                    appendSettingsLog(ts() + " | PASS /health");
                    appendSettingsLog(ts() + " | PASS /api/status");
                } catch (Exception ex) {
                    styleInlineStatus(settingsStatusLabel, "Settings: connection test failed", COLOR_DANGER);
                    appendSettingsLog(ts() + " | Connection test failed: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void copySettingsExportCommands() {
        AppSettingsConfig.SettingsValues values;
        try {
            values = readSettingsFromForm();
        } catch (IllegalArgumentException ex) {
            styleInlineStatus(settingsStatusLabel, "Settings: " + ex.getMessage(), COLOR_DANGER);
            return;
        }
        String commands = buildSettingsExportCommands(values);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(commands), null);
        styleInlineStatus(settingsStatusLabel, "Settings: export commands copied", COLOR_SUCCESS);
        appendSettingsLog(ts() + " | Copied export commands to clipboard.");
    }

    private String buildSettingsExportCommands(AppSettingsConfig.SettingsValues values) {
        AppSettingsConfig.SettingsValues safe = AppSettingsConfig.sanitize(values);
        Map<String, String> env = buildRuntimeEnvironment(safe);
        StringBuilder sb = new StringBuilder();
        sb.append(isWindows()
                ? "# PowerShell environment exports\n"
                : "# zsh/bash environment exports\n");
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isBlank()) {
                continue;
            }
            if (isWindows()) {
                sb.append("$env:").append(key).append("=").append(quotePowerShell(value)).append("\n");
            } else {
                sb.append("export ").append(key).append("=").append(quotePosix(value)).append("\n");
            }
        }
        return sb.toString();
    }

    private String quotePosix(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String quotePowerShell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("`", "``").replace("\"", "`\"") + "\"";
    }

    private void appendSettingsLog(String line) {
        if (settingsLogArea == null) {
            return;
        }
        settingsLogArea.append(line + "\n");
        settingsLogArea.setCaretPosition(settingsLogArea.getDocument().getLength());
    }

    private void applySettingsToRuntime(AppSettingsConfig.SettingsValues values, boolean syncUi) {
        AppSettingsConfig.SettingsValues safe = AppSettingsConfig.sanitize(values);
        appSettings = safe;

        if (syncUi) {
            populateSettingsForm(safe);
        }
        if (hostField != null) {
            hostField.setText(safe.backendHost());
        }
        if (portField != null) {
            portField.setText(String.valueOf(safe.backendPort()));
        }
        if (systemConfigHostField != null) {
            systemConfigHostField.setText(safe.backendHost());
        }
        if (systemConfigPortField != null) {
            systemConfigPortField.setText(String.valueOf(safe.backendPort()));
        }
        if (systemConfigAiPathField != null) {
            systemConfigAiPathField.setText(safe.aiRepoPath());
        }
        if (systemConfigOpsPathField != null) {
            systemConfigOpsPathField.setText(safe.opsRepoPath());
        }
        if (backendPathField != null) {
            backendPathField.setText(expandUserHome(safe.aiRepoPath()));
        }

        apiClient.configure(safe.backendHost(), safe.backendPort());
        apiClient.setLocalCommandEnvironment(buildRuntimeEnvironment(safe));
    }

    private Map<String, String> buildRuntimeEnvironment(AppSettingsConfig.SettingsValues settings) {
        AppSettingsConfig.SettingsValues safe = AppSettingsConfig.sanitize(settings);
        Map<String, String> env = new LinkedHashMap<>();
        env.put("LLM_PROVIDER", safe.llmProvider());
        env.put("ANTHROPIC_API_KEY", safe.anthropicApiKey());
        env.put("OPENAI_API_KEY", safe.openAiApiKey());
        env.put("NEWS_API_KEY", safe.newsApiKey());
        env.put("SLACK_WEBHOOK_URL", safe.slackWebhookUrl());
        env.put("DPOLARIS_DATA_DIR", safe.dataDir());
        return env;
    }

    private void applyEnvironmentValues(Map<String, String> target, Map<String, String> values) {
        if (target == null || values == null) {
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank()) {
                continue;
            }
            if (value == null || value.isBlank()) {
                target.remove(key);
            } else {
                target.put(key, value);
            }
        }
    }

    private SystemControlConfig.ConfigValues toSystemControlConfig(AppSettingsConfig.SettingsValues settings) {
        AppSettingsConfig.SettingsValues safe = AppSettingsConfig.sanitize(settings);
        return SystemControlConfig.sanitize(new SystemControlConfig.ConfigValues(
                safe.backendHost(),
                safe.backendPort(),
                safe.aiRepoPath(),
                safe.opsRepoPath(),
                SystemControlConfig.DEFAULT_DEVICE_PREFERENCE
        ));
    }

    private void refreshStartupBanner(boolean backendHealthy) {
        if (startupBannerPanel == null) {
            return;
        }
        startupBannerPanel.setVisible(!backendHealthy);
        if (startupBannerStartButton != null) {
            startupBannerStartButton.setEnabled(!backendHealthy && !systemControlActionInFlight);
        }
        if (startupBannerLabel != null) {
            startupBannerLabel.setText(backendHealthy
                    ? "Backend healthy."
                    : "Backend is not healthy at " + currentHostPort() + ". Start it with one click.");
        }
    }

    private void reloadSystemControlConfig() {
        SystemControlConfig.ConfigValues loaded = SystemControlConfig.load();
        applyConfigToUi(loaded);
        appendSystemControlLog(ts() + " | Loaded config from user home.");
        if (systemConfigStatusLabel != null) {
            styleInlineStatus(systemConfigStatusLabel, "Config: loaded from file", COLOR_SUCCESS);
        }
        checkConnection();
        refreshSystemStatuses(false);
    }

    private void saveSystemControlConfig() {
        String host = systemConfigHostField == null ? "" : systemConfigHostField.getText().trim();
        String portText = systemConfigPortField == null ? "" : systemConfigPortField.getText().trim();
        String aiPath = systemConfigAiPathField == null ? "" : systemConfigAiPathField.getText().trim();
        String opsPath = systemConfigOpsPathField == null ? "" : systemConfigOpsPathField.getText().trim();

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port <= 0) {
                throw new NumberFormatException("Port must be positive");
            }
        } catch (NumberFormatException ex) {
            if (systemConfigStatusLabel != null) {
                styleInlineStatus(systemConfigStatusLabel, "Config: invalid port", COLOR_DANGER);
            }
            appendSystemControlLog(ts() + " | Config save failed: invalid port (" + portText + ").");
            return;
        }

        SystemControlConfig.ConfigValues values = SystemControlConfig.sanitize(
                new SystemControlConfig.ConfigValues(host, port, aiPath, opsPath, SystemControlConfig.DEFAULT_DEVICE_PREFERENCE)
        );
        try {
            SystemControlConfig.save(values);
            applyConfigToUi(values);
            try {
                AppSettingsConfig.save(appSettings);
            } catch (Exception ignored) {
                // Legacy save path should not fail because settings persistence is unavailable.
            }
            if (systemConfigStatusLabel != null) {
                styleInlineStatus(systemConfigStatusLabel, "Config: saved", COLOR_SUCCESS);
            }
            appendSystemControlLog(ts() + " | Saved config to user home.");
            checkConnection();
            refreshSystemStatuses(false);
        } catch (IOException ex) {
            if (systemConfigStatusLabel != null) {
                styleInlineStatus(systemConfigStatusLabel, "Config: save failed", COLOR_DANGER);
            }
            appendSystemControlLog(ts() + " | Config save failed: " + ex.getMessage());
        }
    }

    private void applyConfigToUi(SystemControlConfig.ConfigValues config) {
        SystemControlConfig.ConfigValues safe = SystemControlConfig.sanitize(config);
        hostField.setText(safe.backendHost());
        portField.setText(String.valueOf(safe.backendPort()));
        if (systemConfigHostField != null) {
            systemConfigHostField.setText(safe.backendHost());
        }
        if (systemConfigPortField != null) {
            systemConfigPortField.setText(String.valueOf(safe.backendPort()));
        }
        if (systemConfigAiPathField != null) {
            systemConfigAiPathField.setText(safe.aiRepoPath());
        }
        if (systemConfigOpsPathField != null) {
            systemConfigOpsPathField.setText(safe.opsRepoPath());
        }
        if (backendPathField != null) {
            backendPathField.setText(expandUserHome(safe.aiRepoPath()));
        }
        AppSettingsConfig.SettingsValues baseline = appSettings == null
                ? AppSettingsConfig.defaults()
                : appSettings;
        appSettings = AppSettingsConfig.sanitize(new AppSettingsConfig.SettingsValues(
                safe.backendHost(),
                safe.backendPort(),
                baseline.llmProvider(),
                baseline.anthropicApiKey(),
                baseline.openAiApiKey(),
                baseline.newsApiKey(),
                baseline.slackWebhookUrl(),
                safe.aiRepoPath(),
                safe.opsRepoPath(),
                baseline.dataDir()
        ));
        populateSettingsForm(appSettings);
        apiClient.configure(appSettings.backendHost(), appSettings.backendPort());
        apiClient.setLocalCommandEnvironment(buildRuntimeEnvironment(appSettings));
    }

    private String resolveConfiguredAiPath() {
        String fallback = appSettings == null ? initialControlConfig.aiRepoPath() : appSettings.aiRepoPath();
        String raw = systemConfigAiPathField == null
                ? fallback
                : systemConfigAiPathField.getText().trim();
        if (raw == null || raw.isBlank()) {
            raw = fallback;
        }
        return expandUserHome(raw);
    }

    private String resolveConfiguredOpsPath() {
        String fallback = appSettings == null ? initialControlConfig.opsRepoPath() : appSettings.opsRepoPath();
        String raw = systemConfigOpsPathField == null
                ? fallback
                : systemConfigOpsPathField.getText().trim();
        if (raw == null || raw.isBlank()) {
            raw = fallback;
        }
        return raw;
    }

    private void applySystemHostPortToHeader() {
        if (systemConfigHostField != null && !systemConfigHostField.getText().trim().isEmpty()) {
            hostField.setText(systemConfigHostField.getText().trim());
        }
        if (systemConfigPortField != null && !systemConfigPortField.getText().trim().isEmpty()) {
            portField.setText(systemConfigPortField.getText().trim());
        }
        configureClientFromUI();
    }

    private void runBackendControlAction(String action, boolean fromBanner) {
        if (systemControlActionInFlight) {
            return;
        }
        applySystemHostPortToHeader();
        String normalizedAction = action == null ? "status" : action.trim().toLowerCase();
        String actionLabel = switch (normalizedAction) {
            case "start" -> "start";
            case "stop" -> "stop";
            case "restart" -> "restart";
            case "reset" -> "reset/restart";
            default -> "status";
        };

        setSystemControlBusy(true, "Backend " + actionLabel + " in progress...", COLOR_WARNING);
        if (systemBackendActionLabel != null) {
            styleInlineStatus(systemBackendActionLabel, "Backend action: " + actionLabel + "...", COLOR_WARNING);
        }
        appendSystemControlLog(ts() + " | Backend action requested: " + actionLabel);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> response;
                switch (normalizedAction) {
                    case "start" -> response = apiClient.startBackendControl();
                    case "stop" -> response = apiClient.stopBackendControl();
                    case "restart" -> response = apiClient.restartBackendControl(false);
                    case "reset" -> {
                        try {
                            response = apiClient.restartBackendControl(true);
                        } catch (Exception cleanError) {
                            Map<String, Object> fallback = new LinkedHashMap<>();
                            fallback.put("clean_restart_error", cleanError.getMessage());
                            fallback.put("stop", apiClient.stopBackendControl());
                            fallback.put("start", apiClient.startBackendControl());
                            response = fallback;
                        }
                    }
                    default -> response = apiClient.fetchBackendControlStatus();
                }
                try {
                    response.put("status_after", apiClient.fetchBackendControlStatus());
                } catch (Exception statusError) {
                    response.put("status_error", statusError.getMessage());
                }
                return response;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> response = get();
                    appendSystemControlLog(ts() + " | Backend " + actionLabel + " completed.");
                    appendSystemControlLog(Json.pretty(response));
                    Object statusAfter = response.get("status_after");
                    Map<String, Object> statusPayload = statusAfter instanceof Map<?, ?> mapRaw
                            ? Json.asObject(mapRaw)
                            : response;
                    renderBackendControlStatus(statusPayload, "");
                    String errorDetail = stringOrEmpty(findAnyValue(response, "error_detail", "error", "detail", "status_error"));
                    boolean hasError = !errorDetail.isBlank();
                    if (("start".equals(normalizedAction)
                            || "restart".equals(normalizedAction)
                            || "reset".equals(normalizedAction))
                            && !hasError) {
                        lastBackendRestart = ts();
                    }
                    if (systemBackendActionLabel != null) {
                        styleInlineStatus(
                                systemBackendActionLabel,
                                "Backend action: " + actionLabel + (hasError ? " warning" : " done"),
                                hasError ? COLOR_WARNING : COLOR_SUCCESS
                        );
                    }
                    setSystemControlBusy(false, "Backend " + actionLabel + (hasError ? " completed with warnings" : " completed"),
                            hasError ? COLOR_WARNING : COLOR_SUCCESS);
                    checkConnection();
                    if (fromBanner) {
                        refreshSystemStatuses(true);
                    }
                } catch (Exception ex) {
                    String message = humanizeError(ex);
                    appendSystemControlLog(ts() + " | Backend " + actionLabel + " failed: " + message);
                    renderBackendControlStatus(lastBackendControlStatus, message);
                    if (systemBackendActionLabel != null) {
                        styleInlineStatus(systemBackendActionLabel, "Backend action: failed", COLOR_DANGER);
                    }
                    setSystemControlBusy(false, "Backend " + actionLabel + " failed", COLOR_DANGER);
                    checkConnection();
                }
            }
        };
        worker.execute();
    }

    private void runOrchestratorControlAction(String action) {
        if (systemControlActionInFlight) {
            return;
        }
        String normalizedAction = action == null ? "status" : action.trim().toLowerCase();
        String opsPath = resolveConfiguredOpsPath();
        setSystemControlBusy(true, "Orchestrator " + normalizedAction + " in progress...", COLOR_WARNING);
        if (systemOpsActionLabel != null) {
            styleInlineStatus(systemOpsActionLabel, "Orchestrator action: " + normalizedAction + "...", COLOR_WARNING);
        }
        appendSystemControlLog(ts() + " | Orchestrator action requested: " + normalizedAction + " (" + opsPath + ")");

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                Map<String, Object> response = switch (normalizedAction) {
                    case "start" -> apiClient.startOrchestrator(opsPath);
                    case "stop" -> apiClient.stopOrchestrator(opsPath);
                    case "restart" -> apiClient.restartOrchestrator(opsPath);
                    default -> apiClient.fetchOrchestratorStatus(opsPath);
                };
                if (!"status".equals(normalizedAction)) {
                    try {
                        response.put("status_after", apiClient.fetchOrchestratorStatus(opsPath));
                    } catch (Exception statusError) {
                        response.put("status_error", statusError.getMessage());
                    }
                }
                return response;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> response = get();
                    appendSystemControlLog(ts() + " | Orchestrator " + normalizedAction + " completed.");
                    appendSystemControlLog(Json.pretty(response));
                    Object statusAfter = response.get("status_after");
                    Map<String, Object> statusPayload = statusAfter instanceof Map<?, ?> mapRaw
                            ? Json.asObject(mapRaw)
                            : response;
                    renderOrchestratorStatus(statusPayload, stringOrEmpty(response.get("status_error")));

                    String errorDetail = stringOrEmpty(findAnyValue(response, "error_detail", "error", "detail", "stderr"));
                    boolean hasError = !errorDetail.isBlank() && !"status".equals(normalizedAction);
                    if (systemOpsActionLabel != null) {
                        styleInlineStatus(
                                systemOpsActionLabel,
                                "Orchestrator action: " + normalizedAction + (hasError ? " warning" : " done"),
                                hasError ? COLOR_WARNING : COLOR_SUCCESS
                        );
                    }
                    setSystemControlBusy(false,
                            "Orchestrator " + normalizedAction + (hasError ? " completed with warnings" : " completed"),
                            hasError ? COLOR_WARNING : COLOR_SUCCESS);
                } catch (Exception ex) {
                    String message = humanizeError(ex);
                    appendSystemControlLog(ts() + " | Orchestrator " + normalizedAction + " failed: " + message);
                    renderOrchestratorStatus(lastOpsControlStatus, message);
                    if (systemOpsActionLabel != null) {
                        styleInlineStatus(systemOpsActionLabel, "Orchestrator action: failed", COLOR_DANGER);
                    }
                    setSystemControlBusy(false, "Orchestrator " + normalizedAction + " failed", COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void setSystemControlBusy(boolean busy, String message, Color color) {
        systemControlActionInFlight = busy;
        setSystemControlButtonsEnabled(!busy);
        if (systemControlProgressLabel != null) {
            styleInlineStatus(systemControlProgressLabel, message, color);
        }
        if (startupBannerStartButton != null && startupBannerPanel != null && startupBannerPanel.isVisible()) {
            startupBannerStartButton.setEnabled(!busy);
        }
    }

    private void setSystemControlButtonsEnabled(boolean enabled) {
        if (systemBackendStartButton != null) {
            systemBackendStartButton.setEnabled(enabled);
        }
        if (systemBackendStopButton != null) {
            systemBackendStopButton.setEnabled(enabled);
        }
        if (systemBackendRestartButton != null) {
            systemBackendRestartButton.setEnabled(enabled);
        }
        if (systemBackendStatusButton != null) {
            systemBackendStatusButton.setEnabled(enabled);
        }
        if (systemBackendResetButton != null) {
            systemBackendResetButton.setEnabled(enabled);
        }
        if (systemOpsStartButton != null) {
            systemOpsStartButton.setEnabled(enabled);
        }
        if (systemOpsStopButton != null) {
            systemOpsStopButton.setEnabled(enabled);
        }
        if (systemOpsRestartButton != null) {
            systemOpsRestartButton.setEnabled(enabled);
        }
        if (systemOpsStatusButton != null) {
            systemOpsStatusButton.setEnabled(enabled);
        }
        if (systemConfigSaveButton != null) {
            systemConfigSaveButton.setEnabled(enabled);
        }
        if (systemConfigReloadButton != null) {
            systemConfigReloadButton.setEnabled(enabled);
        }
    }

    private void refreshSystemStatuses(boolean manual) {
        if (systemBackendStatusArea == null || systemOpsStatusArea == null || systemControlActionInFlight) {
            return;
        }
        applySystemHostPortToHeader();
        if (manual && systemControlProgressLabel != null) {
            styleInlineStatus(systemControlProgressLabel, "Refreshing statuses...", COLOR_WARNING);
        }

        String opsPath = resolveConfiguredOpsPath();
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> payload = new LinkedHashMap<>();
                try {
                    payload.put("backend", apiClient.fetchBackendControlStatus());
                } catch (Exception backendError) {
                    payload.put("backend_error", humanizeError(backendError));
                }
                try {
                    payload.put("ops", apiClient.fetchOrchestratorStatus(opsPath));
                } catch (Exception opsError) {
                    payload.put("ops_error", humanizeError(opsError));
                }
                return payload;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> payload = get();
                    Object backend = payload.get("backend");
                    if (backend instanceof Map<?, ?> mapRaw) {
                        renderBackendControlStatus(Json.asObject(mapRaw), "");
                    } else {
                        renderBackendControlStatus(lastBackendControlStatus, stringOrEmpty(payload.get("backend_error")));
                    }

                    Object ops = payload.get("ops");
                    if (ops instanceof Map<?, ?> mapRaw) {
                        renderOrchestratorStatus(Json.asObject(mapRaw), "");
                    } else {
                        renderOrchestratorStatus(lastOpsControlStatus, stringOrEmpty(payload.get("ops_error")));
                    }
                    if (manual && systemControlProgressLabel != null) {
                        styleInlineStatus(systemControlProgressLabel, "Status refreshed " + ts(), COLOR_SUCCESS);
                    }
                } catch (Exception ex) {
                    if (manual && systemControlProgressLabel != null) {
                        styleInlineStatus(systemControlProgressLabel, "Status refresh failed", COLOR_DANGER);
                    }
                    appendSystemControlLog(ts() + " | Status refresh failed: " + humanizeError(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderBackendControlStatus(Map<String, Object> status, String fallbackError) {
        Map<String, Object> payload = status == null ? new LinkedHashMap<>() : new LinkedHashMap<>(status);
        if (!payload.isEmpty()) {
            lastBackendControlStatus = payload;
        }

        String runningRaw = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "running", "is_running", "alive", "status")),
                "unknown"
        );
        String running = normalizeRunningValue(runningRaw);
        String pid = firstNonBlank(stringOrEmpty(findAnyValue(payload, "pid", "process_id", "processId")), "—");
        String pythonExecutable = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "python_executable", "python", "python_path", "executable")),
                "—"
        );
        String port = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "port", "backend_port")),
                String.valueOf(currentPort())
        );
        String lastHealth = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "last_health", "health", "lastHealth")),
                lastBackendHealth
        );
        String lastRestart = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "last_restart", "restarted_at", "lastRestart")),
                this.lastBackendRestart
        );
        String errorDetail = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "error_detail", "error", "detail", "stderr")),
                fallbackError == null ? "" : fallbackError
        );

        StringBuilder sb = new StringBuilder();
        sb.append("running?: ").append(running).append("\n");
        sb.append("pid: ").append(pid).append("\n");
        sb.append("python_executable: ").append(pythonExecutable).append("\n");
        sb.append("port: ").append(port).append("\n");
        sb.append("last_health: ").append(firstNonBlank(lastHealth, "—")).append("\n");
        sb.append("last_restart: ").append(firstNonBlank(lastRestart, "—")).append("\n");
        sb.append("error detail: ").append(errorDetail.isBlank() ? "—" : errorDetail);
        if (systemBackendStatusArea != null) {
            systemBackendStatusArea.setText(sb.toString());
            systemBackendStatusArea.setCaretPosition(0);
        }
    }

    private void renderOrchestratorStatus(Map<String, Object> status, String fallbackError) {
        Map<String, Object> payload = status == null ? new LinkedHashMap<>() : new LinkedHashMap<>(status);
        if (!payload.isEmpty()) {
            lastOpsControlStatus = payload;
        }

        String runningRaw = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "running", "is_running", "alive", "status")),
                "unknown"
        );
        String running = normalizeRunningValue(runningRaw);
        String pid = firstNonBlank(stringOrEmpty(findAnyValue(payload, "pid", "process_id", "processId")), "—");
        String heartbeat = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "last_heartbeat", "heartbeat", "lastHeartbeat")),
                "—"
        );
        String lastScanRun = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "last_scan_run", "lastScanRun", "scan_run")),
                "—"
        );
        String errorDetail = firstNonBlank(
                stringOrEmpty(findAnyValue(payload, "error_detail", "error", "detail", "stderr")),
                fallbackError == null ? "" : fallbackError
        );

        StringBuilder sb = new StringBuilder();
        sb.append("running?: ").append(running).append("\n");
        sb.append("pid: ").append(pid).append("\n");
        sb.append("last_heartbeat: ").append(heartbeat).append("\n");
        sb.append("last_scan_run: ").append(lastScanRun).append("\n");
        sb.append("error detail: ").append(errorDetail.isBlank() ? "—" : errorDetail);
        if (systemOpsStatusArea != null) {
            systemOpsStatusArea.setText(sb.toString());
            systemOpsStatusArea.setCaretPosition(0);
        }
    }

    private String normalizeRunningValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String lowered = raw.trim().toLowerCase();
        if ("true".equals(lowered) || "running".equals(lowered) || "up".equals(lowered) || "active".equals(lowered)) {
            return "yes";
        }
        if ("false".equals(lowered) || "stopped".equals(lowered) || "down".equals(lowered)
                || "inactive".equals(lowered) || "not running".equals(lowered)) {
            return "no";
        }
        return raw;
    }

    private void copySystemDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("dPolaris System Diagnostics\n");
        sb.append("generated_at=").append(ts()).append("\n");
        sb.append("backend_target=").append(currentHostPort()).append("\n");
        sb.append("backend_health=").append(lastBackendHealth).append("\n");
        sb.append("last_backend_restart=").append(lastBackendRestart).append("\n");
        sb.append("ai_repo=").append(resolveConfiguredAiPath()).append("\n");
        sb.append("ops_repo=").append(expandUserHome(resolveConfiguredOpsPath())).append("\n\n");

        sb.append("[Backend Status]\n");
        sb.append(systemBackendStatusArea == null ? "n/a" : systemBackendStatusArea.getText()).append("\n\n");
        sb.append("[Orchestrator Status]\n");
        sb.append(systemOpsStatusArea == null ? "n/a" : systemOpsStatusArea.getText()).append("\n\n");
        sb.append("[Backend Raw JSON]\n").append(Json.pretty(lastBackendControlStatus)).append("\n\n");
        sb.append("[Orchestrator Raw JSON]\n").append(Json.pretty(lastOpsControlStatus)).append("\n\n");
        sb.append("[Recent Activity]\n");
        sb.append(systemControlLogArea == null ? "n/a" : limitLines(systemControlLogArea.getText(), 120));

        String text = sb.toString();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        appendSystemControlLog(ts() + " | Diagnostics copied to clipboard.");
        if (systemControlProgressLabel != null) {
            styleInlineStatus(systemControlProgressLabel, "Diagnostics copied", COLOR_SUCCESS);
        }
    }

    private void appendSystemControlLog(String line) {
        if (systemControlLogArea == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            systemControlLogArea.append(line + "\n");
            systemControlLogArea.setCaretPosition(systemControlLogArea.getDocument().getLength());
        });
    }

    private String defaultBackendPath() {
        String configured = resolveConfiguredAiPath();
        if (configured != null && !configured.isBlank() && new File(configured).isDirectory()) {
            return configured;
        }

        String envPath = System.getenv("DPOLARIS_AI_PATH");
        if (envPath != null && !envPath.isBlank()) {
            String expanded = expandUserHome(envPath.trim());
            if (new File(expanded).isDirectory()) {
                return expanded;
            }
        }

        String home = System.getProperty("user.home");
        List<String> candidates = List.of(
                home + File.separator + "my-git" + File.separator + "dpolaris_ai",
                home + File.separator + "my-git" + File.separator + "dPolaris_ai",
                home + File.separator + "dpolaris_ai"
        );
        for (String candidate : candidates) {
            if (new File(candidate).isDirectory()) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private static String expandUserHome(String path) {
        if (path.startsWith("~" + File.separator) || "~".equals(path)) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private boolean backendIsRunning() {
        Process process = backendProcess;
        return process != null && process.isAlive();
    }

    private void refreshBackendControls() {
        boolean managedRunning = backendIsRunning();
        boolean externalConnected = backendExternalConnected && !managedRunning && !backendStarting;
        boolean isRunning = managedRunning || externalConnected;
        boolean isBusy = backendStarting;

        // Update status label
        if (backendStatusValue != null) {
            if (backendStarting) {
                styleInlineStatus(backendStatusValue, "Backend: starting...", COLOR_WARNING);
            } else if (managedRunning) {
                styleInlineStatus(backendStatusValue, "Backend: running", COLOR_SUCCESS);
            } else if (externalConnected) {
                styleInlineStatus(backendStatusValue, "Backend: connected (external)", COLOR_SUCCESS);
            } else {
                styleInlineStatus(backendStatusValue, "Backend: stopped", COLOR_MUTED);
            }
        }

        // Update System Control panel buttons
        if (systemBackendStartButton != null) {
            systemBackendStartButton.setEnabled(!isRunning && !isBusy);
        }
        if (systemBackendStopButton != null) {
            systemBackendStopButton.setEnabled(isRunning && !isBusy);
        }
        if (systemBackendRestartButton != null) {
            systemBackendRestartButton.setEnabled(isRunning && !isBusy);
        }
    }

    private void startBackendProcess() {
        appendBackendLog(ts() + " | Start Backend button clicked");
        if (backendStarting) {
            appendBackendLog(ts() + " | Backend is already starting, please wait...");
            return;
        }
        if (backendIsRunning()) {
            appendBackendLog(ts() + " | Backend process is already running.");
            return;
        }

        String rawPath;
        if (backendPathField != null) {
            rawPath = backendPathField.getText().trim();
        } else if (systemConfigAiPathField != null) {
            rawPath = systemConfigAiPathField.getText().trim();
        } else {
            rawPath = resolveConfiguredAiPath();
        }
        if (rawPath == null || rawPath.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter the backend project path.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String backendPath = expandUserHome(rawPath);
        File backendDir = new File(backendPath);
        if (!backendDir.isDirectory()) {
            JOptionPane.showMessageDialog(frame, "Backend path does not exist:\n" + backendPath, "Invalid Path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (backendPathField != null) {
            backendPathField.setText(backendPath);
        }

        configureClientFromUI();
        backendPortConflictLogged = false;
        backendExternalConnected = false;
        backendStarting = true;
        refreshBackendControls();
        appendBackendLog(ts() + " | Starting AI backend from: " + backendPath);
        AppSettingsConfig.SettingsValues runtimeSettings = appSettings == null
                ? AppSettingsConfig.defaults()
                : AppSettingsConfig.sanitize(appSettings);
        String targetHost = runtimeSettings.backendHost();
        int targetPort = runtimeSettings.backendPort();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean reusedExternal;
            private boolean replacedExternal;
            private boolean healthyAfterStart;

            @Override
            protected Void doInBackground() throws Exception {
                if (apiClient.healthCheck(2)) {
                    if (apiClient.supportsScanApi()) {
                        reusedExternal = true;
                        return null;
                    }

                    int port = currentPort();
                    appendBackendLog(ts() + " | Existing backend on port " + port
                            + " does not expose scan endpoints; replacing it.");
                    List<Long> pids = findListeningPids(port);
                    if (pids.isEmpty()) {
                        throw new IOException("No listening process found on port " + port + ".");
                    }
                    for (Long pid : pids) {
                        stopPid(pid);
                    }
                    replacedExternal = true;

                    for (int i = 0; i < 20; i++) {
                        if (!apiClient.healthCheck(1)) {
                            break;
                        }
                        Thread.sleep(250);
                    }
                }

                List<String> command = buildBackendCommand(backendPath, targetHost, targetPort);
                appendBackendLog(ts() + " | Command: " + String.join(" ", command));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(backendDir);
                builder.redirectErrorStream(true);
                builder.environment().put("PYTHONUNBUFFERED", "1");
                applyEnvironmentValues(builder.environment(), buildRuntimeEnvironment(runtimeSettings));

                Process process = builder.start();
                backendProcess = process;

                startBackendLogPump(process);
                startBackendExitWatcher(process);
                healthyAfterStart = waitForBackendReady(40, 500L);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (reusedExternal) {
                        backendExternalConnected = true;
                        appendBackendLog(ts() + " | Backend already reachable on "
                                + hostField.getText().trim() + ":" + portField.getText().trim()
                                + ". Reusing compatible backend.");
                    } else {
                        backendExternalConnected = false;
                        if (replacedExternal) {
                            appendBackendLog(ts() + " | Replaced incompatible backend instance.");
                        }
                        appendBackendLog(ts() + " | Backend process started.");
                        if (!healthyAfterStart) {
                            appendBackendLog(ts() + " | Backend started but health check is still pending.");
                        }
                    }
                    backendStarting = false;
                    refreshBackendControls();
                    checkConnection();
                } catch (Exception ex) {
                    backendExternalConnected = false;
                    backendStarting = false;
                    backendProcess = null;
                    refreshBackendControls();
                    appendBackendLog(ts() + " | Failed to start backend: " + ex.getMessage());
                    checkConnection();
                }
            }
        };
        worker.execute();
    }

    private List<String> buildBackendCommand(String backendPath, String host, int port) {
        String venvPython = isWindows()
                ? backendPath + File.separator + ".venv" + File.separator + "Scripts" + File.separator + "python.exe"
                : backendPath + File.separator + ".venv" + File.separator + "bin" + File.separator + "python";

        String pythonExecutable;
        if (new File(venvPython).isFile()) {
            pythonExecutable = venvPython;
        } else if (isWindows()) {
            pythonExecutable = "python";
        } else {
            List<String> preferred = List.of(
                    "/opt/homebrew/bin/python3.12",
                    "/usr/local/bin/python3.12",
                    "/Library/Frameworks/Python.framework/Versions/3.12/bin/python3.12",
                    "/opt/homebrew/bin/python3.11",
                    "/usr/local/bin/python3.11",
                    "/Library/Frameworks/Python.framework/Versions/3.11/bin/python3.11"
            );
            pythonExecutable = "python3";
            for (String candidate : preferred) {
                if (new File(candidate).isFile()) {
                    pythonExecutable = candidate;
                    break;
                }
            }
        }
        String safeHost = host == null || host.isBlank() ? "127.0.0.1" : host.trim();
        int safePort = port > 0 ? port : 8420;
        return List.of(
                pythonExecutable,
                "-m",
                "cli.main",
                "server",
                "--host",
                safeHost,
                "--port",
                String.valueOf(safePort)
        );
    }

    private void startBackendLogPump(Process process) {
        Thread pump = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendBackendLog(line);
                    String lowered = line.toLowerCase();
                    if (!backendPortConflictLogged && lowered.contains("address already in use")) {
                        backendPortConflictLogged = true;
                        appendBackendLog(ts() + " | Port " + portField.getText().trim()
                                + " is already in use. Use the existing backend or stop the process on that port.");
                    }
                }
            } catch (IOException ex) {
                if (process.isAlive()) {
                    appendBackendLog(ts() + " | Backend log stream error: " + ex.getMessage());
                }
            }
        }, "dpolaris-backend-log-pump");
        pump.setDaemon(true);
        pump.start();
    }

    private void startBackendExitWatcher(Process process) {
        Thread watcher = new Thread(() -> {
            int exitCode = -1;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                if (backendProcess == process) {
                    backendProcess = null;
                }
                int code = exitCode;
                SwingUtilities.invokeLater(() -> {
                    backendStarting = false;
                    refreshBackendControls();
                    appendBackendLog(ts() + " | Backend exited with code " + code);
                    checkConnection();
                });
            }
        }, "dpolaris-backend-exit-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void stopBackendProcess() {
        Process process = backendProcess;
        if (backendStarting) {
            appendBackendLog(ts() + " | Backend operation already in progress.");
            return;
        }

        // If we have a managed process, stop it
        if (process != null && process.isAlive()) {
            backendStarting = false;
            backendExternalConnected = false;
            refreshBackendControls();
            appendBackendLog(ts() + " | Stopping managed backend process...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    process.destroy();
                    try {
                        if (!process.waitFor(5, TimeUnit.SECONDS)) {
                            appendBackendLog(ts() + " | Graceful stop timed out; forcing termination.");
                            process.destroyForcibly();
                            process.waitFor(5, TimeUnit.SECONDS);
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    backendProcess = null;
                    backendExternalConnected = false;
                    refreshBackendControls();
                    appendBackendLog(ts() + " | Managed backend process stopped.");
                }
            };
            worker.execute();
            return;
        }

        // Otherwise, try to stop any process on the port
        stopExternalBackendByPort();
    }

    private void stopExternalBackendByPort() {
        configureClientFromUI();
        int port = currentPort();
        appendBackendLog(ts() + " | Stopping external backend on port " + port + "...");
        backendStarting = true;
        refreshBackendControls();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Long> pids = findListeningPids(port);
                if (pids.isEmpty()) {
                    throw new IOException("No listening process found on port " + port + ".");
                }
                for (Long pid : pids) {
                    stopPid(pid);
                }
                for (int i = 0; i < 10; i++) {
                    if (!apiClient.healthCheck(1)) {
                        return null;
                    }
                    Thread.sleep(300);
                }
                return null;
            }

            @Override
            protected void done() {
                backendStarting = false;
                backendExternalConnected = false;
                try {
                    get();
                    appendBackendLog(ts() + " | External backend stop requested.");
                } catch (Exception ex) {
                    String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
                    appendBackendLog(ts() + " | Failed to stop external backend: " + message);
                    appendBackendLog(ts() + " | Manual fallback: kill process listening on port " + port + ".");
                }
                refreshBackendControls();
                checkConnection();
            }
        };
        worker.execute();
    }

    private int currentPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            return 8420;
        }
    }

    private boolean waitForBackendReady(int attempts, long sleepMillis) throws InterruptedException {
        int safeAttempts = Math.max(1, attempts);
        long safeSleepMs = Math.max(100L, sleepMillis);
        for (int i = 0; i < safeAttempts; i++) {
            if (apiClient.healthCheck(1)) {
                return true;
            }
            Process process = backendProcess;
            if (process != null && !process.isAlive() && i >= 2) {
                return false;
            }
            Thread.sleep(safeSleepMs);
        }
        return apiClient.healthCheck(1);
    }

    private List<Long> findListeningPids(int port) throws IOException, InterruptedException {
        LinkedHashSet<Long> pids = new LinkedHashSet<>();
        if (isWindows()) {
            List<String> lines = runCommandAndCollect("cmd", "/c", "netstat -ano -p tcp");
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (!lower.contains("listen")) {
                    continue;
                }
                if (!line.contains(":" + port)) {
                    continue;
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    try {
                        pids.add(Long.parseLong(parts[parts.length - 1]));
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed rows.
                    }
                }
            }
        } else {
            List<String> lines = runCommandAndCollect("lsof", "-nP", "-ti", "TCP:" + port, "-sTCP:LISTEN");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    pids.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed rows.
                }
            }
        }
        return new ArrayList<>(pids);
    }

    private List<String> runCommandAndCollect(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        process.waitFor(5, TimeUnit.SECONDS);
        return lines;
    }

    private void stopPid(long pid) throws IOException, InterruptedException {
        if (isWindows()) {
            runCommandAndCollect("taskkill", "/PID", String.valueOf(pid), "/F");
            return;
        }
        runCommandAndCollect("kill", "-TERM", String.valueOf(pid));
        Thread.sleep(250);
        ProcessHandle.of(pid).ifPresent(handle -> {
            if (handle.isAlive()) {
                try {
                    runCommandAndCollect("kill", "-KILL", String.valueOf(pid));
                } catch (Exception ignored) {
                    // Best effort kill.
                }
            }
        });
    }

    private void startDaemon() {
        configureClientFromUI();
        setDaemonStatus("Scheduler: starting...", COLOR_WARNING);
        appendBackendLog(ts() + " | Starting scheduler daemon...");
        runDaemonRequest(true);
    }

    private void stopDaemon() {
        configureClientFromUI();
        setDaemonStatus("Scheduler: stopping...", COLOR_WARNING);
        appendBackendLog(ts() + " | Stopping scheduler daemon...");
        runDaemonRequest(false);
    }

    private void runDaemonRequest(boolean start) {
        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                return start ? apiClient.startDaemon() : apiClient.stopDaemon();
            }

            @Override
            protected void done() {
                try {
                    Object response = get();
                    appendBackendLog(ts() + " | Daemon " + (start ? "started" : "stopped") + ".");
                    appendBackendLog(Json.pretty(response));
                    setDaemonStatus(start ? "Scheduler: running" : "Scheduler: stopped",
                            start ? COLOR_SUCCESS : COLOR_MUTED);
                } catch (Exception ex) {
                    appendBackendLog(ts() + " | Daemon " + (start ? "start" : "stop") + " failed: "
                            + ex.getMessage());
                    setDaemonStatus("Scheduler: error", COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void refreshTrainingControls() {
        // Training panel was removed - skip if controls are null
        if (modeCombo == null || deepModelCombo == null || epochsSpinner == null) {
            return;
        }
        boolean deepMode = MODE_DEEP.equals(modeCombo.getSelectedItem());
        deepModelCombo.setEnabled(deepMode);
        epochsSpinner.setEnabled(deepMode);
        updateDeepModelTooltip();
    }

    private void updateDeepModelTooltip() {
        if (deepModelCombo == null) {
            return;
        }
        Object selected = deepModelCombo.getSelectedItem();
        String model = selected == null ? null : String.valueOf(selected);
        deepModelCombo.setToolTipText(buildDeepModelTooltip(model));
    }

    private String buildTrainingModeTooltip() {
        return "<html><b>Training Modes</b><br>"
                + "<b>Stable (XGBoost)</b>: faster classical model for baseline signals.<br>"
                + "<b>Deep Learning</b>: neural models (LSTM/Transformer), usually slower but can model complex sequences."
                + "</html>";
    }

    private String buildDeepModelTooltip(String selectedModel) {
        String selected = selectedModel == null ? "lstm" : selectedModel.toLowerCase();
        String selectedLabel = "transformer".equals(selected) ? "Transformer" : "LSTM";
        return "<html><b>Deep Model Guide</b><br>"
                + "<b>LSTM</b>: sequence memory model, typically lighter/faster on smaller datasets.<br>"
                + "<b>Transformer</b>: attention-based model, better at longer-range patterns but usually needs more data/compute.<br>"
                + "<br><b>Selected:</b> " + selectedLabel + "</html>";
    }

    private void startTraining() {
        if (activeTrainingWorker != null && !activeTrainingWorker.isDone()) {
            JOptionPane.showMessageDialog(frame, "Training worker already running.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        configureClientFromUI();
        String symbol = symbolField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a symbol.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String mode = String.valueOf(modeCombo.getSelectedItem());
        String deepModel = String.valueOf(deepModelCombo.getSelectedItem());
        int epochs = (Integer) epochsSpinner.getValue();
        deliveredLogCount = 0;
        initializeTrainingAudit(symbol, mode, deepModel, epochs);

        trainButton.setEnabled(false);
        stopButton.setEnabled(true);
        setTrainingStatus("Training: running", COLOR_WARNING);
        appendTrainingLog("------------------------------------------------------------");
        appendTrainingLog(ts() + " | Starting training: symbol=" + symbol + ", mode=" + mode
                + (MODE_DEEP.equals(mode) ? ", model=" + deepModel + ", epochs=" + epochs : ""));

        activeTrainingWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (MODE_STABLE.equals(mode)) {
                    runStableTraining(symbol);
                } else {
                    runDeepTraining(symbol, deepModel, epochs);
                }
                return null;
            }

            @Override
            protected void done() {
                finalizeTrainingAuditIfNeededFromStatus();
                trainButton.setEnabled(true);
                stopButton.setEnabled(false);
                if (trainingStatusValue != null && "Training: running".equals(trainingStatusValue.getText())) {
                    setTrainingStatus("Training: idle", COLOR_MUTED);
                }
            }
        };
        activeTrainingWorker.execute();
    }

    private void runStableTraining(String symbol) {
        try {
            Map<String, Object> response = apiClient.trainStable(symbol);
            appendTrainingLog(ts() + " | Stable training completed");
            appendTrainingLog(Json.pretty(response));
            String runId = extractRunId(response);
            finalizeTrainingAudit("completed", runId, null, response, null);
            setTrainingStatus("Training: completed", COLOR_SUCCESS);
        } catch (Exception ex) {
            appendTrainingLog(ts() + " | Stable training failed: " + ex.getMessage());
            finalizeTrainingAudit("failed", null, null, null, ex.getMessage());
            setTrainingStatus("Training: failed", COLOR_DANGER);
        }
    }

    private void runDeepTraining(String symbol, String modelType, int epochs) {
        try {
            Map<String, Object> queueResponse = apiClient.enqueueDeepLearningJob(symbol, modelType, epochs);
            String jobId = Json.asString(queueResponse.get("id"));
            if (jobId == null || jobId.isBlank()) {
                throw new IOException("Missing job id in response: " + Json.pretty(queueResponse));
            }

            activeTrainingJobId = jobId;
            appendTrainingLog(ts() + " | Job queued: " + jobId);
            appendTrainingLog(ts() + " | Polling /api/jobs/" + jobId);
            pollDeepTrainingJob(jobId);
        } catch (Exception queueError) {
            String msg = queueError.getMessage() == null ? "" : queueError.getMessage().toLowerCase();
            if (msg.contains("404")) {
                appendTrainingLog(ts() + " | Job endpoint unavailable; using legacy deep-learning endpoint.");
                try {
                    Map<String, Object> legacy = apiClient.trainDeepLegacy(symbol, modelType, epochs);
                    appendTrainingLog(ts() + " | Legacy deep-learning training completed.");
                    appendTrainingLog(Json.pretty(legacy));
                    String runId = extractRunId(legacy);
                    finalizeTrainingAudit("completed", runId, null, legacy, null);
                    setTrainingStatus("Training: completed", COLOR_SUCCESS);
                    return;
                } catch (Exception legacyError) {
                    appendTrainingLog(ts() + " | Legacy deep-learning training failed: " + legacyError.getMessage());
                    finalizeTrainingAudit("failed", null, null, null, legacyError.getMessage());
                    setTrainingStatus("Training: failed", COLOR_DANGER);
                    return;
                }
            }
            appendTrainingLog(ts() + " | Deep-learning queue failed: " + queueError.getMessage());
            finalizeTrainingAudit("failed", null, null, null, queueError.getMessage());
            setTrainingStatus("Training: failed", COLOR_DANGER);
        }
    }

    private void pollDeepTrainingJob(String jobId) {
        String lastStatus = null;
        while (!Thread.currentThread().isInterrupted()) {
            if (activeTrainingWorker == null || activeTrainingWorker.isCancelled()) {
                appendTrainingLog(ts() + " | Polling cancelled.");
                return;
            }

            try {
                Map<String, Object> job = apiClient.fetchJob(jobId);
                String status = Json.asString(job.get("status"));
                List<String> logs = extractLogs(job.get("logs"));

                if (logs != null && deliveredLogCount < logs.size()) {
                    for (int i = deliveredLogCount; i < logs.size(); i++) {
                        appendTrainingLog(logs.get(i));
                    }
                    deliveredLogCount = logs.size();
                }

                if (status != null && !status.equalsIgnoreCase(lastStatus)) {
                    appendTrainingLog(ts() + " | Job status: " + status);
                    lastStatus = status;
                }

                if ("completed".equalsIgnoreCase(status)) {
                    appendTrainingLog(ts() + " | Deep-learning training completed.");
                    Object result = job.get("result");
                    if (result != null) {
                        appendTrainingLog(Json.pretty(result));
                    }
                    String runId = extractRunId(firstNonNull(result, job));
                    finalizeTrainingAudit("completed", runId, jobId, firstNonNull(result, job), null);
                    setTrainingStatus("Training: completed", COLOR_SUCCESS);
                    return;
                }
                if ("failed".equalsIgnoreCase(status)) {
                    appendTrainingLog(ts() + " | Deep-learning training failed.");
                    Object error = job.get("error");
                    if (error != null) {
                        appendTrainingLog("Error: " + error);
                    }
                    String runId = extractRunId(job.get("result"));
                    finalizeTrainingAudit("failed", runId, jobId, job, error == null ? null : String.valueOf(error));
                    setTrainingStatus("Training: failed", COLOR_DANGER);
                    return;
                }

                Thread.sleep(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                appendTrainingLog(ts() + " | Polling interrupted.");
                finalizeTrainingAudit("stopped", null, jobId, null, "Polling interrupted");
                setTrainingStatus("Training: stopped", COLOR_WARNING);
                return;
            } catch (Exception ex) {
                appendTrainingLog(ts() + " | Polling error: " + ex.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    appendTrainingLog(ts() + " | Polling interrupted.");
                    finalizeTrainingAudit("stopped", null, jobId, null, "Polling interrupted");
                    setTrainingStatus("Training: stopped", COLOR_WARNING);
                    return;
                }
            }
        }
    }

    private static List<String> extractLogs(Object rawLogs) {
        if (!(rawLogs instanceof List<?> logsRaw)) {
            return null;
        }
        List<String> logs = new ArrayList<>(logsRaw.size());
        for (Object item : logsRaw) {
            logs.add(String.valueOf(item));
        }
        return logs;
    }

    private void stopTraining() {
        SwingWorker<Void, String> worker = activeTrainingWorker;
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        trainButton.setEnabled(true);
        stopButton.setEnabled(false);
        setTrainingStatus("Training: stopped", COLOR_WARNING);
        appendTrainingLog(ts() + " | Stop requested.");
        finalizeTrainingAudit("stopped", null, activeTrainingJobId, null, "User requested stop");
    }

    private void initializeTrainingAudit(String symbol, String mode, String deepModel, int epochs) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("symbol", symbol);
        config.put("mode", mode);
        if (MODE_DEEP.equals(mode)) {
            config.put("deep_model", deepModel);
            config.put("epochs", epochs);
        }
        config.put("host", hostField == null ? "127.0.0.1" : hostField.getText().trim());
        config.put("port", currentPort());
        config.put("app", "dPolaris Java");

        synchronized (trainingAuditLock) {
            activeTrainingAuditConfig = config;
            activeTrainingAuditStartedAt = LocalDateTime.now().toString();
            activeTrainingJobId = null;
            activeTrainingAuditFinalized = false;
        }
    }

    private void finalizeTrainingAuditIfNeededFromStatus() {
        String statusText = trainingStatusValue == null ? "" : trainingStatusValue.getText();
        String normalized = "unknown";
        String lowered = statusText == null ? "" : statusText.toLowerCase();
        if (lowered.contains("completed")) {
            normalized = "completed";
        } else if (lowered.contains("failed")) {
            normalized = "failed";
        } else if (lowered.contains("stopped")) {
            normalized = "stopped";
        } else if (lowered.contains("running")) {
            normalized = "running";
        }
        finalizeTrainingAudit(normalized, null, activeTrainingJobId, null, null);
    }

    private void finalizeTrainingAudit(
            String status,
            String runId,
            String jobId,
            Object responsePayload,
            String error
    ) {
        Map<String, Object> configSnapshot;
        String initiatedAt;
        String resolvedJobId;
        synchronized (trainingAuditLock) {
            if (activeTrainingAuditFinalized || activeTrainingAuditConfig == null || activeTrainingAuditConfig.isEmpty()) {
                return;
            }
            activeTrainingAuditFinalized = true;
            configSnapshot = new LinkedHashMap<>(activeTrainingAuditConfig);
            initiatedAt = activeTrainingAuditStartedAt;
            resolvedJobId = firstNonBlank(jobId, activeTrainingJobId);
            activeTrainingJobId = null;
        }

        String resolvedRunId = firstNonBlank(runId, extractRunId(responsePayload));
        if (resolvedRunId.isBlank()) {
            resolvedRunId = "n/a";
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("event", "train_action");
        entry.put("user", resolveAuditUser());
        entry.put("initiated_at", firstNonBlank(initiatedAt, LocalDateTime.now().toString()));
        entry.put("completed_at", LocalDateTime.now().toString());
        entry.put("status", firstNonBlank(status, "unknown"));
        entry.put("run_id", resolvedRunId);
        entry.put("runId", resolvedRunId);
        entry.put("job_id", resolvedJobId);
        entry.put("config", configSnapshot);
        if (error != null && !error.isBlank()) {
            entry.put("error", error);
        }

        if (responsePayload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> payloadSummary = new LinkedHashMap<>();
            Map<String, Object> payload = Json.asObject(mapRaw);
            payloadSummary.put("keys", new ArrayList<>(payload.keySet()));
            payloadSummary.put("run_id", extractRunId(payload));
            payloadSummary.put("status", stringOrEmpty(findAnyValue(payload, "status")));
            payloadSummary.put("error", stringOrEmpty(findAnyValue(payload, "error", "detail")));
            entry.put("response_summary", payloadSummary);
        } else if (responsePayload instanceof String text && !text.isBlank()) {
            entry.put("response_summary", text.length() > 400 ? text.substring(0, 400) + "..." : text);
        }

        try {
            auditLogStore.append(entry);
            String logLine = ts() + " | Audit logged (" + entry.get("status") + ", runId=" + resolvedRunId + ")";
            appendTrainingLog(logLine);
            SwingUtilities.invokeLater(() -> {
                if (auditLogStatusLabel != null) {
                    styleInlineStatus(auditLogStatusLabel, "Audit: updated at " + ts(), COLOR_SUCCESS);
                }
            });
        } catch (IOException ex) {
            appendTrainingLog(ts() + " | Audit log write failed: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> {
                if (auditLogStatusLabel != null) {
                    styleInlineStatus(auditLogStatusLabel, "Audit: write failed", COLOR_DANGER);
                }
            });
        } finally {
            synchronized (trainingAuditLock) {
                activeTrainingAuditConfig = new LinkedHashMap<>();
                activeTrainingAuditStartedAt = null;
            }
        }
    }

    private String resolveAuditUser() {
        String osUser = System.getProperty("user.name");
        if (osUser != null && !osUser.isBlank()) {
            return osUser;
        }
        String envUser = System.getenv("USER");
        if (envUser != null && !envUser.isBlank()) {
            return envUser;
        }
        return "unknown";
    }

    private String extractRunId(Object payload) {
        String runId = extractRunIdRecursive(payload, 0);
        if (runId != null && !runId.isBlank()) {
            return runId;
        }
        if (payload == null) {
            return "";
        }
        String text = String.valueOf(payload);
        Matcher matcher = RUN_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractRunIdRecursive(Object payload, int depth) {
        if (payload == null || depth > 8) {
            return "";
        }
        if (payload instanceof Map<?, ?> mapRaw) {
            Map<String, Object> map = Json.asObject(mapRaw);
            String direct = firstNonBlank(
                    stringOrEmpty(findAnyValue(map, "run_id", "runId", "run-id")),
                    stringOrEmpty(map.get("run"))
            );
            if (!direct.isBlank()) {
                return direct;
            }
            for (Object value : map.values()) {
                String nested = extractRunIdRecursive(value, depth + 1);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
            return "";
        }
        if (payload instanceof List<?> list) {
            for (Object item : list) {
                String nested = extractRunIdRecursive(item, depth + 1);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
            return "";
        }
        if (payload instanceof String text) {
            Matcher matcher = RUN_ID_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        }
        return "";
    }

    private void fetchStatus() {
        configureClientFromUI();
        runDataFetch("status", () -> apiClient.fetchStatus());
    }

    private void fetchModels() {
        configureClientFromUI();
        runDataFetch("models", () -> apiClient.fetchModels());
    }

    private void fetchMemories() {
        configureClientFromUI();
        runDataFetch("memories", () -> apiClient.fetchMemories(200));
    }

    private void resetDashboardState() {
        dashboardBiasValue.setText("—");
        dashboardConfidenceValue.setText("—");
        dashboardEntryValue.setText("—");
        dashboardStopValue.setText("—");
        dashboardTargetsValue.setText("—");
        dashboardSizingValue.setText("—");
        dashboardModelValue.setText("—");
        resetDashboardLabelColors();
        setDashboardStatus("Status: idle", COLOR_MUTED);
        dashboardPredictionArea.setText("Pick a symbol, then click \"Get Prediction + Setup\".");
        dashboardPlanArea.setText("Execution checklist will appear here.");
        dashboardInsightsArea.setText("Supporting reasons, risk notes, and model context will appear here.");
    }

    private void refreshDashboard() {
        configureClientFromUI();
        String symbol = dashboardSymbolField.getText().trim().toUpperCase();
        if (symbol.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a symbol for dashboard analysis.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int horizonDays = (Integer) dashboardHorizonSpinner.getValue();

        refreshDashboardButton.setEnabled(false);
        setDashboardStatus("Status: loading...", COLOR_WARNING);
        dashboardPredictionArea.setText(ts() + " | Loading market read for " + symbol + "...");
        dashboardPlanArea.setText(ts() + " | Building execution checklist...");
        dashboardInsightsArea.setText("");
        resetDashboardLabels();

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                if (!apiClient.healthCheck(2)) {
                    throw new IOException(
                            "Backend is not reachable at " + currentHostPort()
                                    + ". Go to AI Management -> Backend Control and click Start AI Backend."
                    );
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("symbol", symbol);
                result.put("prediction", apiClient.predictSymbol(symbol));
                result.put("signal", apiClient.generateTradeSetup(symbol, horizonDays));
                return result;
            }

            @Override
            protected void done() {
                refreshDashboardButton.setEnabled(true);
                try {
                    Map<String, Object> payload = get();
                    Map<String, Object> prediction = safeObject(payload.get("prediction"));
                    Map<String, Object> signal = safeObject(payload.get("signal"));
                    renderDashboardPrediction(symbol, prediction);
                    renderDashboardSignal(signal);
                    setDashboardStatus("Updated " + ts(), COLOR_SUCCESS);
                } catch (Exception ex) {
                    String message = humanizeDashboardError(ex);
                    setDashboardStatus("Status: load failed", COLOR_DANGER);
                    dashboardPredictionArea.setText(ts() + " | Dashboard load failed: " + message);
                    dashboardPredictionArea.setCaretPosition(0);
                    dashboardPlanArea.setText(
                            "Next step:\n- Open AI Management -> Backend Control.\n"
                                    + "- Click Start AI Backend.\n"
                                    + "- Verify top status shows Connected.\n"
                                    + "- Retry Dashboard fetch."
                    );
                    dashboardInsightsArea.setText("Connection details:\n" + message);
                    resetDashboardLabels();
                }
            }
        };
        worker.execute();
    }

    private void resetPredictionInspectorState() {
        if (inspectorRegimeValue != null) {
            inspectorRegimeValue.setText("—");
            inspectorRegimeValue.setForeground(COLOR_TEXT);
        }
        if (inspectorDecisionValue != null) {
            inspectorDecisionValue.setText("—");
            inspectorDecisionValue.setForeground(COLOR_TEXT);
        }
        if (inspectorConfidenceValue != null) {
            inspectorConfidenceValue.setText("—");
            inspectorConfidenceValue.setForeground(COLOR_TEXT);
        }
        setInspectorStatus("Status: idle", COLOR_MUTED);
        if (inspectorInputArea != null) {
            inspectorInputArea.setText("Set ticker/time/run context, then click \"Inspect Prediction\".");
        }
        if (inspectorFeaturesArea != null) {
            inspectorFeaturesArea.setText("Feature vector (raw + normalized) will appear here.");
        }
        if (inspectorDecisionArea != null) {
            inspectorDecisionArea.setText("Model output, confidence, decision, and explanation will appear here.");
        }
        if (inspectorWarningsArea != null) {
            inspectorWarningsArea.setText("Warnings and fallback behavior will appear here.");
        }
        if (inspectorRawTraceArea != null) {
            inspectorRawTraceArea.setText("Full backend trace JSON will appear here.");
        }
    }

    private void setInspectorStatus(String text, Color color) {
        if (inspectorStatusLabel == null) {
            return;
        }
        styleDashboardStatus(inspectorStatusLabel, text, color);
    }

    private void refreshPredictionInspector() {
        configureClientFromUI();
        String ticker = inspectorTickerField.getText().trim().toUpperCase();
        if (ticker.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a ticker symbol.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String inspectTime = inspectorTimestampField.getText().trim();
        String runId = inspectorRunIdField.getText().trim();
        int horizon = (Integer) inspectorHorizonSpinner.getValue();

        inspectorFetchButton.setEnabled(false);
        setInspectorStatus("Status: loading...", COLOR_WARNING);
        inspectorInputArea.setText(ts() + " | Loading prediction trace for " + ticker + "...");
        inspectorFeaturesArea.setText("");
        inspectorDecisionArea.setText("");
        inspectorWarningsArea.setText("");
        inspectorRawTraceArea.setText("");

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                if (!apiClient.healthCheck(2)) {
                    throw new IOException(
                            "Backend is not reachable at " + currentHostPort()
                                    + ". Start backend from AI Management -> Backend Control."
                    );
                }
                return apiClient.inspectPrediction(ticker, inspectTime, horizon, runId);
            }

            @Override
            protected void done() {
                inspectorFetchButton.setEnabled(true);
                try {
                    Map<String, Object> payload = get();
                    renderPredictionInspectorTrace(payload);
                    setInspectorStatus("Updated " + ts(), COLOR_SUCCESS);
                } catch (Exception ex) {
                    String message = humanizeDashboardError(ex);
                    setInspectorStatus("Status: load failed", COLOR_DANGER);
                    inspectorInputArea.setText(ts() + " | Inspector load failed: " + message);
                    inspectorWarningsArea.setText("Next step:\n"
                            + "- Ensure backend is running and reachable.\n"
                            + "- Confirm /predict/inspect endpoint is available.\n"
                            + "- Retry with blank timestamp to inspect latest data.");
                    inspectorRawTraceArea.setText(String.valueOf(ex.getMessage()));
                    inspectorRegimeValue.setText("—");
                    inspectorDecisionValue.setText("—");
                    inspectorConfidenceValue.setText("—");
                }
            }
        };
        worker.execute();
    }

    private void renderPredictionInspectorTrace(Map<String, Object> trace) {
        Map<String, Object> rawInput = safeObject(trace.get("raw_input_snapshot"));
        Map<String, Object> ohlcv = safeObject(rawInput.get("ohlcv"));
        Map<String, Object> macro = safeObject(rawInput.get("macro_values"));
        Map<String, Object> sentiment = safeObject(rawInput.get("sentiment_counts"));
        Map<String, Object> featureVector = safeObject(trace.get("feature_vector"));
        Map<String, Object> rawFeatures = safeObject(featureVector.get("raw"));
        Map<String, Object> normalizedFeatures = safeObject(featureVector.get("normalized"));
        Map<String, Object> normalization = safeObject(featureVector.get("normalization"));
        Map<String, Object> regime = safeObject(trace.get("regime"));
        Map<String, Object> modelOutput = safeObject(trace.get("model_output"));
        Map<String, Object> decision = safeObject(trace.get("decision"));
        Map<String, Object> explanation = safeObject(trace.get("explanation"));

        String regimeLabel = firstNonBlank(stringOrEmpty(regime.get("label")), stringOrEmpty(trace.get("regime_label")));
        String decisionAction = firstNonBlank(
                stringOrEmpty(decision.get("action")),
                firstNonBlank(stringOrEmpty(decision.get("outcome")), stringOrEmpty(modelOutput.get("prediction_label")))
        );
        double confidence = Json.asDouble(modelOutput.get("confidence"), Double.NaN);
        if (Double.isNaN(confidence)) {
            confidence = Json.asDouble(decision.get("confidence"), Double.NaN);
        }
        if (Double.isNaN(confidence)) {
            confidence = Json.asDouble(modelOutput.get("probability_up"), Double.NaN);
        }

        inspectorRegimeValue.setText(stringOrDash(regimeLabel));
        inspectorDecisionValue.setText(stringOrDash(decisionAction));
        inspectorConfidenceValue.setText(asPercent(confidence));
        inspectorRegimeValue.setForeground(COLOR_TEXT);
        inspectorDecisionValue.setForeground(colorForBias(decisionAction));
        inspectorConfidenceValue.setForeground(colorForConfidence(confidence));

        StringBuilder rawInputText = new StringBuilder();
        rawInputText.append("Ticker: ").append(stringOrDash(trace.get("ticker"))).append("\n");
        rawInputText.append("Requested time: ").append(stringOrDash(trace.get("requested_time"))).append("\n");
        rawInputText.append("Resolved time: ").append(stringOrDash(trace.get("resolved_time"))).append("\n");
        rawInputText.append("Horizon: ").append(stringOrDash(trace.get("horizon"))).append("\n");
        rawInputText.append("Run ID: ").append(stringOrDash(trace.get("run_id"))).append("\n\n");
        rawInputText.append(formatMapBlock("OHLCV", ohlcv, 20)).append("\n\n");
        rawInputText.append(formatMapBlock("Macro Values", macro, 25)).append("\n\n");
        rawInputText.append(formatMapBlock("Sentiment Counts", sentiment, 25));
        inspectorInputArea.setText(rawInputText.toString());
        inspectorInputArea.setCaretPosition(0);

        StringBuilder featureText = new StringBuilder();
        featureText.append(formatMapBlock("Normalization", normalization, 20)).append("\n\n");
        featureText.append(formatMapBlock("Raw Features", rawFeatures, 180)).append("\n\n");
        featureText.append(formatMapBlock("Normalized Features", normalizedFeatures, 180));
        inspectorFeaturesArea.setText(featureText.toString());
        inspectorFeaturesArea.setCaretPosition(0);

        List<Object> topFeatures = safeArray(explanation.get("top_features"));
        List<Object> notes = safeArray(explanation.get("notes"));
        StringBuilder decisionText = new StringBuilder();
        decisionText.append(formatMapBlock("Model Output", modelOutput, 40)).append("\n\n");
        decisionText.append(formatMapBlock("Decision", decision, 40)).append("\n\n");
        decisionText.append("Top contributing features:\n");
        if (topFeatures.isEmpty()) {
            decisionText.append("- None\n");
        } else {
            for (Object item : topFeatures) {
                decisionText.append("- ").append(String.valueOf(item)).append("\n");
            }
        }
        if (!notes.isEmpty()) {
            decisionText.append("\nExplanation notes:\n");
            for (Object note : notes) {
                decisionText.append("- ").append(String.valueOf(note)).append("\n");
            }
        }
        inspectorDecisionArea.setText(decisionText.toString());
        inspectorDecisionArea.setCaretPosition(0);

        List<Object> warnings = safeArray(trace.get("warnings"));
        StringBuilder warningsText = new StringBuilder();
        if (warnings.isEmpty()) {
            warningsText.append("No warnings reported.");
        } else {
            warningsText.append("Warnings:\n");
            for (Object warning : warnings) {
                warningsText.append("- ").append(String.valueOf(warning)).append("\n");
            }
        }
        inspectorWarningsArea.setText(warningsText.toString());
        inspectorWarningsArea.setCaretPosition(0);

        inspectorRawTraceArea.setText(Json.pretty(trace));
        inspectorRawTraceArea.setCaretPosition(0);
    }

    private String formatMapBlock(String title, Map<String, Object> payload, int maxEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(":\n");
        if (payload == null || payload.isEmpty()) {
            sb.append("- none");
            return sb.toString();
        }
        int shown = 0;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (shown >= maxEntries) {
                sb.append("- ... (").append(payload.size() - shown).append(" more)\n");
                break;
            }
            sb.append("- ").append(entry.getKey()).append(": ").append(renderMapValue(entry.getValue())).append("\n");
            shown++;
        }
        return sb.toString().trim();
    }

    private String renderMapValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number number) {
            double asDouble = number.doubleValue();
            if (Double.isFinite(asDouble)) {
                if (Math.abs(asDouble) >= 1000.0 || Math.abs(asDouble) < 0.0001) {
                    return String.format("%.6e", asDouble);
                }
                return String.format("%.6f", asDouble);
            }
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            String text = Json.pretty(value);
            return text.length() > 180 ? text.substring(0, 180) + "..." : text;
        }
        return String.valueOf(value);
    }

    private String currentHostPort() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }
        String port = portField.getText().trim();
        if (port.isEmpty()) {
            port = "8420";
        }
        return host + ":" + port;
    }

    private String humanizeDashboardError(Exception ex) {
        Throwable cause = ex;
        if (ex.getCause() != null) {
            cause = ex.getCause();
        }

        String className = cause.getClass().getSimpleName();
        String message = cause.getMessage() == null ? "" : cause.getMessage().trim();

        String full = (className + " " + message).toLowerCase();
        if (full.contains("connectexception")
                || full.contains("connection refused")
                || full.contains("failed to connect")
                || full.contains("backend is not reachable")) {
            return "Cannot connect to backend at " + currentHostPort()
                    + ". Start backend from AI Management -> Backend Control.";
        }

        if (message.isBlank()) {
            return className;
        }
        return message;
    }

    private void renderDashboardPrediction(String symbol, Map<String, Object> prediction) {
        String predictionLabel = stringOrDash(prediction.get("prediction"));
        double confidence = Json.asDouble(prediction.get("confidence"), Double.NaN);
        double probabilityUp = Json.asDouble(prediction.get("probability_up"), Double.NaN);
        double probabilityDown = Json.asDouble(prediction.get("probability_down"), Double.NaN);
        String modelType = stringOrDash(prediction.get("model_type"));
        String modelAccuracy = asPercent(prediction.get("model_accuracy"));
        String confidenceBand = confidenceBand(confidence);

        StringBuilder sb = new StringBuilder();
        sb.append("SYMBOL ").append(symbol).append("\n\n");
        sb.append("Directional bias: ").append(predictionLabel).append("\n");
        sb.append("Confidence: ").append(asPercent(confidence))
                .append(" (").append(confidenceBand).append(")\n");
        sb.append("Upside probability: ").append(asPercent(probabilityUp)).append("\n");
        sb.append("Downside probability: ").append(asPercent(probabilityDown)).append("\n");
        sb.append("Model: ").append(modelType).append("\n");
        if (!"—".equals(modelAccuracy)) {
            sb.append("Model backtest accuracy: ").append(modelAccuracy).append("\n");
        }
        if (prediction.containsKey("source")) {
            sb.append("Source: ").append(stringOrDash(prediction.get("source"))).append("\n");
        }

        sb.append("\nQuick read:\n");
        if (!Double.isNaN(confidence) && confidence < 0.55) {
            sb.append("- Edge is weak. Treat this as low-conviction and reduce size.\n");
        } else if (!Double.isNaN(confidence) && confidence >= 0.70) {
            sb.append("- Edge is strong. Setup can support normal risk if execution is clean.\n");
        } else {
            sb.append("- Edge is moderate. Favor disciplined entries and strict stop placement.\n");
        }
        if (!Double.isNaN(probabilityUp) && !Double.isNaN(probabilityDown)) {
            sb.append("- Probability spread: ")
                    .append(String.format("%.1f%%", Math.abs((probabilityUp - probabilityDown) * 100.0)))
                    .append(".\n");
        }

        dashboardPredictionArea.setText(sb.toString());
        dashboardPredictionArea.setCaretPosition(0);
    }

    private void renderDashboardSignal(Map<String, Object> signal) {
        String bias = stringOrDash(signal.get("bias"));
        String confidence = asPercent(signal.get("confidence"));
        Map<String, Object> entry = safeObject(signal.get("entry"));
        Map<String, Object> risk = safeObject(signal.get("risk"));
        List<Object> targets = safeArray(signal.get("targets"));
        List<Object> reasons = safeArray(signal.get("reasons"));
        List<Object> riskFlags = safeArray(signal.get("risk_flags"));
        List<Object> insights = safeArray(signal.get("insights"));
        Map<String, Object> model = safeObject(signal.get("model"));

        dashboardBiasValue.setText(bias);
        dashboardConfidenceValue.setText(confidence);
        dashboardEntryValue.setText(formatEntrySummary(entry));
        dashboardStopValue.setText(asPrice(risk.get("stop_loss")));
        dashboardTargetsValue.setText(formatTargetsSummary(targets));
        dashboardSizingValue.setText(formatSizingSummary(risk));
        dashboardModelValue.setText(formatModelSummary(model));
        dashboardBiasValue.setForeground(colorForBias(bias));
        dashboardConfidenceValue.setForeground(colorForConfidence(Json.asDouble(signal.get("confidence"), Double.NaN)));
        dashboardStopValue.setForeground(COLOR_DANGER);
        dashboardTargetsValue.setForeground(COLOR_SUCCESS);

        StringBuilder plan = new StringBuilder();
        plan.append("1) Entry\n");
        plan.append("- Zone: ").append(asPrice(entry.get("zone_low")))
                .append(" to ").append(asPrice(entry.get("zone_high"))).append("\n");
        plan.append("- Trigger: ").append(asPrice(entry.get("trigger"))).append("\n");
        plan.append("- Condition: ").append(stringOrDash(entry.get("condition"))).append("\n\n");

        plan.append("2) Risk\n");
        plan.append("- Stop loss: ").append(asPrice(risk.get("stop_loss"))).append("\n");
        plan.append("- Invalidation: ").append(stringOrDash(risk.get("invalidation"))).append("\n");
        plan.append("- Suggested size: ").append(formatSizingSummary(risk)).append("\n\n");

        plan.append("3) Targets\n");
        if (targets.isEmpty()) {
            plan.append("- No targets returned by backend.\n");
        } else {
            for (Object targetObj : targets) {
                Map<String, Object> target = safeObject(targetObj);
                plan.append("- ").append(stringOrDash(target.get("label"))).append(": ")
                        .append(asPrice(target.get("price")));
                if (target.get("r_multiple") != null) {
                    plan.append(" (").append(target.get("r_multiple")).append("R)");
                }
                plan.append("\n");
            }
        }
        plan.append("\n4) Trade envelope\n");
        plan.append("- Setup type: ").append(stringOrDash(signal.get("setup_type"))).append("\n");
        plan.append("- Time horizon: ").append(stringOrDash(signal.get("time_horizon_days"))).append(" days\n");

        dashboardPlanArea.setText(plan.toString());
        dashboardPlanArea.setCaretPosition(0);

        StringBuilder insightsBlock = new StringBuilder();
        if (!reasons.isEmpty()) {
            insightsBlock.append("Why this setup\n");
            for (Object reason : reasons) {
                insightsBlock.append("- ").append(String.valueOf(reason)).append("\n");
            }
            insightsBlock.append("\n");
        }

        if (!insights.isEmpty()) {
            insightsBlock.append("Supporting signals\n");
            for (Object insightObj : insights) {
                Map<String, Object> insight = safeObject(insightObj);
                insightsBlock.append("- ").append(stringOrDash(insight.get("title")))
                        .append(": ").append(stringOrDash(insight.get("detail"))).append("\n");
            }
            insightsBlock.append("\n");
        }

        if (!riskFlags.isEmpty()) {
            insightsBlock.append("Risk notes\n");
            for (Object flag : riskFlags) {
                insightsBlock.append("- ").append(String.valueOf(flag)).append("\n");
            }
        }

        if (insightsBlock.isEmpty()) {
            insightsBlock.append("No extra narrative returned by backend yet.");
        }
        dashboardInsightsArea.setText(insightsBlock.toString());
        dashboardInsightsArea.setCaretPosition(0);
    }

    private String formatEntrySummary(Map<String, Object> entry) {
        return asPrice(entry.get("zone_low")) + " - " + asPrice(entry.get("zone_high"))
                + " (trigger " + asPrice(entry.get("trigger")) + ")";
    }

    private String formatTargetsSummary(List<Object> targets) {
        if (targets.isEmpty()) {
            return "—";
        }
        List<String> parts = new ArrayList<>();
        for (Object targetObj : targets) {
            Map<String, Object> target = safeObject(targetObj);
            String label = stringOrDash(target.get("label"));
            String price = asPrice(target.get("price"));
            String rMultiple = target.get("r_multiple") == null ? "" : " (" + target.get("r_multiple") + "R)";
            parts.add(label + " " + price + rMultiple);
        }
        return String.join(" | ", parts);
    }

    private String formatSizingSummary(Map<String, Object> risk) {
        String shares = stringOrDash(risk.get("suggested_shares"));
        String positionPercent = risk.get("suggested_position_percent") == null
                ? "—"
                : String.format("%.2f%%", Json.asDouble(risk.get("suggested_position_percent"), 0.0));
        String notional = asDollars(risk.get("suggested_notional"));
        return shares + " shares, " + positionPercent + ", " + notional;
    }

    private String formatModelSummary(Map<String, Object> model) {
        if (model.isEmpty()) {
            return "—";
        }
        String source = stringOrDash(model.get("source"));
        String type = stringOrDash(model.get("type"));
        String accuracy = asPercent(model.get("accuracy"));
        if ("—".equals(accuracy)) {
            return source + " / " + type;
        }
        return source + " / " + type + " (" + accuracy + ")";
    }

    private void resetDashboardLabels() {
        dashboardBiasValue.setText("—");
        dashboardConfidenceValue.setText("—");
        dashboardEntryValue.setText("—");
        dashboardStopValue.setText("—");
        dashboardTargetsValue.setText("—");
        dashboardSizingValue.setText("—");
        dashboardModelValue.setText("—");
        resetDashboardLabelColors();
    }

    private void resetDashboardLabelColors() {
        dashboardBiasValue.setForeground(COLOR_TEXT);
        dashboardConfidenceValue.setForeground(COLOR_TEXT);
        dashboardEntryValue.setForeground(COLOR_TEXT);
        dashboardStopValue.setForeground(COLOR_TEXT);
        dashboardTargetsValue.setForeground(COLOR_TEXT);
        dashboardSizingValue.setForeground(COLOR_TEXT);
        dashboardModelValue.setForeground(COLOR_TEXT);
    }

    private void setDashboardStatus(String text, Color color) {
        if (dashboardUpdatedLabel == null) {
            return;
        }
        styleDashboardStatus(dashboardUpdatedLabel, text, color);
    }

    private void setDaemonStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (daemonStatusValue != null) {
                styleInlineStatus(daemonStatusValue, text, color);
            }
        });
    }

    private void setTrainingStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (trainingStatusValue != null) {
                styleInlineStatus(trainingStatusValue, text, color);
            }
        });
    }

    private void setTrainingGuardrailLoading(String runId) {
        SwingUtilities.invokeLater(() -> {
            if (trainingGuardrailSummaryLabel == null) {
                return;
            }
            String suffix = (runId == null || runId.isBlank()) ? "" : " (" + shortenRunId(runId) + ")";
            styleInlineStatus(trainingGuardrailSummaryLabel, "Guardrails: loading" + suffix, COLOR_WARNING);
            styleInlineStatus(trainingGuardrailLeakageLabel, "Leakage: ...", COLOR_WARNING);
            styleInlineStatus(trainingGuardrailCostsLabel, "Costs/slippage: ...", COLOR_WARNING);
            styleInlineStatus(trainingGuardrailWalkForwardLabel, "Walk-forward: ...", COLOR_WARNING);
            styleInlineStatus(trainingGuardrailQualityLabel, "Quality gates: ...", COLOR_WARNING);
        });
    }

    private void setTrainingGuardrailIdle() {
        SwingUtilities.invokeLater(() -> {
            if (trainingGuardrailSummaryLabel == null) {
                return;
            }
            styleInlineStatus(
                    trainingGuardrailSummaryLabel,
                    "Guardrails: load a run in Training Runs",
                    COLOR_MUTED
            );
            styleInlineStatus(trainingGuardrailLeakageLabel, "Leakage: not evaluated", COLOR_MUTED);
            styleInlineStatus(trainingGuardrailCostsLabel, "Costs/slippage: not evaluated", COLOR_MUTED);
            styleInlineStatus(trainingGuardrailWalkForwardLabel, "Walk-forward: not evaluated", COLOR_MUTED);
            styleInlineStatus(trainingGuardrailQualityLabel, "Quality gates: not evaluated", COLOR_MUTED);
        });
    }

    private void updateTrainingGuardrailBanners(GuardrailEngine.GuardrailReport report, String runId) {
        SwingUtilities.invokeLater(() -> {
            if (trainingGuardrailSummaryLabel == null) {
                return;
            }
            if (report == null) {
                setTrainingGuardrailIdle();
                return;
            }

            String suffix = (runId == null || runId.isBlank()) ? "" : " (" + shortenRunId(runId) + ")";
            boolean hasCosts = report.checklist().getOrDefault("backtest_costs", false);
            boolean walkForward = report.checklist().getOrDefault("walk_forward", false);
            boolean qualityGates = report.checklist().getOrDefault("quality_gates", false);
            boolean leakageChecks = report.checklist().getOrDefault("leakage_checks", false);
            boolean leakageFailed = report.leakageFailed();

            if (report.checklistGreen()) {
                styleInlineStatus(trainingGuardrailSummaryLabel, "Guardrails: GREEN" + suffix, COLOR_SUCCESS);
            } else if (report.hasBlocker() || leakageFailed) {
                styleInlineStatus(trainingGuardrailSummaryLabel, "Guardrails: BLOCKED" + suffix, COLOR_DANGER);
            } else {
                styleInlineStatus(trainingGuardrailSummaryLabel, "Guardrails: ACTION REQUIRED" + suffix, COLOR_WARNING);
            }

            styleInlineStatus(
                    trainingGuardrailLeakageLabel,
                    leakageFailed ? "Leakage: FAILED (blocked)" : (leakageChecks ? "Leakage: PASS" : "Leakage: UNKNOWN"),
                    leakageFailed ? COLOR_DANGER : (leakageChecks ? COLOR_SUCCESS : COLOR_WARNING)
            );
            styleInlineStatus(
                    trainingGuardrailCostsLabel,
                    hasCosts ? "Costs/slippage: PASS" : "Costs/slippage: FAIL",
                    hasCosts ? COLOR_SUCCESS : COLOR_DANGER
            );
            styleInlineStatus(
                    trainingGuardrailWalkForwardLabel,
                    walkForward ? "Walk-forward: PASS" : "Walk-forward: FAIL",
                    walkForward ? COLOR_SUCCESS : COLOR_DANGER
            );
            styleInlineStatus(
                    trainingGuardrailQualityLabel,
                    qualityGates ? "Quality gates: PASS" : "Quality gates: FAIL",
                    qualityGates ? COLOR_SUCCESS : COLOR_DANGER
            );
        });
    }

    private void setDataStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (dataStatusValue != null) {
                styleInlineStatus(dataStatusValue, text, color);
            }
        });
    }

    private Color colorForBias(String bias) {
        String normalized = bias.toLowerCase();
        if (normalized.contains("bull") || normalized.contains("long") || normalized.contains("up")) {
            return COLOR_SUCCESS;
        }
        if (normalized.contains("bear") || normalized.contains("short") || normalized.contains("down")) {
            return COLOR_DANGER;
        }
        return COLOR_WARNING;
    }

    private Color colorForConfidence(double confidence) {
        if (Double.isNaN(confidence)) {
            return COLOR_TEXT;
        }
        if (confidence <= 1.0) {
            confidence *= 100.0;
        }
        if (confidence >= 70.0) {
            return COLOR_SUCCESS;
        }
        if (confidence >= 55.0) {
            return COLOR_WARNING;
        }
        return COLOR_DANGER;
    }

    private String confidenceBand(double confidence) {
        if (Double.isNaN(confidence)) {
            return "unknown";
        }
        if (confidence <= 1.0) {
            confidence *= 100.0;
        }
        if (confidence >= 70.0) {
            return "high";
        }
        if (confidence >= 55.0) {
            return "moderate";
        }
        return "low";
    }

    private Map<String, Object> safeObject(Object value) {
        if (value instanceof Map<?, ?>) {
            return Json.asObject(value);
        }
        return new LinkedHashMap<>();
    }

    private List<Object> safeArray(Object value) {
        if (value instanceof List<?>) {
            return Json.asArray(value);
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> asMapList(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return out;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add(Json.asObject(map));
            }
        }
        return out;
    }

    private String humanizeError(Exception ex) {
        Throwable cause = ex;
        if (ex.getCause() != null) {
            cause = ex.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }

    private String stringOrDash(Object value) {
        String text = Json.asString(value);
        if (text == null || text.isBlank()) {
            return "—";
        }
        return text;
    }

    private String asPercent(Object value) {
        if (value == null) {
            return "—";
        }
        double number = Json.asDouble(value, Double.NaN);
        if (Double.isNaN(number)) {
            return "—";
        }
        if (number <= 1.0) {
            number *= 100.0;
        }
        return String.format("%.1f%%", number);
    }

    private String asPrice(Object value) {
        if (value == null) {
            return "—";
        }
        double number = Json.asDouble(value, Double.NaN);
        if (Double.isNaN(number)) {
            return "—";
        }
        return String.format("%.2f", number);
    }

    private String asDollars(Object value) {
        if (value == null) {
            return "—";
        }
        double number = Json.asDouble(value, Double.NaN);
        if (Double.isNaN(number)) {
            return "—";
        }
        return String.format("$%,.2f", number);
    }

    private void runDataFetch(String type, FetchTask task) {
        setDataStatus("Data: loading " + type + "...", COLOR_WARNING);
        dataArea.setText(ts() + " | Loading " + type + "...");
        if (!"memories".equals(type)) {
            memoryDistributionArea.setText("");
        }

        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                return task.fetch();
            }

            @Override
            protected void done() {
                try {
                    Object response = get();
                    dataArea.setText(Json.pretty(response));
                    dataArea.setCaretPosition(0);
                    if ("memories".equals(type)) {
                        memoryDistributionArea.setText(buildMemoryDistribution(response));
                        memoryDistributionArea.setCaretPosition(0);
                    }
                    setDataStatus("Data: loaded " + type + " at " + ts(), COLOR_SUCCESS);
                } catch (Exception ex) {
                    dataArea.setText(ts() + " | Failed to load " + type + ": " + ex.getMessage());
                    if ("memories".equals(type)) {
                        memoryDistributionArea.setText("");
                    }
                    setDataStatus("Data: failed to load " + type, COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private String buildMemoryDistribution(Object response) {
        List<Object> memories;
        if (response instanceof List<?>) {
            memories = Json.asArray(response);
        } else if (response instanceof Map<?, ?>) {
            Map<String, Object> map = Json.asObject(response);
            Object nested = map.get("memories");
            if (nested instanceof List<?>) {
                memories = Json.asArray(nested);
            } else {
                return "No memories array found in response.";
            }
        } else {
            return "Unexpected memory payload type.";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Object memoryObj : memories) {
            if (!(memoryObj instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> memory = Json.asObject(memoryObj);
            String category = Json.asString(memory.get("category"));
            if (category == null || category.isBlank()) {
                category = "(uncategorized)";
            }
            counts.put(category, counts.getOrDefault(category, 0) + 1);
        }

        if (counts.isEmpty()) {
            return "No memory categories found.";
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed());

        StringBuilder sb = new StringBuilder();
        sb.append("Total memories: ").append(memories.size()).append("\n\n");
        for (Map.Entry<String, Integer> entry : sorted) {
            sb.append(String.format("%-24s %5d%n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    private record SeriesPoint(String label, double value) {
    }

    private record TradeLogRow(
            String ticker,
            String entryTime,
            String exitTime,
            String size,
            String entryPrice,
            String exitPrice,
            String pnl,
            String mae,
            String mfe,
            String reason,
            String regime,
            String confidence,
            Map<String, Object> raw
    ) {
    }

    private static final class TradeLogTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Ticker",
                "Entry Time",
                "Exit Time",
                "Size",
                "Entry Px",
                "Exit Px",
                "PnL",
                "MAE",
                "MFE",
                "Reason",
                "Regime",
                "Confidence"
        };
        private List<TradeLogRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            TradeLogRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.ticker();
                case 1 -> row.entryTime();
                case 2 -> row.exitTime();
                case 3 -> row.size();
                case 4 -> row.entryPrice();
                case 5 -> row.exitPrice();
                case 6 -> row.pnl();
                case 7 -> row.mae();
                case 8 -> row.mfe();
                case 9 -> row.reason();
                case 10 -> row.regime();
                case 11 -> row.confidence();
                default -> "";
            };
        }

        void setRows(List<TradeLogRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        TradeLogRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }

        List<TradeLogRow> rows() {
            return new ArrayList<>(rows);
        }
    }

    private record AttributionRow(
            String bucket,
            String trades,
            String pnl,
            String winRate,
            String sharpe,
            String avgReturn,
            Map<String, Object> raw
    ) {
    }

    private static final class AttributionTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Bucket",
                "Trades",
                "PnL",
                "Win Rate",
                "Sharpe",
                "Avg Return"
        };
        private List<AttributionRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            AttributionRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.bucket();
                case 1 -> row.trades();
                case 2 -> row.pnl();
                case 3 -> row.winRate();
                case 4 -> row.sharpe();
                case 5 -> row.avgReturn();
                default -> "";
            };
        }

        void setRows(List<AttributionRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }
    }

    private static final class LineChartPanel extends JPanel {
        private final String title;
        private final Color lineColor;
        private List<SeriesPoint> series = new ArrayList<>();
        private boolean percentAxis;

        LineChartPanel(String title, Color lineColor, boolean percentAxis) {
            this.title = title;
            this.lineColor = lineColor;
            this.percentAxis = percentAxis;
            setOpaque(true);
            setBackground(COLOR_LOG_BG);
            setBorder(new CompoundBorder(
                    new LineBorder(COLOR_BORDER, 1, true),
                    new EmptyBorder(6, 6, 6, 6)
            ));
            setPreferredSize(new Dimension(420, 220));
        }

        void setSeries(List<SeriesPoint> series, boolean percentAxis) {
            this.series = series == null ? new ArrayList<>() : new ArrayList<>(series);
            this.percentAxis = percentAxis;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int left = 56;
                int right = 18;
                int top = 18;
                int bottom = 34;
                int chartW = Math.max(10, w - left - right);
                int chartH = Math.max(10, h - top - bottom);

                g2.setColor(COLOR_MUTED);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                g2.drawString(title, left, 14);

                if (series == null || series.isEmpty()) {
                    g2.setColor(COLOR_MUTED);
                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                    g2.drawString("No data", left + 8, top + chartH / 2);
                    return;
                }

                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (SeriesPoint point : series) {
                    double v = point.value();
                    if (Double.isNaN(v) || Double.isInfinite(v)) {
                        continue;
                    }
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
                if (!Double.isFinite(min) || !Double.isFinite(max)) {
                    g2.setColor(COLOR_MUTED);
                    g2.drawString("No numeric data", left + 8, top + chartH / 2);
                    return;
                }
                if (Math.abs(max - min) < 1e-12) {
                    max += 1.0;
                    min -= 1.0;
                }

                g2.setColor(COLOR_BORDER);
                for (int i = 0; i <= 4; i++) {
                    int y = top + (chartH * i / 4);
                    g2.drawLine(left, y, left + chartW, y);
                    double yValue = max - ((max - min) * i / 4.0);
                    String yLabel = formatAxisValue(yValue, percentAxis);
                    g2.setColor(COLOR_MUTED);
                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(yLabel, Math.max(2, left - fm.stringWidth(yLabel) - 6), y + 4);
                    g2.setColor(COLOR_BORDER);
                }
                g2.drawLine(left, top, left, top + chartH);
                g2.drawLine(left, top + chartH, left + chartW, top + chartH);

                int n = series.size();
                int prevX = -1;
                int prevY = -1;
                g2.setColor(lineColor);
                g2.setStroke(new BasicStroke(2.0f));
                for (int i = 0; i < n; i++) {
                    double v = series.get(i).value();
                    int x = n <= 1 ? left : left + (int) Math.round((i / (double) (n - 1)) * chartW);
                    int y = top + (int) Math.round((max - v) / (max - min) * chartH);
                    if (prevX >= 0) {
                        g2.drawLine(prevX, prevY, x, y);
                    }
                    prevX = x;
                    prevY = y;
                }

                g2.setColor(COLOR_MUTED);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                String leftLabel = series.get(0).label();
                String rightLabel = series.get(series.size() - 1).label();
                if (leftLabel != null && !leftLabel.isBlank()) {
                    g2.drawString(leftLabel, left, top + chartH + 16);
                }
                if (rightLabel != null && !rightLabel.isBlank()) {
                    FontMetrics fm = g2.getFontMetrics();
                    int textW = fm.stringWidth(rightLabel);
                    g2.drawString(rightLabel, left + chartW - textW, top + chartH + 16);
                }
            } finally {
                g2.dispose();
            }
        }

        private String formatAxisValue(double value, boolean percentAxis) {
            if (percentAxis) {
                double v = value;
                if (Math.abs(v) <= 1.5) {
                    v *= 100.0;
                }
                return String.format("%.1f%%", v);
            }
            return String.format("%.2f", value);
        }
    }

    private record RunBundle(
            String runId,
            Map<String, Object> runPayload,
            Map<String, String> artifactIndex,
            Map<String, Object> artifactPayloads
    ) {
    }

    private record FeatureDiffSummary(
            String text,
            int addedCount,
            int removedCount,
            int changedCount
    ) {
    }

    private record BacktestCompare(
            List<SeriesPoint> equityCurve,
            int tradeCount,
            Map<String, Object> summary,
            Object assumptions
    ) {
    }

    private record RunComparisonModel(
            RunsTableRow left,
            RunsTableRow right,
            String configDiff,
            String dataDiff,
            String featureDiff,
            String metricsDiff,
            String backtestDiff,
            String driftDiff,
            List<SeriesPoint> equityA,
            List<SeriesPoint> equityB,
            String explanation
    ) {
    }

    private static final class CompareLineChartPanel extends JPanel {
        private final String title;
        private final Color leftColor;
        private final Color rightColor;
        private List<SeriesPoint> leftSeries = new ArrayList<>();
        private List<SeriesPoint> rightSeries = new ArrayList<>();
        private String leftLabel = "Run A";
        private String rightLabel = "Run B";

        CompareLineChartPanel(String title, Color leftColor, Color rightColor) {
            this.title = title;
            this.leftColor = leftColor;
            this.rightColor = rightColor;
            setOpaque(true);
            setBackground(COLOR_LOG_BG);
            setBorder(new CompoundBorder(
                    new LineBorder(COLOR_BORDER, 1, true),
                    new EmptyBorder(6, 6, 6, 6)
            ));
            setPreferredSize(new Dimension(420, 320));
        }

        void setSeries(
                List<SeriesPoint> leftSeries,
                List<SeriesPoint> rightSeries,
                String leftLabel,
                String rightLabel
        ) {
            this.leftSeries = leftSeries == null ? new ArrayList<>() : new ArrayList<>(leftSeries);
            this.rightSeries = rightSeries == null ? new ArrayList<>() : new ArrayList<>(rightSeries);
            if (leftLabel != null && !leftLabel.isBlank()) {
                this.leftLabel = leftLabel;
            }
            if (rightLabel != null && !rightLabel.isBlank()) {
                this.rightLabel = rightLabel;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int left = 56;
                int right = 24;
                int top = 24;
                int bottom = 36;
                int chartW = Math.max(10, w - left - right);
                int chartH = Math.max(10, h - top - bottom);

                g2.setColor(COLOR_MUTED);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                g2.drawString(title, left, 14);
                drawLegend(g2, w);

                if (leftSeries.isEmpty() && rightSeries.isEmpty()) {
                    g2.setColor(COLOR_MUTED);
                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                    g2.drawString("No data for overlay", left + 8, top + chartH / 2);
                    return;
                }

                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                min = updateMin(min, leftSeries);
                max = updateMax(max, leftSeries);
                min = updateMin(min, rightSeries);
                max = updateMax(max, rightSeries);

                if (!Double.isFinite(min) || !Double.isFinite(max)) {
                    g2.setColor(COLOR_MUTED);
                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                    g2.drawString("No numeric values", left + 8, top + chartH / 2);
                    return;
                }
                if (Math.abs(max - min) < 1e-12) {
                    max += 1.0;
                    min -= 1.0;
                }

                g2.setColor(COLOR_BORDER);
                for (int i = 0; i <= 4; i++) {
                    int y = top + (chartH * i / 4);
                    g2.drawLine(left, y, left + chartW, y);
                    double yValue = max - ((max - min) * i / 4.0);
                    String yLabel = String.format("%.2f", yValue);
                    g2.setColor(COLOR_MUTED);
                    g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(yLabel, Math.max(2, left - fm.stringWidth(yLabel) - 6), y + 4);
                    g2.setColor(COLOR_BORDER);
                }
                g2.drawLine(left, top, left, top + chartH);
                g2.drawLine(left, top + chartH, left + chartW, top + chartH);

                drawSeries(g2, leftSeries, leftColor, left, top, chartW, chartH, min, max);
                drawSeries(g2, rightSeries, rightColor, left, top, chartW, chartH, min, max);

                String leftX = !leftSeries.isEmpty() ? leftSeries.get(0).label() : "";
                String rightX = !leftSeries.isEmpty()
                        ? leftSeries.get(leftSeries.size() - 1).label()
                        : (!rightSeries.isEmpty() ? rightSeries.get(rightSeries.size() - 1).label() : "");
                g2.setColor(COLOR_MUTED);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                if (leftX != null && !leftX.isBlank()) {
                    g2.drawString(leftX, left, top + chartH + 16);
                }
                if (rightX != null && !rightX.isBlank()) {
                    FontMetrics fm = g2.getFontMetrics();
                    int textW = fm.stringWidth(rightX);
                    g2.drawString(rightX, left + chartW - textW, top + chartH + 16);
                }
            } finally {
                g2.dispose();
            }
        }

        private void drawLegend(Graphics2D g2, int width) {
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
            FontMetrics fm = g2.getFontMetrics();
            int rightX = Math.max(240, width - 360);
            int y = 14;

            g2.setColor(leftColor);
            g2.drawLine(rightX, y - 4, rightX + 18, y - 4);
            g2.setColor(COLOR_MUTED);
            g2.drawString(leftLabel, rightX + 22, y);

            int secondX = rightX + 22 + fm.stringWidth(leftLabel) + 26;
            g2.setColor(rightColor);
            g2.drawLine(secondX, y - 4, secondX + 18, y - 4);
            g2.setColor(COLOR_MUTED);
            g2.drawString(rightLabel, secondX + 22, y);
        }

        private double updateMin(double current, List<SeriesPoint> points) {
            double min = current;
            for (SeriesPoint point : points) {
                double v = point.value();
                if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                    min = Math.min(min, v);
                }
            }
            return min;
        }

        private double updateMax(double current, List<SeriesPoint> points) {
            double max = current;
            for (SeriesPoint point : points) {
                double v = point.value();
                if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                    max = Math.max(max, v);
                }
            }
            return max;
        }

        private void drawSeries(
                Graphics2D g2,
                List<SeriesPoint> series,
                Color color,
                int left,
                int top,
                int chartW,
                int chartH,
                double min,
                double max
        ) {
            if (series == null || series.isEmpty()) {
                return;
            }
            int n = series.size();
            int prevX = -1;
            int prevY = -1;
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f));
            for (int i = 0; i < n; i++) {
                double v = series.get(i).value();
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    continue;
                }
                int x = n <= 1 ? left : left + (int) Math.round((i / (double) (n - 1)) * chartW);
                int y = top + (int) Math.round((max - v) / (max - min) * chartH);
                if (prevX >= 0) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
            }
        }
    }

    private static final class UniverseTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Select",
                "Ticker",
                "Name",
                "Sector",
                "Market Cap",
                "Avg Vol (7d)",
                "1D Change",
                "Analysis Date",
                "Mentions"
        };
        private List<UniverseRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 0 || rowIndex < 0 || rowIndex >= rows.size()) {
                return;
            }
            UniverseRow row = rows.get(rowIndex);
            row.setSelected(Boolean.TRUE.equals(aValue));
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            UniverseRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.selected();
                case 1 -> row.ticker();
                case 2 -> row.name();
                case 3 -> row.sector();
                case 4 -> formatMarketCap(row.marketCap());
                case 5 -> formatVolume(row.avgVolume7d());
                case 6 -> formatChangePercent(row.changePct1d());
                case 7 -> formatAnalysisDate(row.analysisDate());
                case 8 -> row.mentionCount() == null ? "—" : String.valueOf(row.mentionCount());
                default -> "";
            };
        }

        void setRows(List<UniverseRow> rows) {
            Map<String, Boolean> selectedByTicker = new LinkedHashMap<>();
            for (UniverseRow row : this.rows) {
                selectedByTicker.put(row.ticker(), row.selected());
            }

            this.rows = new ArrayList<>();
            if (rows != null) {
                for (UniverseRow row : rows) {
                    Boolean selected = selectedByTicker.get(row.ticker());
                    if (selected != null) {
                        row.setSelected(selected);
                    }
                    this.rows.add(row);
                }
            }
            fireTableDataChanged();
        }

        UniverseRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }

        void setSelected(int rowIndex, boolean selected) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return;
            }
            rows.get(rowIndex).setSelected(selected);
            fireTableCellUpdated(rowIndex, 0);
        }

        List<UniverseRow> selectedRows() {
            List<UniverseRow> out = new ArrayList<>();
            for (UniverseRow row : rows) {
                if (row.selected()) {
                    out.add(row);
                }
            }
            return out;
        }

        List<UniverseRow> rows() {
            return new ArrayList<>(rows);
        }

        private String formatMarketCap(Double value) {
            if (value == null || !Double.isFinite(value)) {
                return "—";
            }
            double abs = Math.abs(value);
            if (abs >= 1_000_000_000_000.0) {
                return String.format("%.2fT", value / 1_000_000_000_000.0);
            }
            if (abs >= 1_000_000_000.0) {
                return String.format("%.1fB", value / 1_000_000_000.0);
            }
            if (abs >= 1_000_000.0) {
                return String.format("%.1fM", value / 1_000_000.0);
            }
            if (abs >= 1_000.0) {
                return String.format("%.1fK", value / 1_000.0);
            }
            return String.format("%.0f", value);
        }

        private String formatVolume(Double value) {
            if (value == null || !Double.isFinite(value)) {
                return "—";
            }
            double abs = Math.abs(value);
            if (abs >= 1_000_000_000.0) {
                return String.format("%.1fB", value / 1_000_000_000.0);
            }
            if (abs >= 1_000_000.0) {
                return String.format("%.1fM", value / 1_000_000.0);
            }
            if (abs >= 1_000.0) {
                return String.format("%.1fK", value / 1_000.0);
            }
            return String.format("%.0f", value);
        }

        private String formatChangePercent(Double value) {
            if (value == null || !Double.isFinite(value)) {
                return "—";
            }
            return String.format("%+.2f%%", value);
        }

        private String formatAnalysisDate(String value) {
            String raw = value == null ? "" : value.trim();
            if (raw.isBlank()) {
                return "—";
            }
            if (raw.length() >= 16 && raw.charAt(4) == '-' && raw.charAt(7) == '-') {
                return raw.substring(0, 16).replace('T', ' ');
            }
            return raw;
        }
    }

    private static final class UniverseRow {
        private boolean selected;
        private final String ticker;
        private final String name;
        private final String sector;
        private final Double marketCap;
        private final Double avgVolume7d;
        private final Double changePct1d;
        private final String analysisDate;
        private final Long mentionCount;
        private final Map<String, Object> raw;

        private UniverseRow(
                boolean selected,
                String ticker,
                String name,
                String sector,
                Double marketCap,
                Double avgVolume7d,
                Double changePct1d,
                String analysisDate,
                Long mentionCount,
                Map<String, Object> raw
        ) {
            this.selected = selected;
            this.ticker = ticker == null ? "" : ticker;
            this.name = name == null ? "—" : name;
            this.sector = sector == null ? "—" : sector;
            this.marketCap = marketCap;
            this.avgVolume7d = avgVolume7d;
            this.changePct1d = changePct1d;
            this.analysisDate = analysisDate == null ? "" : analysisDate;
            this.mentionCount = mentionCount;
            this.raw = raw == null ? new LinkedHashMap<>() : raw;
        }

        boolean selected() {
            return selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        String ticker() {
            return ticker;
        }

        String name() {
            return name;
        }

        String sector() {
            return sector;
        }

        Double marketCap() {
            return marketCap;
        }

        Double avgVolume7d() {
            return avgVolume7d;
        }

        Double changePct1d() {
            return changePct1d;
        }

        String analysisDate() {
            return analysisDate;
        }

        Long mentionCount() {
            return mentionCount;
        }

        Map<String, Object> raw() {
            return raw;
        }
    }

    private static final class ScanResultsTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Expand",
                "Ticker",
                "Status",
                "Overall Score",
                "Regime",
                "Trend/Momentum",
                "Vol/IV",
                "Top Strategy",
                "Confidence",
                "Warnings",
                "Updated"
        };
        private List<ScanResultRow> rows = new ArrayList<>();
        private String expandedTicker;

        @Override
        public int getRowCount() {
            return rows.size();
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
            ScanResultRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.ticker().equals(expandedTicker) ? "▼" : "▶";
                case 1 -> row.ticker();
                case 2 -> row.status();
                case 3 -> Double.isNaN(row.overallScore()) ? "—" : String.format("%.4f", row.overallScore());
                case 4 -> row.regime();
                case 5 -> row.trendMomentum();
                case 6 -> row.volatilityIv();
                case 7 -> row.topStrategy();
                case 8 -> Double.isNaN(row.confidence()) ? "—" : String.format("%.3f", row.confidence());
                case 9 -> row.warningsCount();
                case 10 -> row.updatedAt();
                default -> "";
            };
        }

        void setRows(List<ScanResultRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        ScanResultRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }

        void setExpandedTicker(String expandedTicker) {
            this.expandedTicker = expandedTicker;
            fireTableRowsUpdated(0, Math.max(0, rows.size() - 1));
        }
    }

    private record ScanResultRow(
            String ticker,
            String status,
            double overallScore,
            String regime,
            String trendMomentum,
            String volatilityIv,
            String topStrategy,
            double confidence,
            int warningsCount,
            String updatedAt,
            Map<String, Object> raw
    ) {
    }

    private static final class ScanRunsTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Run ID",
                "Date/Time",
                "Universe Hash",
                "Config",
                "Primary Score",
                "Status"
        };
        private List<ScanRunRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            ScanRunRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.shortRunId();
                case 1 -> row.startedAt();
                case 2 -> row.universeHash();
                case 3 -> row.configSummary();
                case 4 -> row.primaryScore();
                case 5 -> row.status();
                default -> "";
            };
        }

        void setRows(List<ScanRunRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        ScanRunRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private record ScanRunRow(
            String runId,
            String startedAt,
            String universeHash,
            String configSummary,
            String primaryScore,
            String status,
            Map<String, Object> raw
    ) {
        String shortRunId() {
            if (runId == null || runId.isBlank()) {
                return "—";
            }
            return runId.length() <= 8 ? runId : runId.substring(0, 8);
        }
    }

    private static final class DataTickerStatsTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Ticker", "Bars", "Missing", "Repaired", "Dropped"};
        private List<DataTickerStatsRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            DataTickerStatsRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.ticker();
                case 1 -> formatCount(row.bars());
                case 2 -> formatCount(row.missingBars());
                case 3 -> formatCount(row.repairedBars());
                case 4 -> formatCount(row.droppedBars());
                default -> "";
            };
        }

        private static String formatCount(Long value) {
            return value == null ? "—" : String.valueOf(value);
        }

        void setRows(List<DataTickerStatsRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }
    }

    private record DataTickerStatsRow(
            String ticker,
            Long bars,
            Long missingBars,
            Long repairedBars,
            Long droppedBars
    ) {
    }

    private static final class FeaturesTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Feature",
                "Category",
                "Parameters",
                "Timeframe",
                "Scaling",
                "Missing %",
                "Zero Var",
                "Corr Cluster",
                "Status",
                "Reason",
                "Dependencies",
                "Join Lag"
        };

        private List<FeatureRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            FeatureRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.featureName();
                case 1 -> row.category();
                case 2 -> row.parameters();
                case 3 -> row.timeframe();
                case 4 -> row.scaling();
                case 5 -> row.missingPct();
                case 6 -> row.zeroVariance();
                case 7 -> row.correlationCluster();
                case 8 -> row.kept() ? "kept" : "dropped";
                case 9 -> row.statusReason();
                case 10 -> row.dependencies();
                case 11 -> row.joinLag();
                default -> "";
            };
        }

        void setRows(List<FeatureRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        FeatureRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private static final class TopFeaturesTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Rank", "Feature", "Importance", "Stability", "Category"};
        private List<TopFeatureRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            TopFeatureRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.rank();
                case 1 -> row.featureName();
                case 2 -> Double.isNaN(row.importance()) ? "—" : String.format("%.6f", row.importance());
                case 3 -> row.stability();
                case 4 -> row.category();
                default -> "";
            };
        }

        void setRows(List<TopFeatureRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }
    }

    private record FeatureRow(
            String featureName,
            String category,
            String parameters,
            String timeframe,
            String scaling,
            String missingPct,
            String zeroVariance,
            String correlationCluster,
            boolean kept,
            String statusReason,
            String dependencies,
            String joinLag,
            Double importance,
            String stability,
            Map<String, Object> raw
    ) {
    }

    private record TopFeatureRow(
            int rank,
            String featureName,
            double importance,
            String stability,
            String category
    ) {
    }

    private static final class FailureTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Timestamp",
                "Ticker",
                "Predicted",
                "Confidence",
                "Actual",
                "Regime",
                "Error/Loss"
        };
        private List<FailureRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            FailureRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.timestamp();
                case 1 -> row.ticker();
                case 2 -> row.predictedValue();
                case 3 -> row.confidence();
                case 4 -> row.actualOutcome();
                case 5 -> row.regime();
                case 6 -> row.errorOrLoss();
                default -> "";
            };
        }

        void setRows(List<FailureRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        FailureRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private record FailureRow(
            String timestamp,
            String ticker,
            String predictedValue,
            String confidence,
            String actualOutcome,
            String regime,
            String errorOrLoss,
            String topContributions,
            Double errorScore,
            Map<String, Object> raw
    ) {
    }

    private static final class RunsTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Run ID",
                "Date/Time",
                "Target/Horizon",
                "Dataset Range",
                "Model",
                "Primary Score",
                "Status"
        };

        private List<RunsTableRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
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
            RunsTableRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.shortRunId();
                case 1 -> row.dateTime();
                case 2 -> row.targetHorizon();
                case 3 -> row.datasetRange();
                case 4 -> row.modelType();
                case 5 -> row.primaryScore();
                case 6 -> row.status();
                default -> "";
            };
        }

        void setRows(List<RunsTableRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        RunsTableRow getRow(int rowIndex) {
            if (rowIndex < 0 || rowIndex >= rows.size()) {
                return null;
            }
            return rows.get(rowIndex);
        }
    }

    private record RunsTableRow(
            String runId,
            String shortRunId,
            String dateTime,
            String targetHorizon,
            String datasetRange,
            String modelType,
            String primaryScore,
            String status,
            String tickers,
            String horizon,
            LocalDate date,
            Map<String, Object> raw
    ) {
        String dateTimeSortKey() {
            return dateTime == null ? "" : dateTime;
        }
    }

    private void appendTrainingLog(String line) {
        SwingUtilities.invokeLater(() -> {
            trainingLogArea.append(line + "\n");
            trainingLogArea.setCaretPosition(trainingLogArea.getDocument().getLength());
        });
    }

    private void appendBackendLog(String line) {
        if (backendLogArea == null) {
            appendSystemControlLog(line);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            backendLogArea.append(line + "\n");
            backendLogArea.setCaretPosition(backendLogArea.getDocument().getLength());
        });
    }

    private static String ts() {
        return LocalDateTime.now().format(TS);
    }

    @FunctionalInterface
    private interface FetchTask {
        Object fetch() throws Exception;
    }
}
