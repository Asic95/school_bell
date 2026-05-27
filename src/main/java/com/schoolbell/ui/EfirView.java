package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Announcement;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.AnnouncementService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.DatabaseManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class EfirView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final AnnouncementService announcementService;
    
    private final VBox announcementsContainer;
    private final VBox deviceListContainer;
    private final Label uptimeLabel;
    private final Label connectionsLabel;
    private final Label wsStatusLabel;
    private final LocalDateTime startTime;

    private final TextField schoolNameField;
    private final TextField cityNameField;
    private final TextField portField;
    private final ComboBox<String> themeCombo;
    private final Label firewallStatusLabel;
    private boolean showArchivedAnnouncements = false;

    private Label addrLabel;
    private HBox liveStatusCard;
    private StackPane liveStatusBadge;
    private Label liveStatusLabelText;
    private Circle liveStatusDotCore;
    private Circle liveStatusDotGlow;
    private Timeline pulseTimeline;

    public EfirView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.announcementService = new AnnouncementService();
        this.startTime = LocalDateTime.now();
        
        this.announcementsContainer = new VBox(15);
        this.deviceListContainer = new VBox(12);
        this.uptimeLabel = new Label("00:00:00");
        this.connectionsLabel = new Label("0 пристроїв");
        this.wsStatusLabel = new Label("Підключено");

        this.schoolNameField = ControlFactory.createStyledField(config.getSchoolName());
        this.cityNameField = ControlFactory.createStyledField(config.getCityName());
        this.portField = ControlFactory.createStyledField(String.valueOf(config.getBroadcastPort()));
        
        this.themeCombo = new ComboBox<>();
        this.themeCombo.getItems().addAll("classic", "modern", "cyber", "soft");
        this.themeCombo.setValue(config.getDashboardTheme());
        this.themeCombo.setStyle(PREMIUM_SELECT_STYLE);
        this.themeCombo.setPrefWidth(200);

        this.firewallStatusLabel = new Label("ПЕРЕВІРКА...");
        
        schoolNameField.setPromptText("Назва закладу");
        cityNameField.setPromptText("Місто");
        portField.setPrefWidth(120);

        setupAutoRefresh();
        updateFirewallStatusLabel();
    }

    private void setupAutoRefresh() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateUptime();
            updateMetrics();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    public Node build() {
        VBox root = new VBox(28);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // --- STANDARD HEADER ---
        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ЗМІНИ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(e -> {
            try {
                config.setSchoolName(schoolNameField.getText());
                config.setCityName(cityNameField.getText());
                config.setBroadcastPort(Integer.parseInt(portField.getText()));
                config.setDashboardTheme(themeCombo.getValue());
                
                mainApp.saveConfig();
                if (config.isBroadcastEnabled()) {
                    mainApp.startBroadcastServers();
                } else {
                    mainApp.stopBroadcastServers();
                }
                ToastService.showSuccess("Налаштування ефіру збережено");
                updateFirewallStatusLabel();
                if (addrLabel != null) {
                    addrLabel.setText("http://" + getLocalIp() + ":" + config.getBroadcastPort());
                }
            } catch (Exception ex) {
                ToastService.showError("Помилка збереження: " + ex.getMessage());
            }
        });

        HBox header = createPageHeader(
            "ЦЕНТР КЕРУВАННЯ ЕФІРОМ",
            "Керування ефіром",
            "Контроль трансляції, планування оголошень та моніторинг підключених пристроїв.",
            ICON_BROADCAST,
            COLOR_INDIGO,
            saveBtn
        );

        // --- TOP STATUS BAR ---
        Pane statusBar = createTopStatusBar();

        // --- MAIN CONTENT AREA ---
        HBox mainContent = new HBox(28);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Left Column: Announcements (Flexible)
        VBox leftCol = createAnnouncementsSection();
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setMinWidth(400);

        // Right Column: Devices (Stable Sidebar)
        VBox rightCol = createSideSection();
        rightCol.setPrefWidth(480); 
        rightCol.setMinWidth(480);
        rightCol.setMaxWidth(480);

        mainContent.getChildren().addAll(leftCol, rightCol);

        // --- BOTTOM NETWORK SECTION ---
        VBox networkSection = createNetworkSection();

        root.getChildren().addAll(header, statusBar, mainContent, networkSection);

        refreshAll();

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scrollPane;
    }

    private Pane createTopStatusBar() {
        StackPane container = new StackPane();
        
        Region bg = new Region();
        bg.setStyle(SOFT_CARD + "-fx-background-radius: 28; -fx-border-radius: 28;");
        bg.setCache(true);
        bg.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        HBox bar = new HBox(24);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 28, 16, 28));
        bar.setStyle("-fx-background-color: transparent;");

        HBox liveStatus = createLiveStatusWidget();
        Region divider1 = createVerticalDivider();
        HBox connStatus = createMonitoringWidget();
        Region divider2 = createVerticalDivider();
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
        openBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://localhost:" + config.getBroadcastPort()));

        bar.getChildren().addAll(liveStatus, divider1, connStatus, divider2, broadcastAddr, spacer, broadcastToggle, openBtn);
        
        container.getChildren().addAll(bg, bar);
        return container;
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

    private VBox createNetworkSection() {
        VBox section = new VBox(22);
        Label title = new Label("МЕРЕЖА ТА ТРАНСЛЯЦІЯ");
        title.setStyle(HEADER_STYLE);

        HBox mainRow = new HBox(24);
        mainRow.setAlignment(Pos.TOP_LEFT);

        mainRow.getChildren().addAll(
            createModernSettingsGroup("ЗАКЛАД", ICON_SCHOOL, COLOR_INDIGO, new VBox(12, createLabeledField("НАЗВА", schoolNameField), createLabeledField("МІСТО", cityNameField))),
            createModernSettingsGroup("WEBSOCKET", ICON_LINK, COLOR_VIOLET, new VBox(12, createLabeledField("ПОРТ ТРАНСЛЯЦІЇ", portField))),
            createModernSettingsGroup("ТЕМА", ICON_SETTINGS, COLOR_PRIMARY, new VBox(12, createLabeledField("ДИЗАЙН ТАБЛО", themeCombo))),
            createModernSettingsGroup("FIREWALL", ICON_SHIELD, COLOR_TEAL_DARK, new VBox(15, firewallStatusLabel, createPrimaryActionButton("ОПТИМІЗУВАТИ", ICON_SHIELD)))
        );
        
        mainRow.getChildren().forEach(n -> ((VBox)n).setMinWidth(320));

        section.getChildren().addAll(title, mainRow);
        return section;
    }

    private HBox createLiveStatusWidget() {
        liveStatusCard = new HBox(12);
        liveStatusCard.setAlignment(Pos.TOP_LEFT);
        liveStatusCard.setPadding(new Insets(10, 18, 10, 14));
        
        liveStatusBadge = new StackPane();
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
        
        uptimeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + ";");
        
        contentRow.getChildren().addAll(liveStatusLabelText, bullet, uptimeLabel);
        textCol.getChildren().addAll(eyebrow, contentRow);

        // Optimization: No animation timeline; static appearance will reduce GPU load
        pulseTimeline = null;
        
        // Apply initial live status style based on configuration
        updateLiveStatusStyle(config.isBroadcastEnabled());
        
        HBox.setMargin(textCol, new Insets(4, 0, 0, 0));
        liveStatusCard.getChildren().addAll(liveStatusBadge, textCol);
        
        return liveStatusCard;
    }

    private void updateLiveStatusStyle(boolean active) {
        String baseColor = active ? COLOR_SUCCESS : COLOR_DANGER;
        String text = active ? "АКТИВНИЙ" : "ВИМКНЕНО";
        
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
        
        connectionsLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_SLATE_MUTED + ";");
        
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

    private void updateFirewallStatusLabel() {
        firewallStatusLabel.setText("ПЕРЕВІРКА...");
        firewallStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-font-family: 'Inter'; -fx-text-fill: " + COLOR_SLATE_LIGHT + ";");

        new Thread(() -> {
            try {
                int port = Integer.parseInt(portField.getText());
                boolean allowed = mainApp.getSystemService().isPortAllowedInFirewall(port);
                javafx.application.Platform.runLater(() -> {
                    firewallStatusLabel.setText(allowed ? "ВІДКРИТО" : "ЗАБЛОКОВАНО");
                    firewallStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-font-family: 'Inter'; -fx-text-fill: " + (allowed ? COLOR_SUCCESS : COLOR_DANGER) + ";");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    firewallStatusLabel.setText("НЕВІДОМО");
                    firewallStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-font-family: 'Inter'; -fx-text-fill: " + COLOR_SLATE_LIGHT + ";");
                });
            }
        }).start();
    }

    private VBox createAnnouncementsSection() {
        VBox card = new VBox(25);
        card.setPadding(new Insets(28));
        card.setStyle(SOFT_CARD);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(4);
        Label eyebrow = new Label("ОГОЛОШЕННЯ ТА ПОВІДОМЛЕННЯ");
        eyebrow.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        Label title = new Label("Стрічка ефіру");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        titleBox.getChildren().addAll(eyebrow, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter Toggle Group
        HBox toggleGroup = new HBox(0);
        toggleGroup.setAlignment(Pos.CENTER);
        toggleGroup.setStyle(PREMIUM_TOGGLE_CONTAINER);
        
        ToggleButton activeBtn = new ToggleButton("Активні");
        ToggleButton archiveBtn = new ToggleButton("Архів");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        activeBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
        archiveBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);

        group.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == activeBtn) {
                activeBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
                archiveBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);
                showArchivedAnnouncements = false;
            } else {
                activeBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);
                archiveBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
                showArchivedAnnouncements = true;
            }
            refreshAnnouncements();
        });
        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        Button addBtn = createSmallPrimaryActionButton("СТВОРИТИ", ICON_PLUS);
        addBtn.setOnAction(e -> openEditDialog(null));

        HBox actionsRow = new HBox(20, toggleGroup, addBtn);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(titleBox, spacer, actionsRow);

        announcementsContainer.setPadding(new Insets(5, 0, 0, 0));
        card.getChildren().addAll(header, announcementsContainer);

        return card;
    }

    private VBox createSideSection() {
        VBox section = new VBox(28);

        // Devices Card
        VBox devicesCard = new VBox(20);
        devicesCard.setPadding(new Insets(28));
        devicesCard.setStyle(SOFT_CARD);

        HBox devicesHeader = new HBox(15);
        devicesHeader.setAlignment(Pos.CENTER_LEFT);
        Label dTitle = new Label("ПРИСТРОЇ ТА МОНІТОРИ");
        dTitle.setStyle(HEADER_STYLE);
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Button refreshBtn = new Button("ОНОВИТИ");
        refreshBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.web(COLOR_PRIMARY), 16));
        refreshBtn.setGraphicTextGap(10);
        String refreshBtnBase = "-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 8 16; -fx-background-radius: 12; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 12; -fx-cursor: hand;";
        refreshBtn.setStyle(refreshBtnBase);
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(refreshBtnBase + "-fx-background-color: " + COLOR_SURFACE_GLASS_START + "; -fx-border-color: " + COLOR_PRIMARY + ";"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle(refreshBtnBase));
        refreshBtn.setOnAction(e -> refreshDevices());
        devicesHeader.getChildren().addAll(dTitle, s, refreshBtn);

        deviceListContainer.setPadding(new Insets(5, 0, 0, 0));
        devicesCard.getChildren().addAll(devicesHeader, deviceListContainer);

        section.getChildren().addAll(devicesCard);
        return section;
    }

    private void refreshAll() {
        refreshAnnouncements();
        refreshDevices();
    }

    private void refreshAnnouncements() {
        List<Announcement> filtered = announcementService.getAllAnnouncements().stream()
                .filter(a -> a.isActive() != showArchivedAnnouncements)
                .toList();
        
        if (filtered.isEmpty()) {
            String title = showArchivedAnnouncements ? "Архів порожній" : "Оголошень немає";
            String sub = showArchivedAnnouncements ? "Тут з'являтимуться оголошення, термін дії яких минув." : "Натисніть 'СТВОРИТИ', щоб додати перше інформаційне повідомлення.";
            announcementsContainer.getChildren().setAll(createEmptyState(ICON_INFO, title, sub));
        } else {
            updateContainer(announcementsContainer, filtered, a -> new AnnouncementCard(a, () -> openEditDialog(a), () -> {
                announcementService.deleteAnnouncement(a.id());
                refreshAnnouncements();
            }));
        }
    }

    private void refreshDevices() {
        List<String> activeIps = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnectedClients() : List.of();
        List<BroadcastDevice> savedDevices = DatabaseManager.getAllBroadcastDevices();

        updateContainer(deviceListContainer, savedDevices, device -> new DeviceRow(device, activeIps.contains(device.ip()), 
            () -> {
                // Edit logic
                TextInputModalDialog dialog = new TextInputModalDialog(mainApp, "Редагування пристрою", "Введіть нову назву для " + device.ip(), device.name(), "Назва пристрою", newName -> {
                    BroadcastDevice updated = new BroadcastDevice(device.ip(), newName, device.isBanned(), device.deviceType(), device.os(), device.lastSeen());
                    DatabaseManager.saveBroadcastDevice(updated);
                    refreshDevices();
                });
                dialog.show();
            },
            () -> {
                // Ban/Unban logic
                boolean newBannedState = !device.isBanned();
                BroadcastDevice toggled = new BroadcastDevice(device.ip(), device.name(), newBannedState, device.deviceType(), device.os(), device.lastSeen());
                DatabaseManager.saveBroadcastDevice(toggled);
                if (mainApp.getBroadcastService() != null) {
                    mainApp.getBroadcastService().loadBannedIps();
                    if (newBannedState) {
                        mainApp.getBroadcastService().getConnections().stream()
                                .filter(c -> c.getRemoteSocketAddress() != null && c.getRemoteSocketAddress().getAddress().getHostAddress().equals(device.ip()))
                                .forEach(c -> c.close(4003, "IP is banned"));
                    }
                }
                refreshDevices();
            },
            () -> {
                // Delete logic
                DatabaseManager.deleteBroadcastDevice(device.ip());
                refreshDevices();
            }
        ));
    }

    private <T> void updateContainer(Pane container, List<T> items, java.util.function.Function<T, Node> mapper) {
        container.getChildren().setAll(items.stream().map(mapper).toList());
    }

    private void updateUptime() {
        Duration diff = Duration.seconds(java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds());
        long s = (long) diff.toSeconds();
        uptimeLabel.setText(String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60));
    }

    private void updateMetrics() {
        int count = mainApp.getBroadcastService() != null ? mainApp.getBroadcastService().getConnections().size() : 0;
        connectionsLabel.setText(formatDevicesCount(count));

        boolean isWsOk = mainApp.getBroadcastService() != null && mainApp.getBroadcastService().isBroadcasting();
        wsStatusLabel.setText(isWsOk ? "Підключено" : "Помилка");
        wsStatusLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: " + (isWsOk ? COLOR_SUCCESS : COLOR_DANGER) + ";");
    }

    private String getLocalIp() {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
            socket.connect(java.net.InetAddress.getByName("192.0.2.1"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) { return "127.0.0.1"; }
    }

    private void openEditDialog(Announcement a) {
        new AnnouncementEditorDialog(mainApp.getStage(), announcementService, a, this::refreshAnnouncements).display();
    }
}
