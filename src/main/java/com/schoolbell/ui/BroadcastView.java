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
        broadcastEnableCb.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        
        schoolNameField = new TextField(config.getSchoolName());
        schoolNameField.setPromptText("Назва школи (для табло)");
        schoolNameField.setStyle(PREMIUM_FIELD_STYLE);
        schoolNameField.setPrefWidth(450);

        cityField = new TextField(config.getCityName());
        cityField.setPromptText("Місто");
        cityField.setStyle(PREMIUM_FIELD_STYLE);
        cityField.setPrefWidth(450);

        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        portField.setPrefWidth(120);
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
                btn.setText(active ? "FIREWALL: ДОЗВОЛЕНО" : "ДОЗВОЛИТИ В FIREWALL");
                String activeStyle = active ? 
                    "-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 10 20; -fx-background-radius: 14; -fx-border-color: #dcfce7; -fx-border-radius: 14; -fx-cursor: hand;" :
                    "-fx-background-color: white; -fx-text-fill: #ea580c; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 10 20; -fx-background-radius: 14; -fx-border-color: #ffedd5; -fx-border-radius: 14; -fx-cursor: hand;";
                btn.setStyle(activeStyle);
                btn.setGraphic(createSVGIcon(active ? ICON_CHECK : ICON_SETTINGS, Color.web(active ? "#16a34a" : "#ea580c"), 16));
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
        
        HBox headerArea = ControlFactory.createPageHeader(
                "СЕРВЕР ТА МЕРЕЖА",
                "Керування трансляцією",
                "Налаштування веб-табло та керування підключеними пристроями у вашій локальній мережі.",
                ICON_BROADCAST,
                "#2980b9",
                null
        );

        HBox contentLayout = new HBox(28);
        VBox mainContent = new VBox(28);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // --- DASHBOARD SETTINGS ---
        VBox settingsCard = createSettingsSection("НАЛАШТУВАННЯ ТАБЛО", "#2980b9", ICON_MONITOR);
        settingsCard.setStyle(SOFT_CARD + "-fx-padding: 30;");
        
        // --- MASTER SWITCH ---
        HBox masterSwitch = new HBox(20);
        masterSwitch.setAlignment(Pos.CENTER_LEFT);
        masterSwitch.setPadding(new Insets(0, 0, 25, 0));
        masterSwitch.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");

        VBox switchText = new VBox(4);
        Label switchTitle = new Label("ТРАНСЛЯЦІЯ РОЗКЛАДУ");
        switchTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #0f172a;");
        Label switchDesc = new Label("Активує сервер для передачі даних на віддалені табло");
        switchDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        switchText.getChildren().addAll(switchTitle, switchDesc);

        broadcastEnableCb.setText("СЕРВЕР АКТИВНИЙ");
        broadcastEnableCb.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #4f46e5;");
        
        Region switchSpacer = new Region();
        HBox.setHgrow(switchSpacer, Priority.ALWAYS);
        masterSwitch.getChildren().addAll(switchText, switchSpacer, broadcastEnableCb);

        String localIp = getLocalIp();
        
        VBox addrBox = new VBox(20);
        addrBox.setPadding(new Insets(25, 0, 0, 0));
        addrBox.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 1 0 0 0;");

        Label addrHeader = new Label("МЕРЕЖЕВИЙ ДОСТУП");
        addrHeader.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");

        HBox infoCard = new HBox(20);
        infoCard.setAlignment(Pos.CENTER_LEFT);
        infoCard.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20; -fx-background-radius: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 18;");
        
        VBox urlGroup = new VBox(6);
        Label urlDisplay = new Label("http://" + localIp + ":" + config.getBroadcastPort());
        urlDisplay.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 18px; -fx-text-fill: #2563eb; -fx-font-weight: 900;");
        Label urlDesc = new Label("Локальне посилання для підключення моніторів");
        urlDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        urlGroup.getChildren().addAll(urlDisplay, urlDesc);
        
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            urlDisplay.setText("http://" + localIp + ":" + newVal);
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button openBrowserBtn = new Button("ВІДКРИТИ");
        openBrowserBtn.setGraphic(createSVGIcon(ICON_MONITOR, Color.WHITE, 16));
        openBrowserBtn.setGraphicTextGap(10);
        openBrowserBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 10 20; -fx-background-radius: 14; -fx-cursor: hand;");
        openBrowserBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://localhost:" + portField.getText().trim()));
        
        infoCard.getChildren().addAll(urlGroup, spacer2, openBrowserBtn);

        HBox firewallRow = new HBox(15);
        firewallRow.setAlignment(Pos.CENTER_LEFT);
        Button firewallBtn = new Button();
        updateFirewallButtonStatus(firewallBtn);
        firewallBtn.setOnAction(e -> {
            setupFirewall();
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            delay.setOnFinished(event -> updateFirewallButtonStatus(firewallBtn));
            delay.play();
        });
        
        Label fwStatus = new Label("Статус брандмауера Windows для вибраного порту");
        fwStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        firewallRow.getChildren().addAll(firewallBtn, fwStatus);
        
        addrBox.getChildren().addAll(addrHeader, infoCard, firewallRow);

        VBox form = new VBox(22,
            masterSwitch,
            createModernField("НАЗВА ЗАКЛАДУ (ДЛЯ ТАБЛО):", schoolNameField),
            createModernField("МІСТО:", cityField),
            createModernField("ПОРТ ТРАНСЛЯЦІЇ (РЕКОМЕНДОВАНО: 8080):", portField),
            addrBox
        );
        settingsCard.getChildren().add(form);

        VBox deviceMonitorNode = (VBox) deviceMonitor.build();

        Button saveBtn = ControlFactory.createPrimaryActionButton("ЗБЕРЕГТИ ПАРАМЕТРИ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(e -> save());

        mainContent.getChildren().addAll(settingsCard, deviceMonitorNode, new HBox(saveBtn));

        VBox rightColumn = createSideHelpPanel(
            createHelpCard(ICON_INFO, "Обмін даними", "Порт " + config.getBroadcastPort() + " використовується для синхронізації. Перевірте, чи не блокує його антивірус.", "#2563eb"),
            createHelpCard(ICON_MONITOR, "Веб-панель", "Табло працює в будь-якому сучасному браузері. Ми рекомендуємо Chrome або Edge.", "#7c3aed"),
            createHelpCard(ICON_CLASS, "Підключення", "Ви можете бачити статус підключених моніторів у списку нижче.", "#059669")
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
        VBox v = new VBox(10);
        Label l = new Label(label);
        l.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        v.getChildren().addAll(l, field);
        return v;
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
