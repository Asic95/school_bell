package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.schoolbell.service.AirAlertService;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import java.util.List;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class SettingsView {
    private final MainApp mainApp;
    private final ConfigService config;
    private final AirAlertService airAlertService;

    private final TextField schoolNameField;
    private final TextField cityNameField;
    private final TextField portField;
    private final Slider volumeSlider;
    private final CheckBox broadcastEnabledCb;
    private final CheckBox simulationModeCb;
    private final TextArea announcementArea;
    private final SignalSettingsPane bellSettingsPane;
    
    private final ToggleButton airRaidToggle;
    private final ComboBox<String> regionCombo;
    private final ComboBox<String> districtCombo;

    public SettingsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.airAlertService = mainApp.getAirAlertService();

        schoolNameField = createStyledField(config.getSchoolName());
        schoolNameField.setStyle(PREMIUM_FIELD_STYLE);
        cityNameField = createStyledField(config.getCityName());
        cityNameField.setStyle(PREMIUM_FIELD_STYLE);
        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        portField.setStyle(PREMIUM_FIELD_STYLE);
        volumeSlider = new Slider(0, 100, config.getSystemVolume());
        broadcastEnabledCb = new CheckBox("Увімкнути веб-трансляцію дашборду");
        broadcastEnabledCb.setSelected(config.isBroadcastEnabled());
        broadcastEnabledCb.setStyle("-fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-font-size: 14px;");

        simulationModeCb = new CheckBox("РЕЖИМ СИМУЛЯЦІЇ (без фізичного реле)");
        simulationModeCb.setSelected(config.isSimulationMode());
        simulationModeCb.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 14px;");

        announcementArea = new TextArea(config.getAnnouncementText());
        announcementArea.setPrefRowCount(3);
        announcementArea.setWrapText(true);
        announcementArea.setStyle(PREMIUM_FIELD_STYLE);

        bellSettingsPane = new SignalSettingsPane(
                config.getRegularBellDuration(),
                config.getAirRaidRingDuration(),
                config.getAirRaidPauseDuration(),
                config.getEmergencyDuration()
        );

        airRaidToggle = new ToggleButton(config.isAirRaidAutomationEnabled() ? "УВІМКНЕНО" : "ВИМКНЕНО");
        airRaidToggle.setSelected(config.isAirRaidAutomationEnabled());
        airRaidToggle.setStyle(config.isAirRaidAutomationEnabled() ? PREMIUM_TOGGLE_ACTIVE : PREMIUM_TOGGLE_INACTIVE);
        airRaidToggle.setOnAction(e -> {
            boolean active = airRaidToggle.isSelected();
            airRaidToggle.setText(active ? "УВІМКНЕНО" : "ВИМКНЕНО");
            airRaidToggle.setStyle(active ? PREMIUM_TOGGLE_ACTIVE : PREMIUM_TOGGLE_INACTIVE);
        });

        regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll(airAlertService.getRegions());
        regionCombo.setValue(config.getSelectedRegionId());
        regionCombo.setPrefWidth(300);
        regionCombo.setStyle(PREMIUM_SELECT_STYLE);
        
        districtCombo = new ComboBox<>();
        if (config.getSelectedRegionId() != null) {
            districtCombo.getItems().addAll(airAlertService.getDistricts(config.getSelectedRegionId()));
            districtCombo.setValue(config.getSelectedDistrictId());
        }
        districtCombo.setPrefWidth(300);
        districtCombo.setStyle(PREMIUM_SELECT_STYLE);

        regionCombo.setOnAction(e -> {
            districtCombo.getItems().clear();
            districtCombo.getItems().addAll(airAlertService.getDistricts(regionCombo.getValue()));
        });
    }

    public Node build() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(35));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ НАЛАШТУВАННЯ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(e -> save());

        HBox header = createPageHeader(
            "НАЛАШТУВАННЯ",
            "Параметри системи",
            "Загальна конфігурація закладу, мережі та системних параметрів.",
            ICON_SETTINGS,
            COLOR_PRIMARY,
            saveBtn
        );

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(25);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        VBox sec1 = createSettingsSection("ІДЕНТИФІКАЦІЯ ЗАКЛАДУ", "#0984e3", ICON_PERSON);
        sec1.setStyle(SOFT_CARD + "-fx-padding: 30;");
        GridPane identityGrid = new GridPane();
        identityGrid.setHgap(20);
        identityGrid.setVgap(20);
        identityGrid.setPadding(new Insets(15, 0, 0, 0));
        Label lblSchool = new Label("Назва закладу:");
        lblSchool.setStyle(HEADER_STYLE);
        schoolNameField.setPrefWidth(400);
        identityGrid.add(lblSchool, 0, 0);
        identityGrid.add(schoolNameField, 1, 0);
        Label lblCity = new Label("Місто:");
        lblCity.setStyle(HEADER_STYLE);
        cityNameField.setPrefWidth(400);
        identityGrid.add(lblCity, 0, 1);
        identityGrid.add(cityNameField, 1, 1);
        sec1.getChildren().add(identityGrid);

        VBox sec2 = createSettingsSection("НАЛАШТУВАННЯ ЗВУКУ", "#00b894", ICON_VOLUME);
        sec2.setStyle(SOFT_CARD + "-fx-padding: 30;");
        Label volVal = new Label(config.getSystemVolume() + "%");
        volVal.setStyle("-fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-font-size: 18px;");
        volumeSlider.valueProperty().addListener((o, ov, nv) -> volVal.setText(nv.intValue() + "%"));
        HBox volRow = new HBox(20, volumeSlider, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);
        Label volLabel = new Label("ЗАГАЛЬНА ГУЧНІСТЬ СИСТЕМИ:");
        volLabel.setStyle(HEADER_STYLE);
        sec2.getChildren().addAll(volLabel, volRow);

        VBox sec4 = createSettingsSection("МЕРЕЖА ТА ТРАНСЛЯЦІЯ", "#6c5ce7", ICON_BROADCAST);
        sec4.setStyle(SOFT_CARD + "-fx-padding: 30;");
        broadcastEnabledCb.setStyle("-fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-font-size: 14px;");
        HBox portRow = createFieldRow("ПОРТ ТРАНСЛЯЦІЇ:", portField);
        ((Label) portRow.getChildren().get(0)).setStyle(HEADER_STYLE);
        portField.setPrefWidth(120);
        sec4.getChildren().addAll(broadcastEnabledCb, portRow);

        VBox sec5 = createSettingsSection("СИСТЕМНІ ПАРАМЕТРИ", "#636e72", ICON_SETTINGS);
        sec5.setStyle(SOFT_CARD + "-fx-padding: 30;");
        simulationModeCb.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 14px;");
        Label simDesc = new Label("Використовуйте цей режим для тестування без підключеного реле");
        simDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        sec5.getChildren().addAll(simulationModeCb, simDesc);
        
        VBox secAir = createSettingsSection("АВТОМАТИЗАЦІЯ ПОВІТРЯНОЇ ТРИВОГИ", "#e17055", ICON_SETTINGS);
        secAir.setStyle(SOFT_CARD + "-fx-padding: 30;");
        HBox airToggleBox = new HBox(15, new Label("СТАТУС АВТОМАТИЗАЦІЇ:"), airRaidToggle);
        airToggleBox.setAlignment(Pos.CENTER_LEFT);
        ((Label)airToggleBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        
        VBox airGrid = new VBox(20);
        airGrid.setPadding(new Insets(15, 0, 0, 0));
        
        VBox regBox = new VBox(8, new Label("ОБЛАСТЬ / РЕГІОН:"), regionCombo);
        ((Label)regBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        
        VBox distBox = new VBox(8, new Label("РАЙОН / ТГ:"), districtCombo);
        ((Label)distBox.getChildren().get(0)).setStyle(HEADER_STYLE);
        
        airGrid.getChildren().addAll(airToggleBox, regBox, distBox);
        secAir.getChildren().add(airGrid);

        grid.add(sec1, 0, 0);
        grid.add(sec2, 1, 0);
        grid.add(bellSettingsPane, 0, 1, 2, 1);
        grid.add(sec4, 0, 2);
        grid.add(sec5, 1, 2);
        grid.add(secAir, 0, 3, 2, 1);

        root.getChildren().addAll(header, grid);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        String css = MODERN_CHECKBOX_STYLE + "\n" + MODERN_DATE_PICKER_STYLE;
        scroll.getStylesheets().add("data:text/css," + css.replace(" ", "%20").replace("\n", "%20"));
        
        return scroll;
    }

    private void save() {
        try {
            config.setSchoolName(schoolNameField.getText());
            config.setCityName(cityNameField.getText());
            config.setSystemVolume((int) volumeSlider.getValue());
            config.setBroadcastEnabled(broadcastEnabledCb.isSelected());
            config.setSimulationMode(simulationModeCb.isSelected());
            config.setBroadcastPort(Integer.parseInt(portField.getText()));
            config.setAnnouncementText(announcementArea.getText());
            config.setRegularBellDuration(bellSettingsPane.getRegularDuration());
            config.setAirRaidRingDuration(bellSettingsPane.getAirRaidRingDuration());
            config.setAirRaidPauseDuration(bellSettingsPane.getAirRaidPauseDuration());
            config.setEmergencyDuration(bellSettingsPane.getEmergencyDuration());
            
            config.setAirRaidAutomationEnabled(airRaidToggle.isSelected());
            config.setSelectedRegionId(regionCombo.getValue());
            config.setSelectedDistrictId(districtCombo.getValue());
            
            mainApp.saveConfig();
            
            if (config.isAirRaidAutomationEnabled()) {
                airAlertService.start();
            } else {
                airAlertService.stop();
            }
            
            ToastService.showSuccess("Загальні налаштування збережено!");
        } catch (NumberFormatException e) {
            ToastService.showError("Некоректні дані: порт має бути числом.");
        }
    }
}

