package com.schoolbell;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.service.*;
import com.schoolbell.ui.*;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    public static final String VERSION = "1.1.1-test";
    private static final String APP_TITLE = "SchoolBell v" + VERSION;

    // Services
    private RelayController relayController;
    private final ScheduleService scheduleService = new ScheduleService();
    private final AcademicService academicService = new AcademicService();
    private final StaffService staffService = new StaffService();
    private final AnnouncementService announcementService = new AnnouncementService();
    private ConfigService configService;
    private AudioService audioService;
    private RadioStationService radioStationService;
    private SignalService signalService;
    private SystemService systemService;
    private MediaSchedulerService mediaSchedulerService;
    private BroadcastService broadcastService;
    private AirAlertService airAlertService;
    private UpdateService updateService;
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
    private DashboardView dashboardView;
    private SchoolView schoolView;
    private ScheduleView scheduleView;
    private EfirView efirView;
    private NotificationsView notificationsView;
    private SystemView systemView;
    private ImportView importView;
    private Stage primaryStage;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting SchoolBell v{}", VERSION);
        java.util.Locale.setDefault(Locale.of("uk", "UA"));
        this.primaryStage = primaryStage;
        
        // 1. Basic Stage Setup
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle(APP_TITLE);
        
        try {
            InputStream iconStream = getClass().getResourceAsStream("/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            logger.warn("Could not load application icon: " + e.getMessage());
        }

        // 2. Initialize Core Services (Lightweight)
        DatabaseManager.initialize();
        configService = new ConfigService();
        configService.loadConfig();
        updateService = new UpdateService();
        updateService.setJournalConsumer(msg -> addLog(msg, "INFO"));
        
        // 3. Setup Instance Guard early
        InstanceGuard.startListener(primaryStage);

        // 4. Build UI Structure (Heavy)
        sidebar = new VBox(10);
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(200);
        sidebar.setStyle(SIDEBAR_STYLE);
        sidebar.setAlignment(Pos.TOP_CENTER);

        // Logo
        VBox logoBox = new VBox(createSVGIcon(ICON_BELL, Color.WHITE, 30));
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setPrefSize(60, 60);
        logoBox.setMaxSize(60, 60);
        logoBox.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 16;");
        VBox logoContainer = new VBox(logoBox);
        logoContainer.setPadding(new Insets(20, 0, 40, 0));
        logoContainer.setAlignment(Pos.CENTER);
        sidebar.getChildren().add(logoContainer);

        contentArea = new StackPane();
        contentArea.setStyle(DEPTH_1);
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        navigation = new AppNavigation(this, sidebar, contentArea);
        navigation.init();
        
        HBox mainLayout = new HBox(sidebar, contentArea);
        StackPane root = new StackPane(mainLayout);
        ToastService.setup(root);

        VBox windowWrapper = new VBox(new TitleBar(primaryStage, this, APP_TITLE), root);
        VBox.setVgrow(root, Priority.ALWAYS);

        Scene scene = new Scene(windowWrapper, 1400, 950);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setScene(scene);

        // 5. Initialize Hardware & Background Services
        relayController = new RelayController(this);
        audioService = new AudioService(configService);
        radioStationService = new RadioStationService();
        signalService = new SignalService(relayController, audioService, configService);
        signalService.setLogConsumer(msg -> journal.addLog(msg, "INFO"));
        systemService = new SystemService(configService);
        mediaSchedulerService = new MediaSchedulerService(this);
        airAlertService = new AirAlertService(this, configService, signalService, scheduler);
        
        if (configService.isAirRaidAutomationEnabled()) {
            airAlertService.start();
        }

        dashboardView = new DashboardView(this);
        schoolView = new SchoolView(this);
        scheduleView = new ScheduleView(this);
        efirView = new EfirView(this);
        notificationsView = new NotificationsView(this);
        systemView = new SystemView(this);
        importView = new ImportView(this);

        // 6. Final Window Configuration & Show
        javafx.geometry.Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

        // Handle Close Request
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

        primaryStage.show();
        navigation.showDashboard();
        
        // 7. Post-Show Tasks
        new TrayManager(this, primaryStage).init();
        
        startBroadcastServers();
        internalSchedules = scheduleService.loadInternalSchedules();
        refreshScheduleOptions();
        startScheduler();
        refreshCaches();

        // Load recent logs from DB
        journal.loadLastLogs();
        systemService.logSystemDiagnostics(journal);

        // Background maintenance
        scheduler.scheduleAtFixedRate(DatabaseManager::cleanupOldData, 1, 24, TimeUnit.HOURS);
        scheduler.schedule(this::checkForUpdates, 10, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 24, 24, TimeUnit.HOURS);

        logger.info("System fully initialized and ready.");
    }

    private UpdateAvailableDialog activeUpdateDialog;

    private void checkForUpdates() {
        updateService.checkForUpdates().thenAccept(manifest -> {
            if (manifest != null) {
                Platform.runLater(() -> {
                    if (activeUpdateDialog != null && activeUpdateDialog.isShowing()) {
                        return;
                    }

                    String message = "ÃƒÂÃ¢â‚¬ÂÃƒÂÃ‚Â¾Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã†â€™ÃƒÂÃ‚Â¿ÃƒÂÃ‚Â½ÃƒÂÃ‚Â° ÃƒÂÃ‚Â½ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â²ÃƒÂÃ‚Â° ÃƒÂÃ‚Â²ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬Ãƒâ€˜Ã‚ÂÃƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã‚Â " + manifest.latestVersion() + ". ÃƒÂÃ‚ÂÃƒÂÃ‚Â°Ãƒâ€˜Ã¢â‚¬Å¡ÃƒÂÃ‚Â¸Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â½Ãƒâ€˜Ã¢â‚¬â€œÃƒâ€˜Ã¢â‚¬Å¡Ãƒâ€˜Ã…â€™ ÃƒÂÃ‚Â´ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚Â ÃƒÂÃ‚Â¿ÃƒÂÃ‚ÂµÃƒâ€˜Ã¢â€šÂ¬ÃƒÂÃ‚ÂµÃƒÂÃ‚Â³ÃƒÂÃ‚Â»Ãƒâ€˜Ã‚ÂÃƒÂÃ‚Â´Ãƒâ€˜Ã†â€™.";
                    ToastService.show(message, ToastService.ToastType.INFO, () -> {
                        if (activeUpdateDialog == null || !activeUpdateDialog.isShowing()) {
                            activeUpdateDialog = new UpdateAvailableDialog(primaryStage, updateService, manifest);
                            activeUpdateDialog.show();
                        }
                    });
                    
                    logger.info("Update available: {}", manifest.latestVersion());
                });
            }
        });
    }

    public void addLog(String message, String level) { journal.addLog(message, level); }
    public ObservableList<String> getSystemLogs() { return journal.getSystemLogs(); }

    public void showEditorTab(int tabIndex) { navigation.showEditorTab(tabIndex); }
    public void showNotifications() { navigation.showNotifications(); }

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
                if (dashboardView != null) dashboardView.update(now);
                
                if (broadcastService != null && broadcastService.isBroadcasting() && dashboardView != null) {
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
                if (dashboardView != null) dashboardView.refreshActiveScheduleLabel();
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

    public String getSubjectName(int id) { return subjectCache.getOrDefault(id, "ÃƒÂÃ‚ÂÃƒÂÃ‚ÂµÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾"); }
    public String getTeacherName(int id) { return teacherCache.getOrDefault(id, "ÃƒÂÃ‚ÂÃƒÂÃ‚ÂµÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾"); }
    public String getClassroomName(int id) { return classroomCache.getOrDefault(id, "ÃƒÂÃ‚ÂÃƒÂÃ‚ÂµÃƒÂÃ‚Â²Ãƒâ€˜Ã¢â‚¬â€œÃƒÂÃ‚Â´ÃƒÂÃ‚Â¾ÃƒÂÃ‚Â¼ÃƒÂÃ‚Â¾"); }
    public List<SchoolClass> getClassCache() { return classCache; }

    public ConfigService getConfigService() { return configService; }
    public AudioService getAudioService() { return audioService; }
    public RadioStationService getRadioStationService() { return radioStationService; }
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
    public AirAlertService getAirAlertService() { return airAlertService; }
    
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
        if (relayController != null) relayController.close(); 
        scheduler.shutdown(); 
        stopBroadcastServers();
        if (audioService != null) audioService.stopAll();
    }

    public static void main(String[] args) {
        if (InstanceGuard.isAnotherInstanceRunning()) {
            System.out.println("Another instance is already running. Notifying it and exiting.");
            System.exit(0);
        }
        launch(args);
    }
}
