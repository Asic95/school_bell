package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;

import static com.schoolbell.ui.ControlFactory.createSmallPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class EfirStatusCard extends StackPane {
    private final MainApp mainApp;
    private final ConfigService config;
    private final LocalDateTime startTime;

    private Label uptimeLabel;
    private Label connectionsLabel;
    private Label wsStatusLabel;
    private Label addrLabel;
    
    private Label liveStatusLabelText;
    private Circle liveStatusDotCore;
    private Circle liveStatusDotGlow;

    private HBox bar;
    private HBox connStatus;
    private Region divider1;
    private Region divider2;

    public EfirStatusCard(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.startTime = LocalDateTime.now();

        Region bg = new Region();
        bg.setStyle(SOFT_CARD + "-fx-background-radius: 28; -fx-border-radius: 28;");
        bg.setCache(true);
        bg.setCacheHint(javafx.scene.CacheHint.SPEED);

        bar = new HBox(24);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 28, 16, 28));
        bar.setStyle("-fx-background-color: transparent;");

        HBox liveStatus = createLiveStatusWidget();
        divider1 = createVerticalDivider();
        connStatus = createMonitoringWidget();
        divider2 = createVerticalDivider();
        HBox broadcastAddr = createAddressWidget();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox broadcastToggle = new HBox(12);
        broadcastToggle.setAlignment(Pos.CENTER_LEFT);
        ToggleButton toggle = createToggleSwitch(config.isBroadcastEnabled());
        toggle.setOnAction(e -> {
            config.setBroadcastEnabled(toggle.isSelected());
            mainApp.saveConfig();
            if (toggle.isSelected()) {
                mainApp.startBroadcastServers();
            } else {
                mainApp.stopBroadcastServers();
            }
            updateLiveStatusStyle(toggle.isSelected());
        });
        Label toggleLabel = new Label("ТРАНСЛЯЦІЯ");
        toggleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        broadcastToggle.getChildren().addAll(toggle, toggleLabel);

        Button openBtn = createSmallPrimaryActionButton("ВІДКРИТИ ТАБЛО", ICON_EXTERNAL_LINK);
        openBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://" + getLocalIp() + ":" + config.getBroadcastPort()));

        bar.getChildren().addAll(liveStatus, divider1, connStatus, divider2, broadcastAddr, spacer, broadcastToggle, openBtn);
        
        getChildren().addAll(bg, bar);
        
        updateMetrics(); // Initial update
        setupAutoRefresh();
    }

    public void setCompactMode(boolean compact) {
        if (connStatus != null) {
            connStatus.setVisible(!compact);
            connStatus.setManaged(!compact);
        }
        if (divider1 != null) {
            divider1.setVisible(!compact);
            divider1.setManaged(!compact);
        }
        if (divider2 != null) {
            divider2.setVisible(!compact);
            divider2.setManaged(!compact);
        }
        
        if (bar != null) {
            bar.setSpacing(compact ? 12 : 24);
            bar.setPadding(new Insets(16, compact ? 15 : 28, 16, compact ? 15 : 28));
        }
    }

    private HBox createLiveStatusWidget() {
        HBox liveStatusCard = new HBox(12);
        liveStatusCard.setAlignment(Pos.TOP_LEFT);
        liveStatusCard.setPadding(new Insets(10, 18, 10, 14));
        
        StackPane liveStatusBadge = new StackPane();
        liveStatusBadge.setPrefSize(44, 44);
        liveStatusBadge.setMinSize(44, 44);
        liveStatusBadge.setMaxSize(44, 44);
        liveStatusBadge.setAlignment(Pos.CENTER);
        
        liveStatusDotGlow = new Circle(14);
        liveStatusDotGlow.setOpacity(0.3);
        liveStatusDotCore = new Circle(7);
        liveStatusBadge.getChildren().addAll(liveStatusDotGlow, liveStatusDotCore);
        
        VBox textCol = new VBox(1);
        textCol.setAlignment(Pos.TOP_LEFT);
        Label eyebrow = new Label("СТАТУС ЕФІРУ");
        eyebrow.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        
        HBox contentRow = new HBox(8);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        
        liveStatusLabelText = new Label();
        liveStatusLabelText.setStyle("-fx-font-size: 15px; -fx-font-weight: 900;");
        
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE_MUTED + ";");
        
        uptimeLabel = new Label("00:00:00");
        uptimeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + ";");
        
        contentRow.getChildren().addAll(liveStatusLabelText, bullet, uptimeLabel);
        textCol.getChildren().addAll(eyebrow, contentRow);

        updateLiveStatusStyle(config.isBroadcastEnabled());
        
        HBox.setMargin(textCol, new Insets(4, 0, 0, 0));
        liveStatusCard.getChildren().addAll(liveStatusBadge, textCol);
        
        return liveStatusCard;
    }

    private void updateLiveStatusStyle(boolean active) {
        String baseColor = active ? COLOR_SUCCESS : COLOR_DANGER;
        String text = active ? "АКТИВНИЙ" : "ВИМКНЕНО";
        
        if (liveStatusDotCore == null) return;

        Color fxColor = Color.web(baseColor);
        liveStatusDotCore.setFill(fxColor);
        liveStatusDotGlow.setFill(fxColor);
        liveStatusLabelText.setText(text);
        liveStatusLabelText.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + baseColor + ";");
    }

    private HBox createMonitoringWidget() {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(10, 18, 10, 14));
        
        VBox iconBox = new VBox(createSVGIcon(ICON_MONITOR, Color.web(COLOR_PRIMARY), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + COLOR_PRIMARY + "15; -fx-background-radius: 12;");
        
        VBox textCol = new VBox(1);
        textCol.setAlignment(Pos.TOP_LEFT);
        Label eyebrow = new Label("МОНІТОРИНГ");
        eyebrow.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        
        HBox contentRow = new HBox(8);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        
        connectionsLabel = new Label("");
        connectionsLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE_MUTED + ";");
        
        wsStatusLabel = new Label("");
        wsStatusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800;");
        
        contentRow.getChildren().addAll(connectionsLabel, bullet, wsStatusLabel);
        textCol.getChildren().addAll(eyebrow, contentRow);
        
        HBox.setMargin(textCol, new Insets(4, 0, 0, 0));
        card.getChildren().addAll(iconBox, textCol);
        
        return card;
    }

    private HBox createAddressWidget() {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(10, 18, 10, 14));
        
        String accentColor = COLOR_INDIGO;
        VBox iconBox = new VBox(createSVGIcon(ICON_LINK, Color.web(accentColor), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + accentColor + "15; -fx-background-radius: 12;");
        
        VBox textCol = new VBox(1);
        textCol.setAlignment(Pos.TOP_LEFT);
        Label eyebrow = new Label("АДРЕСА ТАБЛО");
        eyebrow.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        
        HBox valueRow = new HBox(8);
        valueRow.setAlignment(Pos.CENTER_LEFT);
        
        addrLabel = new Label("http://" + getLocalIp() + ":" + config.getBroadcastPort());
        addrLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + "; -fx-font-family: 'Inter';");
        
        Button copyBtn = new Button();
        copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_SLATE), 12));
        String copyBaseStyle = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + BORDER_SLATE_85 + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 4 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_03 + ", 5, 0, 0, 1);";
        copyBtn.setStyle(copyBaseStyle);
        copyBtn.setOnMouseEntered(e -> {
            copyBtn.setStyle(copyBaseStyle + "-fx-background-color: " + COLOR_SURFACE_SOFT + "; -fx-border-color: " + COLOR_PRIMARY + ";");
            copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_PRIMARY), 12));
        });
        copyBtn.setOnMouseExited(e -> {
            copyBtn.setStyle(copyBaseStyle);
            copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_SLATE), 12));
        });
        
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(addrLabel.getText());
            clipboard.setContent(content);
            ToastService.showSuccess("Адресу табло скопійовано");
        });
        
        Tooltip.install(copyBtn, new Tooltip("Скопіювати адресу"));
        
        valueRow.getChildren().addAll(addrLabel, copyBtn);
        textCol.getChildren().addAll(eyebrow, valueRow);
        
        HBox.setMargin(textCol, new Insets(4, 0, 0, 0));
        card.getChildren().addAll(iconBox, textCol);
        
        return card;
    }

    private Region createVerticalDivider() {
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(32);
        divider.setMinHeight(32);
        divider.setMaxHeight(32);
        divider.setStyle("-fx-background-color: " + COLOR_BORDER_SOFT + ";");
        return divider;
    }

    private void setupAutoRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateUptime();
            updateMetrics();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateUptime() {
        long s = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        safeSetText(uptimeLabel, String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
    }

    private void updateMetrics() {
        int count = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnections().size() : 0;
        safeSetText(connectionsLabel, formatDevicesCount(count));

        boolean isWsOk = mainApp.getBroadcastService() != null && mainApp.getBroadcastService().isBroadcasting();
        String statusText = isWsOk ? "Підключено" : "Неактивно";
        
        wsStatusLabel.setText(statusText);
        wsStatusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + (isWsOk ? COLOR_SUCCESS : COLOR_DANGER) + ";");
    }

    public void refreshAddress() {
        if (addrLabel != null) {
            addrLabel.setText("http://" + getLocalIp() + ":" + config.getBroadcastPort());
        }
    }

    private String formatDevicesCount(int count) {
        int lastDigit = count % 10;
        int lastTwoDigits = count % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
            return count + " пристроїв";
        }
        if (lastDigit == 1) {
            return count + " пристрій";
        }
        if (lastDigit >= 2 && lastDigit <= 4) {
            return count + " пристрої";
        }
        return count + " пристроїв";
    }

    private String getLocalIp() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("192.0.2.1"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) { return "127.0.0.1"; }
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }
}
