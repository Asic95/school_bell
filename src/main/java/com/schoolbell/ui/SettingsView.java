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
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f1f2f6;");

        Label title = new Label("ЗАГАЛЬНІ НАЛАШТУВАННЯ СИСТЕМИ");
        title.setStyle(HEADER_STYLE + "-fx-font-size: 24px;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle(SOFT_CARD);

        // Section 1: Identity
        VBox sec1 = createSettingsSection("ІДЕНТИФІКАЦІЯ ЗАКЛАДУ", "#0984e3", ICON_PERSON);
        GridPane identityGrid = new GridPane();
        identityGrid.setHgap(20);
        identityGrid.setVgap(15);
        identityGrid.setPadding(new Insets(10, 0, 0, 0));
        
        Label lblSchool = new Label("Назва закладу:");
        lblSchool.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
        schoolNameField.setPrefWidth(400); 
        identityGrid.add(lblSchool, 0, 0);
        identityGrid.add(schoolNameField, 1, 0);
        
        Label lblCity = new Label("Місто:");
        lblCity.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
        cityNameField.setPrefWidth(400);
        identityGrid.add(lblCity, 0, 1);
        identityGrid.add(cityNameField, 1, 1);
        
        sec1.getChildren().add(identityGrid);

        // Section 2: Audio
        VBox sec2 = createSettingsSection("НАЛАШТУВАННЯ ЗВУКУ", "#00b894", ICON_VOLUME);
        Label volVal = new Label(config.getSystemVolume() + "%");
        volVal.setStyle("-fx-font-weight: 900; -fx-text-fill: " + COLOR_SUCCESS + ";");
        volumeSlider.valueProperty().addListener((o, ov, nv) -> volVal.setText(nv.intValue() + "%"));
        HBox volRow = new HBox(15, volumeSlider, volVal);
        volRow.setAlignment(Pos.CENTER_LEFT);
        sec2.getChildren().addAll(new Label("Загальна гучність системи:"), volRow);

        // Section 3: Network
        VBox sec3 = createSettingsSection("МЕРЕЖЕВІ НАЛАШТУВАННЯ", "#6c5ce7", ICON_BROADCAST);
        sec3.getChildren().addAll(
            broadcastEnabledCb,
            createFieldRow("Порт трансляції:", portField)
        );

        // Section 5: System
        VBox sec5 = createSettingsSection("СИСТЕМНІ НАЛАШТУВАННЯ", "#636e72", ICON_SETTINGS);
        sec5.getChildren().add(simulationModeCb);

        // Section 4: Announcement
        VBox sec4 = createSettingsSection("ТЕКСТ ОГОЛОШЕННЯ (ДАШБОРД)", "#f39c12", ICON_MESSAGE);
        sec4.getChildren().add(announcementArea);

        Button saveBtn = new Button("ЗБЕРЕГТИ ВСІ НАЛАШТУВАННЯ");
        saveBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 12 40; -fx-background-radius: 12;");
        saveBtn.setOnAction(e -> save());
        
        HBox footer = new HBox(saveBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(sec1, sec2, sec3, sec5, sec4, footer);
        root.getChildren().addAll(title, content);

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
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успіх");
            alert.setHeaderText(null);
            alert.setContentText("Загальні налаштування збережено!");
            alert.showAndWait();
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Помилка");
            alert.setHeaderText("Некоректні дані");
            alert.setContentText("Будь ласка, перевірте правильність введених даних (порт має бути числом).");
            alert.showAndWait();
        }
    }
}
