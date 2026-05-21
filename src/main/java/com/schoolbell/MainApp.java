package com.schoolbell;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.service.*;
import com.schoolbell.ui.*;
import com.sun.net.httpserver.HttpServer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    // Services
    private final RelayController relayController = new RelayController();
    private final ScheduleService scheduleService = new ScheduleService();
    private final AcademicService academicService = new AcademicService();
    private final StaffService staffService = new StaffService();
    private final AnnouncementService announcementService = new AnnouncementService();
    private ConfigService configService;
    private AudioService audioService;
    private SignalService signalService;
    private SystemService systemService;
    private MediaSchedulerService mediaSchedulerService;
    private BroadcastService broadcastService;
    private HttpServer httpServer;

    // Controllers
    private final SystemJournal journal = new SystemJournal();
    private AppNavigation navigation;

    // Data
    private List<BellEntry> schedule = Collections.emptyList();
    private List<DaySchedule> internalSchedules = new ArrayList<>();
    private final Map<Integer, String> teacherCache = new HashMap<>();
    private final Map<Integer, String> subjectCache = new HashMap<>();
    private final Map<Integer, String> classroomCache = new HashMap<>();
    private final List<SchoolClass> classCache = new ArrayList<>();

    // UI
    private StackPane contentArea;
    private VBox sidebar;
    private Label sidebarStatusTime;
    private Circle sidebarStatusDot;
    private DashboardView dashboardView;
    private SchoolView schoolView;
    private ScheduleView scheduleView;
    private EfirView efirView;
    private NotificationsView notificationsView;
    private SystemView systemView;
    private ImportView importView;
    private Stage primaryStage;
    private TrayIcon trayIcon;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) {
        java.util.Locale.setDefault(Locale.of("uk", "UA"));
        this.primaryStage = primaryStage;
        DatabaseManager.initialize();

        // Initialize Services
        configService = new ConfigService();
        configService.loadConfig();
        relayController.setMainApp(this);
        audioService = new AudioService(configService);
        signalService = new SignalService(relayController, audioService, configService);
        signalService.setLogConsumer(msg -> journal.addLog(msg, "INFO"));
        systemService = new SystemService(configService);
        mediaSchedulerService = new MediaSchedulerService(this);

        startBroadcastServers();

        primaryStage.setTitle("SchoolBell Dashboard v4.0");

        // Tray Support
        initTray();
        primaryStage.setOnCloseRequest(event -> {
            if (configService.isMinimizeToTray()) {
                event.consume();
                primaryStage.hide();
            } else {
                stop();
                Platform.exit();
                System.exit(0);
            }
        });

        // --- SIDEBAR ---
        sidebar = new VBox(10);
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(200);
        sidebar.setStyle(SIDEBAR_STYLE);
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Logo Section
        VBox logoBox = new VBox(createSVGIcon(ICON_BELL, Color.WHITE, 30));
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPrefSize(60, 60);
        logoBox.setMaxSize(60, 60);
        logoBox.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 16;");

        VBox logoContainer = new VBox(logoBox);
        logoContainer.setPadding(new Insets(20, 0, 40, 0));
        logoContainer.setAlignment(Pos.CENTER);
        sidebar.getChildren().add(logoContainer);

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        
        // System Status Indicator at bottom
        sidebarStatusDot = new Circle(4, Color.web(COLOR_SUCCESS));
        sidebarStatusDot.setCache(true);
        sidebarStatusDot.setCacheHint(javafx.scene.CacheHint.SPEED);

        Label statusText = new Label("Онлайн");
        statusText.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        sidebarStatusTime = new Label("00:00:00");
        sidebarStatusTime.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox statusInfo = new VBox(5, new HBox(10, sidebarStatusDot, statusText), sidebarStatusTime);
        statusInfo.setStyle(SIDEBAR_STATUS_STYLE);
        statusInfo.setCache(true);
        statusInfo.setCacheHint(javafx.scene.CacheHint.SPEED);

        Timeline pulseIndicator = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(sidebarStatusDot.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.8), new KeyValue(sidebarStatusDot.opacityProperty(), 0.3)),
            new KeyFrame(Duration.seconds(1.6), new KeyValue(sidebarStatusDot.opacityProperty(), 1.0))
        );
        pulseIndicator.setCycleCount(Animation.INDEFINITE);
        pulseIndicator.play();

        // --- CONTENT AREA ---
        contentArea = new StackPane();
        contentArea.setStyle(DEPTH_1);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Initialize Navigation
        navigation = new AppNavigation(this, sidebar, contentArea);
        navigation.init();
        
        sidebar.getChildren().add(spacer);
        sidebar.getChildren().add(statusInfo);

        HBox mainLayout = new HBox(sidebar, contentArea);
        StackPane root = new StackPane(mainLayout);

        // Initialize Toast System
        ToastService.setup(root);

        Scene scene = new Scene(root, 1400, 950);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Initialize Views
        dashboardView = new DashboardView(this);
        schoolView = new SchoolView(this);
        scheduleView = new ScheduleView(this);
        efirView = new EfirView(this);
        notificationsView = new NotificationsView(this);
        systemView = new SystemView(this);
        importView = new ImportView(this);

        navigation.showDashboard();

        relayController.scanDevices();
        relayController.connect();
        internalSchedules = scheduleService.loadInternalSchedules();
        refreshScheduleOptions();
        startScheduler();
        refreshCaches();
        logger.info("System ready.");
    }

    public void addLog(String message, String level) { journal.addLog(message, level); }
    public ObservableList<String> getSystemLogs() { return journal.getSystemLogs(); }

    public void showDashboard() { navigation.showDashboard(); }
    public void showSchool() { navigation.showSchool(); }
    public void showSchedule() { navigation.showSchedule(); }
    public void showEditorTab(int tabIndex) { navigation.showEditorTab(tabIndex); }
    public void showEfir() { navigation.showEfir(); }
    public void showNotifications() { navigation.showNotifications(); }
    public void showSystem() { navigation.showSystem(); }
    public void showImport() { navigation.showImport(); }

    public DashboardView getDashboardView() { return dashboardView; }
    public SchoolView getSchoolView() { return schoolView; }
    public ScheduleView getScheduleView() { return scheduleView; }
    public EfirView getEfirView() { return efirView; }
    public NotificationsView getNotificationsView() { return notificationsView; }
    public SystemView getSystemView() { return systemView; }
    public ImportView getImportView() { return importView; }

    public Stage getStage() { return primaryStage; }

    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            java.time.LocalDate today = java.time.LocalDate.now();
            signalService.checkAndTriggerBell(now, schedule);
            Platform.runLater(() -> {
                dashboardView.update(now);
                if (sidebarStatusTime != null) sidebarStatusTime.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                if (relayController.isConnected()) {
                    if (sidebarStatusDot != null) sidebarStatusDot.setFill(Color.web(COLOR_SUCCESS));
                } else {
                    if (sidebarStatusDot != null) sidebarStatusDot.setFill(Color.web(COLOR_DANGER));
                }
                
                if (broadcastService != null && broadcastService.isBroadcasting()) {
                    Map<String, Object> data = dashboardView.getExtendedDashboardData(now);
                    String activeAnnouncement = announcementService.getActiveAnnouncementText(today, now);
                    data.put("announcement", activeAnnouncement != null ? activeAnnouncement : "");
                    broadcastService.broadcastUpdate(data);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void reloadSchedule() {
        String name = configService.getSelectedScheduleName();
        if (name == null) return;
        internalSchedules.stream().filter(ds -> ds.getName().equals(name)).findFirst().ifPresent(ds -> {
            schedule = scheduleService.convertToBellEntries(ds);
            Platform.runLater(() -> {
                dashboardView.refreshActiveScheduleLabel();
                dashboardView.clearFlow();
            });
            logger.info("Schedule reloaded: " + name);
        });
    }

    public void refreshScheduleOptions() {
        Platform.runLater(this::reloadSchedule);
    }

    public void refreshCaches() {
        teacherCache.clear();
        staffService.getAllTeachers().forEach(t -> teacherCache.put(t.id(), t.name()));
        subjectCache.clear();
        staffService.getAllSubjects().forEach(s -> subjectCache.put(s.id(), s.name()));
        classroomCache.clear();
        academicService.getAllClassrooms().forEach(c -> classroomCache.put(c.id(), c.name()));
        classCache.clear();
        classCache.addAll(academicService.getAllClasses());
    }

    public String getSubjectName(int id) { return subjectCache.getOrDefault(id, "—"); }
    public String getTeacherName(int id) { return teacherCache.getOrDefault(id, "—"); }
    public String getClassroomName(int id) { return classroomCache.getOrDefault(id, "—"); }
    public List<SchoolClass> getClassCache() { return classCache; }

    public ConfigService getConfigService() { return configService; }
    public AudioService getAudioService() { return audioService; }
    public SignalService getSignalService() { return signalService; }
    public SystemService getSystemService() { return systemService; }
    public MediaSchedulerService getMediaSchedulerService() { return mediaSchedulerService; }
    public BroadcastService getBroadcastService() { return broadcastService; }
    public RelayController getRelayController() { return relayController; }
    public AcademicService getAcademicService() { return academicService; }
    public StaffService getStaffService() { return staffService; }
    public ScheduleService getScheduleService() { return scheduleService; }
    public List<DaySchedule> getInternalSchedules() { return internalSchedules; }
    public List<BellEntry> getSchedule() { return schedule; }
    
    public void saveConfig() { configService.saveConfig(); }

    private void startHttpServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", exchange -> {
                String theme = configService.getDashboardTheme();
                String resourceName = "/dashboard_" + theme + ".html";
                
                try (InputStream is = getClass().getResourceAsStream(resourceName)) {
                    if (is == null) {
                        try (InputStream fis = getClass().getResourceAsStream("/dashboard_classic.html")) {
                            if (fis == null) {
                                exchange.sendResponseHeaders(404, 0);
                                exchange.close();
                                return;
                            }
                            byte[] response = fis.readAllBytes();
                            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                            exchange.sendResponseHeaders(200, response.length);
                            try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
                            return;
                        }
                    }
                    byte[] response = is.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
                }
            });
            httpServer.setExecutor(null);
            httpServer.start();
            logger.info("HTTP Server started on port: " + port);
        } catch (IOException e) {
            logger.error("Failed to start HTTP server", e);
        }
    }

    public void startBroadcastServers() {
        stopBroadcastServers();
        
        if (configService.isBroadcastEnabled()) {
            try {
                broadcastService = new BroadcastService(configService.getBroadcastPort() + 2);
                broadcastService.start();
                startHttpServer(configService.getBroadcastPort());
                logger.info("Broadcast servers started successfully.");
            } catch (Exception e) {
                logger.error("Failed to start broadcast server", e);
            }
        }
    }

    public void stopBroadcastServers() {
        if (httpServer != null) {
            try {
                httpServer.stop(0);
                logger.info("HTTP Server stopped.");
            } catch (Exception e) {
                logger.error("Failed to stop HTTP server", e);
            } finally {
                httpServer = null;
            }
        }
        if (broadcastService != null) {
            try {
                broadcastService.stop(1000);
                logger.info("Broadcast WebSocket Server stopped.");
            } catch (Exception e) {
                logger.error("Failed to stop Broadcast WebSocket Server", e);
            } finally {
                broadcastService = null;
            }
        }
    }

    @Override public void stop() { 
        relayController.close(); 
        scheduler.shutdown(); 
        stopBroadcastServers();
        if (audioService != null) audioService.stopAll();
    }
    private void initTray() {
        if (!SystemTray.isSupported()) return;
        Platform.runLater(() -> Platform.setImplicitExit(false));
        SwingUtilities.invokeLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                int size = 32;
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(new java.awt.Color(9, 132, 227));
                g2.fillRoundRect(0, 0, size, size, 8, 8);
                g2.setColor(java.awt.Color.WHITE);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.fill(new java.awt.geom.Area(createBellShape()));
                g2.dispose();
                java.awt.PopupMenu popup = new java.awt.PopupMenu();
                java.awt.MenuItem showItem = new java.awt.MenuItem("Відкрити");
                showItem.addActionListener(e -> Platform.runLater(() -> {
                    primaryStage.show();
                    primaryStage.toFront();
                }));
                java.awt.MenuItem exitItem = new java.awt.MenuItem("Вихід");
                exitItem.addActionListener(e -> {
                    Platform.runLater(() -> {
                        stop();
                        Platform.exit();
                        System.exit(0);
                    });
                });
                popup.add(showItem);
                popup.addSeparator();
                popup.add(exitItem);
                trayIcon = new TrayIcon(image, "SchoolBell", popup);
                trayIcon.setImageAutoSize(true);
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> {
                                if (primaryStage.isShowing()) {
                                    primaryStage.hide();
                                } else {
                                    primaryStage.show();
                                    primaryStage.toFront();
                                }
                            });
                        }
                    }
                });
                tray.add(trayIcon);
            } catch (Exception e) {
                logger.error("Failed to initialize tray icon", e);
            }
        });
    }

    private java.awt.Shape createBellShape() {
        java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
        path.moveTo(12, 2);
        path.curveTo(11.45, 2, 10.95, 2.22, 10.59, 2.59);
        path.lineTo(10, 4);
        path.curveTo(10, 4.1, 10, 4.2, 10, 4.29);
        path.curveTo(7.12, 5.14, 5, 7.82, 5, 11);
        path.lineTo(5, 17);
        path.lineTo(3, 19);
        path.lineTo(3, 20);
        path.lineTo(21, 20);
        path.lineTo(21, 19);
        path.lineTo(19, 17);
        path.lineTo(19, 11);
        path.curveTo(19, 7.82, 16.88, 5.14, 14, 4.29);
        path.curveTo(14, 4.19, 14, 4.1, 14, 4);
        path.curveTo(14, 2.9, 13.1, 2, 12, 2);
        path.closePath();
        path.moveTo(10, 21);
        path.curveTo(10, 22.1, 10.9, 23, 12, 23);
        path.curveTo(13.1, 23, 14, 22.1, 14, 21);
        path.lineTo(10, 21);
        path.closePath();
        return path;
    }

    public static void main(String[] args) { launch(args); }
}
