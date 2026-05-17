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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import java.awt.MenuItem;
import java.awt.PopupMenu;
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

import static com.schoolbell.ui.UIStyles.*;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    // Services
    private final RelayController relayController = new RelayController();
    private final ScheduleService scheduleService = new ScheduleService();
    private final AcademicService academicService = new AcademicService();
    private final AnnouncementService announcementService = new AnnouncementService();
    private ConfigService configService;
    private AudioService audioService;
    private SignalService signalService;
    private SystemService systemService;
    private MediaSchedulerService mediaSchedulerService;
    private BroadcastService broadcastService;
    private HttpServer httpServer;

    // Data
    private List<BellEntry> schedule = Collections.emptyList();
    private List<DaySchedule> internalSchedules = new ArrayList<>();
    private final Map<Integer, String> teacherCache = new HashMap<>();
    private final Map<Integer, String> subjectCache = new HashMap<>();
    private final Map<Integer, String> classroomCache = new HashMap<>();
    private final List<SchoolClass> classCache = new ArrayList<>();
    private final javafx.collections.ObservableList<String> systemLogs = javafx.collections.FXCollections.observableArrayList();

    // UI
    private StackPane contentArea;
    private VBox sidebar;
    private final Map<String, Button> navButtons = new HashMap<>();
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
        signalService.setLogConsumer(msg -> addLog(msg, "INFO"));
        systemService = new SystemService(configService);
        mediaSchedulerService = new MediaSchedulerService(this);

        if (configService.isBroadcastEnabled()) {
            try {
                broadcastService = new BroadcastService(configService.getBroadcastPort() + 2);
                broadcastService.start();
                startHttpServer(configService.getBroadcastPort());
            } catch (Exception e) {
                logger.error("Failed to start broadcast server", e);
            }
        }

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

        createNavButton("DASHBOARD", "Головна", ICON_DASHBOARD, this::showDashboard);
        createNavButton("SCHEDULE", "Розклад", ICON_CALENDAR, this::showSchedule);
        createNavButton("NOTIFICATIONS", "Сповіщення", ICON_NOTIFICATIONS, this::showNotifications);
        createNavButton("EFIR", "Ефір", ICON_BROADCAST, this::showEfir);
        createNavButton("SCHOOL", "Школа", ICON_FOLDER, this::showSchool);
        createNavButton("IMPORT", "Імпорт", ICON_PLUS, this::showImport);

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        createNavButton("SYSTEM", "Система", ICON_SETTINGS, this::showSystem);

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
        sidebar.getChildren().add(statusInfo);

        // --- CONTENT AREA ---
        contentArea = new StackPane();
        contentArea.setStyle(DEPTH_1);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        HBox mainLayout = new HBox(sidebar, contentArea);
        StackPane root = new StackPane(mainLayout);

        // Initialize Toast System
        ToastService.setup(root);

        Scene scene = new Scene(root, 1400, 950);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        dashboardView = new DashboardView(this);
        schoolView = new SchoolView(this);
        scheduleView = new ScheduleView(this);
        efirView = new EfirView(this);
        notificationsView = new NotificationsView(this);
        systemView = new SystemView(this);
        importView = new ImportView(this);

        showDashboard();

        relayController.scanDevices();
        relayController.connect();
        internalSchedules = scheduleService.loadInternalSchedules();
        refreshScheduleOptions();
        startScheduler();
        refreshCaches();
        logger.info("System ready.");
    }

    private void createNavButton(String id, String text, String iconPath, Runnable action) {
        Button btn = new Button(text);
        btn.setGraphic(createSVGIcon(iconPath, Color.web("#b2bec3"), 20));
        btn.setGraphicTextGap(15);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(NAV_BTN_BASE);
        VBox.setMargin(btn, new Insets(2, 15, 2, 15));
        btn.setOnAction(e -> {
            setActiveNav(id);
            action.run();
        });
        btn.setOnMouseEntered(e -> { if (!btn.getStyle().contains(NAV_BTN_ACTIVE)) btn.setStyle(NAV_BTN_BASE + NAV_BTN_HOVER); });
        btn.setOnMouseExited(e -> { if (!btn.getStyle().contains(NAV_BTN_ACTIVE)) btn.setStyle(NAV_BTN_BASE); });
        sidebar.getChildren().add(btn);
        navButtons.put(id, btn);
    }

    private void setActiveNav(String id) {
        navButtons.forEach((k, v) -> {
            v.setStyle(NAV_BTN_BASE);
            if (v.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Color.web("#b2bec3"));
        });
        Button active = navButtons.get(id);
        if (active != null) {
            active.setStyle(NAV_BTN_BASE + NAV_BTN_ACTIVE);
            if (active.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Color.WHITE);
        }
    }

    public void showDashboard() {
        setActiveNav("DASHBOARD");
        contentArea.getChildren().setAll(dashboardView.build());
    }

    public void showSchool() {
        setActiveNav("SCHOOL");
        contentArea.getChildren().setAll(schoolView.build());
    }

    public void showSchedule() {
        setActiveNav("SCHEDULE");
        contentArea.getChildren().setAll(scheduleView.build());
    }

    public void showEditorTab(int tabIndex) {
        ScheduleEditorDialog editor = new ScheduleEditorDialog(this);
        Node content = editor.createTabContent(tabIndex);
        contentArea.getChildren().setAll(content);
    }

    public void showEfir() { 
        setActiveNav("EFIR"); 
        contentArea.getChildren().setAll(efirView.build()); 
    }

    public void showNotifications() {
        setActiveNav("NOTIFICATIONS");
        contentArea.getChildren().setAll(notificationsView.build());
    }

    public void showSystem() {
        setActiveNav("SYSTEM");
        contentArea.getChildren().setAll(systemView.build());
    }

    public void showImport() {
        setActiveNav("IMPORT");
        contentArea.getChildren().setAll(importView.build());
    }
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
                    
                    // Logic for announcements: only show active ones from the dedicated system
                    String activeAnnouncement = announcementService.getActiveAnnouncementText(today, now);
                    data.put("announcement", activeAnnouncement != null ? activeAnnouncement : "");
                    
                    broadcastService.broadcastUpdate(data);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void addLog(String message, String level) {
        String timestamp = LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String fullMsg = "[" + timestamp + "] [" + level + "] " + message;
        logger.info(fullMsg);
        Platform.runLater(() -> {
            systemLogs.add(0, fullMsg);
            if (systemLogs.size() > 100) systemLogs.remove(100, systemLogs.size());
        });
    }

    public javafx.collections.ObservableList<String> getSystemLogs() { return systemLogs; }

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
        academicService.getAllTeachers().forEach(t -> teacherCache.put(t.id(), t.name()));
        subjectCache.clear();
        academicService.getAllSubjects().forEach(s -> subjectCache.put(s.id(), s.name()));
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
                        // Fallback to classic if theme not found
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

    @Override public void stop() { 
        relayController.close(); 
        scheduler.shutdown(); 
        if (httpServer != null) httpServer.stop(0);
        if (audioService != null) audioService.stopAll();
    }
    private void initTray() {
        if (!SystemTray.isSupported()) return;

        Platform.runLater(() -> Platform.setImplicitExit(false));

        SwingUtilities.invokeLater(() -> {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                
                // Create a high-quality bell icon from SVG path
                int size = 32; // Use larger size for better scaling
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                
                // Draw a soft rounded background (optional, but looks modern)
                g2.setColor(new java.awt.Color(9, 132, 227)); // COLOR_PRIMARY
                g2.fillRoundRect(0, 0, size, size, 8, 8);
                
                // Render the SVG Bell Icon
                g2.setColor(java.awt.Color.WHITE);
                java.awt.geom.Path2D bellPath = new java.awt.geom.Path2D.Double();
                // ICON_BELL path parsed to AWT Path
                // M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z
                // Simplified manual drawing or using a shape to match the brand
                double s = size / 24.0;
                g2.scale(s, s);
                
                // Draw the bell shape manually to match ICON_BELL exactly
                g2.fill(new java.awt.geom.Area(createBellShape()));
                g2.dispose();

                PopupMenu popup = new PopupMenu();
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
        // M12,2 A2,2 0 0,0 10,4 A2,2 0 0,0 10,4.29 C7.12,5.14 5,7.82 5,11 V17 L3,19 V20 H21 V19 L19,17 V11 C19,7.82 16.88,5.14 14,4.29 C14,4.19 14,4.1 14,4 A2,2 0 0,0 12,2 M10,21 A2,2 0 0,0 12,23 A2,2 0 0,0 14,21 H10 Z
        path.moveTo(12, 2);
        path.curveTo(11.45, 2, 10.95, 2.22, 10.59, 2.59); // Simplified A2,2 arc
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
