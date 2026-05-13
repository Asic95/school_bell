package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

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

        // Section 3: Network
        VBox sec3 = createSettingsSection("МЕРЕЖА ТА ТРАНСЛЯЦІЯ", "#6c5ce7", ICON_BROADCAST);
        sec3.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        broadcastEnabledCb.setStyle("-fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + "; -fx-font-size: 13px;");
        
        HBox portRow = createFieldRow("ПОРТ ТРАНСЛЯЦІЇ:", portField);
        ((Label)portRow.getChildren().get(0)).setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        portField.setPrefWidth(120);
        
        sec3.getChildren().addAll(broadcastEnabledCb, portRow);

        // Section 5: System
        VBox sec5 = createSettingsSection("СИСТЕМНІ ПАРАМЕТРИ", "#636e72", ICON_SETTINGS);
        sec5.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        simulationModeCb.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 13px;");
        Label simDesc = new Label("Використовуйте цей режим для тестування без підключеного реле");
        simDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        sec5.getChildren().addAll(simulationModeCb, simDesc);

        // Section 4: Announcement
        VBox sec4 = createSettingsSection("ТЕКСТ ОГОЛОШЕННЯ НА ДАШБОРДІ", "#f39c12", ICON_MESSAGE);
        sec4.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 24;");
        announcementArea.setStyle(FIELD_STYLE + "-fx-font-size: 14px;");
        sec4.getChildren().add(announcementArea);

        grid.add(sec1, 0, 0);
        grid.add(sec2, 1, 0);
        grid.add(sec3, 0, 1);
        grid.add(sec5, 1, 1);
        grid.add(sec4, 0, 2, 2, 1);

        content.getChildren().addAll(grid);
        root.getChildren().addAll(headerArea, content);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
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
            
            mainApp.saveConfig();
            ToastService.showSuccess("Загальні налаштування збережено!");
            } catch (NumberFormatException e) {
            ToastService.showError("Некоректні дані: порт має бути числом.");
            }    }
}
