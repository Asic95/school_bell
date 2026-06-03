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
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
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
    public static final String VERSION = "1.1.0";

    private static final String APP_TITLE = "SchoolBell v" + VERSION;

    // Services
    private RelayController relayController;
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

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage primaryStage) {
        java.util.Locale.setDefault(Locale.of("uk", "UA"));
        this.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        DatabaseManager.initialize();

        // Initialize Services
        configService = new ConfigService();
        configService.loadConfig();
        updateService = new UpdateService();
        updateService.setJournalConsumer(msg -> addLog(msg, "INFO"));
        relayController = new RelayController(this);
        audioService = new AudioService(configService);
        signalService = new SignalService(relayController, audioService, configService);
        signalService.setLogConsumer(msg -> journal.addLog(msg, "INFO"));
        systemService = new SystemService(configService);
        mediaSchedulerService = new MediaSchedulerService(this);
        airAlertService = new AirAlertService(this, configService, signalService, scheduler);
        if (configService.isAirRaidAutomationEnabled()) {
            airAlertService.start();
        }

        startBroadcastServers();
        InstanceGuard.startListener(primaryStage);

        primaryStage.setTitle(APP_TITLE);
        try {
            InputStream iconStream = getClass().getResourceAsStream("/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            logger.warn("Could not load application icon: " + e.getMessage());
        }

        // Tray Support
        new TrayManager(this, primaryStage).init();

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

        // Force refresh icons after a small delay to ensure Taskbar picks them up (fixes generic icon on boot)
        scheduler.schedule(() -> Platform.runLater(() -> {
            try {
                InputStream iconStream = getClass().getResourceAsStream("/icon.png");
                if (iconStream != null) {
                    Image icon = new Image(iconStream);
                    primaryStage.getIcons().setAll(icon);
                }
            } catch (Exception e) {
                logger.warn("Failed to refresh icon: " + e.getMessage());
            }
        }), 1500, TimeUnit.MILLISECONDS);

        // Initialize Views
        dashboardView = new DashboardView(this);
        schoolView = new SchoolView(this);
        scheduleView = new ScheduleView(this);
        efirView = new EfirView(this);
        notificationsView = new NotificationsView(this);
        systemView = new SystemView(this);
        importView = new ImportView(this);

        navigation.showDashboard();

        internalSchedules = scheduleService.loadInternalSchedules();
        refreshScheduleOptions();
        startScheduler();
        refreshCaches();
        
        // Schedule database cleanup (once every 24 hours)
        scheduler.scheduleAtFixedRate(DatabaseManager::cleanupOldData, 1, 24, TimeUnit.HOURS);

        // Check for updates
        scheduler.schedule(this::checkForUpdates, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 24, 24, TimeUnit.HOURS);

        logger.info("System ready.");
    }

    private void checkForUpdates() {
        updateService.checkForUpdates().thenAccept(manifest -> {
            if (manifest != null) {
                Platform.runLater(() -> {
                    new UpdateAvailableDialog(primaryStage, updateService, manifest).show();
                    logger.info("Update available: {}", manifest.latestVersion());
                });
            }
        });
    }

    public void checkForUpdatesManual(Runnable onFinish) {
        updateService.checkForUpdates().thenAccept(manifest -> {
            Platform.runLater(() -> {
                if (manifest != null) {
                    new UpdateAvailableDialog(primaryStage, updateService, manifest).show();
                } else {
                    ToastService.showInfo("У вас встановлена актуальна версія.");
                }
                if (onFinish != null) onFinish.run();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                ToastService.showError("Помилка перевірки оновлень.");
                if (onFinish != null) onFinish.run();
            });
            return null;
        });
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
        relayController.close(); 
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
