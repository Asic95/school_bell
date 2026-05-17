package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SystemService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class SystemView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final SystemService systemService;

    // Profile
    private final TextField schoolNameField;
    private final TextField cityNameField;

    // Operation
    private ToggleButton autostartTg;
    private ToggleButton trayTg;
    private ToggleButton simulationTg;
    private ComboBox<String> themeCombo;

    // Network
    private final TextField portField;
    private Label firewallStatus;

    // Signal Durations
    private Spinner<Integer> regularBellDur;
    private Spinner<Integer> airRaidRingDur;
    private Spinner<Integer> airRaidPauseDur;
    private Spinner<Integer> emergencyDur;
    private HBox regularPreview;
    private HBox airRaidPreview;
    private HBox emergencyPreview;

    public SystemView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.systemService = mainApp.getSystemService();

        // Profile
        schoolNameField = createStyledField(config.getSchoolName());
        cityNameField = createStyledField(config.getCityName());
        schoolNameField.setPrefWidth(450);
        cityNameField.setPrefWidth(300);

        // Operation
        autostartTg = createToggleSwitch(config.isAutostartEnabled());
        trayTg = createToggleSwitch(config.isMinimizeToTray());
        simulationTg = createToggleSwitch(config.isSimulationMode());
        
        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("classic", "modern", "cyber");
        themeCombo.setValue(config.getDashboardTheme());
        themeCombo.setStyle(COMBO_STYLE);
        themeCombo.setPrefWidth(150);

        // Network
        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        portField.setMaxWidth(100);

        // Signal Durations
        regularBellDur = createStyledSpinner(1, 60, config.getRegularBellDuration());
        airRaidRingDur = createStyledSpinner(1, 30, config.getAirRaidRingDuration());
        airRaidPauseDur = createStyledSpinner(1, 30, config.getAirRaidPauseDuration());
        emergencyDur = createStyledSpinner(1, 120, config.getEmergencyDuration());
        
        regularPreview = new HBox();
        airRaidPreview = new HBox();
        emergencyPreview = new HBox();

        javafx.beans.value.ChangeListener<Integer> previewUpdater = (o, ov, nv) -> refreshPreviews();
        regularBellDur.valueProperty().addListener(previewUpdater);
        airRaidRingDur.valueProperty().addListener(previewUpdater);
        airRaidPauseDur.valueProperty().addListener(previewUpdater);
        emergencyDur.valueProperty().addListener(previewUpdater);
        
        firewallStatus = new Label("ПЕРЕВІРКА...");
        updateFirewallStatusLabel();
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ВСЕ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Системна конфігурація",
                "Глобальні параметри роботи програми, мережі та профілю закладу",
                "#2d3436",
                ICON_SETTINGS,
                saveBtn
        );

        HBox contentLayout = new HBox(25);
        VBox mainCol = new VBox(25);
        HBox.setHgrow(mainCol, Priority.ALWAYS);

        // --- PROFILE CARD ---
        mainCol.getChildren().add(buildProfileCard());

        // --- OPERATION CARD ---
        mainCol.getChildren().add(buildOperationCard());
        
        // --- SIGNAL CARD ---
        mainCol.getChildren().add(buildSignalCard());

        // --- NETWORK CARD ---
        mainCol.getChildren().add(buildNetworkCard());

        // --- HELP PANEL ---
        VBox helpPanel = createSideHelpPanel(
            createHelpCard(ICON_PERSON, "Профіль", "Ці дані використовуються для заголовків у звітах та на веб-табло.", COLOR_PRIMARY),
            createHelpCard(ICON_POWER, "Автостарт", "Рекомендуємо увімкнути автостарт, щоб система відновлювалась після вимкнення світла.", COLOR_SUCCESS),
            createHelpCard(ICON_WAVEFORM, "Сигнали", "Ви можете налаштувати тривалість дзвінків та бачити їх візуалізацію в реальному часі.", COLOR_WARNING),
            createHelpCard(ICON_MONITOR, "Мережа", "Якщо табло не відкривається на інших ПК, спробуйте кнопку 'Оптимізувати Firewall'.", COLOR_PURPLE)
        );

        contentLayout.getChildren().addAll(mainCol, helpPanel);
        root.getChildren().addAll(headerArea, contentLayout);

        // Update firewall status when building the view
        updateFirewallStatusLabel();
        refreshPreviews();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private VBox buildProfileCard() {
        VBox card = createCard("ПРОФІЛЬ ЗАКЛАДУ", ICON_PERSON, COLOR_PRIMARY);
        
        GridPane grid = new GridPane();
        grid.setHgap(30); grid.setVgap(20);
        
        grid.add(createLabel("Назва закладу:"), 0, 0);
        grid.add(schoolNameField, 1, 0);
        
        grid.add(createLabel("Місто / Населений пункт:"), 0, 1);
        grid.add(cityNameField, 1, 1);
        
        card.getChildren().add(grid);
        return card;
    }

    private VBox buildOperationCard() {
        VBox card = createCard("ЗАПУСК ТА РОБОТА", ICON_CLOCK, COLOR_SUCCESS);
        
        VBox rows = new VBox(8);
        
        rows.getChildren().add(createToggleRow("Автозапуск разом з Windows", 
            "Програма буде автоматично запускатись при вході в систему", ICON_POWER, COLOR_PRIMARY, autostartTg));
        
        rows.getChildren().add(createToggleRow("Згортати у системний трей", 
            "При закритті вікно буде ховатись у трей замість виходу", ICON_TRAY, COLOR_PURPLE, trayTg));

        rows.getChildren().add(createToggleRow("Режим симуляції", 
            "Робота без фізичного підключення до реле (тільки логування)", ICON_FLASK, COLOR_WARNING, simulationTg));

        HBox themeRow = new HBox(20);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        themeRow.setPadding(new Insets(10, 15, 10, 15));
        themeRow.setStyle("-fx-background-radius: 12;");
        themeRow.setOnMouseEntered(e -> themeRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12;"));
        themeRow.setOnMouseExited(e -> themeRow.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));

        VBox iconBox = new VBox(createSVGIcon(ICON_PALETTE, Color.web(COLOR_NEUTRAL), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: #f1f2f6; -fx-background-radius: 10;");

        VBox themeTexts = new VBox(2);
        Label tt = new Label("Дизайн веб-табло");
        tt.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label td = new Label("Виберіть візуальний стиль для зовнішніх моніторів");
        td.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_NEUTRAL + ";");
        themeTexts.getChildren().addAll(tt, td);
        
        Region ts = new Region(); HBox.setHgrow(ts, Priority.ALWAYS);
        themeRow.getChildren().addAll(iconBox, themeTexts, ts, themeCombo);
        rows.getChildren().add(themeRow);
        
        card.getChildren().add(rows);
        return card;
    }

    private VBox buildSignalCard() {
        VBox card = createCard("ПАРАМЕТРИ ДЗВІНКІВ", ICON_WAVEFORM, "#f39c12");
        
        VBox rows = new VBox(15);
        
        // --- Regular Bell Row ---
        HBox regRow = new HBox(25);
        regRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox regLabelBox = new VBox(2);
        regLabelBox.setMinWidth(160);
        regLabelBox.setPrefWidth(160);
        Label regTitle = new Label("ЗВИЧАЙНИЙ ДЗВІНОК");
        regTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label regSub = new Label("Автоматичні сигнали");
        regSub.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        regLabelBox.getChildren().addAll(regTitle, regSub);
        
        HBox.setHgrow(regularPreview, Priority.ALWAYS);
        regRow.getChildren().addAll(regLabelBox, regularBellDur, regularPreview);
        
        // --- Air Raid Row ---
        HBox arRow = new HBox(25);
        arRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox arLabelBox = new VBox(2);
        arLabelBox.setMinWidth(160);
        arLabelBox.setPrefWidth(160);
        Label arTitle = new Label("ПОВІТРЯНА ТРИВОГА");
        arTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label arSub = new Label("Циклічний сигнал");
        arSub.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        arLabelBox.getChildren().addAll(arTitle, arSub);
        
        HBox arInputs = new HBox(15, 
            new VBox(2, createMiniLabel("ЗВУК (СЕК)"), airRaidRingDur),
            new VBox(2, createMiniLabel("ПАУЗА (СЕК)"), airRaidPauseDur)
        );
        
        HBox.setHgrow(airRaidPreview, Priority.ALWAYS);
        arRow.getChildren().addAll(arLabelBox, arInputs, airRaidPreview);
        
        // --- Emergency Row ---
        HBox emRow = new HBox(25);
        emRow.setAlignment(Pos.CENTER_LEFT);
        
        VBox emLabelBox = new VBox(2);
        emLabelBox.setMinWidth(160);
        emLabelBox.setPrefWidth(160);
        Label emTitle = new Label("ЕКСТРЕНА СИТУАЦІЯ");
        emTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label emSub = new Label("Сигнал евакуації");
        emSub.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        emLabelBox.getChildren().addAll(emTitle, emSub);
        
        HBox.setHgrow(emergencyPreview, Priority.ALWAYS);
        emRow.getChildren().addAll(emLabelBox, emergencyDur, emergencyPreview);
        
        rows.getChildren().addAll(regRow, createSeparator(), arRow, createSeparator(), emRow);
        card.getChildren().add(rows);
        return card;
    }

    private void refreshPreviews() {
        updatePreview(regularPreview, "", java.util.List.of(regularBellDur.getValue()), 0, COLOR_PRIMARY);
        updatePreview(airRaidPreview, "", java.util.List.of(airRaidRingDur.getValue(), airRaidRingDur.getValue(), airRaidRingDur.getValue()), airRaidPauseDur.getValue(), "#f39c12");
        updatePreview(emergencyPreview, "", java.util.List.of(emergencyDur.getValue()), 0, COLOR_DANGER);
    }

    private Label createMiniLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: 900; -fx-font-size: 10px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        return l;
    }

    private VBox buildNetworkCard() {
        VBox card = createCard("МЕРЕЖА ТА БЕЗПЕКА", ICON_BROADCAST, "#6c5ce7");
        
        HBox portRow = new HBox(20);
        portRow.setAlignment(Pos.CENTER_LEFT);
        portRow.setPadding(new Insets(5, 15, 5, 15));
        portRow.getChildren().addAll(createLabel("Порт веб-табло:"), new Region(), portField);
        HBox.setHgrow(portRow.getChildren().get(1), Priority.ALWAYS);
        
        HBox firewallRow = new HBox(20);
        firewallRow.setAlignment(Pos.CENTER_LEFT);
        firewallRow.setPadding(new Insets(10, 15, 10, 15));
        
        VBox fwInfo = new VBox(5);
        Label fwTitle = new Label("СТАТУС ПОРТУ В БРАНДМАУЕРІ:");
        fwTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        firewallStatus.setStyle("-fx-font-weight: 900; -fx-font-size: 14px;");
        fwInfo.getChildren().addAll(fwTitle, firewallStatus);
        
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button optimizeBtn = new Button("ОПТИМІЗУВАТИ FIREWALL");
        optimizeBtn.setStyle(BTN_BASE + "-fx-background-color: #6c5ce7; -fx-padding: 8 20;");
        optimizeBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 16));
        optimizeBtn.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                systemService.optimizeFirewall(port);
                
                // Wait a bit for the elevated process to finish before re-checking
                new Thread(() -> {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::updateFirewallStatusLabel);
                }).start();
                
                ToastService.showSuccess("Запит на оптимізацію надіслано!");
            } catch (Exception ex) {
                ToastService.showError("Помилка: " + ex.getMessage());
            }
        });
        
        firewallRow.getChildren().addAll(fwInfo, spacer, optimizeBtn);
        
        card.getChildren().addAll(portRow, createSeparator(), firewallRow);
        return card;
    }

    private VBox createCard(String title, String icon, String color) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(25));
        card.setStyle(SOFT_CARD);
        
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 12;");
        
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + "; -fx-letter-spacing: 1px;");
        
        header.getChildren().addAll(iconBox, t);
        card.getChildren().add(header);
        
        return card;
    }

    private HBox createToggleRow(String title, String desc, String icon, String iconColor, ToggleButton toggle) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-radius: 12;");
        
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(iconColor), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + iconColor + "12; -fx-background-radius: 10;");

        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_NEUTRAL + ";");
        texts.getChildren().addAll(t, d);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        row.getChildren().addAll(iconBox, texts, spacer, toggle);
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));
        
        return row;
    }

    private HBox createToggleRow(String title, String desc, ToggleButton toggle) {
        return createToggleRow(title, desc, ICON_SETTINGS, COLOR_PRIMARY, toggle);
    }

    private Label createLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT + ";");
        return l;
    }

    private Separator createSeparator() {
        Separator s = new Separator();
        s.setStyle("-fx-opacity: 0.4;");
        return s;
    }

    private void updateFirewallStatusLabel() {
        firewallStatus.setText("ПЕРЕВІРКА...");
        firewallStatus.setTextFill(Color.GRAY);
        
        new Thread(() -> {
            try {
                int port = Integer.parseInt(portField.getText());
                boolean allowed = systemService.isPortAllowedInFirewall(port);
                
                javafx.application.Platform.runLater(() -> {
                    firewallStatus.setText(allowed ? "ВІДКРИТИЙ" : "ЗАБЛОКОВАНИЙ");
                    firewallStatus.setTextFill(Color.web(allowed ? COLOR_SUCCESS : COLOR_DANGER));
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    firewallStatus.setText("НЕВІДОМО");
                    firewallStatus.setTextFill(Color.GRAY);
                });
            }
        }).start();
    }

    private void save() {
        try {
            config.setSchoolName(schoolNameField.getText());
            config.setCityName(cityNameField.getText());
            
            boolean autostartChanged = config.isAutostartEnabled() != autostartTg.isSelected();
            config.setAutostartEnabled(autostartTg.isSelected());
            config.setMinimizeToTray(trayTg.isSelected());
            config.setSimulationMode(simulationTg.isSelected());
            config.setDashboardTheme(themeCombo.getValue());
            
            int oldPort = config.getBroadcastPort();
            int newPort = Integer.parseInt(portField.getText());
            config.setBroadcastPort(newPort);

            // Signal Durations
            config.setRegularBellDuration(regularBellDur.getValue());
            config.setAirRaidRingDuration(airRaidRingDur.getValue());
            config.setAirRaidPauseDuration(airRaidPauseDur.getValue());
            config.setEmergencyDuration(emergencyDur.getValue());

            mainApp.saveConfig();
            
            if (autostartChanged) {
                systemService.updateAutostart(config.isAutostartEnabled());
            }

            mainApp.addLog("Системні налаштування оновлено", "SUCCESS");
            ToastService.showSuccess("Налаштування збережено!");
            updateFirewallStatusLabel();
            
        } catch (Exception e) {
            ToastService.showError("Помилка при збереженні: " + e.getMessage());
        }
    }
}
