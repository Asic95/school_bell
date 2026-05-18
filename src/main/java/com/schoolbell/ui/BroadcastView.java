package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createSettingsSection;
import static com.schoolbell.ui.ControlFactory.createStyledField;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class BroadcastView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final CheckBox broadcastEnableCb;
    private final TextField schoolNameField;
    private final TextField cityField;
    private final TextField portField;
    private final DeviceMonitor deviceMonitor;

    public BroadcastView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.deviceMonitor = new DeviceMonitor(mainApp);

        broadcastEnableCb = new CheckBox("Увімкнути трансляцію розкладу");
        broadcastEnableCb.setSelected(config.isBroadcastEnabled());
        broadcastEnableCb.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        schoolNameField = new TextField(config.getSchoolName());
        schoolNameField.setPromptText("Назва школи (для табло)");
        schoolNameField.setStyle(FIELD_STYLE);
        schoolNameField.setPrefWidth(300);

        cityField = new TextField(config.getCityName());
        cityField.setPromptText("Місто");
        cityField.setStyle(FIELD_STYLE);
        cityField.setPrefWidth(300);

        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        portField.setPrefWidth(100);
        portField.setAlignment(Pos.BASELINE_LEFT);
        portField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> 
            change.getControlNewText().matches("\\d*") ? change : null));
    }

    private void updateFirewallButtonStatus(Button btn) {
        btn.setDisable(true);
        btn.setText("ПЕРЕВІРКА...");

        Thread thread = new Thread(() -> {
            boolean active = isFirewallRuleActive(config.getBroadcastPort());
            javafx.application.Platform.runLater(() -> {
                btn.setDisable(false);
                btn.setText(active ? "Firewall: ДОЗВОЛЕНО" : "ДОЗВОЛИТИ В Firewall");
                btn.setStyle(active ? 
                    "-fx-background-color: #e8f5e9; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 8 15; -fx-background-radius: 10; -fx-border-color: " + COLOR_SUCCESS + "; -fx-border-radius: 10; -fx-cursor: hand;" :
                    "-fx-background-color: white; -fx-text-fill: " + COLOR_WARNING + "; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 8 15; -fx-background-radius: 10; -fx-border-color: " + COLOR_WARNING + "; -fx-border-radius: 10; -fx-cursor: hand;"
                );
                btn.setGraphic(createSVGIcon(active ? ICON_CHECK : ICON_SETTINGS, Color.web(active ? COLOR_SUCCESS : COLOR_WARNING), 14));
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private boolean isFirewallRuleActive(int port) {
        try {
            String script = String.format(
                "if (Get-NetFirewallRule -DisplayName 'SchoolBell Dashboard' -Enabled True -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq '%d' }) { exit 0 } else { exit 1 }", 
                port
            );
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", script);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");
        
        VBox headerArea = createSectionHeader(
                "Керування трансляцією",
                "Налаштування веб-табло та керування підключеними пристроями",
                "#2980b9",
                ICON_BROADCAST
        );

        HBox contentLayout = new HBox(25);
        VBox mainContent = new VBox(25);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // --- DASHBOARD SETTINGS ---
        VBox settingsCard = createSettingsSection("НАЛАШТУВАННЯ ТАБЛО", "#2980b9", ICON_MONITOR);
        settingsCard.setStyle(SOFT_CARD);
        settingsCard.setPadding(new Insets(25));
        
        // --- MASTER SWITCH ---
        HBox masterSwitch = new HBox(20);
        masterSwitch.setAlignment(Pos.CENTER_LEFT);
        masterSwitch.setPadding(new Insets(0, 0, 20, 0));
        masterSwitch.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;");

        VBox switchText = new VBox(4);
        Label switchTitle = new Label("ТРАНСЛЯЦІЯ РОЗКЛАДУ");
        switchTitle.setStyle(SUB_HEADER_STYLE + "-fx-font-size: 14px;");
        Label switchDesc = new Label("Активує сервер для передачі даних на табло");
        switchDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        switchText.getChildren().addAll(switchTitle, switchDesc);

        broadcastEnableCb.setText("Увімкнено");
        broadcastEnableCb.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARY + ";");
        
        Region switchSpacer = new Region();
        HBox.setHgrow(switchSpacer, Priority.ALWAYS);
        masterSwitch.getChildren().addAll(switchText, switchSpacer, broadcastEnableCb);

        String localIp = getLocalIp();
        
        VBox addrBox = new VBox(15);
        addrBox.setPadding(new Insets(20, 0, 0, 0));
        addrBox.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 1 0 0 0;");

        Label addrHeader = new Label("МЕРЕЖЕВИЙ ДОСТУП");
        addrHeader.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");

        HBox infoCard = new HBox(15);
        infoCard.setAlignment(Pos.CENTER_LEFT);
        infoCard.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 12; -fx-border-color: #dfe6e9; -fx-border-radius: 12;");
        
        VBox urlGroup = new VBox(5);
        Label urlDisplay = new Label("http://" + localIp + ":" + config.getBroadcastPort());
        urlDisplay.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 14px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");
        Label urlDesc = new Label("Посилання для підключення пристроїв");
        urlDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
        urlGroup.getChildren().addAll(urlDisplay, urlDesc);
        
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            urlDisplay.setText("http://" + localIp + ":" + newVal);
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button openBrowserBtn = new Button("ВІДКРИТИ");
        openBrowserBtn.setGraphic(createSVGIcon(ICON_MONITOR, Color.WHITE, 14));
        openBrowserBtn.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        openBrowserBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://localhost:" + portField.getText().trim()));
        
        infoCard.getChildren().addAll(urlGroup, spacer2, openBrowserBtn);

        HBox firewallRow = new HBox(10);
        firewallRow.setAlignment(Pos.CENTER_LEFT);
        Button firewallBtn = new Button();
        updateFirewallButtonStatus(firewallBtn);
        firewallBtn.setGraphic(createSVGIcon(ICON_SETTINGS, Color.web(COLOR_WARNING), 14));
        firewallBtn.setGraphicTextGap(8);
        firewallBtn.setStyle("-fx-background-color: white; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 8 15; -fx-background-radius: 10; -fx-border-color: #dfe6e9; -fx-cursor: hand;");
        firewallBtn.setOnAction(e -> {
            setupFirewall();
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            delay.setOnFinished(event -> updateFirewallButtonStatus(firewallBtn));
            delay.play();
        });
        
        Label fwStatus = new Label("Статус Firewall для портів");
        fwStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        firewallRow.getChildren().addAll(firewallBtn, fwStatus);
        
        addrBox.getChildren().addAll(addrHeader, infoCard, firewallRow);

        VBox form = new VBox(20,
            masterSwitch,
            createModernField("Назва закладу (для табло):", schoolNameField),
            createModernField("Місто:", cityField),
            createModernField("Основний порт (для веб-табло):", portField),
            addrBox
        );
        settingsCard.getChildren().add(form);

        VBox deviceMonitorNode = (VBox) deviceMonitor.build();

        Button saveBtn = new Button("ЗБЕРЕГТИ НАЛАШТУВАННЯ");
        saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 18));
        saveBtn.setGraphicTextGap(10);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 14 50; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.3), 10, 0, 0, 5);");
        saveBtn.setOnAction(e -> save());

        mainContent.getChildren().addAll(settingsCard, deviceMonitorNode, new HBox(saveBtn));

        VBox rightColumn = createSideHelpPanel(
            createHelpCard(ICON_INFO, "Обмін даними", "Порт " + config.getBroadcastPort() + " використовується для синхронізації даних. Він відкривається автоматично.", "#2980b9"),
            createHelpCard(ICON_MONITOR, "Веб-панель", "Табло доступне за вказаною HTTP-адресою. Будь-який пристрій з браузером може стати табло.", "#8e44ad"),
            createHelpCard(ICON_CLASS, "Керування доступом", "Ви можете бачити всі пристрої, що підключалися. Блокуйте невідомі IP для безпеки.", "#6c5ce7")
        );

        contentLayout.getChildren().addAll(mainContent, rightColumn);
        root.getChildren().addAll(headerArea, contentLayout);
        
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void save() {
        config.setBroadcastEnabled(broadcastEnableCb.isSelected());
        config.setSchoolName(schoolNameField.getText());
        config.setCityName(cityField.getText());
        try { config.setBroadcastPort(Integer.parseInt(portField.getText())); } catch (Exception e) {}
        mainApp.saveConfig();
        mainApp.addLog("Налаштування оновлено.", "SUCCESS");
        ToastService.showSuccess("Налаштування трансляції збережено!");
    }

    private String getLocalIp() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("192.0.2.1"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) { return "127.0.0.1"; }
    }

    private Node createModernField(String label, TextField field) {
        VBox v = new VBox(8);
        Label l = createStyledLabel(label);
        v.getChildren().addAll(l, field);
        return v;
    }

    private Label createStyledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
        return l;
    }

    private void setupFirewall() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            mainApp.addLog("Некоректний порт для налаштування Firewall", "ERROR");
            return;
        }

        String script = String.format(
                "if (Get-NetFirewallRule -DisplayName 'SchoolBell Dashboard' -ErrorAction SilentlyContinue) { Remove-NetFirewallRule -DisplayName 'SchoolBell Dashboard' }; " +
                "New-NetFirewallRule -DisplayName 'SchoolBell Dashboard' -Direction Inbound -LocalPort %d -Protocol TCP -Action Allow; " +
                "if (Get-NetFirewallRule -DisplayName 'SchoolBell Data' -ErrorAction SilentlyContinue) { Remove-NetFirewallRule -DisplayName 'SchoolBell Data' }; " +
                "New-NetFirewallRule -DisplayName 'SchoolBell Data' -Direction Inbound -LocalPort %d -Protocol TCP -Action Allow;",
                port, port + 2
        );

        String escapedScript = script.replace("\"", "\\\"");
        String psCommand = "Start-Process powershell -Verb RunAs -ArgumentList '-Command \"" + escapedScript + "\"'";

        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psCommand);
            pb.start();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Налаштування Firewall");
            alert.setHeaderText("Запит на відкриття портів надіслано");
            alert.setContentText("Будь ласка, підтвердіть запит у вікні системного контролю (UAC), яке щойно з'явилося. " +
                    "\n\nЦе дозволить іншим пристроям у вашій мережі бачити табло на порту " + port + ".");
            alert.showAndWait();
            
            mainApp.addLog("Надіслано запит на відкриття портів " + port + " та " + (port + 2), "INFO");
        } catch (Exception e) {
            mainApp.addLog("Помилка запуску налаштування Firewall: " + e.getMessage(), "ERROR");
        }
    }
}
