package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class EmergencyAlertsPanel {
    private static final String SECTION_CARD =
            "-fx-background-color: " + GLASS_WHITE + ";" +
            "-fx-background-radius: 32;" +
            "-fx-border-color: " + BORDER_SLATE_72 + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 32;" +
            "-fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_08 + ", 30, 0, 0, 10);";
    private static final String ALERT_CARD =
            "-fx-background-color: linear-gradient(to bottom right, " + COLOR_WHITE + ", " + COLOR_SURFACE_CLOUD + ");" +
            "-fx-background-radius: 26;" +
            "-fx-border-color: " + BORDER_SLATE_60 + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 26;" +
            "-fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_06 + ", 22, 0, 0, 6);";

    private final MainApp mainApp;
    private final ConfigService config;

    private ToggleButton arAudioTg;
    private TextField arAudioPath;
    private ToggleButton arVisualTg;

    private ToggleButton emAudioTg;
    private TextField emAudioPath;
    private ToggleButton emVisualTg;

    private ToggleButton siAudioTg;
    private TextField siAudioPath;
    private ToggleButton siVisualTg;
    private Runnable onChanged;

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    private void trigger() {
        if (onChanged != null) onChanged.run();
    }

    public EmergencyAlertsPanel(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
    }

    public Node build() {
        VBox section = new VBox(18);
        section.setPadding(new Insets(28));
        section.setStyle(SECTION_CARD);

        Label title = new Label("Конфігурація сигналів");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";");
        Label subtitle = new Label("Налаштуйте параметри сповіщень та оберіть звукові файли для кожного сценарію.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");

        arAudioTg = createToggleSwitch(config.isAudioAirRaidEnabled());
        arAudioPath = hiddenField(config.getAudioAirRaidPath());
        arVisualTg = createToggleSwitch(config.isVisualAirRaidEnabled());
        
        emAudioTg = createToggleSwitch(config.isAudioEmergencyEnabled());
        emAudioPath = hiddenField(config.getAudioEmergencyPath());
        emVisualTg = createToggleSwitch(config.isVisualEmergencyEnabled());
        
        siAudioTg = createToggleSwitch(config.isAudioSilenceEnabled());
        siAudioPath = hiddenField(config.getAudioSilencePath());
        siVisualTg = createToggleSwitch(config.isVisualSilenceEnabled());

        setupAutoSaveListeners();

        VBox list = new VBox(18);
        list.getChildren().addAll(
                createAlertCard("Повітряна тривога", "Аудіо • Екран", ICON_AIR_RAID, COLOR_WARNING_AMBER, COLOR_AMBER_LIGHT, "AIR_RAID",
                        arAudioTg, arAudioPath, arVisualTg),
                createAlertCard("Екстрена ситуація", "Аудіо • Екран", ICON_LIFEBUOY, COLOR_ALERT_RED, COLOR_ALERT_RED_LIGHT, "EMERGENCY",
                        emAudioTg, emAudioPath, emVisualTg),
                createAlertCard("Хвилина мовчання", "Аудіо • Екран", ICON_CLOCK, COLOR_ALERT_BLUE, COLOR_ALERT_BLUE_LIGHT, "SILENCE",
                        siAudioTg, siAudioPath, siVisualTg)
        );

        section.getChildren().addAll(title, subtitle, list);
        return section;
    }

    private void setupAutoSaveListeners() {
        arAudioTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        arVisualTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        arAudioPath.textProperty().addListener((o, ov, nv) -> trigger());

        emAudioTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        emVisualTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        emAudioPath.textProperty().addListener((o, ov, nv) -> trigger());

        siAudioTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        siVisualTg.selectedProperty().addListener((o, ov, nv) -> trigger());
        siAudioPath.textProperty().addListener((o, ov, nv) -> trigger());
    }

    private TextField hiddenField(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setVisible(false);
        field.setManaged(false);
        return field;
    }

    private VBox createAlertCard(
            String title,
            String channels,
            String iconPath,
            String accent,
            String iconBg,
            String alertType,
            ToggleButton audioToggle,
            TextField pathField,
            ToggleButton visualToggle
    ) {
        VBox card = new VBox();
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(ALERT_CARD);

        HBox row = new HBox(18);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setFillHeight(true); // Allow stretching to match tallest sibling

        Node iconNode = createSVGIcon(iconPath, Color.web(accent), 26);

        VBox iconBox = new VBox(iconNode);
        iconBox.setAlignment(Pos.CENTER);
        // Force perfect square 54x54
        iconBox.setPrefSize(54, 54);
        iconBox.setMinSize(54, 54);
        iconBox.setMaxSize(54, 54);
        HBox.setHgrow(iconBox, Priority.NEVER);
        iconBox.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 18;");

        Label status = createStatusBadge(audioToggle.isSelected() || visualToggle.isSelected());
        Label name = new Label(title);
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 19px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_NAVY + ";");
        Label meta = new Label(channels);
        meta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");

        audioToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));
        visualToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));

        VBox info = new VBox(3, status, name, meta);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(170);
        info.setPrefWidth(180);

        VBox audioConfigBtn = createAudioConfigButton(alertType, pathField);
        audioConfigBtn.setMinWidth(140);
        audioConfigBtn.setPrefWidth(160);
        audioConfigBtn.setPrefHeight(78); // Slightly taller for better balance
        HBox.setHgrow(audioConfigBtn, Priority.SOMETIMES);

        HBox controls = new HBox(10,
                createToggleSurface("Аудіо", ICON_VOLUME, audioToggle, 78),
                createToggleSurface("Екран", ICON_MONITOR, visualToggle, 78)
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        MenuButton more = new MenuButton();
        // ... (rest of the MenuButton code)
        more.setGraphic(createSVGIcon("M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z", Color.web(COLOR_SLATE_DARK), 18));
        more.setStyle(
                "-fx-background-color: " + GLASS_SKY + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: " + BORDER_SLATE_85 + ";" +
                "-fx-border-radius: 18;" +
                "-fx-padding: 6;" +
                "-fx-cursor: hand;"
        );

        MenuItem testAudio = new MenuItem("Тест Аудіо (Початок)");
        testAudio.setOnAction(e -> mainApp.getAudioService().playAudioFile(pathField.getText()));

        MenuItem testVisual = new MenuItem("Тест Екрану");
        testVisual.setOnAction(e -> mainApp.getSignalService().setTemporaryAlertType(alertType, 15000));

        javafx.scene.control.SeparatorMenuItem sep = new javafx.scene.control.SeparatorMenuItem();

        MenuItem pick = new MenuItem("Налаштувати звуки");
        pick.setOnAction(e -> {
            new SignalAudioEditorDialog(mainApp, alertType).showAndWait();
            refreshPathsFromConfig();
        });

        more.getItems().addAll(testAudio, testVisual, sep, pick);

        row.getChildren().addAll(iconBox, info, audioConfigBtn, controls, more);
        card.getChildren().add(row);

        applyStatusStyle(status, audioToggle, visualToggle);
        return card;
    }

    private void refreshPathsFromConfig() {
        arAudioPath.setText(config.getAudioAirRaidPath());
        emAudioPath.setText(config.getAudioEmergencyPath());
        siAudioPath.setText(config.getAudioSilencePath());
    }

    private VBox createAudioConfigButton(String alertType, TextField pathField) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: " + GLASS_SKY_95 + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: " + BORDER_FIELD_75 + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;" +
                "-fx-cursor: hand;"
        );

        Label label = new Label("КОНФІГУРАЦІЯ ЗВУКІВ");
        label.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1px;");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(createSVGIcon(ICON_MUSIC, Color.web(COLOR_INDIGO), 18));

        Label fileName = new Label();
        fileName.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + ";");
        fileName.setText(alertType.equals("AIR_RAID") ? "Параметри звуків" : "Звуковий файл");
        
        Label fileMeta = new Label("Натисніть для зміни");
        fileMeta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");

        VBox meta = new VBox(3, fileName, fileMeta);
        meta.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(meta);
        card.getChildren().addAll(label, row);

        card.setOnMouseClicked(e -> {
            new SignalAudioEditorDialog(mainApp, alertType).showAndWait();
            refreshPathsFromConfig();
        });
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: " + COLOR_SURFACE_SOFT + ";"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: " + COLOR_SURFACE_SOFT + ";", "")));

        return card;
    }

    private HBox createToggleSurface(String text, String iconPath, ToggleButton toggle, double height) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 16, 0, 16));
        box.setPrefHeight(height);
        box.setMinHeight(height);
        box.setStyle(
                "-fx-background-color: " + GLASS_WHITE + ";" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: " + BORDER_SLATE_90 + ";" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;"
        );
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + ";");
        label.setMinWidth(Region.USE_PREF_SIZE);
        box.getChildren().addAll(createSVGIcon(iconPath, Color.web(COLOR_SLATE_STRONG), 16), label, toggle);
        return box;
    }

    private Label createStatusBadge(boolean enabled) {
        Label status = new Label(enabled ? "Активний" : "Вимкнений");
        status.setPadding(new Insets(6, 12, 6, 12));
        status.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 700;");
        return status;
    }

    private void applyStatusStyle(Label status, ToggleButton audioToggle, ToggleButton visualToggle) {
        boolean enabled = audioToggle.isSelected() || visualToggle.isSelected();
        status.setText(enabled ? "● Активний" : "● Вимкнений");
        if (enabled) {
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-text-fill: " + COLOR_GREEN_DARK + ";" +
                    "-fx-background-color: " + COLOR_SUCCESS_PALE + ";" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        } else {
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-text-fill: " + COLOR_SLATE + ";" +
                    "-fx-background-color: " + COLOR_SURFACE_SOFT + ";" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        }
    }

    public void syncToConfig() {
        config.setAudioAirRaidEnabled(arAudioTg.isSelected());
        config.setAudioAirRaidPath(arAudioPath.getText());
        config.setVisualAirRaidEnabled(arVisualTg.isSelected());

        config.setAudioEmergencyEnabled(emAudioTg.isSelected());
        config.setAudioEmergencyPath(emAudioPath.getText());
        config.setVisualEmergencyEnabled(emVisualTg.isSelected());

        config.setAudioSilenceEnabled(siAudioTg.isSelected());
        config.setAudioSilencePath(siAudioPath.getText());
        config.setVisualSilenceEnabled(siVisualTg.isSelected());
    }
}
