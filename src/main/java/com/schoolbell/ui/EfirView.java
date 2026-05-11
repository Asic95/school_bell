package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.DatabaseManager;
import com.schoolbell.ui.editor.AnnouncementsEditorTab;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class EfirView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final AnnouncementsEditorTab announcementsEditor;
    
    private final CheckBox broadcastEnableCb;
    private final VBox deviceListContainer;

    public EfirView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.announcementsEditor = new AnnouncementsEditorTab(mainApp);
        
        this.broadcastEnableCb = new CheckBox("Увімкнути трансляцію");
        this.broadcastEnableCb.setSelected(config.isBroadcastEnabled());
        this.broadcastEnableCb.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        
        this.deviceListContainer = new VBox(10);
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ НАЛАШТУВАННЯ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Керування ефіром",
                "Контроль трансляції, планування оголошень та моніторинг пристроїв",
                "#6c5ce7",
                ICON_BROADCAST,
                saveBtn
        );

        // --- BROADCAST CONTROL CENTER (REDESIGNED HERO CARD) ---
        HBox heroCard = new HBox(40);
        heroCard.setAlignment(Pos.CENTER_LEFT);
        heroCard.setPadding(new Insets(25, 35, 25, 35));
        heroCard.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 20, 0, 0, 8); -fx-border-color: #f1f2f6; -fx-border-width: 1;");

        // 1. Live Indicator Section
        VBox statusSection = new VBox(8);
        statusSection.setAlignment(Pos.CENTER);
        
        StackPane indicatorStack = new StackPane();
        Circle pulseCircle = new Circle(12, Color.web(config.isBroadcastEnabled() ? COLOR_SUCCESS : COLOR_DANGER, 0.2));
        Circle mainCircle = new Circle(6, Color.web(config.isBroadcastEnabled() ? COLOR_SUCCESS : COLOR_DANGER));
        
        if (config.isBroadcastEnabled()) {
            javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.seconds(1.5), pulseCircle);
            pulse.setFromX(1); pulse.setFromY(1);
            pulse.setToX(2.5); pulse.setToY(2.5);
            pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            pulse.setAutoReverse(false);
            
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), pulseCircle);
            fade.setFromValue(0.6); fade.setToValue(0);
            fade.setCycleCount(javafx.animation.Animation.INDEFINITE);
            fade.setAutoReverse(false);
            
            pulse.play(); fade.play();
        }
        indicatorStack.getChildren().addAll(pulseCircle, mainCircle);
        
        Label statusLbl = new Label(config.isBroadcastEnabled() ? "LIVE" : "OFFLINE");
        statusLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 10px; -fx-letter-spacing: 1.5; -fx-text-fill: " + (config.isBroadcastEnabled() ? COLOR_SUCCESS : COLOR_DANGER) + ";");
        
        statusSection.getChildren().addAll(indicatorStack, statusLbl);

        // 2. Metrics Grid
        HBox metricsGrid = new HBox(50);
        metricsGrid.setAlignment(Pos.CENTER_LEFT);

        // Connections
        int connectedCount = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnections().size() : 0;
        VBox m1 = createMetricItem("ПІДКЛЮЧЕНО", connectedCount + " ПРИСТРОЇВ", ICON_MONITOR, COLOR_PURPLE);
        
        // IP Address
        VBox m2 = createMetricItem("АДРЕСА ТАБЛО", "http://" + getLocalIp() + ":" + config.getBroadcastPort(), ICON_BROADCAST, COLOR_PRIMARY);
        
        metricsGrid.getChildren().addAll(m1, m2);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Quick Actions
        VBox actionsArea = new VBox(12);
        actionsArea.setAlignment(Pos.CENTER_RIGHT);
        
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = new Button("ВІДКРИТИ ТАБЛО");
        openBtn.setGraphic(createSVGIcon(ICON_MONITOR, Color.WHITE, 16));
        openBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 12px; -fx-padding: 12 25; -fx-background-radius: 14; -fx-cursor: hand;");
        openBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://localhost:" + config.getBroadcastPort()));
        
        // Hover effect for openBtn
        openBtn.setOnMouseEntered(e -> openBtn.setStyle(openBtn.getStyle() + "-fx-effect: dropshadow(three-pass-box, " + COLOR_PRIMARY + "66, 12, 0, 0, 4);"));
        openBtn.setOnMouseExited(e -> openBtn.setStyle(openBtn.getStyle().replace("-fx-effect: dropshadow(three-pass-box, " + COLOR_PRIMARY + "66, 12, 0, 0, 4);", "")));

        broadcastEnableCb.setText(config.isBroadcastEnabled() ? "ТРАНСЛЯЦІЯ УВІМКНЕНА" : "ТРАНСЛЯЦІЯ ВИМКНЕНА");
        broadcastEnableCb.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT + "; -fx-background-color: #f1f2f6; -fx-padding: 10 20; -fx-background-radius: 14;");
        broadcastEnableCb.setOnAction(e -> {
            config.setBroadcastEnabled(broadcastEnableCb.isSelected());
            mainApp.saveConfig();
            mainApp.showEfir(); // Refresh UI
        });

        actionButtons.getChildren().addAll(broadcastEnableCb, openBtn);
        actionsArea.getChildren().add(actionButtons);

        heroCard.getChildren().addAll(statusSection, metricsGrid, spacer, actionsArea);

        // --- MAIN CONTENT (2 COLUMNS) ---
        HBox content = new HBox(25);
        VBox leftCol = new VBox(25);
        VBox.setVgrow(leftCol, Priority.ALWAYS);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        VBox rightCol = new VBox(25);
        rightCol.setPrefWidth(550); // Wider for full actions

        // --- LEFT: ANNOUNCEMENTS ---
        VBox scheduledContainer = new VBox();
        scheduledContainer.getChildren().add(announcementsEditor.createContent());
        scheduledContainer.setStyle(SOFT_CARD);
        
        Node editorContent = scheduledContainer.getChildren().get(0);
        if (editorContent instanceof VBox vb) {
            vb.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
            vb.setPadding(new Insets(20));
            if (!vb.getChildren().isEmpty()) vb.getChildren().remove(0); 
            Label schedTitle = new Label("ЗАПЛАНОВАНІ ПОВІДОМЛЕННЯ");
            schedTitle.setStyle(SUB_HEADER_STYLE);
            vb.getChildren().add(0, schedTitle);
        }

        leftCol.getChildren().add(scheduledContainer);

        // --- RIGHT: DEVICES ---
        VBox devicesCard = createSettingsSection("МОНІТОРИНГ ПРИСТРОЇВ", "#6c5ce7", ICON_MONITOR);
        devicesCard.setStyle(SOFT_CARD);
        devicesCard.setPadding(new Insets(25));
        
        Button refreshBtn = new Button("ОНОВИТИ");
        refreshBtn.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: bold; -fx-background-color: transparent; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> refreshDevices());
        
        HBox devHeader = (HBox) devicesCard.getChildren().get(0);
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        devHeader.getChildren().addAll(s2, refreshBtn);
        
        ScrollPane devScroll = new ScrollPane(deviceListContainer);
        devScroll.setFitToWidth(true);
        devScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        devScroll.setPrefHeight(600);
        
        devicesCard.getChildren().add(devScroll);
        VBox.setVgrow(devScroll, Priority.ALWAYS);

        rightCol.getChildren().add(devicesCard);

        content.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(headerArea, heroCard, content);

        refreshDevices();
        
        ScrollPane mainScroll = new ScrollPane(root);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return mainScroll;
    }

    private VBox createMetricItem(String title, String value, String icon, String color) {
        VBox box = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1;");
        
        HBox valBox = new HBox(10);
        valBox.setAlignment(Pos.CENTER_LEFT);
        
        Node iconNode = createSVGIcon(icon, Color.web(color), 18);
        Label v = new Label(value);
        v.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + "; -fx-font-family: 'Monospaced';");
        
        valBox.getChildren().addAll(iconNode, v);
        box.getChildren().addAll(t, valBox);
        return box;
    }

    private HBox createStatusSubCard(String title, boolean active) {
        VBox info = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        
        HBox statusLine = new HBox(8);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        
        Circle dot = new Circle(5, Color.web(active ? COLOR_SUCCESS : COLOR_DANGER));
        if (active) {
            javafx.animation.FadeTransition pulse = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1), dot);
            pulse.setFromValue(1.0); pulse.setToValue(0.3);
            pulse.setAutoReverse(true); pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            pulse.play();
        }
        
        Label statusText = new Label(active ? "В ЕФІРІ" : "ОФЛАЙН");
        statusText.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + (active ? COLOR_SUCCESS : COLOR_DANGER) + ";");
        statusLine.getChildren().addAll(dot, statusText);
        
        info.getChildren().addAll(t, statusLine);
        return wrapInCard(info, ICON_SIGNAL, active ? "#e8f5e9" : "#ffebee", active ? COLOR_SUCCESS : COLOR_DANGER);
    }

    private HBox wrapInCard(Node content, String icon, String bgColor, String iconColor) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 20, 10, 15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #f1f2f6; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 8, 0, 0, 3);");
        
        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(iconColor), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");
        
        card.getChildren().addAll(iconBox, content);
        return card;
    }

    private void save() {
        config.setBroadcastEnabled(broadcastEnableCb.isSelected());
        mainApp.saveConfig();
        ToastService.showSuccess("Налаштування ефіру збережено!");
    }

    private void refreshDevices() {
        deviceListContainer.getChildren().clear();
        List<String> activeIps = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnectedClients() : List.of();
        List<BroadcastDevice> savedDevices = DatabaseManager.getAllBroadcastDevices();
        
        if (savedDevices.isEmpty() && activeIps.isEmpty()) {
            Label none = new Label("Немає підключених моніторів");
            none.setStyle("-fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-style: italic; -fx-padding: 20;");
            deviceListContainer.getChildren().add(none);
        } else {
            for (BroadcastDevice device : savedDevices) {
                deviceListContainer.getChildren().add(createDeviceFullRow(device, activeIps.contains(device.ip())));
            }
        }
    }

    private Node createDeviceFullRow(BroadcastDevice device, boolean isActive) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #f1f2f6; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 5, 0, 0, 2);");
        
        // --- ICON BOX (SQUARE STYLE) ---
        String iconPath = ICON_MONITOR;
        if ("MOBILE".equals(device.deviceType())) iconPath = ICON_PHONE;
        else if ("TABLET".equals(device.deviceType())) iconPath = ICON_TABLET;

        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(isActive ? COLOR_PRIMARY : COLOR_NEUTRAL), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(52, 52);
        iconBox.setMinSize(52, 52);
        iconBox.setStyle("-fx-background-color: " + (isActive ? COLOR_BLUE_LIGHT : "#f1f2f6") + "; -fx-background-radius: 14;");

        // --- INFO BOX ---
        VBox info = new VBox(4);
        Label name = new Label(device.name());
        name.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT + ";");
        
        HBox ipLine = new HBox(8);
        ipLine.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4, Color.web(isActive ? COLOR_SUCCESS : COLOR_DANGER));
        Label ip = new Label(device.ip() + (device.isBanned() ? " [ЗАБЛОКОВАНО]" : ""));
        ip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (device.isBanned() ? COLOR_DANGER : "#57606f") + "; -fx-font-family: 'Monospaced';");
        ipLine.getChildren().addAll(dot, ip);
        
        info.getChildren().addAll(name, ipLine);
        HBox.setHgrow(info, Priority.ALWAYS);

        // --- ACTIONS (TEACHER STYLE) ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = createSoftActionButton(ICON_EDIT, "#636e72", "#dfe6e9", "#f1f2f6", "#b2bec3", () -> editDevice(device));
        Button banBtn = createSoftActionButton(device.isBanned() ? ICON_CHECK : ICON_BAN, device.isBanned() ? COLOR_SUCCESS : COLOR_PRIMARY, "#dfe6e9", "#f1f2f6", COLOR_PRIMARY, () -> toggleBan(device));
        Button delBtn = createSoftActionButton(ICON_TRASH, "#ff7675", "#ffeaa7", "#fff5f5", COLOR_DANGER, () -> deleteDevice(device));

        actions.getChildren().addAll(editBtn, banBtn, delBtn);
        
        row.getChildren().addAll(iconBox, info, actions);
        
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 16; -fx-border-color: #dfe6e9; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #f1f2f6; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 5, 0, 0, 2);"));
        
        return row;
    }

    private Button createSoftActionButton(String icon, String iconColor, String borderColor, String hoverBg, String hoverBorder, Runnable action) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(icon, Color.web(iconColor), 18));
        btn.setPrefSize(36, 36);
        String baseStyle = "-fx-background-color: white; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 4, 0, 0, 1);";
        btn.setStyle(baseStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-background-color: " + hoverBg + "; -fx-border-color: " + hoverBorder + ";"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void editDevice(BroadcastDevice device) {
        TextInputDialog dialog = new TextInputDialog(device.name());
        dialog.setTitle("Пристрій");
        dialog.setHeaderText("Перейменувати пристрій " + device.ip());
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
        }
        refreshDevices();
    }

    private void deleteDevice(BroadcastDevice device) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Видалити " + device.ip() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                DatabaseManager.deleteBroadcastDevice(device.ip());
                refreshDevices();
            }
        });
    }


    private String getLocalIp() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("192.0.2.1"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) { return "127.0.0.1"; }
    }
}
