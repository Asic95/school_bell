package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;

import static com.schoolbell.ui.ControlFactory.createDialogRoot;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createSecondaryDialogButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_STYLE;

public class SignalAudioEditorDialog extends BasePremiumDialog {
    private final ConfigService config;
    private final String alertType;

    private TextField pathStart;
    private TextField pathClear;
    private TextField pathError;

    public SignalAudioEditorDialog(MainApp mainApp, String alertType) {
        super(mainApp.getStage(),
                "ЗВУКОВІ СИГНАЛИ",
                "Налаштування звуків",
                formatTitle(alertType),
                "ЗБЕРЕГТИ",
                600);

        this.config = mainApp.getConfigService();
        this.alertType = alertType;

        VBox fields = new VBox(22);

        if (alertType.equals("AIR_RAID")) {
            pathStart = createFileRow(fields, "Звук початку тривоги", config.getAudioAirRaidPath());
            pathClear = createFileRow(fields, "Звук відбою тривоги", config.getAudioAirRaidClearPath());
            pathError = createFileRow(fields, "Звук помилки автоматизації", config.getAudioAirRaidErrorPath());
        } else if (alertType.equals("EMERGENCY")) {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioEmergencyPath());
        } else {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioSilencePath());
        }

        content.getChildren().add(fields);
    }

    @Override
    protected boolean onSave() {
        if (alertType.equals("AIR_RAID")) {
            config.setAudioAirRaidPath(pathStart.getText());
            config.setAudioAirRaidClearPath(pathClear.getText());
            config.setAudioAirRaidErrorPath(pathError.getText());
        } else if (alertType.equals("EMERGENCY")) {
            config.setAudioEmergencyPath(pathStart.getText());
        } else {
            config.setAudioSilencePath(pathStart.getText());
        }
        return true;
    }

    private TextField createFileRow(VBox container, String labelText, String initialValue) {
        VBox box = new VBox(8);
        
        HBox labelRow = new HBox(8);
        labelRow.setAlignment(Pos.CENTER_LEFT);
        
        Label lbl = new Label(labelText.toUpperCase());
        lbl.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        
        Label alertIcon = new Label();
        alertIcon.setGraphic(createSVGIcon(ICON_ALERT, Color.web(COLOR_DANGER), 14));
        alertIcon.setVisible(false);
        Tooltip alertTooltip = new Tooltip("Файл не знайдено за вказаним шляхом!");
        Tooltip.install(alertIcon, alertTooltip);
        
        labelRow.getChildren().addAll(lbl, alertIcon);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField field = new TextField(initialValue);
        field.setEditable(false);
        field.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 14px; -fx-padding: 11 16;");
        HBox.setHgrow(field, Priority.ALWAYS);

        Runnable validate = () -> {
            String path = field.getText();
            boolean exists = path != null && !path.isEmpty() && new File(path).exists();
            alertIcon.setVisible(!exists && path != null && !path.isEmpty());
            if (!exists && path != null && !path.isEmpty()) {
                field.setStyle(PREMIUM_FIELD_ERROR_STYLE + "-fx-font-size: 14px; -fx-padding: 11 16;");
            } else {
                field.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 14px; -fx-padding: 11 16;");
            }
        };

        field.textProperty().addListener((obs, old, nv) -> validate.run());
        validate.run();

        Button pickBtn = new Button();
        pickBtn.setGraphic(createSVGIcon(ICON_FOLDER, Color.web(COLOR_PRIMARY), 18));
        String pickStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 14; -fx-padding: 10; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_03 + ", 5, 0, 0, 1);";
        pickBtn.setStyle(pickStyle);
        pickBtn.setOnMouseEntered(e -> pickBtn.setStyle(pickStyle + "-fx-background-color: " + COLOR_SURFACE_GLASS_START + "; -fx-border-color: " + COLOR_PRIMARY + ";"));
        pickBtn.setOnMouseExited(e -> pickBtn.setStyle(pickStyle));
        pickBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Оберіть " + labelText);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо (WAV)", "*.wav"));
            File file = chooser.showOpenDialog(this);
            if (file != null) {
                field.setText(file.getAbsolutePath());
            }
        });

        Button clearBtn = new Button();
        clearBtn.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 18));
        String clearStyle = "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: " + COLOR_DANGER_BORDER + "; -fx-border-radius: 14; -fx-padding: 10; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, " + SHADOW_BLACK_03 + ", 5, 0, 0, 1);";
        clearBtn.setStyle(clearStyle);
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(clearStyle + "-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-border-color: " + COLOR_DANGER + ";"));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle(clearStyle));
        clearBtn.setOnAction(e -> field.setText(""));

        row.getChildren().addAll(field, pickBtn, clearBtn);
        box.getChildren().addAll(labelRow, row);
        container.getChildren().add(box);
        return field;
    }

    private static String formatTitle(String type) {
        return switch (type) {
            case "AIR_RAID" -> "Повітряна тривога";
            case "EMERGENCY" -> "Екстрена ситуація";
            default -> "Хвилина мовчання";
        };
    }
}
