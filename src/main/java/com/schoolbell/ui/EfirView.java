package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Announcement;
import com.schoolbell.model.BroadcastDevice;
import com.schoolbell.service.AnnouncementService;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.DatabaseManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
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
    private ScaleTransition liveStatusGlowAnim;

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
        this.themeCombo.getItems().addAll("classic", "modern", "cyber");
        this.themeCombo.setValue(config.getDashboardTheme());
        this.themeCombo.setStyle(COMBO_STYLE + "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 14; -fx-border-radius: 14;");
        this.themeCombo.setPrefWidth(160);

        this.firewallStatusLabel = new Label("ПЕРЕВІРКА...");
        
        schoolNameField.setPromptText("Назва навчального закладу");
        cityNameField.setPromptText("Населений пункт");
        portField.setPrefWidth(80);

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
        Button saveBtn = createPrimaryActionButton("Зберегти зміни", ICON_SAVE);
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
            "#4f46e5",
            saveBtn
        );

        // --- TOP STATUS BAR ---
        HBox statusBar = createTopStatusBar();

        // --- MAIN CONTENT AREA ---
        HBox mainContent = new HBox(28);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Left Column: Announcements (Flexible)
        VBox leftCol = createAnnouncementsSection();
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setMinWidth(400);

        // Right Column: Devices (Stable Sidebar)
        VBox rightCol = createSideSection();
        rightCol.setPrefWidth(480); // Increased stable width
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

    private HBox createTopStatusBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 28, 14, 28));
        bar.setStyle("-fx-background-color: rgba(255,255,255,0.96); " +
                     "-fx-border-color: rgba(226, 232, 240, 0.7); " +
                     "-fx-border-width: 1; " +
                     "-fx-background-radius: 28; " +
                     "-fx-border-radius: 28; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 12, 0, 0, 4);"); // Reduced shadow for performance
        bar.setCache(true);
        bar.setCacheHint(javafx.scene.CacheHint.SPEED);

        HBox liveStatus = createLiveStatusWidget();
        Region divider1 = createVerticalDivider();
        HBox connStatus = createMonitoringWidget();
        Region divider2 = createVerticalDivider();
        HBox broadcastAddr = createAddressWidget();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Main Control
        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER_LEFT);

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
        Label toggleLabel = new Label("Трансляція");
        toggleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_TEXT + ";");
        broadcastToggle.getChildren().addAll(toggle, toggleLabel);

        Button openBtn = new Button("Відкрити табло");
        openBtn.setGraphic(createSVGIcon(ICON_EXTERNAL_LINK, Color.WHITE, 16));
        openBtn.setStyle("-fx-background-color: linear-gradient(to right, #4f46e5, #2563eb); " +
                         "-fx-text-fill: white; " +
                         "-fx-font-weight: 800; " +
                         "-fx-font-size: 13px; " +
                         "-fx-padding: 12 24; " +
                         "-fx-background-radius: 18; " +
                         "-fx-cursor: hand; " +
                         "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.28), 24, 0, 0, 8);");
        openBtn.setOnAction(e -> mainApp.getHostServices().showDocument("http://localhost:" + config.getBroadcastPort()));

        controls.getChildren().addAll(broadcastToggle, openBtn);

        bar.getChildren().addAll(liveStatus, divider1, connStatus, divider2, broadcastAddr, spacer, controls);
        return bar;
    }

    private Region createVerticalDivider() {
        Region divider = new Region();
        divider.setPrefWidth(1);
        divider.setMinWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(28);
        divider.setMinHeight(28);
        divider.setMaxHeight(28);
        divider.setStyle("-fx-background-color: rgba(226, 232, 240, 0.8);");
        return divider;
    }

    private VBox createNetworkSection() {
        VBox section = new VBox(22);
        Label title = new Label("МЕРЕЖА ТА ТРАНСЛЯЦІЯ");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 1.5px;");

        HBox mainRow = new HBox(24);
        mainRow.setAlignment(Pos.TOP_LEFT);

        mainRow.getChildren().addAll(
            createModernSettingsGroup("ЗАКЛАД", ICON_SCHOOL, "#4f46e5", new VBox(12, createLabeledField("НАЗВА", schoolNameField), createLabeledField("МІСТО", cityNameField))),
            createModernSettingsGroup("WEBSOCKET", ICON_LINK, "#7c3aed", new VBox(12, createLabeledField("ПОРТ ТРАНСЛЯЦІЇ", portField))),
            createModernSettingsGroup("ТЕМА", ICON_SETTINGS, "#2563eb", new VBox(12, createLabeledField("ДИЗАЙН ТАБЛО", themeCombo))),
            createModernSettingsGroup("FIREWALL", ICON_SHIELD, "#059669", new VBox(15, firewallStatusLabel, createPrimaryActionButton("ОПТИМІЗУВАТИ", ICON_SHIELD)))
        );
        
        // Ensure all groups have equal width
        mainRow.getChildren().forEach(n -> ((VBox)n).setPrefWidth(300));

        section.getChildren().addAll(title, mainRow);
        return section;
    }

    private HBox createLiveStatusWidget() {
        liveStatusCard = new HBox(14);
        liveStatusCard.setAlignment(Pos.CENTER_LEFT);
        liveStatusCard.setPadding(new Insets(4, 8, 4, 8));
        liveStatusCard.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        liveStatusBadge = new StackPane();
        liveStatusBadge.setPrefSize(42, 42);
        liveStatusBadge.setMinSize(42, 42);
        liveStatusBadge.setMaxSize(42, 42);
        liveStatusBadge.setAlignment(Pos.CENTER);
        
        liveStatusDotGlow = new Circle(10);
        liveStatusDotGlow.setOpacity(0.3);
        
        liveStatusDotCore = new Circle(5.5);
        
        liveStatusBadge.getChildren().addAll(liveStatusDotGlow, liveStatusDotCore);
        
        liveStatusGlowAnim = new ScaleTransition(Duration.millis(1200), liveStatusDotGlow);
        liveStatusGlowAnim.setFromX(0.8);
        liveStatusGlowAnim.setFromY(0.8);
        liveStatusGlowAnim.setToX(1.5);
        liveStatusGlowAnim.setToY(1.5);
        liveStatusGlowAnim.setCycleCount(Animation.INDEFINITE);
        liveStatusGlowAnim.setAutoReverse(true);
        
        VBox textCol = new VBox(2);
        Label eyebrow = new Label("СТАТУС ЕФІРУ");
        eyebrow.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-letter-spacing: 0.8px;");
        
        HBox contentRow = new HBox(8);
        contentRow.setAlignment(Pos.CENTER_LEFT);
        
        liveStatusLabelText = new Label();
        liveStatusLabelText.setStyle("-fx-font-size: 14px; -fx-font-weight: 800;");
        
        Label bullet = new Label("•");
        bullet.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_ZINC_200 + ";");
        
        uptimeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-font-family: 'Inter';");
        
        contentRow.getChildren().addAll(liveStatusLabelText, bullet, uptimeLabel);
        textCol.getChildren().addAll(eyebrow, contentRow);
        
        liveStatusCard.getChildren().addAll(liveStatusBadge, textCol);
        
        updateLiveStatusStyle(config.isBroadcastEnabled());
        
        return liveStatusCard;
    }

    private void updateLiveStatusStyle(boolean active) {
        String baseColor = active ? COLOR_SUCCESS : COLOR_DANGER;
        String text = active ? "АКТИВНИЙ" : "ВИМКНЕНО";
        
        if (liveStatusBadge != null) {
            String bgGradient = active ? "linear-gradient(to bottom right, #f0fdf4, #dcfce7)" : "linear-gradient(to bottom right, #fef2f2, #fee2e2)";
            String borderColor = active ? "rgba(22, 163, 74, 0.15)" : "rgba(220, 38, 38, 0.15)";
            liveStatusBadge.setStyle(
                "-fx-background-color: " + bgGradient + "; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 1; " +
                "-fx-background-radius: 14; " +
                "-fx-border-radius: 14;"
            );
        }
        
        Color fxColor = Color.web(baseColor);
        liveStatusDotCore.setFill(fxColor);
        liveStatusDotGlow.setFill(fxColor);
        liveStatusLabelText.setText(text);
        liveStatusLabelText.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + baseColor + ";");
        
        if (active) {
            // liveStatusGlowAnim.play(); // Disabled to reduce GPU usage
        } else {
            // liveStatusGlowAnim.stop(); // Disabled to reduce GPU usage
            liveStatusDotGlow.setScaleX(1.0);
            liveStatusDotGlow.setScaleY(1.0);
        }
    }

    private HBox createMonitoringWidget() {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(4, 8, 4, 8));
        card.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        VBox iconBox = new VBox(createSVGIcon(ICON_MONITOR, Color.web(COLOR_PRIMARY), 18));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(42, 42);
        iconBox.setMinSize(42, 42);
        iconBox.setMaxSize(42, 42);
        iconBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #eff6ff, #dbeafe); " +
            "-fx-border-color: rgba(37, 99, 235, 0.15); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 14; " +
            "-fx-border-radius: 14;"
        );
        
        VBox textCol = new VBox(2);
        Label eyebrow = new Label("МОНІТОРИНГ");
        eyebrow.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-letter-spacing: 0.8px;");
        
        connectionsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_ZINC_900 + ";");
        
        textCol.getChildren().addAll(eyebrow, connectionsLabel);
        card.getChildren().addAll(iconBox, textCol);
        
        return card;
    }

    private HBox createAddressWidget() {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(4, 8, 4, 8));
        card.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        String accentColor = "#6366f1";
        VBox iconBox = new VBox(createSVGIcon(ICON_LINK, Color.web(accentColor), 18));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(42, 42);
        iconBox.setMinSize(42, 42);
        iconBox.setMaxSize(42, 42);
        iconBox.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #f5f3ff, #e0e7ff); " +
            "-fx-border-color: rgba(99, 102, 241, 0.15); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 14; " +
            "-fx-border-radius: 14;"
        );
        
        VBox textCol = new VBox(2);
        Label eyebrow = new Label("АДРЕСА ТАБЛО");
        eyebrow.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-letter-spacing: 0.8px;");
        
        HBox valueRow = new HBox(10);
        valueRow.setAlignment(Pos.CENTER_LEFT);
        
        addrLabel = new Label("http://" + getLocalIp() + ":" + config.getBroadcastPort());
        addrLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_ZINC_900 + "; -fx-font-family: 'Inter';");
        
        Button copyBtn = new Button();
        copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_ZINC_500), 12));
        String copyBaseStyle = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 6; -fx-cursor: hand;";
        copyBtn.setStyle(copyBaseStyle);
        copyBtn.setOnMouseEntered(e -> {
            copyBtn.setStyle(copyBaseStyle + "-fx-background-color: #f1f5f9;");
            copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_ZINC_900), 12));
        });
        copyBtn.setOnMouseExited(e -> {
            copyBtn.setStyle(copyBaseStyle);
            copyBtn.setGraphic(createSVGIcon(ICON_CLONE, Color.web(COLOR_ZINC_500), 12));
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
        firewallStatusLabel.setTextFill(Color.GRAY);
        firewallStatusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-font-family: 'Inter';");

        new Thread(() -> {
            try {
                int port = Integer.parseInt(portField.getText());
                boolean allowed = mainApp.getSystemService().isPortAllowedInFirewall(port);
                javafx.application.Platform.runLater(() -> {
                    firewallStatusLabel.setText(allowed ? "ВІДКРИТО" : "ЗАБЛОКОВАНО");
                    firewallStatusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-font-family: 'Inter'; -fx-text-fill: " + (allowed ? COLOR_SUCCESS : COLOR_DANGER) + ";");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    firewallStatusLabel.setText("НЕВІДОМО");
                    firewallStatusLabel.setTextFill(Color.GRAY);
                });
            }
        }).start();
    }

    private VBox createAnnouncementsSection() {
        VBox section = new VBox(24);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Button addBtn = createPrimaryActionButton("Створити оголошення", ICON_PLUS);
        addBtn.setOnAction(e -> openEditDialog(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toggleGroup = new HBox(0);
        toggleGroup.setAlignment(Pos.CENTER);
        toggleGroup.setStyle("-fx-background-color: #f1f2f6; -fx-background-radius: 14; -fx-padding: 4;");
        
        ToggleButton activeBtn = new ToggleButton("Активні");
        ToggleButton archiveBtn = new ToggleButton("Архів");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        String baseToggleStyle = "-fx-background-insets: 0; -fx-background-radius: 11; -fx-font-size: 13px; -fx-padding: 8 22; -fx-cursor: hand;";
        String activeStyle = baseToggleStyle + "-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 800; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 4, 0, 0, 1);";
        String inactiveStyle = baseToggleStyle + "-fx-background-color: transparent; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-font-weight: 700;";

        activeBtn.setStyle(activeStyle);
        archiveBtn.setStyle(inactiveStyle);

        group.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == activeBtn) {
                activeBtn.setStyle(activeStyle);
                archiveBtn.setStyle(inactiveStyle);
                showArchivedAnnouncements = false;
            } else {
                activeBtn.setStyle(inactiveStyle);
                archiveBtn.setStyle(activeStyle);
                showArchivedAnnouncements = true;
            }
            refreshAnnouncements();
        });

        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        header.getChildren().addAll(addBtn, spacer, toggleGroup);

        announcementsContainer.setPadding(new Insets(5));
        section.getChildren().addAll(header, announcementsContainer);

        return section;
    }

    private VBox createSideSection() {
        VBox section = new VBox(28);

        // Devices Card
        VBox devicesCard = new VBox(20);
        devicesCard.setPadding(new Insets(28));
        devicesCard.setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 25, 0, 0, 8);");

        HBox devicesHeader = new HBox(15);
        devicesHeader.setAlignment(Pos.CENTER_LEFT);
        Label dTitle = new Label("ПРИСТРОЇ ТА МОНІТОРИ");
        dTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Region s = new Region(); HBox.setHgrow(s, Priority.ALWAYS);
        Button refreshBtn = new Button("Оновити");
        refreshBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.web(COLOR_PRIMARY), 18));
        String refreshBtnBase = "-fx-background-color: " + COLOR_PRIMARY + "15; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: 800; -fx-font-size: 14px; -fx-padding: 8 16; -fx-background-radius: 14; -fx-cursor: hand;";
        refreshBtn.setStyle(refreshBtnBase);
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(refreshBtnBase + "-fx-background-color: " + COLOR_PRIMARY + "25;"));
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
        
        updateContainer(announcementsContainer, filtered, a -> new AnnouncementCard(a, () -> openEditDialog(a), () -> {
            announcementService.deleteAnnouncement(a.id());
            refreshAnnouncements();
        }));
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
        new AnnouncementEditorDialog(announcementService, this::refreshAnnouncements).show(a);
    }
}
