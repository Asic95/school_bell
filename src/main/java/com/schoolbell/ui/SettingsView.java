package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class SettingsView {
    private final MainApp mainApp;
    private final ConfigService config;
    
    private final TextField schoolNameField;
    private final TextField cityNameField;
    private final TextField portField;
    private final Slider volumeSlider;
    private final CheckBox broadcastEnabledCb;
    private final CheckBox simulationModeCb;
    private final TextArea announcementArea;

    // Signal Durations
    private Spinner<Integer> regularBellDur;
    private Spinner<Integer> airRaidRingDur;
    private Spinner<Integer> airRaidPauseDur;
    private Spinner<Integer> emergencyDur;
    
    private HBox regularPreview;
    private HBox airRaidPreview;
    private HBox emergencyPreview;

    public SettingsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();

        schoolNameField = createStyledField(config.getSchoolName());
        cityNameField = createStyledField(config.getCityName());
        portField = createStyledField(String.valueOf(config.getBroadcastPort()));
        volumeSlider = new Slider(0, 100, config.getSystemVolume());
        broadcastEnabledCb = new CheckBox("Увімкнути веб-трансляцію дашборду");
        broadcastEnabledCb.setSelected(config.isBroadcastEnabled());
        broadcastEnabledCb.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");

        simulationModeCb = new CheckBox("РЕЖИМ СИМУЛЯЦІЇ (без фізичного реле)");
        simulationModeCb.setSelected(config.isSimulationMode());
        simulationModeCb.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARY + ";");
        
        announcementArea = new TextArea(config.getAnnouncementText());
        announcementArea.setPrefRowCount(3);
        announcementArea.setWrapText(true);
        announcementArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #dfe6e9; -fx-padding: 10;");

        // Signal Durations
        regularBellDur = createStyledSpinner(1, 60, config.getRegularBellDuration());
        airRaidRingDur = createStyledSpinner(1, 30, config.getAirRaidRingDuration());
        airRaidPauseDur = createStyledSpinner(1, 30, config.getAirRaidPauseDuration());
        emergencyDur = createStyledSpinner(1, 120, config.getEmergencyDuration());
        
        regularPreview = new HBox();
        airRaidPreview = new HBox();
        emergencyPreview = new HBox();
        
        javafx.beans.value.ChangeListener<Integer> previewUpdater = (o, ov, nv) -> refreshPreviews();
        regularBellDur.valueProperty().addListener(previewUpdater);
        airRaidRingDur.valueProperty().addListener(previewUpdater);
        airRaidPauseDur.valueProperty().addListener(previewUpdater);
        emergencyDur.valueProperty().addListener(previewUpdater);
    }

    public Node build() {
        VBox root = new VBox(30);
        root.setPadding(new Insets(35));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ НАЛАШТУВАННЯ", ICON_SAVE);
        saveBtn.setOnAction(e -> save());

        VBox headerArea = createSectionHeader(
                "Налаштування системи",
                "Загальна конфігурація закладу, мережі та системних параметрів",
                COLOR_PRIMARY,
                ICON_SETTINGS,
                saveBtn
        );

        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_LEFT);

        // --- GRID LAYOUT FOR SECTIONS ---
        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(25);
        
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Section 1: Identity
        VBox sec1 = createSettingsSection("ІДЕНТИФІКАЦІЯ ЗАКЛАДУ", "#0984e3", ICON_PERSON);
        sec1.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        GridPane identityGrid = new GridPane();
        identityGrid.setHgap(20);
        identityGrid.setVgap(15);
        identityGrid.setPadding(new Insets(10, 0, 0, 0));
        
        Label lblSchool = new Label("Назва закладу:");
        lblSchool.setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-size: 13px;");
        schoolNameField.setPrefWidth(400); 
        identityGrid.add(lblSchool, 0, 0);
        identityGrid.add(schoolNameField, 1, 0);
        
        Label lblCity = new Label("Місто:");
        lblCity.setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-size: 13px;");
        cityNameField.setPrefWidth(400);
        identityGrid.add(lblCity, 0, 1);
        identityGrid.add(cityNameField, 1, 1);
        sec1.getChildren().add(identityGrid);

        // Section 2: Audio
        VBox sec2 = createSettingsSection("НАЛАШТУВАННЯ ЗВУКУ", "#00b894", ICON_VOLUME);
        sec2.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        Label volVal = new Label(config.getSystemVolume() + "%");
        volVal.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-size: 16px;");
        volumeSlider.valueProperty().addListener((o, ov, nv) -> volVal.setText(nv.intValue() + "%"));
        HBox volRow = new HBox(20, volumeSlider, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);
        Label volLabel = new Label("ЗАГАЛЬНА ГУЧНІСТЬ СИСТЕМИ:");
        volLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        sec2.getChildren().addAll(volLabel, volRow);

        // Section 3: Signal Durations
        VBox sec3 = createSettingsSection("ПАРАМЕТРИ ДЗВІНКІВ", "#f39c12", ICON_WAVEFORM);
        sec3.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        
        GridPane durGrid = new GridPane();
        durGrid.setHgap(30); durGrid.setVgap(15);
        
        durGrid.add(new Label("ЗВИЧАЙНИЙ ДЗВІНОК (СЕК):"), 0, 0);
        durGrid.add(regularBellDur, 1, 0);
        
        durGrid.add(new Label("ТРИВОГА: ЦИКЛ ДЗВОНУ (СЕК):"), 0, 1);
        durGrid.add(airRaidRingDur, 1, 1);
        
        durGrid.add(new Label("ТРИВОГА: ПАУЗА (СЕК):"), 0, 2);
        durGrid.add(airRaidPauseDur, 1, 2);
        
        durGrid.add(new Label("ЕКСТРЕНА СИТУАЦІЯ (СЕК):"), 0, 3);
        durGrid.add(emergencyDur, 1, 3);
        
        for (Node n : durGrid.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-font-weight: 900; -fx-font-size: 10px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        }
        
        VBox previews = new VBox(5, regularPreview, airRaidPreview, emergencyPreview);
        previews.setPadding(new Insets(10, 0, 0, 0));
        
        sec3.getChildren().addAll(durGrid, createSeparator(), previews);

        // Section 4: Network
        VBox sec4 = createSettingsSection("МЕРЕЖА ТА ТРАНСЛЯЦІЯ", "#6c5ce7", ICON_BROADCAST);
        sec4.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        broadcastEnabledCb.setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + "; -fx-font-size: 13px;");
        
        HBox portRow = createFieldRow("ПОРТ ТРАНСЛЯЦІЇ:", portField);
        ((Label)portRow.getChildren().get(0)).setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        portField.setPrefWidth(120);
        
        sec4.getChildren().addAll(broadcastEnabledCb, portRow);

        // Section 5: System
        VBox sec5 = createSettingsSection("СИСТЕМНІ ПАРАМЕТРИ", "#636e72", ICON_SETTINGS);
        sec5.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        simulationModeCb.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 13px;");
        Label simDesc = new Label("Використовуйте цей режим для тестування без підключеного реле");
        simDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        sec5.getChildren().addAll(simulationModeCb, simDesc);

        grid.add(sec1, 0, 0);
        grid.add(sec2, 1, 0);
        grid.add(sec3, 0, 1, 1, 2); // Span 2 rows
        grid.add(sec4, 1, 1);
        grid.add(sec5, 1, 2);

        content.getChildren().addAll(grid);
        root.getChildren().addAll(headerArea, content);

        refreshPreviews();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private void refreshPreviews() {
        updatePreview(regularPreview, "ЗВИЧАЙНИЙ", List.of(regularBellDur.getValue()), 0, COLOR_PRIMARY);
        updatePreview(airRaidPreview, "ТРИВОГА", List.of(airRaidRingDur.getValue(), airRaidRingDur.getValue(), airRaidRingDur.getValue()), airRaidPauseDur.getValue(), "#f39c12");
        updatePreview(emergencyPreview, "ЕКСТРЕНА", List.of(emergencyDur.getValue()), 0, COLOR_DANGER);
    }

    private Separator createSeparator() {
        Separator s = new Separator();
        s.setStyle("-fx-opacity: 0.3;");
        s.setPadding(new Insets(10, 0, 10, 0));
        return s;
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
            
            // Signal Durations
            config.setRegularBellDuration(regularBellDur.getValue());
            config.setAirRaidRingDuration(airRaidRingDur.getValue());
            config.setAirRaidPauseDuration(airRaidPauseDur.getValue());
            config.setEmergencyDuration(emergencyDur.getValue());
            
            mainApp.saveConfig();
            ToastService.showSuccess("Загальні налаштування збережено!");
        } catch (NumberFormatException e) {
            ToastService.showError("Некоректні дані: порт має бути числом.");
        }
    }
}
