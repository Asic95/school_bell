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

        // --- NETWORK CARD ---
        mainCol.getChildren().add(buildNetworkCard());

        // --- HELP PANEL ---
        VBox helpPanel = createSideHelpPanel(
            createHelpCard(ICON_PERSON, "Профіль", "Ці дані використовуються для заголовків у звітах та на веб-табло.", COLOR_PRIMARY),
            createHelpCard(ICON_BROADCAST, "Автостарт", "Рекомендуємо увімкнути автостарт, щоб система відновлювалась після вимкнення світла.", COLOR_SUCCESS),
            createHelpCard(ICON_MONITOR, "Мережа", "Якщо табло не відкривається на інших ПК, спробуйте кнопку 'Оптимізувати Firewall'.", COLOR_PURPLE)
        );

        contentLayout.getChildren().addAll(mainCol, helpPanel);
        root.getChildren().addAll(headerArea, contentLayout);

        // Update firewall status when building the view
        updateFirewallStatusLabel();

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
        
        VBox rows = new VBox(15);
        
        rows.getChildren().add(createToggleRow("Автозапуск разом з Windows", 
            "Програма буде автоматично запускатись при вході в систему", autostartTg));
        
        rows.getChildren().add(createSeparator());
        
        rows.getChildren().add(createToggleRow("Згортати у системний трей", 
            "При закритті вікно буде ховатись у трей замість виходу", trayTg));

        rows.getChildren().add(createSeparator());

        rows.getChildren().add(createToggleRow("Режим симуляції", 
            "Робота без фізичного підключення до реле (тільки логування)", simulationTg));

        rows.getChildren().add(createSeparator());

        HBox themeRow = new HBox(20);
        themeRow.setAlignment(Pos.CENTER_LEFT);
        VBox themeTexts = new VBox(2);
        Label tt = new Label("Дизайн веб-табло");
        tt.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
        Label td = new Label("Виберіть візуальний стиль для зовнішніх моніторів");
        td.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        themeTexts.getChildren().addAll(tt, td);
        Region ts = new Region(); HBox.setHgrow(ts, Priority.ALWAYS);
        themeRow.getChildren().addAll(themeTexts, ts, themeCombo);
        rows.getChildren().add(themeRow);
        
        card.getChildren().add(rows);
        return card;
    }

    private VBox buildNetworkCard() {
        VBox card = createCard("МЕРЕЖА ТА БЕЗПЕКА", ICON_BROADCAST, "#6c5ce7");
        
        HBox portRow = new HBox(20);
        portRow.setAlignment(Pos.CENTER_LEFT);
        portRow.getChildren().addAll(createLabel("Порт веб-табло:"), portField);
        
        HBox firewallRow = new HBox(20);
        firewallRow.setAlignment(Pos.CENTER_LEFT);
        firewallRow.setPadding(new Insets(10, 0, 0, 0));
        
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
        card.setPadding(new Insets(30));
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

    private HBox createToggleRow(String title, String desc, ToggleButton toggle) {
        VBox texts = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        texts.getChildren().addAll(t, d);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox row = new HBox(20, texts, spacer, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
