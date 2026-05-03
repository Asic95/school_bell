package com.schoolbell;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.service.AcademicService;
import com.schoolbell.service.DatabaseManager;
import com.schoolbell.service.ScheduleService;
import com.schoolbell.ui.ScheduleEditorDialog;
import com.schoolbell.ui.SystemConfigDialog;
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
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.schoolbell.ui.UIStyles.*;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CONFIG_FILE = "config.properties";

    private final RelayController relayController = new RelayController();
    private final ScheduleService scheduleService = new ScheduleService();
    private final AcademicService academicService = new AcademicService();
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

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.initialize();
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
        Button airRaidBtn = createBigButton("ПОВІТРЯНА\nТРИВОГА", "#fdb827", ICON_MEGAPHONE);
        airRaidBtn.setPrefSize(230, 75);
        airRaidBtn.setOnAction(e -> runAirRaidSignal());
        Button emergencyBtn = createBigButton("НАДЗВИЧАЙНА\nСИТУАЦІЯ", "#e74c3c", ICON_ALERT);
        emergencyBtn.setPrefSize(230, 75);
        emergencyBtn.setOnAction(e -> runEmergencySignal());
        Button settingsBtn = createBigButton("НАЛАШТУВАННЯ\nСИСТЕМИ", "#636e72", ICON_SETTINGS);
        settingsBtn.setPrefSize(230, 75);
        settingsBtn.setOnAction(e -> new SystemConfigDialog(this).show(primaryStage));
        Button scheduleEditorBtn = createBigButton("НАЛАШТУВАННЯ\nРОЗКЛАДУ", "#0984e3", ICON_CALENDAR);
        scheduleEditorBtn.setPrefSize(230, 75);
        scheduleEditorBtn.setOnAction(e -> new ScheduleEditorDialog(this).show(primaryStage));
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

    public void addLog(String message, String level) {
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
                        javafx.scene.media.Media media = new javafx.scene.media.Media(file.toURI().toString());
                        if (currentPlayer != null) currentPlayer.stop();
                        currentPlayer = new MediaPlayer(media); currentPlayer.play();
                        addLog("Відтворення (Системний): " + file.getName(), "SUCCESS");
                    } catch (Exception e) { logger.error("Media player failed", e); }
                });
            } catch (Exception e) { logger.error("Audio playback error", e); }
        }).start();
    }

    public void reloadSchedule() {
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

    public void saveConfig() {
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

    public void refreshScheduleOptions() {
        Platform.runLater(() -> {
            mainScheduleSelector.getItems().clear(); for (DaySchedule ds : internalSchedules) mainScheduleSelector.getItems().add(ds.getName());
            if (selectedScheduleName != null && mainScheduleSelector.getItems().contains(selectedScheduleName)) mainScheduleSelector.setValue(selectedScheduleName);
            else if (!internalSchedules.isEmpty()) { mainScheduleSelector.setValue(internalSchedules.get(0).getName()); selectedScheduleName = internalSchedules.get(0).getName(); }
            reloadSchedule();
        });
    }

    public ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>(); for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current)); cb.setPrefWidth(65); cb.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 5; -fx-border-color: #dfe6e9; -fx-border-radius: 5;"); return cb;
    }

    // Getters / Setters for Dialogs
    public AcademicService getAcademicService() { return academicService; }
    public ScheduleService getScheduleService() { return scheduleService; }
    public List<DaySchedule> getInternalSchedules() { return internalSchedules; }
    public int getRegularBellDuration() { return regularBellDuration; }
    public void setRegularBellDuration(int val) { this.regularBellDuration = val; }
    public int getAirRaidRingDuration() { return airRaidRingDuration; }
    public void setAirRaidRingDuration(int val) { this.airRaidRingDuration = val; }
    public int getAirRaidPauseDuration() { return airRaidPauseDuration; }
    public void setAirRaidPauseDuration(int val) { this.airRaidPauseDuration = val; }
    public int getEmergencyDuration() { return emergencyDuration; }
    public void setEmergencyDuration(int val) { this.emergencyDuration = val; }
    public String getAudioAirRaidPath() { return audioAirRaidPath; }
    public void setAudioAirRaidPath(String val) { this.audioAirRaidPath = val; }
    public boolean isAudioAirRaidEnabled() { return isAudioAirRaidEnabled; }
    public void setAudioAirRaidEnabled(boolean val) { this.isAudioAirRaidEnabled = val; }
    public String getAudioEmergencyPath() { return audioEmergencyPath; }
    public void setAudioEmergencyPath(String val) { this.audioEmergencyPath = val; }
    public boolean isAudioEmergencyEnabled() { return isAudioEmergencyEnabled; }
    public void setAudioEmergencyEnabled(boolean val) { this.isAudioEmergencyEnabled = val; }
    public String getAudioSilencePath() { return audioSilencePath; }
    public void setAudioSilencePath(String val) { this.audioSilencePath = val; }
    public boolean isAudioSilenceEnabled() { return isAudioSilenceEnabled; }
    public void setAudioSilenceEnabled(boolean val) { this.isAudioSilenceEnabled = val; }
    public String getSelectedAudioDeviceName() { return selectedAudioDeviceName; }
    public void setSelectedAudioDeviceName(String val) { this.selectedAudioDeviceName = val; }

    @Override public void stop() { relayController.close(); scheduler.shutdown(); }
    public static void main(String[] args) { launch(args); }
}
