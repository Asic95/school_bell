package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;

import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class SignalAudioEditorDialog extends Stage {
    private final ConfigService config;
    private final String alertType;
    
    private TextField pathStart;
    private TextField pathClear;
    private TextField pathError;

    public SignalAudioEditorDialog(MainApp mainApp, String alertType) {
        this.config = mainApp.getConfigService();
        this.alertType = alertType;
        
        initModality(Modality.APPLICATION_MODAL);
        initOwner(mainApp.getStage());
        initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(25);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD + "-fx-background-radius: 32; -fx-border-radius: 32; -fx-border-width: 2; -fx-border-color: #e2e8f0;");
        root.setPrefWidth(550);

        Label title = new Label("Налаштування звуків: " + formatTitle(alertType));
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + ";");

        VBox fields = new VBox(18);
        
        if (alertType.equals("AIR_RAID")) {
            pathStart = createFileRow(fields, "Звук початку тривоги", config.getAudioAirRaidPath());
            pathClear = createFileRow(fields, "Звук відбою тривоги", config.getAudioAirRaidClearPath());
            pathError = createFileRow(fields, "Звук помилки автоматизації", config.getAudioAirRaidErrorPath());
        } else if (alertType.equals("EMERGENCY")) {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioEmergencyPath());
        } else {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioSilencePath());
        }

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ", ICON_SAVE);
        saveBtn.setOnAction(e -> {
            save();
            close();
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(title, fields, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
    }

    private TextField createFileRow(VBox container, String labelText, String initialValue) {
        VBox box = new VBox(6);
        Label lbl = new Label(labelText.toUpperCase());
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px;");
        
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        TextField field = new TextField(initialValue);
        field.setEditable(false);
        field.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 10; -fx-font-size: 13px;");
        HBox.setHgrow(field, Priority.ALWAYS);
        
        Button pickBtn = new Button();
        pickBtn.setGraphic(createSVGIcon(ICON_FOLDER, Color.web(COLOR_PRIMARY), 18));
        pickBtn.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 8; -fx-cursor: hand;");
        pickBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Оберіть " + labelText);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо", "*.mp3", "*.wav"));
            File file = chooser.showOpenDialog(this);
            if (file != null) field.setText(file.getAbsolutePath());
        });

        Button clearBtn = new Button();
        clearBtn.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 18));
        clearBtn.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 8; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> field.setText(""));

        row.getChildren().addAll(field, pickBtn, clearBtn);
        box.getChildren().addAll(lbl, row);
        container.getChildren().add(box);
        return field;
    }

    private String formatTitle(String type) {
        return switch (type) {
            case "AIR_RAID" -> "Повітряна тривога";
            case "EMERGENCY" -> "Екстрена ситуація";
            default -> "Хвилина мовчання";
        };
    }

    private void save() {
        if (alertType.equals("AIR_RAID")) {
            config.setAudioAirRaidPath(pathStart.getText());
            config.setAudioAirRaidClearPath(pathClear.getText());
            config.setAudioAirRaidErrorPath(pathError.getText());
        } else if (alertType.equals("EMERGENCY")) {
            config.setAudioEmergencyPath(pathStart.getText());
        } else {
            config.setAudioSilencePath(pathStart.getText());
        }
    }
}
