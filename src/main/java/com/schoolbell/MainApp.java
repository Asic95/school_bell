package com.schoolbell;

import com.schoolbell.hardware.RelayController;
import com.schoolbell.model.BellEntry;
import com.schoolbell.service.ScheduleService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static final String[] NTP_SERVERS = {
            "2.europe.pool.ntp.org",
            "time2.google.com",
            "ntp2.qix.ca"
    };

    private final RelayController relayController = new RelayController();
    private final ScheduleService scheduleService = new ScheduleService();
    private List<BellEntry> schedule = Collections.emptyList();
    private File selectedFile;

    private int regularBellDuration = 5;
    private int airRaidRingDuration = 3;
    private int airRaidPauseDuration = 1;
    private int emergencyDuration = 12;

    private Label currentTimeLabel;
    private Label syncStatusLabel;
    private Label relayStatusLabel;
    private Label countdownLabel;
    private Label nextBellTypeLabel;
    private Label filePathLabel;
    private TextArea logArea;
    private VBox scheduleOptionsBox;
    private ToggleGroup scheduleToggleGroup;
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isActionInProgress = false;

    private static final String PANEL_STYLE = "-fx-background-color: white; -fx-border-color: #dcdde1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 4);";
    private static final String HEADER_STYLE = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #95a5a6; -fx-letter-spacing: 1.5px;";
    private static final String VALUE_STYLE = "-fx-font-size: 26px; -fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-text-fill: #2d3436;";
    private static final String RELAY_OK_STYLE = "-fx-text-fill: #2ecc71;";
    private static final String RELAY_FAIL_STYLE = "-fx-text-fill: #e17055;";
    private static final String LOG_STYLE = "-fx-font-family: 'Monospaced'; -fx-font-size: 13px; -fx-control-inner-background: #2d3436; -fx-text-fill: #dfe6e9; -fx-background-radius: 5; -fx-border-radius: 5;";
    private static final String BTN_BASE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand; -fx-text-alignment: center;";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("SchoolBell Dashboard v1.8");

        syncStatusLabel = new Label("ПЕРЕВІРКА NTP...");
        syncStatusLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d;");
        VBox timeBox = new VBox(2, 
            new Label("ПОТОЧНИЙ ЧАС"), 
            currentTimeLabel = new Label("00:00:00"),
            syncStatusLabel
        );
        ((Label)timeBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        currentTimeLabel.setStyle(VALUE_STYLE);

        VBox relayBox = createInfoBox("СТАТУС РЕЛЕ", relayStatusLabel = new Label("● ПЕРЕВІРКА..."));
        HBox topBar = new HBox(20, timeBox, new Region(), relayBox);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dcdde1; -fx-border-radius: 12; -fx-border-width: 1;");

        VBox scheduleBox = new VBox(12);
        scheduleBox.setPadding(new Insets(20));
        scheduleBox.setStyle(PANEL_STYLE);
        Label schHeader = new Label("ОБЕРІТЬ РОЗКЛАД");
        schHeader.setStyle(HEADER_STYLE);
        scheduleOptionsBox = new VBox(10);
        scheduleToggleGroup = new ToggleGroup();
        scheduleBox.getChildren().addAll(schHeader, scheduleOptionsBox);

        VBox countdownBox = new VBox(10);
        countdownBox.setPadding(new Insets(20));
        countdownBox.setStyle(PANEL_STYLE);
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
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        logArea.setStyle(LOG_STYLE);
        logBox.getChildren().addAll(logHeader, logArea);

        Label actionsHeader = new Label("КЕРУВАННЯ СИГНАЛАМИ");
        actionsHeader.setStyle(HEADER_STYLE);
        Button airRaidBtn = createBigButton("ПОВІТРЯНА\nТРИВОГА", "#fdb827");
        airRaidBtn.setOnAction(e -> runAirRaidSignal());
        Button emergencyBtn = createBigButton("НАДЗВИЧАЙНА\nСИТУАЦІЯ", "#e74c3c");
        emergencyBtn.setOnAction(e -> runEmergencySignal());
        Button settingsBtn = createBigButton("НАЛАШТУВАННЯ\nСИСТЕМИ", "#636e72");
        settingsBtn.setOnAction(e -> showSettingsDialog(primaryStage));
        HBox actionsBar = new HBox(25, airRaidBtn, emergencyBtn, settingsBtn);
        actionsBar.setAlignment(Pos.CENTER);

        filePathLabel = new Label("Файл не вибрано");
        filePathLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2d3436; -fx-font-weight: bold;");
        Button chooseBtn = new Button("📁 Обрати файл");
        chooseBtn.setStyle("-fx-background-radius: 5;");
        chooseBtn.setOnAction(e -> chooseFile(primaryStage));
        Button refreshBtn = new Button("🔄 Оновити");
        refreshBtn.setStyle("-fx-background-radius: 5;");
        refreshBtn.setOnAction(e -> reloadSchedule());

        HBox footer = new HBox(20, filePathLabel, new Region(), chooseBtn, refreshBtn);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 20, 10, 20));
        footer.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dcdde1; -fx-border-radius: 12; -fx-border-width: 1;");

        VBox mainLayout = new VBox(25, topBar, middleSection, logBox, actionsHeader, actionsBar, footer);
        mainLayout.setPadding(new Insets(25, 30, 25, 30));
        mainLayout.setStyle("-fx-background-color: #f1f2f6;");

        Scene scene = new Scene(mainLayout, 900, 850);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        relayController.scanDevices();
        relayController.connect();
        loadConfig();
        checkTimeNTP();
        startScheduler();
        addLog("Система готова до роботи.");
    }

    private void checkTimeNTP() {
        new Thread(() -> {
            NTPUDPClient client = new NTPUDPClient();
            client.setDefaultTimeout(3000);
            
            for (String host : NTP_SERVERS) {
                try {
                    logger.info("Checking time via NTP: {}", host);
                    InetAddress hostAddr = InetAddress.getByName(host);
                    TimeInfo info = client.getTime(hostAddr);
                    info.computeDetails();
                    
                    long offsetMs = info.getOffset(); // Difference between local and NTP
                    long driftSec = Math.abs(offsetMs) / 1000;

                    Platform.runLater(() -> {
                        if (driftSec > 60) {
                            syncStatusLabel.setText("⚠️ NTP ПОМИЛКА: ЧАС ЗБИТО (" + driftSec + "с)");
                            syncStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #d63031; -fx-font-weight: bold;");
                            addLog("❌ NTP: Час на ПК відрізняється від точного на " + driftSec + " секунд! [" + host + "]");
                        } else {
                            syncStatusLabel.setText("✅ NTP СИНХРОНІЗОВАНО (" + host + ")");
                            syncStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60;");
                        }
                    });
                    return; // Successfully got time, exit loop
                } catch (Exception e) {
                    logger.warn("NTP server {} failed", host);
                }
            }
            
            Platform.runLater(() -> {
                syncStatusLabel.setText("○ NTP: НЕМАЄ ЗВ'ЯЗКУ (час локальний)");
                syncStatusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
            });
        }).start();
    }

    private Button createBigButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle(BTN_BASE + "-fx-background-color: " + color + "; -fx-font-size: 15px; -fx-padding: 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        b.setPrefSize(260, 80);
        return b;
    }

    private void showSettingsDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Конфігурація сигналів");

        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f8f9fa;");

        VBox sec1 = createSettingsSection("📅 СТАНДАРТНИЙ ДЗВІНОК", "#0984e3");
        TextField regField = createStyledField(String.valueOf(regularBellDuration));
        HBox prev1 = new HBox(); prev1.setAlignment(Pos.CENTER); prev1.setPadding(new Insets(15, 0, 5, 0));
        updatePreview(prev1, List.of(regularBellDuration), 0, "#0984e3");
        regField.textProperty().addListener((o, ov, nv) -> updatePreview(prev1, parseSafe(nv), 0, "#0984e3"));
        sec1.getChildren().addAll(createFieldRow("Тривалість (сек):", regField), prev1);

        VBox sec2 = createSettingsSection("⚠️ ПОВІТРЯНА ТРИВОГА", "#f39c12");
        TextField arRingField = createStyledField(String.valueOf(airRaidRingDuration));
        TextField arPauseField = createStyledField(String.valueOf(airRaidPauseDuration));
        HBox prev2 = new HBox(); prev2.setAlignment(Pos.CENTER); prev2.setPadding(new Insets(15, 0, 5, 0));
        updatePreview(prev2, List.of(airRaidRingDuration, airRaidRingDuration, airRaidRingDuration), airRaidPauseDuration, "#f39c12");
        arRingField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(nv).get(0), parseSafe(nv).get(0), parseSafe(nv).get(0)), parseSafe(arPauseField.getText()).get(0), "#f39c12"));
        arPauseField.textProperty().addListener((o, ov, nv) -> updatePreview(prev2, List.of(parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0), parseSafe(arRingField.getText()).get(0)), parseSafe(nv).get(0), "#f39c12"));
        sec2.getChildren().addAll(createFieldRow("Довжина гудка (сек):", arRingField), createFieldRow("Пауза між ними (сек):", arPauseField), prev2);

        VBox sec3 = createSettingsSection("🆘 НАДЗВИЧАЙНА СИТУАЦІЯ", "#d63031");
        TextField emField = createStyledField(String.valueOf(emergencyDuration));
        HBox prev3 = new HBox(); prev3.setAlignment(Pos.CENTER); prev3.setPadding(new Insets(15, 0, 5, 0));
        updatePreview(prev3, List.of(emergencyDuration), 0, "#d63031");
        emField.textProperty().addListener((o, ov, nv) -> updatePreview(prev3, parseSafe(nv), 0, "#d63031"));
        sec3.getChildren().addAll(createFieldRow("Тривалість (сек):", emField), prev3);

        Button saveBtn = new Button("💾 ЗБЕРЕГТИ ВСІ ЗМІНИ");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 15 50; -fx-background-radius: 10; -fx-font-size: 14px;");
        saveBtn.setOnAction(e -> {
            try {
                regularBellDuration = Integer.parseInt(regField.getText());
                airRaidRingDuration = Integer.parseInt(arRingField.getText());
                airRaidPauseDuration = Integer.parseInt(arPauseField.getText());
                emergencyDuration = Integer.parseInt(emField.getText());
                saveConfig();
                addLog("⚙️ Налаштування оновлено.");
                dialog.close();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Введіть коректні числа!").show();
            }
        });

        root.getChildren().addAll(sec1, sec2, sec3, saveBtn);
        root.setAlignment(Pos.CENTER);
        dialog.setScene(new Scene(root, 550, 850));
        dialog.showAndWait();
    }

    private void updatePreview(HBox box, List<Integer> rings, int pause, String hexColor) {
        box.getChildren().clear();
        double scale = 25.0; 
        for (int i = 0; i < rings.size(); i++) {
            int d = rings.get(i);
            if (d > 0) {
                VBox block = new VBox(5); block.setAlignment(Pos.CENTER);
                Rectangle r = new Rectangle(d * scale, 45);
                r.setArcWidth(12); r.setArcHeight(12);
                r.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, 
                    new Stop(0, Color.web(hexColor).deriveColor(0, 1, 1.3, 1)),
                    new Stop(1, Color.web(hexColor))));
                r.setEffect(new DropShadow(5, Color.web(hexColor).deriveColor(0, 1, 1, 0.4)));
                Label l = new Label(d + "с"); l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
                block.getChildren().addAll(r, l);
                box.getChildren().add(block);
            }
            if (i < rings.size() - 1 && pause > 0) {
                VBox pBlock = new VBox(5); pBlock.setAlignment(Pos.CENTER);
                Rectangle p = new Rectangle(pause * scale, 45); p.setFill(Color.web("#dfe6e9", 0.3));
                p.setStroke(Color.web("#b2bec3")); p.getStrokeDashArray().addAll(4.0, 4.0);
                Label l = new Label(pause + "с"); l.setStyle("-fx-font-size: 11px; -fx-text-fill: #b2bec3;");
                pBlock.getChildren().addAll(p, l);
                box.getChildren().add(pBlock);
            }
        }
    }

    private VBox createSettingsSection(String title, String color) {
        VBox v = new VBox(10); v.setPadding(new Insets(20)); v.setStyle(PANEL_STYLE);
        Label l = new Label(title); l.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + color + ";");
        v.getChildren().add(l);
        return v;
    }

    private HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field);
        HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private TextField createStyledField(String val) {
        TextField f = new TextField(val); f.setPrefWidth(70); f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; -fx-border-color: #dfe6e9;");
        return f;
    }

    private List<Integer> parseSafe(String val) {
        try { return List.of(Math.max(0, Integer.parseInt(val))); } catch (Exception e) { return List.of(0); }
    }

    private VBox createInfoBox(String header, Label valueLabel) {
        Label h = new Label(header); h.setStyle(HEADER_STYLE);
        valueLabel.setStyle(VALUE_STYLE);
        return new VBox(5, h, valueLabel);
    }

    private void addLog(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.appendText("[" + timestamp + "] " + message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void saveConfig() {
        Properties props = new Properties();
        if (selectedFile != null) props.setProperty("lastFile", selectedFile.getAbsolutePath());
        RadioButton selected = (RadioButton) scheduleToggleGroup.getSelectedToggle();
        if (selected != null) props.setProperty("scheduleSheet", (String) selected.getUserData());
        props.setProperty("dur.regular", String.valueOf(regularBellDuration));
        props.setProperty("dur.arRing", String.valueOf(airRaidRingDuration));
        props.setProperty("dur.arPause", String.valueOf(airRaidPauseDuration));
        props.setProperty("dur.emergency", String.valueOf(emergencyDuration));
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "SchoolBell Config");
        } catch (IOException e) { logger.error("Failed to save config", e); }
    }

    private void loadConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                regularBellDuration = Integer.parseInt(props.getProperty("dur.regular", "5"));
                airRaidRingDuration = Integer.parseInt(props.getProperty("dur.arRing", "3"));
                airRaidPauseDuration = Integer.parseInt(props.getProperty("dur.arPause", "1"));
                emergencyDuration = Integer.parseInt(props.getProperty("dur.emergency", "12"));
                String lastFile = props.getProperty("lastFile");
                if (lastFile != null && new File(lastFile).exists()) {
                    selectedFile = new File(lastFile);
                    filePathLabel.setText("Шлях: " + selectedFile.getAbsolutePath());
                    loadSheetOptions(props.getProperty("scheduleSheet"));
                }
            } catch (Exception e) { logger.error("Failed to load config", e); }
        }
    }

    private void loadSheetOptions(String targetSheet) {
        if (selectedFile == null) return;
        try {
            List<String> sheets = scheduleService.getSheetNames(selectedFile.getAbsolutePath());
            Platform.runLater(() -> {
                scheduleOptionsBox.getChildren().clear();
                scheduleToggleGroup.getToggles().clear();
                for (String sheet : sheets) {
                    RadioButton rb = new RadioButton(sheet);
                    rb.setToggleGroup(scheduleToggleGroup); rb.setUserData(sheet);
                    rb.setOnAction(e -> { saveConfig(); reloadSchedule(); });
                    scheduleOptionsBox.getChildren().add(rb);
                    if (sheet.equals(targetSheet)) rb.setSelected(true);
                }
                if (scheduleToggleGroup.getSelectedToggle() == null && !sheets.isEmpty())
                    scheduleToggleGroup.getToggles().get(0).setSelected(true);
                reloadSchedule();
            });
        } catch (IOException e) { addLog("❌ ПОМИЛКА: Не вдалося прочитати листи Excel!"); }
    }

    private void runAirRaidSignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            addLog("⚠️ ЗАПУСК СИГНАЛУ: ПОВІТРЯНА ТРИВОГА");
            try {
                for (int i = 1; i <= 3; i++) {
                    relayController.turnOn(); Thread.sleep(airRaidRingDuration * 1000L);
                    relayController.turnOff();
                    if (i < 3) Thread.sleep(airRaidPauseDuration * 1000L);
                }
                addLog("✅ Сигнал тривоги завершено.");
            } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
        }).start();
    }

    private void runEmergencySignal() {
        if (isActionInProgress) return;
        new Thread(() -> {
            isActionInProgress = true;
            addLog("🆘 ЗАПУСК СИГНАЛУ: НАДЗВИЧАЙНА СИТУАЦІЯ");
            try {
                relayController.turnOn(); Thread.sleep(emergencyDuration * 1000L);
                relayController.turnOff();
                addLog("✅ Сигнал НС завершено.");
            } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
        }).start();
    }

    private void chooseFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Виберіть файл розкладу Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файли Excel", "*.xlsx"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file; filePathLabel.setText("Шлях: " + file.getAbsolutePath());
            saveConfig(); loadSheetOptions(null);
        }
    }

    private void reloadSchedule() {
        if (selectedFile == null) return;
        RadioButton selected = (RadioButton) scheduleToggleGroup.getSelectedToggle();
        if (selected == null) return;
        String sheetName = (String) selected.getUserData();
        try {
            schedule = scheduleService.loadSchedule(selectedFile.getAbsolutePath(), sheetName);
            addLog("Розклад завантажено [" + sheetName + "]. Дзвінків: " + schedule.size());
        } catch (IOException e) { addLog("❌ ПОМИЛКА: Не вдалося прочитати розклад!"); }
    }

    private void updateUI() {
        LocalTime now = LocalTime.now();
        currentTimeLabel.setText(now.format(HH_MM_SS));
        if (relayController.isConnected()) {
            relayStatusLabel.setText("● ПІДКЛЮЧЕНО (USB)");
            relayStatusLabel.setStyle(VALUE_STYLE + RELAY_OK_STYLE);
        } else {
            relayStatusLabel.setText("○ НЕМАЄ ЗВ'ЯЗКУ");
            relayStatusLabel.setStyle(VALUE_STYLE + RELAY_FAIL_STYLE);
        }
        if (isActionInProgress) return;
        Optional<BellEntry> nextBell = schedule.stream()
                .filter(entry -> entry.time().isAfter(now))
                .findFirst();
        if (nextBell.isPresent()) {
            BellEntry entry = nextBell.get();
            nextBellTypeLabel.setText("(" + entry.type() + ")");
            Duration duration = Duration.between(now, entry.time());
            long h = duration.toHours(); long m = duration.toMinutesPart(); long s = duration.toSecondsPart();
            countdownLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        } else {
            countdownLabel.setText("--:--:--"); nextBellTypeLabel.setText("(на сьогодні все)");
        }
    }

    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            if (now.getSecond() == 0 && !isActionInProgress) {
                LocalTime minuteOnly = now.truncatedTo(ChronoUnit.MINUTES);
                schedule.stream()
                        .filter(entry -> entry.time().equals(minuteOnly))
                        .findFirst()
                        .ifPresent(entry -> {
                            new Thread(() -> {
                                isActionInProgress = true;
                                try {
                                    addLog("🔔 Автодзвінок: " + entry.type());
                                    relayController.turnOn(); Thread.sleep(regularBellDuration * 1000L);
                                    relayController.turnOff();
                                } catch (InterruptedException e) { relayController.turnOff(); } finally { isActionInProgress = false; }
                            }).start();
                        });
            }
            Platform.runLater(this::updateUI);
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void stop() { relayController.close(); scheduler.shutdown(); }

    public static void main(String[] args) { launch(args); }
}
