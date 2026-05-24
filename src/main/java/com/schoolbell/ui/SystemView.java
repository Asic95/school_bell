package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import com.schoolbell.service.SystemService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.util.List;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
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

    public SystemView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.systemService = mainApp.getSystemService();

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
        });

        bellSettingsPane = new BellSettingsPane(
                config.getRegularBellDuration(),
                config.getAirRaidRingDuration(),
                config.getAirRaidPauseDuration(),
                config.getEmergencyDuration(),
                config.getEarlyBellMinutes(),
                config.getEarlyBellSeconds(),
                true
        );

        journalPane = new SystemJournalPane(mainApp);
        
        simulationTg.selectedProperty().addListener((obs, old, nv) -> updateJournalVisibility(nv));
    }

    public Node build() {
        ScrollPane scroll = new ScrollPane();
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ВСЕ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(e -> save());

        HBox header = createPageHeader(
            "КОНФІГУРАЦІЯ",
            "Системні налаштування",
            "Глобальні параметри роботи програми, дизайн та поведінка системи.",
            ICON_SETTINGS,
            "#2d3436",
            saveBtn
        );

        mainCol = new VBox(25);
        HBox.setHgrow(mainCol, Priority.ALWAYS);
        mainCol.getChildren().addAll(buildOperationCard(), buildSignalCard());
        
        updateJournalVisibility(simulationTg.isSelected());

        VBox helpPanel = createSideHelpPanel(
                createHelpCard(ICON_POWER, "Автостарт", "Рекомендуємо увімкнути автостарт, щоб система відновлювалась після вимкнення світла.", COLOR_SUCCESS),
                createHelpCard(ICON_WAVEFORM, "Сигнали", "Тривалості сигналів редагуються в новому візуальному блоці параметрів дзвінків.", COLOR_WARNING)
        );

        HBox contentLayout = new HBox(25, mainCol, helpPanel);
        root.getChildren().addAll(header, contentLayout);

        scroll.setContent(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void updateJournalVisibility(boolean visible) {
        if (visible) {
            if (!mainCol.getChildren().contains(journalPane)) {
                mainCol.getChildren().add(journalPane);
            }
        } else {
            mainCol.getChildren().remove(journalPane);
        }
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
        rows.getChildren().add(createToggleRow("Автоматизація повітряної тривоги",
                "Пошук та автоматичне оповіщення про повітряну тривогу", ICON_SETTINGS, "#e17055", airRaidTg));
        
        // Region Selection with Label
        VBox regionBox = new VBox(8);
        Label regionLbl = new Label("ОБЕРІТЬ РЕГІОН:");
        regionLbl.setStyle(HEADER_STYLE);
        regionCombo.setMaxWidth(Double.MAX_VALUE);
        regionBox.getChildren().addAll(regionLbl, regionCombo);
        regionBox.setPadding(new Insets(10, 15, 0, 15));
        
        // District Selection with Label and Container for dynamic visibility
        VBox districtBox = new VBox(8);
        Label districtLbl = new Label("ОБЕРІТЬ РАЙОН:");
        districtLbl.setStyle(HEADER_STYLE);
        districtCombo.setMaxWidth(Double.MAX_VALUE);
        districtBox.getChildren().addAll(districtLbl, districtCombo);
        districtBox.setPadding(new Insets(0, 15, 10, 15));
        
        // Dynamic visibility logic integration
        Runnable refreshVisibility = () -> {
            String sel = regionCombo.getValue();
            boolean hasDistricts = sel != null && !mainApp.getAirAlertService().getDistricts(sel).isEmpty();
            districtBox.setVisible(hasDistricts);
            districtBox.setManaged(hasDistricts);
        };
        
        regionCombo.valueProperty().addListener((obs, old, nv) -> refreshVisibility.run());
        refreshVisibility.run();

        rows.getChildren().addAll(regionBox, districtBox);

        card.getChildren().add(rows);
        return card;
    }

    private VBox buildSignalCard() {
        VBox card = createCard("ПАРАМЕТРИ ДЗВІНКІВ", ICON_WAVEFORM, "#f39c12");
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
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-letter-spacing: 1.5px;");

        header.getChildren().addAll(iconBox, t);
        card.getChildren().add(header);

        return card;
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
        t.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #0f172a;");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        texts.getChildren().addAll(t, d);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(iconBox, texts, spacer, toggle);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 18;"));
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

            mainApp.addLog("Системні налаштування оновлено", "SUCCESS");
            ToastService.showSuccess("Налаштування збережено!");
        } catch (Exception e) {
            ToastService.showError("Помилка при збереженні: " + e.getMessage());
        }
    }
}
