package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class BroadcastView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final CheckBox broadcastEnableCb;
    private final TextField schoolNameField;
    private final TextField cityField;
    private final TextField portField;
    private final VBox deviceListContainer;

    public BroadcastView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();

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
        
        deviceListContainer = new VBox(10);
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
            // Check if a rule exists that is enabled AND matches the specific port
            String checkCommand = String.format(
                "powershell -Command \"if (Get-NetFirewallRule -DisplayName 'SchoolBell Dashboard' -Enabled True -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq '%d' }) { exit 0 } else { exit 1 }\"", 
                port
            );
            Process process = Runtime.getRuntime().exec(checkCommand);
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
        
        // --- ADDR BOX (Unified Layout) ---
        VBox addrBox = new VBox(15);
        addrBox.setPadding(new Insets(20, 0, 0, 0));
        addrBox.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 1 0 0 0;");

        Label addrHeader = new Label("МЕРЕЖЕВИЙ ДОСТУП");
        addrHeader.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");

        // Info Card
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

        // Firewall Status Row
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


        // --- DEVICE MANAGEMENT ---
        VBox deviceCard = createSettingsSection("КЕРУВАННЯ ПРИСТРОЯМИ", "#8e44ad", ICON_MONITOR);
        deviceCard.setStyle(SOFT_CARD);
        deviceCard.setPadding(new Insets(25));
        
        Button refreshBtn = new Button("ОНОВИТИ СПИСОК");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshDevices());

        HBox header = (HBox) deviceCard.getChildren().get(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(spacer, refreshBtn);

        deviceCard.getChildren().add(deviceListContainer);
        refreshDevices();

        Button saveBtn = new Button("ЗБЕРЕГТИ НАЛАШТУВАННЯ");
        saveBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 18));
        saveBtn.setGraphicTextGap(10);
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 14 50; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(39, 174, 96, 0.3), 10, 0, 0, 5);");
        saveBtn.setOnAction(e -> save());

        mainContent.getChildren().addAll(settingsCard, deviceCard, new HBox(saveBtn));

        // --- SIDE PANEL (Help & Tips - Standardized) ---
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


    private void refreshDevices() {
        deviceListContainer.getChildren().clear();
        
        HBox listHeader = new HBox(20);
        listHeader.setPadding(new Insets(10, 20, 10, 20));
        listHeader.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12;");
        
        Label hDevice = new Label("ПРИСТРІЙ"); hDevice.setPrefWidth(300);
        Label hNetwork = new Label("МЕРЕЖА"); hNetwork.setPrefWidth(180);
        Label hLastSeen = new Label("ОСТАННЯ АКТИВНІСТЬ"); hLastSeen.setPrefWidth(200);
        Label hActions = new Label("ДІЇ");
        
        String hStyle = "-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #95a5a6; -fx-letter-spacing: 1px;";
        hDevice.setStyle(hStyle); hNetwork.setStyle(hStyle); hLastSeen.setStyle(hStyle); hActions.setStyle(hStyle);
        
        listHeader.getChildren().addAll(hDevice, hNetwork, hLastSeen, hActions);
        deviceListContainer.getChildren().add(listHeader);

        List<String> activeIps = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnectedClients() : List.of();
        List<BroadcastDevice> savedDevices = DatabaseManager.getAllBroadcastDevices();
        
        if (savedDevices.isEmpty() && activeIps.isEmpty()) {
            Label none = new Label("Немає підключених пристроїв");
            none.setStyle("-fx-font-style: italic; -fx-text-fill: #95a5a6; -fx-padding: 30;");
            deviceListContainer.getChildren().add(none);
        } else {
            for (BroadcastDevice device : savedDevices) {
                deviceListContainer.getChildren().add(createModernDeviceRow(device, activeIps.contains(device.ip())));
            }
        }
    }

    private Node createModernDeviceRow(BroadcastDevice device, boolean isActive) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15, 20, 15, 20));
        row.setStyle("-fx-background-color: white; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;");
        
        // --- DEVICE INFO ---
        VBox deviceBox = new VBox(4);
        deviceBox.setPrefWidth(300);
        
        String iconPath = ICON_MONITOR;
        if ("MOBILE".equals(device.deviceType())) iconPath = ICON_PHONE;
        else if ("TABLET".equals(device.deviceType())) iconPath = ICON_TABLET;
        
        HBox nameLine = new HBox(12);
        nameLine.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconCircle = new VBox(createSVGIcon(iconPath, Color.web(isActive ? COLOR_PRIMARY : COLOR_TEXT_DIM), 20));
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.setPrefSize(40, 40);
        iconCircle.setStyle("-fx-background-color: " + (isActive ? COLOR_BLUE_LIGHT : "#f1f2f6") + "; -fx-background-radius: 10;");
        
        VBox labels = new VBox(2);
        Label name = new Label(device.name());
        name.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label os = new Label(device.os() + " • " + device.deviceType());
        os.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        labels.getChildren().addAll(name, os);
        
        nameLine.getChildren().addAll(iconCircle, labels);
        deviceBox.getChildren().add(nameLine);

        // --- NETWORK ---
        VBox netBox = new VBox(4);
        netBox.setPrefWidth(180);
        Label ip = new Label(device.ip());
        ip.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-font-size: 13px;");
        
        HBox statusLine = new HBox(6);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4, Color.web(isActive ? COLOR_SUCCESS : COLOR_DANGER));
        Label status = new Label(device.isBanned() ? "ЗАБЛОКОВАНО" : (isActive ? "В мережі" : "Поза мережею"));
        status.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (isActive ? COLOR_SUCCESS : COLOR_TEXT_DIM) + ";");
        statusLine.getChildren().addAll(dot, status);
        netBox.getChildren().addAll(ip, statusLine);

        // --- LAST SEEN ---
        Label lastSeen = new Label(device.lastSeen());
        lastSeen.setPrefWidth(200);
        lastSeen.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-weight: bold;");

        // --- ACTIONS ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = createIconButton(ICON_EDIT, COLOR_PRIMARY, () -> editDeviceName(device));
        Button banBtn = new Button(device.isBanned() ? "ДОЗВОЛИТИ" : "БЛОКУВАТИ");
        banBtn.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-background-color: " + (device.isBanned() ? COLOR_SUCCESS : COLOR_DANGER) + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 10; -fx-cursor: hand;");
        banBtn.setOnAction(e -> toggleBan(device));
        
        Button delBtn = createIconButton(ICON_TRASH, COLOR_DANGER, () -> deleteDevice(device));
        
        actions.getChildren().addAll(editBtn, banBtn, delBtn);
        
        row.getChildren().addAll(deviceBox, netBox, lastSeen, actions);
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #fafafa; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: white; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;"));
        
        return row;
    }

    private Button createIconButton(String icon, String color, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(icon, Color.web(color), 16));
        btn.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + color + "25; -fx-background-radius: 8; -fx-padding: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 8; -fx-padding: 8;"));
        return btn;
    }

    private void editDeviceName(BroadcastDevice device) {
        TextInputDialog dialog = new TextInputDialog(device.name());
        dialog.setTitle("Налаштування пристрою");
        dialog.setHeaderText("Змінити назву для " + device.ip());
        dialog.setContentText("Назва:");
        dialog.showAndWait().ifPresent(newName -> {
            DatabaseManager.saveBroadcastDevice(new BroadcastDevice(device.ip(), newName, device.isBanned(), device.deviceType(), device.os(), device.lastSeen()));
            refreshDevices();
        });
    }

    private void toggleBan(BroadcastDevice device) {
        DatabaseManager.saveBroadcastDevice(new BroadcastDevice(device.ip(), device.name(), !device.isBanned(), device.deviceType(), device.os(), device.lastSeen()));
        if (mainApp.getBroadcastService() != null) {
            mainApp.getBroadcastService().loadBannedIps();
            if (!device.isBanned()) {
                mainApp.getBroadcastService().getConnections().stream()
                        .filter(c -> c.getRemoteSocketAddress().getAddress().getHostAddress().equals(device.ip()))
                        .forEach(c -> c.close(4003, "IP is banned"));
            }
        }
        refreshDevices();
    }

    private void deleteDevice(BroadcastDevice device) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити історію для " + device.ip() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                DatabaseManager.deleteBroadcastDevice(device.ip());
                refreshDevices();
            }
        });
    }

    private void save() {
        config.setBroadcastEnabled(broadcastEnableCb.isSelected());
        config.setSchoolName(schoolNameField.getText());
        config.setCityName(cityField.getText());
        try { config.setBroadcastPort(Integer.parseInt(portField.getText())); } catch (Exception e) {}
        mainApp.saveConfig();
        mainApp.addLog("Налаштування оновлено.", "SUCCESS");
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

        // Escape double quotes for PowerShell argument
        String escapedScript = script.replace("\"", "\\\"");
        String command = "powershell -Command \"Start-Process powershell -Verb RunAs -ArgumentList '-Command \"" + escapedScript + "\"'\"";

        try {
            Runtime.getRuntime().exec(command);
            
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
