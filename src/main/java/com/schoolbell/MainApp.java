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

import java.io.*;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private BroadcastService broadcastService;
    private HttpServer httpServer;

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
    private final Map<String, Button> navButtons = new HashMap<>();
    private Label sidebarStatusTime;
    private Circle sidebarStatusDot;
    private DashboardView dashboardView;
    private SchoolView schoolView;
    private ScheduleView scheduleView;
    private EfirView efirView;
    private NotificationsView notificationsView;
    private SystemView systemView;
    private Stage primaryStage;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        DatabaseManager.initialize();
        
        // Initialize Services
        configService = new ConfigService();
        configService.loadConfig();
        relayController.setMainApp(this);
        audioService = new AudioService(configService);
        signalService = new SignalService(relayController, audioService, configService);
        signalService.setLogConsumer(msg -> logger.info(msg));
        
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
                    
                    // Logic for announcements: priority to scheduled ones
                    String activeAnnouncement = announcementService.getActiveAnnouncementText(today, now);
                    if (activeAnnouncement == null || activeAnnouncement.isEmpty()) {
                        activeAnnouncement = configService.getAnnouncementText();
                    }
                    data.put("announcement", activeAnnouncement);
                    
                    broadcastService.broadcastUpdate(data);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void addLog(String message, String level) {
        logger.info("[" + level + "] " + message);
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
                try (InputStream is = getClass().getResourceAsStream("/dashboard.html")) {
                    if (is == null) { exchange.sendResponseHeaders(404, 0); exchange.close(); return; }
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
    public static void main(String[] args) { launch(args); }
}
