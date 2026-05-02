package com.schoolbell;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.service.ScheduleService;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CONFIG_FILE = "config.properties";

    // SVG Icons
    private static final String ICON_BELL = "M12,2A2,2 0 0,0 10,4A2,2 0 0,0 10,4.29C7.12,5.14 5,7.82 5,11V17L3,19V20H21V19L19,17V11C19,7.82 16.88,5.14 14,4.29C14,4.19 14,4.1 14,4A2,2 0 0,0 12,2M10,21A2,2 0 0,0 12,23A2,2 0 0,0 14,21H10Z";
    private static final String ICON_SIREN = "M12,8H15V9H12V8M12,10H15V11H12V10M12,12H15V13H12V12M12,14H15V15H12V14M18,3V21H16V3H18M11,3H9V21H11V3M8,3V21H6V3H8M21,3H19V21H21V3M5,3V21H3V3H5Z";
    private static final String ICON_ALERT = "M13,14H11V9H13M13,18H11V16H13M1,21H23L12,2L1,21Z";
    private static final String ICON_SETTINGS = "M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.35 19.43,11.03L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.97 19.05,5.05L16.56,5.9C16.04,5.5 15.48,5.19 14.88,4.97L14.5,2.33C14.46,2.1 14.26,1.92 14.03,1.92H10.03C9.8,1.92 9.6,2.1 9.57,2.33L9.18,4.97C8.58,5.19 8.02,5.5 7.5,5.9L5.01,5.05C4.79,4.97 4.52,5.05 4.4,5.27L2.4,8.73C2.28,8.95 2.33,9.22 2.52,9.37L4.63,11.03C4.59,11.35 4.56,11.67 4.56,12C4.56,12.33 4.59,12.65 4.63,12.97L2.52,14.63C2.33,14.78 2.28,15.05 2.4,15.27L4.4,18.73C4.52,18.95 4.79,19.03 5.01,18.95L7.5,18.1C8.02,18.5 8.58,18.81 9.18,19.03L9.57,21.67C9.6,21.9 9.8,22.08 10.03,22.08H14.03C14.26,22.08 14.46,21.9 14.5,21.67L14.88,19.03C15.48,18.81 16.04,18.5 16.56,18.1L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z";
    private static final String ICON_CALENDAR = "M19,19H5V8H19M16,1V3H8V1H6V3H5C3.89,3 3,3.9 3,5V19A2,2 0 0,0 5,21H19A2,2 0 0,0 21,19V5C21,3.89 20.1,3 19,3H18V1M17,12H12V17H17V12Z";
    private static final String ICON_SAVE = "M17,3L21,7V19A2,2 0 0,1 19,21H5C3.89,21 3,20.1 3,19V5A2,2 0 0,1 5,3H17M12,12A3,3 0 0,0 9,15A3,3 0 0,0 12,18A3,3 0 0,0 15,15A3,3 0 0,0 12,12M15,5H5V9H15V5Z";
    private static final String ICON_MUSIC = "M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14C6.04,14 6.55,14.12 7,14.34V4.53L21,2V3Z";
    private static final String ICON_FOLDER = "M10,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V8C22,6.89 21.1,6 20,6H12L10,4Z";

    private final RelayController relayController = new RelayController();
    private final ScheduleService scheduleService = new ScheduleService();
    private List<BellEntry> schedule = Collections.emptyList();
    private List<DaySchedule> internalSchedules = new ArrayList<>();
    private String selectedScheduleName;

    // Durations
    private int regularBellDuration = 5;
    private int airRaidRingDuration = 3;
    private int airRaidPauseDuration = 1;
    private int emergencyDuration = 12;

    // Audio
    private String audioAirRaidPath = "";
    private boolean isAudioAirRaidEnabled = false;
    private String audioEmergencyPath = "";
    private boolean isAudioEmergencyEnabled = false;
    private String audioSilencePath = "";
    private boolean isAudioSilenceEnabled = false;
    private String selectedAudioDeviceName = "Системний за замовчуванням";

    private Label currentTimeLabel;
    private Label syncStatusLabel;
    private Label relayStatusLabel;
    private Circle relayIndicator;
    private Label countdownLabel;
    private Label nextBellTypeLabel;
    private VBox logContainer;
    private ScrollPane logScrollPane;
    private ComboBox<String> mainScheduleSelector;
    private GridPane quickViewGrid;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isActionInProgress = false;
    private MediaPlayer currentPlayer;

    private static final String DEPTH_1 = "-fx-background-color: #f1f2f6;";
    private static final String DEPTH_2 = "-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);";
    private static final String DEPTH_3 = "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 8, 0, 0, 2);";

    private static final String HEADER_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6; -fx-letter-spacing: 1.5px;";
    private static final String VALUE_STYLE = "-fx-font-size: 26px; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #2d3436;";
    private static final String BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;";
    private static final String COMBO_STYLE = "-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SchoolBell Dashboard v3.9");
        syncStatusLabel = new Label("ПЕРЕВІРКА NTP...");
        syncStatusLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d;");
        VBox timeBox = new VBox(2, new Label("ПОТОЧНИЙ ЧАС"), currentTimeLabel = new Label("00:00:00"), syncStatusLabel);
        ((Label)timeBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        currentTimeLabel.setStyle(VALUE_STYLE);
        relayIndicator = new Circle(6);
        relayIndicator.setFill(Color.web("#e17055"));
        relayStatusLabel = new Label("НЕМАЄ ЗВ'ЯЗКУ");
        relayStatusLabel.setStyle(VALUE_STYLE + "-fx-text-fill: #e17055;");
        HBox relayStatusBox = new HBox(10, relayIndicator, relayStatusLabel);
        relayStatusBox.setAlignment(Pos.CENTER_LEFT);
        VBox relayBox = new VBox(5, new Label("СТАТУС РЕЛЕ"), relayStatusBox);
        ((Label)relayBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        HBox topBar = new HBox(20, timeBox, new Region(), relayBox);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle(DEPTH_2);
        VBox scheduleBox = new VBox(12);
        scheduleBox.setPadding(new Insets(20));
        scheduleBox.setStyle(DEPTH_2);
        Label schHeader = new Label("АКТИВНИЙ РОЗКЛАД");
        schHeader.setStyle(HEADER_STYLE);
        mainScheduleSelector = new ComboBox<>();
        mainScheduleSelector.setMaxWidth(Double.MAX_VALUE);
        mainScheduleSelector.setStyle(COMBO_STYLE);
        applyHoverEffect(mainScheduleSelector);
        mainScheduleSelector.setOnAction(e -> { selectedScheduleName = mainScheduleSelector.getValue(); saveConfig(); reloadSchedule(); });
        quickViewGrid = new GridPane();
        quickViewGrid.setHgap(15); quickViewGrid.setVgap(5);
        quickViewGrid.setPadding(new Insets(10, 0, 0, 0));
        Label qvHeader = new Label("ДЕТАЛІ ДНЯ");
        qvHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");
        scheduleBox.getChildren().addAll(schHeader, mainScheduleSelector, qvHeader, quickViewGrid);
        VBox countdownBox = new VBox(15);
        countdownBox.setPadding(new Insets(20));
        countdownBox.setStyle(DEPTH_2);
        countdownBox.setAlignment(Pos.CENTER);
        Label cdHeader = new Label("ДО НАСТУПНОГО ДЗВІНКА");
        cdHeader.setStyle(HEADER_STYLE);
        countdownLabel = new Label("00:00:00");
        countdownLabel.setStyle("-fx-font-size: 46px; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #0984e3;");
        nextBellTypeLabel = new Label("(очікування)");
        nextBellTypeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #636e72;");
        countdownBox.getChildren().addAll(cdHeader, countdownLabel, nextBellTypeLabel);
        HBox middleSection = new HBox(25, scheduleBox, countdownBox);
        HBox.setHgrow(scheduleBox, Priority.ALWAYS);
        HBox.setHgrow(countdownBox, Priority.ALWAYS);
        VBox logBox = new VBox(8);
        Label logHeader = new Label("МОНІТОР ПОДІЙ");
        logHeader.setStyle(HEADER_STYLE);
        logContainer = new VBox(1);
        logContainer.setPadding(new Insets(10));
        logContainer.setStyle("-fx-background-color: #2d3436;");
        logScrollPane = new ScrollPane(logContainer);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setPrefHeight(250);
        logScrollPane.setStyle("-fx-background: #2d3436; -fx-background-color: #2d3436; -fx-background-radius: 10;");
        logBox.getChildren().addAll(logHeader, logScrollPane);
        VBox controlCenter = new VBox(15);
        controlCenter.setPadding(new Insets(20));
        controlCenter.setStyle(DEPTH_2);
        Label ccHeader = new Label("ЦЕНТР КЕРУВАННЯ");
        ccHeader.setStyle(HEADER_STYLE);
        Button airRaidBtn = createBigButton("ПОВІТРЯНА\nТРИВОГА", "#fdb827", ICON_SIREN);
        airRaidBtn.setPrefSize(230, 75);
        airRaidBtn.setOnAction(e -> runAirRaidSignal());
        Button emergencyBtn = createBigButton("НАДЗВИЧАЙНА\nСИТУАЦІЯ", "#e74c3c", ICON_ALERT);
        emergencyBtn.setPrefSize(230, 75);
        emergencyBtn.setOnAction(e -> runEmergencySignal());
        Button settingsBtn = createBigButton("НАЛАШТУВАННЯ\nСИСТЕМИ", "#636e72", ICON_SETTINGS);
        settingsBtn.setPrefSize(230, 75);
        settingsBtn.setOnAction(e -> showSettingsDialog(primaryStage));
        Button scheduleEditorBtn = createBigButton("НАЛАШТУВАННЯ\nРОЗКЛАДУ", "#0984e3", ICON_CALENDAR);
        scheduleEditorBtn.setPrefSize(230, 75);
        scheduleEditorBtn.setOnAction(e -> showScheduleEditorDialog(primaryStage));
        HBox buttonBar = new HBox(15, airRaidBtn, emergencyBtn, settingsBtn, scheduleEditorBtn);
        buttonBar.setAlignment(Pos.CENTER);
        controlCenter.getChildren().addAll(ccHeader, buttonBar);
        VBox mainLayout = new VBox(25, topBar, middleSection, logBox, controlCenter);
        mainLayout.setPadding(new Insets(25, 30, 25, 30));
        mainLayout.setStyle(DEPTH_1);
        primaryStage.setScene(new Scene(mainLayout, 1050, 880));
        primaryStage.setResizable(false);
        primaryStage.show();
        startPulsingIndicator();
        relayController.scanDevices();
        relayController.connect();
        loadConfig();
        internalSchedules = scheduleService.loadInternalSchedules();
        refreshScheduleOptions();
        syncStatusLabel.setText("○ NTP ВИМКНЕНО (час локальний)");
        startScheduler();
        addLog("Система готова до роботи (NTP вимкнено).", "SUCCESS");
    }

    private void applyHoverEffect(javafx.scene.control.Control control) {
        control.setOnMouseEntered(e -> { control.setScaleX(1.02); control.setScaleY(1.02); control.setOpacity(0.9); });
        control.setOnMouseExited(e -> { control.setScaleX(1.0); control.setScaleY(1.0); control.setOpacity(1.0); });
    }

    private void startPulsingIndicator() {
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(relayIndicator.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.8), new KeyValue(relayIndicator.opacityProperty(), 0.3)),
            new KeyFrame(Duration.seconds(1.6), new KeyValue(relayIndicator.opacityProperty(), 1.0))
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    private Node createSVGIcon(String pathData, Color color, double size) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setFill(color);
        double scale = size / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }

    private void addLog(String message, String level) {
        Platform.runLater(() -> {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            Text ts = new Text("[" + timestamp + "] ");
            ts.setFill(Color.web("#95a5a6"));
            ts.setFont(Font.font("Monospaced", 13));
            Color msgColor = switch (level.toUpperCase()) {
                case "SUCCESS" -> Color.web("#2ecc71");
                case "ERROR" -> Color.web("#e17055");
                case "WARNING" -> Color.web("#f1c40f");
                default -> Color.web("#dfe6e9");
            };
            Text msg = new Text(message);
            msg.setFont(Font.font("Monospaced", 14));
            msg.setFill(msgColor);
            logContainer.getChildren().add(new TextFlow(ts, msg));
            Platform.runLater(() -> logScrollPane.setVvalue(1.0));
            if (logContainer.getChildren().size() > 50) logContainer.getChildren().remove(0);
        });
    }

    private Button createBigButton(String text, String color, String svgPath) {
        Button b = new Button();
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-alignment: center;");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        HBox content = new HBox(15, createSVGIcon(svgPath, Color.WHITE, 24), label);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 20, 0, 20));
        HBox.setHgrow(label, Priority.ALWAYS);
        b.setGraphic(content);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        String baseStyle = BTN_BASE + "-fx-background-color: " + color + "; " + DEPTH_3;
        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> {
            b.setStyle(baseStyle + "-fx-background-color: " + Color.web(color).deriveColor(0, 1, 1.2, 1).toString().replace("0x", "#") + ";");
            b.setScaleX(1.03); b.setScaleY(1.03);
        });
        b.setOnMouseExited(e -> { b.setStyle(baseStyle); b.setScaleX(1.0); b.setScaleY(1.0); });
        return b;
    }

    private void playAudioFile(String path) {
        if (path == null || path.isEmpty()) return;
        new Thread(() -> {
            try {
                File file = new File(path);
                if (!file.exists()) return;
                Mixer.Info selectedMixerInfo = null;
                if (!"Системний за замовчуванням".equals(selectedAudioDeviceName)) {
                    for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                        if (info.getName().equals(selectedAudioDeviceName)) { selectedMixerInfo = info; break; }
                    }
                }
                if (path.toLowerCase().endsWith(".wav") && selectedMixerInfo != null) {
                    try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
                        Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
                        try (SourceDataLine line = (SourceDataLine) mixer.getLine(info)) {
                            line.open(ais.getFormat()); line.start(); byte[] buffer = new byte[4096]; int read;
                            while ((read = ais.read(buffer)) != -1) { line.write(buffer, 0, read); }
                            line.drain(); Platform.runLater(() -> addLog("Відтворено (WAV): " + file.getName(), "SUCCESS")); return;
                        }
                    } catch (Exception ex) { logger.warn("Fallback for " + file.getName()); }
                }
                Platform.runLater(() -> {
                    try {
                        Media media = new Media(file.toURI().toString());
                        if (currentPlayer != null) currentPlayer.stop();
                        currentPlayer = new MediaPlayer(media); currentPlayer.play();
                        addLog("Відтворення (Системний): " + file.getName(), "SUCCESS");
                    } catch (Exception e) { logger.error("Media player failed", e); }
                });
            } catch (Exception e) { logger.error("Audio playback error", e); }
        }).start();
    }

    private void reloadSchedule() {
        if (selectedScheduleName == null) return;
        internalSchedules.stream().filter(ds -> ds.getName().equals(selectedScheduleName)).findFirst().ifPresent(ds -> {
            schedule = scheduleService.convertToBellEntries(ds);
            Platform.runLater(() -> {
                quickViewGrid.getChildren().clear();
                for (int i = 0; i < ds.getLessons().size(); i++) {
                    DaySchedule.LessonInfo li = ds.getLessons().get(i);
                    if (li.start != null && li.end != null) {
                        Label l = new Label((i + 1) + ". " + li.start + " - " + li.end);
                        l.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-font-size: 13px;");
                        l.setPadding(new Insets(3, 5, 3, 5));
                        quickViewGrid.add(l, i / 4, i % 4);
                    }
                }
            });
            addLog("Розклад активовано: " + selectedScheduleName, "INFO");
        });
    }

    private void updateUI() {
        LocalTime now = LocalTime.now();
        currentTimeLabel.setText(now.format(HH_MM_SS));
        if (relayController.isConnected()) {
            relayStatusLabel.setText("Підключено");
            relayStatusLabel.setStyle(VALUE_STYLE + "-fx-text-fill: #2ecc71;");
            relayIndicator.setFill(Color.web("#2ecc71"));
        } else {
            relayStatusLabel.setText("Немає зв'язку");
            relayStatusLabel.setStyle(VALUE_STYLE + "-fx-text-fill: #e17055;");
            relayIndicator.setFill(Color.web("#e17055"));
        }
        if (isActionInProgress) return;
        schedule.stream().filter(entry -> entry.time().isAfter(now)).findFirst().ifPresentOrElse(entry -> {
            nextBellTypeLabel.setText("(" + entry.type() + ")");
            java.time.Duration d = java.time.Duration.between(now, entry.time());
            countdownLabel.setText(String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart()));
        }, () -> { countdownLabel.setText("--:--:--"); nextBellTypeLabel.setText("(на сьогодні все)"); });
    }

    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            if (now.getHour() == 9 && now.getMinute() == 0 && now.getSecond() == 0) {
                if (isAudioSilenceEnabled) playAudioFile(audioSilencePath);
            }
            if (now.getSecond() == 0 && !isActionInProgress) {
                LocalTime minuteOnly = now.truncatedTo(ChronoUnit.MINUTES);
                schedule.stream().filter(entry -> entry.time().equals(minuteOnly)).findFirst().ifPresent(entry -> {
                    new Thread(() -> {
                        isActionInProgress = true;
                        try {
                            addLog("🔔 Автодзвінок: " + entry.type(), "SUCCESS");
                            relayController.turnOn(); Thread.sleep(regularBellDuration * 1000L); relayController.turnOff();
                        } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
                    }).start();
                });
            }
            Platform.runLater(this::updateUI);
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void runAirRaidSignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true; addLog("ЗАПУСК СИГНАЛУ: ПОВІТРЯНА ТРИВОГА", "WARNING");
            try { 
                for (int i = 1; i <= 3; i++) { 
                    relayController.turnOn(); Thread.sleep(airRaidRingDuration * 1000L); 
                    relayController.turnOff(); if (i < 3) Thread.sleep(airRaidPauseDuration * 1000L); 
                }
                if (isAudioAirRaidEnabled) { Thread.sleep(1500); playAudioFile(audioAirRaidPath); }
                addLog("Сигнал тривоги завершено.", "SUCCESS");
            } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
        }).start();
    }

    private void runEmergencySignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true; addLog("ЗАПУСК СИГНАЛУ: НАДЗВИЧАЙНА СИТУАЦІЯ", "ERROR");
            try { 
                relayController.turnOn(); Thread.sleep(emergencyDuration * 1000L); relayController.turnOff(); 
                if (isAudioEmergencyEnabled) { Thread.sleep(1500); playAudioFile(audioEmergencyPath); }
                addLog("Сигнал НС завершено.", "SUCCESS");
            } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
        }).start();
    }

    private void showSettingsDialog(Stage owner) {
        Stage dialog = new Stage(); dialog.initModality(Modality.WINDOW_MODAL); dialog.initOwner(owner); dialog.setTitle("Конфігурація системи");
        VBox root = new VBox(15); root.setPadding(new Insets(20)); root.setStyle("-fx-background-color: #f1f2f6;");
        TabPane tabPane = new TabPane();
        tabPane.getStylesheets().add("data:text/css," + 
            ".tab-pane { -fx-focus-color: transparent; -fx-faint-focus-color: transparent; }" +
            ".tab-pane .tab-header-area .tab-header-background { -fx-opacity: 0; }" +
            ".tab { -fx-background-color: #e1e2e1; -fx-background-radius: 10 10 0 0; -fx-padding: 10 35; }" +
            ".tab:selected { -fx-background-color: white; -fx-background-insets: 0; }" +
            ".tab .tab-label { -fx-text-fill: #636e72; -fx-font-weight: bold; -fx-font-size: 13px; }" +
            ".tab:selected .tab-label { -fx-text-fill: #2d3436; }" +
            ".tab:focused .tab-label { -fx-focus-color: transparent; }");
        
        Tab relayTab = new Tab("🔔 СИГНАЛИ РЕЛЕ"); relayTab.setClosable(false);
        VBox relayContent = new VBox(15); relayContent.setPadding(new Insets(15)); relayContent.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        VBox sec1 = createSettingsSection("СТАНДАРТНИЙ ДЗВІНОК", "#0984e3", ICON_BELL);
        TextField regField = createStyledField(String.valueOf(regularBellDuration));
        HBox prev1 = new HBox(); prev1.setAlignment(Pos.CENTER); updatePreview(prev1, List.of(regularBellDuration), 0, "#0984e3");
        regField.textProperty().addListener((o, ov, nv) -> updatePreview(prev1, parseSafe(nv), 0, "#0984e3"));
        sec1.getChildren().addAll(createFieldRow("Тривалість (сек):", regField), prev1);
        VBox sec2 = createSettingsSection("ПОВІТРЯНА ТРИВОГА", "#f39c12", ICON_SIREN);
        TextField arRingField = createStyledField(String.valueOf(airRaidRingDuration));
        TextField arPauseField = createStyledField(String.valueOf(airRaidPauseDuration));
        HBox prev2 = new HBox(); prev2.setAlignment(Pos.CENTER); updatePreview(prev2, List.of(airRaidRingDuration, airRaidRingDuration, airRaidRingDuration), airRaidPauseDuration, "#f39c12");
        arRingField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(nv).get(0), parseSafe(nv).get(0), parseSafe(nv).get(0)), parseSafe(arPauseField.getText()).get(0), "#f39c12"));
        arPauseField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0)), parseSafe(nv).get(0), "#f39c12"));
        sec2.getChildren().addAll(createFieldRow("Сигнал (сек):", arRingField), createFieldRow("Пауза (сек):", arPauseField), prev2);
        VBox sec3 = createSettingsSection("НАДЗВИЧАЙНА СИТУАЦІЯ", "#d63031", ICON_ALERT);
        TextField emField = createStyledField(String.valueOf(emergencyDuration));
        HBox prev3 = new HBox(); prev3.setAlignment(Pos.CENTER); updatePreview(prev3, List.of(emergencyDuration), 0, "#d63031");
        emField.textProperty().addListener((o, ov, nv) -> updatePreview(prev3, parseSafe(nv), 0, "#d63031"));
        sec3.getChildren().addAll(createFieldRow("Тривалість (сек):", emField), prev3);
        relayContent.getChildren().addAll(sec1, sec2, sec3); 
        relayTab.setContent(relayContent);

        Tab notifyTab = new Tab("🔊 СПОВІЩЕННЯ"); notifyTab.setClosable(false);
        VBox notifyContent = new VBox(15); notifyContent.setPadding(new Insets(15)); notifyContent.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 15 15;");
        VBox deviceSec = createSettingsSection("АУДІО ВИХІД", "#00b894", ICON_SETTINGS);
        ComboBox<String> deviceCombo = new ComboBox<>(); deviceCombo.getItems().add("Системний за замовчуванням");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (AudioSystem.getMixer(info).isLineSupported(new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false)))) { deviceCombo.getItems().add(info.getName()); }
        }
        deviceCombo.setValue(selectedAudioDeviceName); deviceCombo.setMaxWidth(Double.MAX_VALUE); deviceCombo.setStyle(COMBO_STYLE);
        applyHoverEffect(deviceCombo);
        deviceSec.getChildren().add(deviceCombo);
        VBox audioSec = createSettingsSection("ФАЙЛИ СПОВІЩЕНЬ", "#6c5ce7", ICON_MUSIC);
        VBox arBox = createAudioFileRow("Повітряна тривога", audioAirRaidPath, path -> audioAirRaidPath = path, isAudioAirRaidEnabled, val -> isAudioAirRaidEnabled = val, dialog);
        VBox emBox = createAudioFileRow("Надзвичайна ситуація", audioEmergencyPath, path -> audioEmergencyPath = path, isAudioEmergencyEnabled, val -> isAudioEmergencyEnabled = val, dialog);
        VBox siBox = createAudioFileRow("Хвилина мовчання (09:00)", audioSilencePath, path -> audioSilencePath = path, isAudioSilenceEnabled, val -> isAudioSilenceEnabled = val, dialog);
        audioSec.getChildren().addAll(arBox, emBox, siBox);
        notifyContent.getChildren().addAll(deviceSec, audioSec); 
        notifyTab.setContent(notifyContent);

        tabPane.getTabs().addAll(relayTab, notifyTab);
        Button saveBtn = new Button("ЗБЕРЕГТИ ВСІ ЗМІНИ"); saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 18)); saveBtn.setGraphicTextGap(10);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 60; -fx-background-radius: 12;");
        saveBtn.setOnAction(e -> { try {
            regularBellDuration = Integer.parseInt(regField.getText()); airRaidRingDuration = Integer.parseInt(arRingField.getText()); airRaidPauseDuration = Integer.parseInt(arPauseField.getText()); emergencyDuration = Integer.parseInt(emField.getText());
            isAudioAirRaidEnabled = ((CheckBox)arBox.getChildren().get(0)).isSelected(); isAudioEmergencyEnabled = ((CheckBox)emBox.getChildren().get(0)).isSelected(); isAudioSilenceEnabled = ((CheckBox)siBox.getChildren().get(0)).isSelected();
            selectedAudioDeviceName = deviceCombo.getValue(); saveConfig(); addLog("Налаштування оновлено.", "SUCCESS"); dialog.close();
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Помилка даних!").show(); } });
        
        HBox footer = new HBox(saveBtn); footer.setAlignment(Pos.CENTER);
        root.getChildren().addAll(tabPane, footer); dialog.setScene(new Scene(root, 650, 920)); dialog.showAndWait();
    }

    private VBox createAudioFileRow(String labelText, String currentPath, java.util.function.Consumer<String> onPathSelected, boolean enabled, java.util.function.Consumer<Boolean> onEnabledToggled, Stage owner) {
        CheckBox cb = new CheckBox(labelText); cb.setSelected(enabled);
        TextField pathF = new TextField(currentPath); pathF.setEditable(false); pathF.setPromptText("Файл не обрано");
        pathF.setStyle("-fx-font-size: 12px; -fx-background-color: #f8f9fa; -fx-background-insets: 0; -fx-border-color: #dfe6e9; -fx-border-radius: 8 0 0 8; -fx-background-radius: 8 0 0 8; -fx-padding: 8; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        HBox.setHgrow(pathF, Priority.ALWAYS);
        Button browseBtn = new Button("ОБРАТИ ФАЙЛ");
        browseBtn.setGraphic(createSVGIcon(ICON_FOLDER, Color.WHITE, 16));
        browseBtn.setGraphicTextGap(8);
        browseBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 0 8 8 0; -fx-padding: 8 20;");
        browseBtn.setOnAction(e -> { File f = new FileChooser().showOpenDialog(owner); if (f != null) { pathF.setText(f.getAbsolutePath()); onPathSelected.accept(f.getAbsolutePath()); } });
        HBox row = new HBox(0, pathF, browseBtn); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(0, 0, 5, 25));
        return new VBox(5, cb, row);
    }

    private void updatePreview(HBox box, List<Integer> rings, int pause, String hexColor) {
        box.getChildren().clear(); box.setPadding(new Insets(5, 0, 5, 0)); double scale = 25.0;
        for (int i = 0; i < rings.size(); i++) {
            int d = rings.get(i);
            if (d > 0) {
                VBox block = new VBox(3); block.setAlignment(Pos.CENTER);
                Rectangle r = new Rectangle(Math.max(25, d * scale), 48); r.setArcWidth(12); r.setArcHeight(12); r.setFill(Color.web(hexColor));
                Label l = new Label(d + "с"); l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
                block.getChildren().addAll(r, l); box.getChildren().add(block);
            }
            if (i < rings.size() - 1 && pause > 0) {
                VBox pBlock = new VBox(3); pBlock.setAlignment(Pos.CENTER);
                Rectangle p = new Rectangle(Math.max(15, pause * scale), 48); p.setFill(Color.web("#f1f2f6"));
                p.setStroke(Color.web("#bdc3c7")); p.getStrokeDashArray().addAll(5.0, 5.0);
                Label l = new Label(pause + "с"); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #bdc3c7;");
                pBlock.getChildren().addAll(p, l); box.getChildren().add(pBlock);
            }
        }
    }

    private VBox createSettingsSection(String title, String color, String svgPath) {
        VBox v = new VBox(8); v.setPadding(new Insets(12)); v.setStyle(DEPTH_2);
        HBox header = new HBox(12, createSVGIcon(svgPath, Color.web(color), 18), new Label(title));
        header.setAlignment(Pos.CENTER_LEFT); ((Label)header.getChildren().get(1)).setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        v.getChildren().add(header); return v;
    }

    private HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field); HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    private TextField createStyledField(String val) {
        TextField f = new TextField(val); f.setPrefWidth(70); f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 6; -fx-border-color: #dfe6e9; -fx-border-radius: 6; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 4 7;"); return f;
    }

    private ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>(); for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current)); cb.setPrefWidth(65); cb.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 5; -fx-border-color: #dfe6e9; -fx-border-radius: 5;"); return cb;
    }

    private List<Integer> parseSafe(String val) { try { return List.of(Math.max(0, Integer.parseInt(val))); } catch (Exception e) { return List.of(0); } }

    private void showScheduleEditorDialog(Stage owner) {
        Stage dialog = new Stage(); dialog.initModality(Modality.WINDOW_MODAL); dialog.initOwner(owner); dialog.setTitle("Редактор розкладів");
        VBox root = new VBox(20); root.setPadding(new Insets(25)); root.setStyle("-fx-background-color: #f1f2f6;");
        Label mainTitle = new Label("КЕРУВАННЯ РОЗКЛАДАМИ");
        mainTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-letter-spacing: 2px;");
        VBox titleBox = new VBox(5, mainTitle, new Separator()); titleBox.setAlignment(Pos.CENTER);
        ComboBox<String> scheduleSelector = new ComboBox<>();
        internalSchedules.forEach(ds -> scheduleSelector.getItems().add(ds.getName()));
        scheduleSelector.setPromptText("Оберіть розклад для редагування");
        scheduleSelector.setMaxWidth(Double.MAX_VALUE); 
        scheduleSelector.setStyle(COMBO_STYLE);
        applyHoverEffect(scheduleSelector);
        VBox editorContainer = new VBox(0);
        editorContainer.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #dcdde1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        Button saveBtn = new Button("ЗБЕРЕГТИ РОЗКЛАД");
        saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 16));
        saveBtn.setGraphicTextGap(8);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 60; -fx-background-radius: 12;");
        saveBtn.setDisable(true); applyHoverEffect(saveBtn);
        Button addBtn = new Button("Додати новий"); addBtn.setStyle("-fx-background-radius: 10;"); applyHoverEffect(addBtn);
        addBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(); tid.setTitle("Новий розклад"); tid.setHeaderText("Введіть назву:");
            tid.showAndWait().ifPresent(name -> { if (internalSchedules.stream().noneMatch(ds -> ds.getName().equalsIgnoreCase(name))) {
                internalSchedules.add(new DaySchedule(name)); scheduleSelector.getItems().add(name); scheduleSelector.setValue(name);
            }});
        });
        Button renameBtn = new Button("Перейменувати"); renameBtn.setStyle("-fx-background-radius: 10;"); renameBtn.setDisable(true); applyHoverEffect(renameBtn);
        renameBtn.setOnAction(e -> {
            String curr = scheduleSelector.getValue(); if (curr == null) return;
            TextInputDialog tid = new TextInputDialog(curr); tid.setTitle("Перейменування"); tid.setHeaderText("Нова назва:");
            tid.showAndWait().ifPresent(newName -> { if (!newName.equals(curr) && internalSchedules.stream().noneMatch(ds -> ds.getName().equalsIgnoreCase(newName))) {
                internalSchedules.stream().filter(d -> d.getName().equals(curr)).findFirst().ifPresent(d -> d.setName(newName));
                int idx = scheduleSelector.getItems().indexOf(curr); scheduleSelector.getItems().set(idx, newName); scheduleSelector.setValue(newName);
                scheduleService.saveInternalSchedules(internalSchedules); refreshScheduleOptions();
            }});
        });
        Button deleteBtn = new Button("Видалити"); deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
        deleteBtn.setDisable(true); applyHoverEffect(deleteBtn);
        deleteBtn.setOnAction(e -> {
            String name = scheduleSelector.getValue(); if (name != null) {
                new Alert(Alert.AlertType.CONFIRMATION, "Видалити '" + name + "'?", ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) { internalSchedules.removeIf(ds -> ds.getName().equals(name)); scheduleSelector.getItems().remove(name); scheduleService.saveInternalSchedules(internalSchedules); refreshScheduleOptions(); }
                });
            }
        });
        scheduleSelector.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) { saveBtn.setDisable(true); renameBtn.setDisable(true); deleteBtn.setDisable(true); return; }
            saveBtn.setDisable(false); renameBtn.setDisable(false); deleteBtn.setDisable(false);
            editorContainer.getChildren().clear(); DaySchedule ds = internalSchedules.stream().filter(d -> d.getName().equals(newV)).findFirst().get();
            HBox header = new HBox(10, new Label("УРОК"), new Label("ПОЧАТОК"), new Region(), new Label("КІНЕЦЬ"));
            header.setPadding(new Insets(15, 20, 15, 20)); header.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15 15 0 0; -fx-border-color: transparent transparent #dcdde1 transparent;");
            ((Label)header.getChildren().get(0)).setStyle(HEADER_STYLE); ((Label)header.getChildren().get(0)).setPrefWidth(60);
            ((Label)header.getChildren().get(1)).setStyle(HEADER_STYLE); ((Label)header.getChildren().get(3)).setStyle(HEADER_STYLE);
            HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS); editorContainer.getChildren().add(header);
            List<ComboBox<String>> startHs = new ArrayList<>(); List<ComboBox<String>> startMs = new ArrayList<>();
            List<ComboBox<String>> endHs = new ArrayList<>(); List<ComboBox<String>> endMs = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                final int curIdx = i; DaySchedule.LessonInfo li = ds.getLessons().get(i);
                ComboBox<String> sh = createTimeCombo(24, li.start != null ? li.start.getHour() : 8);
                ComboBox<String> sm = createTimeCombo(60, li.start != null ? li.start.getMinute() : 0);
                ComboBox<String> eh = createTimeCombo(24, li.end != null ? li.end.getHour() : 8);
                ComboBox<String> em = createTimeCombo(60, li.end != null ? li.end.getMinute() : 45);
                startHs.add(sh); startMs.add(sm); endHs.add(eh); endMs.add(em);
                Label lessonNum = new Label((i + 1) + " урок"); lessonNum.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;"); lessonNum.setPrefWidth(60);
                HBox lessonRow = new HBox(8, lessonNum, sh, new Label(":"), sm, new Region(), eh, new Label(":"), em);
                lessonRow.setAlignment(Pos.CENTER_LEFT); lessonRow.setPadding(new Insets(10, 20, 10, 20));
                if (i % 2 != 0) lessonRow.setStyle("-fx-background-color: #fafafa;");
                HBox.setHgrow(lessonRow.getChildren().get(4), Priority.ALWAYS);
                lessonRow.setOnMouseEntered(ev -> lessonRow.setStyle("-fx-background-color: #f1f2f6;"));
                lessonRow.setOnMouseExited(ev -> lessonRow.setStyle(curIdx % 2 != 0 ? "-fx-background-color: #fafafa;" : "-fx-background-color: white;"));
                editorContainer.getChildren().add(lessonRow);
                if (i < 6) {
                    TextField breakF = new TextField(String.valueOf(li.breakAfterMinutes)); breakF.setPrefWidth(45); breakF.setAlignment(Pos.CENTER);
                    breakF.setStyle("-fx-font-size: 11px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 10; -fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 2 5;");
                    Label bLabel = new Label("перерва"); bLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #0984e3; -fx-font-weight: bold;");
                    HBox breakRow = new HBox(8, new Region(), bLabel, breakF, new Label("хв"), new Region());
                    breakRow.setAlignment(Pos.CENTER); breakRow.setPadding(new Insets(2, 0, 2, 0)); breakRow.setStyle("-fx-background-color: rgba(9, 132, 227, 0.05);");
                    editorContainer.getChildren().add(breakRow);
                    Runnable upd = () -> { try {
                        LocalTime next = LocalTime.of(Integer.parseInt(eh.getValue()), Integer.parseInt(em.getValue())).plusMinutes(Integer.parseInt(breakF.getText()));
                        startHs.get(curIdx + 1).setValue(String.format("%02d", next.getHour())); startMs.get(curIdx + 1).setValue(String.format("%02d", next.getMinute()));
                    } catch (Exception ex) {} };
                    eh.valueProperty().addListener((o, ov, nv) -> upd.run()); em.valueProperty().addListener((o, ov, nv) -> upd.run()); breakF.textProperty().addListener((o, ov, nv) -> upd.run());
                }
            }
        });
        saveBtn.setOnAction(e -> {
            String name = scheduleSelector.getValue(); DaySchedule ds = internalSchedules.stream().filter(d -> d.getName().equals(name)).findFirst().get();
            List<DaySchedule.LessonInfo> newLessons = new ArrayList<>();
            try {
                int rowIdx = 1;
                for (int i = 0; i < 7; i++) {
                    HBox lRow = (HBox) editorContainer.getChildren().get(rowIdx);
                    ComboBox<String> sh = (ComboBox<String>) lRow.getChildren().get(1); ComboBox<String> sm = (ComboBox<String>) lRow.getChildren().get(3);
                    ComboBox<String> eh = (ComboBox<String>) lRow.getChildren().get(5); ComboBox<String> em = (ComboBox<String>) lRow.getChildren().get(7);
                    int bVal = 0; if (i < 6) { bVal = Integer.parseInt(((TextField)((HBox)editorContainer.getChildren().get(rowIdx+1)).getChildren().get(2)).getText()); }
                    newLessons.add(new DaySchedule.LessonInfo(LocalTime.of(Integer.parseInt(sh.getValue()), Integer.parseInt(sm.getValue())), LocalTime.of(Integer.parseInt(eh.getValue()), Integer.parseInt(em.getValue())), bVal));
                    rowIdx += 2;
                }
                ds.setLessons(newLessons); scheduleService.saveInternalSchedules(internalSchedules); refreshScheduleOptions();
                addLog("Розклад '" + name + "' збережено.", "SUCCESS"); dialog.close();
            } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Помилка збереження!").show(); }
        });
        HBox actionButtons = new HBox(12, addBtn, renameBtn, new Region(), deleteBtn); HBox.setHgrow(actionButtons.getChildren().get(2), Priority.ALWAYS);
        root.getChildren().addAll(titleBox, new VBox(15, scheduleSelector, actionButtons), editorContainer, saveBtn); root.setAlignment(Pos.CENTER);
        dialog.setScene(new Scene(root, 620, 920)); dialog.showAndWait();
    }

    private void saveConfig() {
        Properties props = new Properties(); if (selectedScheduleName != null) props.setProperty("selectedSchedule", selectedScheduleName);
        props.setProperty("dur.regular", String.valueOf(regularBellDuration)); props.setProperty("dur.arRing", String.valueOf(airRaidRingDuration));
        props.setProperty("dur.arPause", String.valueOf(airRaidPauseDuration)); props.setProperty("dur.emergency", String.valueOf(emergencyDuration));
        props.setProperty("audio.arPath", audioAirRaidPath); props.setProperty("audio.arEnabled", String.valueOf(isAudioAirRaidEnabled));
        props.setProperty("audio.emPath", audioEmergencyPath); props.setProperty("audio.emEnabled", String.valueOf(isAudioEmergencyEnabled));
        props.setProperty("audio.siPath", audioSilencePath); props.setProperty("audio.siEnabled", String.valueOf(isAudioSilenceEnabled));
        props.setProperty("audio.device", selectedAudioDeviceName);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) { props.store(out, "SchoolBell Config"); } catch (IOException e) { logger.error("Failed to save config", e); }
    }

    private void loadConfig() {
        Properties props = new Properties(); File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in); regularBellDuration = Integer.parseInt(props.getProperty("dur.regular", "5"));
                airRaidRingDuration = Integer.parseInt(props.getProperty("dur.arRing", "3")); airRaidPauseDuration = Integer.parseInt(props.getProperty("dur.arPause", "1"));
                emergencyDuration = Integer.parseInt(props.getProperty("dur.emergency", "12")); selectedScheduleName = props.getProperty("selectedSchedule");
                audioAirRaidPath = props.getProperty("audio.arPath", ""); isAudioAirRaidEnabled = Boolean.parseBoolean(props.getProperty("audio.arEnabled", "false"));
                audioEmergencyPath = props.getProperty("audio.emPath", ""); isAudioEmergencyEnabled = Boolean.parseBoolean(props.getProperty("audio.emEnabled", "false"));
                audioSilencePath = props.getProperty("audio.siPath", ""); isAudioSilenceEnabled = Boolean.parseBoolean(props.getProperty("audio.siEnabled", "false"));
                selectedAudioDeviceName = props.getProperty("audio.device", "Системний за замовчуванням");
            } catch (Exception e) { logger.error("Failed to load config", e); }
        }
    }

    private void refreshScheduleOptions() {
        Platform.runLater(() -> {
            mainScheduleSelector.getItems().clear(); for (DaySchedule ds : internalSchedules) mainScheduleSelector.getItems().add(ds.getName());
            if (selectedScheduleName != null && mainScheduleSelector.getItems().contains(selectedScheduleName)) mainScheduleSelector.setValue(selectedScheduleName);
            else if (!internalSchedules.isEmpty()) { mainScheduleSelector.setValue(internalSchedules.get(0).getName()); selectedScheduleName = internalSchedules.get(0).getName(); }
            reloadSchedule();
        });
    }

    @Override public void stop() { relayController.close(); scheduler.shutdown(); }
    public static void main(String[] args) { launch(args); }
}
