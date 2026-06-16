package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SystemService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class SystemView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final SystemService systemService;

    private final ToggleButton autostartTg;
    private final ToggleButton trayTg;
    private final ToggleButton simulationTg;
    private final ToggleButton airRaidTg;
    private final ComboBox<String> regionCombo;
    private final ComboBox<String> districtCombo;

    private final BellSettingsPane bellSettingsPane;
    private final SystemJournalPane journalPane;
    private VBox mainCol;
    private final javafx.animation.PauseTransition saveDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));

    public SystemView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.systemService = mainApp.getSystemService();

        saveDebounce.setOnFinished(e -> {
            mainApp.addLog("Системні налаштування оновлено", "SUCCESS");
            ToastService.showSuccess("Налаштування збережено!");
        });

        autostartTg = createToggleSwitch(config.isAutostartEnabled());
        trayTg = createToggleSwitch(config.isMinimizeToTray());
        simulationTg = createToggleSwitch(config.isSimulationMode());
        airRaidTg = createToggleSwitch(config.isAirRaidAutomationEnabled());

        regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll(mainApp.getAirAlertService().getRegions());
        regionCombo.setValue(config.getSelectedRegionId());
        regionCombo.setStyle(PREMIUM_SELECT_STYLE);

        districtCombo = new ComboBox<>();
        districtCombo.setStyle(PREMIUM_SELECT_STYLE);

        if (config.getSelectedRegionId() != null) {
            districtCombo.getItems().addAll(mainApp.getAirAlertService().getDistricts(config.getSelectedRegionId()));
            districtCombo.setValue(config.getSelectedDistrictId());
        }
        regionCombo.setOnAction(e -> {
            districtCombo.getItems().clear();
            districtCombo.getItems().addAll(mainApp.getAirAlertService().getDistricts(regionCombo.getValue()));
            save();
        });
        districtCombo.setOnAction(e -> save());

        bellSettingsPane = new BellSettingsPane(
                config.getRegularBellDuration(),
                config.getAirRaidRingDuration(),
                config.getAirRaidPauseDuration(),
                config.getEmergencyDuration(),
                config.getEarlyBellMinutes(),
                config.getEarlyBellSeconds(),
                true
        );
        bellSettingsPane.setOnSettingsChanged(this::save);

        journalPane = new SystemJournalPane(mainApp);

        simulationTg.selectedProperty().addListener((obs, old, nv) -> save());
        autostartTg.selectedProperty().addListener((obs, old, nv) -> save());
        trayTg.selectedProperty().addListener((obs, old, nv) -> save());
        airRaidTg.selectedProperty().addListener((obs, old, nv) -> save());
    }

    public Node build() {
        ScrollPane scroll = new ScrollPane();
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        HBox header = createPageHeader(
                "КОНФІГУРАЦІЯ",
                "Системні налаштування",
                "Глобальні параметри роботи програми, дизайн та поведінка системи.",
                ICON_SETTINGS,
                COLOR_TEXT,
                null
        );

        mainCol = new VBox(25);
        HBox.setHgrow(mainCol, Priority.ALWAYS);
        mainCol.getChildren().addAll(buildOperationCard(), buildRelayCard(), buildSignalCard(), journalPane);

        VBox helpPanel = createSideHelpPanel(
                createHelpCard(ICON_POWER, "Автостарт", "Рекомендуємо увімкнути автостарт, щоб система відновлювалась після вимкнення світла.", COLOR_SUCCESS),
                createHelpCard(ICON_NET, "Wi-Fi мережа", "Для роботи через Wi-Fi пристрій (Shelly) та цей комп'ютер мають бути в одній локальній мережі.", COLOR_PRIMARY),
                createHelpCard(ICON_SAVE, "Бекап", "Регулярно створюйте копію бази, щоб не втратити розклад при перевстановленні системи.", COLOR_INDIGO)
        );

        HBox contentLayout = new HBox(25, mainCol, helpPanel);
        root.getChildren().addAll(header, contentLayout);

        scroll.setContent(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        return scroll;
    }

    private VBox buildOperationCard() {
        VBox card = createCard("ЗАПУСК ТА РОБОТА", ICON_CLOCK, COLOR_SUCCESS);

        VBox rows = new VBox(15);
        rows.getChildren().add(createToggleRow("Автозапуск разом з Windows",
                "Програма буде автоматично запускатись при вході в систему", ICON_POWER, COLOR_PRIMARY, autostartTg));
        rows.getChildren().add(createToggleRow("Згортати у системний трей",
                "При закритті вікно буде ховатись у трей замість виходу", ICON_TRAY, COLOR_PURPLE, trayTg));
        rows.getChildren().add(createToggleRow("Режим симуляції",
                "Робота без фізичного підключення до реле (тільки логування)", ICON_FLASK, COLOR_WARNING, simulationTg));

        String btnStyle = "-fx-font-weight: 900; -fx-font-size: 11px; -fx-padding: 10 20; -fx-background-radius: 12; -fx-cursor: hand;";

        Button backupBtn = createPrimaryActionButton("СТВОРИТИ КОПІЮ", ICON_SAVE);
        backupBtn.setStyle(PREMIUM_BTN_STYLE + btnStyle);
        backupBtn.setPrefWidth(190);
        backupBtn.setOnAction(e -> {
            if (systemService.createDatabaseBackup((javafx.stage.Stage) mainCol.getScene().getWindow())) {
                ToastService.showSuccess("Резервну копію успішно створено!");
            }
        });

        Button restoreBtn = new Button("ВІДНОВИТИ");
        restoreBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.web(COLOR_PRIMARY), 14));
        String secondaryStyle = "-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 12;";
        restoreBtn.setStyle(secondaryStyle + btnStyle);
        restoreBtn.setPrefWidth(190);
        restoreBtn.setOnAction(e -> {
            if (systemService.restoreDatabaseBackup((javafx.stage.Stage) mainCol.getScene().getWindow())) {
                new RestoreSuccessDialog((javafx.stage.Stage) mainCol.getScene().getWindow()).display();
            }
        });

        HBox btnBox = new HBox(12, restoreBtn, backupBtn);
        rows.getChildren().add(createActionRow("Резервне копіювання та відновлення", "Збережіть налаштування у файл або відновіть їх із копії", ICON_SAVE, COLOR_INDIGO, btnBox));

        Button refreshRadioBtn = new Button("ОНОВИТИ КАТАЛОГ");
        refreshRadioBtn.setGraphic(createSVGIcon(ICON_REFRESH, Color.web(COLOR_PRIMARY), 14));
        refreshRadioBtn.setStyle(secondaryStyle + btnStyle);
        refreshRadioBtn.setPrefWidth(190);
        refreshRadioBtn.setOnAction(e -> {
            refreshRadioBtn.setDisable(true);
            ToastService.showInfo("Оновлення каталогу радіо розпочато...");
            mainApp.getRadioStationService().refreshCatalog(() -> {
                refreshRadioBtn.setDisable(false);
                int count = mainApp.getRadioStationService().getTotalCatalogSize();
                mainApp.addLog("Каталог онлайн-радіо оновлено. Знайдено станцій: " + count, "SUCCESS");
                ToastService.showSuccess("Каталог радіо успішно оновлено!");
            });
        });
        rows.getChildren().add(createActionRow("Каталог онлайн-радіо", "Завантажити актуальний список станцій з Radio Browser API", ICON_RADIO, COLOR_PRIMARY, refreshRadioBtn));

        rows.getChildren().add(createToggleRow("Автоматизація повітряної тривоги",
                "Пошук та автоматичне оповіщення про повітряну тривогу", ICON_SETTINGS, COLOR_TANGERINE, airRaidTg));

        VBox regionBox = new VBox(8);
        Label regionLbl = new Label("ОБЕРІТЬ РЕГІОН:");
        regionLbl.setStyle(HEADER_STYLE);
        regionCombo.setMaxWidth(Double.MAX_VALUE);
        regionBox.getChildren().addAll(regionLbl, regionCombo);
        regionBox.setPadding(new Insets(10, 15, 0, 15));

        VBox districtBox = new VBox(8);
        Label districtLbl = new Label("ОБЕРІТЬ РАЙОН:");
        districtLbl.setStyle(HEADER_STYLE);
        districtCombo.setMaxWidth(Double.MAX_VALUE);
        districtBox.getChildren().addAll(districtLbl, districtCombo);
        districtBox.setPadding(new Insets(0, 15, 10, 15));

        Runnable refreshVisibility = () -> {
            boolean active = airRaidTg.isSelected();
            regionBox.setVisible(active);
            regionBox.setManaged(active);

            String sel = regionCombo.getValue();
            boolean hasDistricts = active && sel != null && !mainApp.getAirAlertService().getDistricts(sel).isEmpty();
            districtBox.setVisible(hasDistricts);
            districtBox.setManaged(hasDistricts);
        };

        airRaidTg.selectedProperty().addListener((obs, old, nv) -> {
            if (nv) {
                new AirRaidInfoDialog((javafx.stage.Stage) mainCol.getScene().getWindow()).display();
            }
            refreshVisibility.run();
        });
        regionCombo.valueProperty().addListener((obs, old, nv) -> refreshVisibility.run());
        refreshVisibility.run();

        rows.getChildren().addAll(regionBox, districtBox);
        card.getChildren().add(rows);
        return card;
    }

    private VBox buildRelayCard() {
        VBox card = createCard("КЕРУВАННЯ ПРИСТРОЄМ ВИКОНАННЯ", ICON_POWER_PLUG, COLOR_INDIGO);
        VBox rows = new VBox(20);

        VBox shellySetup = new VBox(15);
        shellySetup.setPadding(new Insets(10, 0, 0, 18));
        Label statusLbl = new Label(mainApp.getRelayController().getConnectionDetails());
        statusLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: 600;");

        // --- UNIFIED PREMIUM TOGGLE ---
        HBox toggleContainer = ControlFactory.createSegmentedFilter("USB HID", "WI-FI SHELLY", "SHELLY".equals(config.getRelayType()), isShelly -> {
            if (isShelly) {
                mainApp.getRelayController().switchDevice("SHELLY", config.getShellyIp(), config.getShellyName());
                shellySetup.setVisible(true);
                shellySetup.setManaged(true);
                ToastService.showSuccess("Перемкнуто на WI-FI");
            } else {
                mainApp.getRelayController().switchDevice("USB", "", "");
                shellySetup.setVisible(false);
                shellySetup.setManaged(false);
                ToastService.showSuccess("Перемкнуто на USB");
            }
            mainApp.saveConfig();
            statusLbl.setText(mainApp.getRelayController().getConnectionDetails());
        });
        toggleContainer.setMinWidth(320);
        toggleContainer.setMaxWidth(320);

        HBox deviceRow = createActionRow("Тип пристрою комутації",
                "Вибір способу зв'язку з фізичним реле для автоматичного керування дзвінками", ICON_LINK, COLOR_INDIGO, toggleContainer);

        ComboBox<com.schoolbell.hardware.ShellyRelayDevice> shellyCombo = new ComboBox<>();
        shellyCombo.setStyle(PREMIUM_SELECT_STYLE);
        shellyCombo.setPromptText("Оберіть знайдений Shelly...");
        shellyCombo.setMaxWidth(Double.MAX_VALUE);
        shellyCombo.setPrefHeight(48);
        HBox.setHgrow(shellyCombo, Priority.ALWAYS);

        // --- PREMIUM TWO-LINE LIST ITEMS, CLEAN SINGLE-LINE SELECTION ---
        shellyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.schoolbell.hardware.ShellyRelayDevice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    VBox container = new VBox(0);
                    Label nameLbl = new Label(item.getCleanName());
                    nameLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: inherit;");
                    Label ipLbl = new Label("IP: " + item.getIp());
                    ipLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: inherit; -fx-opacity: 0.8;");
                    container.getChildren().addAll(nameLbl, ipLbl);
                    setGraphic(container);
                }
            }
        });

        shellyCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(com.schoolbell.hardware.ShellyRelayDevice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Оберіть знайдений Shelly...");
                } else {
                    setText(item.getCleanName() + " [" + item.getIp() + "]");
                    setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + ";");
                }
            }
        });

        Button scanBtn = createPrimaryActionButton("ПОШУК РЕЛЕ", ICON_SEARCH);
        scanBtn.setStyle(PREMIUM_BTN_STYLE + "-fx-font-size: 14px; -fx-padding: 0 30; -fx-background-radius: 12; -fx-min-width: 210; -fx-pref-height: 50;");
        scanBtn.setPrefHeight(55);
        scanBtn.setMinHeight(55);
        scanBtn.setMaxHeight(55);

        scanBtn.setOnAction(e -> {
            scanBtn.setDisable(true);
            scanBtn.setText("ШУКАЮ...");
            mainApp.getRelayController().getDiscoveryService().startScan(5000, () -> {
                javafx.application.Platform.runLater(() -> {
                    scanBtn.setDisable(false);
                    scanBtn.setText("ПОШУК РЕЛЕ");
                    List<com.schoolbell.hardware.ShellyRelayDevice> devices = mainApp.getRelayController().getDiscoveryService().getDiscoveredDevices();
                    shellyCombo.getItems().setAll(devices);
                    if (!devices.isEmpty()) {
                        ToastService.showSuccess("Знайдено пристроїв: " + devices.size());
                    } else {
                        ToastService.showError("Shelly не знайдено.");
                    }
                });
            });
        });

        shellyCombo.setOnAction(e -> {
            com.schoolbell.hardware.ShellyRelayDevice sel = shellyCombo.getValue();
            if (sel != null) {
                mainApp.getRelayController().switchDevice("SHELLY", sel.getIp(), sel.getCleanName());
                mainApp.saveConfig();
                statusLbl.setText(mainApp.getRelayController().getConnectionDetails());
                ToastService.showSuccess("Shelly підключено!");
            }
        });

        shellySetup.setVisible("SHELLY".equals(config.getRelayType()));
        shellySetup.setManaged(shellySetup.isVisible());

        shellySetup.getChildren().addAll(
            new HBox(15, scanBtn, shellyCombo)
        );

        rows.getChildren().addAll(
            deviceRow,
            shellySetup,
            new Region() {{ setMinHeight(5); }},
            new HBox(10, createSVGIcon(ICON_INFO, Color.web(COLOR_PRIMARY), 16), statusLbl)
        );

        ((HBox) rows.getChildren().get(3)).setAlignment(Pos.CENTER_LEFT);

        card.getChildren().add(rows);
        return card;
    }

    private VBox buildSignalCard() {
        VBox card = createCard("ПАРАМЕТРИ ДЗВІНКІВ", ICON_WAVEFORM, COLOR_ORANGE_DARK);
        card.getChildren().add(bellSettingsPane);
        return card;
    }

    private VBox createCard(String title, String icon, String color) {
        VBox card = new VBox(25);
        card.setPadding(new Insets(30));
        card.setStyle(SOFT_CARD);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 24));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(54, 54);
        iconBox.setStyle(ICON_BADGE_STYLE);

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + "; -fx-letter-spacing: 1.5px;");

        header.getChildren().addAll(iconBox, t);
        card.getChildren().add(header);
        return card;
    }

    private HBox createActionRow(String title, String desc, String icon, String iconColor, Node actionNode) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setStyle("-fx-background-radius: 18;");

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(iconColor), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + iconColor + "10; -fx-background-radius: 14;");

        VBox texts = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: " + COLOR_NAVY + ";");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + ";");
        texts.getChildren().addAll(t, d);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(iconBox, texts, spacer, actionNode);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + COLOR_SURFACE_SOFT + "; -fx-background-radius: 18;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 18;"));
        return row;
    }

    private HBox createToggleRow(String title, String desc, String icon, String iconColor, ToggleButton toggle) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 18, 12, 18));
        row.setStyle("-fx-background-radius: 18;");

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(iconColor), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle("-fx-background-color: " + iconColor + "10; -fx-background-radius: 14;");

        VBox texts = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: " + COLOR_NAVY + ";");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + ";");
        texts.getChildren().addAll(t, d);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(iconBox, texts, spacer, toggle);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: " + COLOR_SURFACE_SOFT + "; -fx-background-radius: 18;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 18;"));
        return row;
    }

    private void save() {
        try {
            boolean autostartChanged = config.isAutostartEnabled() != autostartTg.isSelected();
            config.setAutostartEnabled(autostartTg.isSelected());
            config.setMinimizeToTray(trayTg.isSelected());
            config.setSimulationMode(simulationTg.isSelected());
            config.setAirRaidAutomationEnabled(airRaidTg.isSelected());
            config.setSelectedRegionId(regionCombo.getValue());
            config.setSelectedDistrictId(districtCombo.getValue());

            config.setEarlyBellMinutes(bellSettingsPane.getEarlyMin());
            config.setEarlyBellSeconds(bellSettingsPane.getEarlySec());

            config.setRegularBellDuration(bellSettingsPane.getRegularDuration());
            config.setAirRaidRingDuration(bellSettingsPane.getAirRaidRingDuration());
            config.setAirRaidPauseDuration(bellSettingsPane.getAirRaidPauseDuration());
            config.setEmergencyDuration(bellSettingsPane.getEmergencyDuration());

            mainApp.saveConfig();

            if (autostartChanged) {
                systemService.updateAutostart(config.isAutostartEnabled());
            }

            if (config.isAirRaidAutomationEnabled()) {
                mainApp.getAirAlertService().start();
            } else {
                mainApp.getAirAlertService().stop();
            }

            // TRIGGER DEBOUNCED NOTIFICATION
            saveDebounce.playFromStart();
        } catch (Exception e) {
            ToastService.showError("Помилка при збереженні: " + e.getMessage());
        }
    }
}
