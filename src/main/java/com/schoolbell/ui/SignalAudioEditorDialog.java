package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Screen;

import java.io.File;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_STYLE;

public class SignalAudioEditorDialog extends BasePremiumDialog {
    private final MainApp mainApp;
    private final ConfigService config;
    private final String alertType;
    private boolean saved;

    private TextField pathStart;
    private TextField pathYellow;
    private TextField pathRed;
    private TextField pathClear;
    private TextField pathError;
    private boolean threatLevelMode;

    public SignalAudioEditorDialog(MainApp mainApp, String alertType) {
        super(mainApp.getStage(),
                "ЗВУКОВІ СИГНАЛИ",
                "Налаштування звуків",
                formatTitle(alertType),
                "ЗБЕРЕГТИ",
                600);

        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.alertType = alertType;

        VBox fields = new VBox(22);

        if (alertType.equals("AIR_RAID")) {
            Label modeLabel = new Label("РЕЖИМ ЗВУКОВОГО СПОВІЩЕННЯ");
            modeLabel.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");

            threatLevelMode = ConfigService.AIR_RAID_AUDIO_MODE_THREAT_LEVEL.equals(config.getAudioAirRaidMode());
            VBox levelFields = new VBox(18);
            pathYellow = createFileRow(levelFields, "Дронова загроза (жовтий рівень)", config.getAudioAirRaidYellowPath());
            pathRed = createFileRow(levelFields, "Ракетна загроза (червоний рівень)", config.getAudioAirRaidRedPath());

            Runnable updateModeFields = () -> {
                boolean useLevels = threatLevelMode;
                levelFields.setManaged(useLevels);
                levelFields.setVisible(useLevels);
                if (pathStart != null && pathStart.getParent() != null) {
                    javafx.scene.Node startFieldBlock = pathStart.getParent().getParent();
                    if (startFieldBlock != null) {
                        startFieldBlock.setManaged(!useLevels);
                        startFieldBlock.setVisible(!useLevels);
                    }
                }
            };
            HBox modeRow = ControlFactory.createWideModeToggle(
                    "Стандартний режим",
                    "За рівнем загрози",
                    threatLevelMode,
                    useLevels -> {
                        threatLevelMode = useLevels;
                        updateModeFields.run();
                        resizeDialogToContent();
                    });
            fields.getChildren().addAll(modeLabel, modeRow, levelFields);

            pathStart = createFileRow(fields, "Звук початку тривоги", config.getAudioAirRaidPath());
            pathClear = createFileRow(fields, "Звук відбою тривоги", config.getAudioAirRaidClearPath());
            pathError = createFileRow(fields, "Звук помилки автоматизації", config.getAudioAirRaidErrorPath());
            boolean useLevels = threatLevelMode;
            if (pathStart != null && pathStart.getParent() != null) {
                javafx.scene.Node startFieldBlock = pathStart.getParent().getParent();
                if (startFieldBlock != null) {
                    startFieldBlock.setManaged(!useLevels);
                    startFieldBlock.setVisible(!useLevels);
                }
            }
            updateModeFields.run();
        } else if (alertType.equals("EMERGENCY")) {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioEmergencyPath());
        } else {
            pathStart = createFileRow(fields, "Основний звук сигналу", config.getAudioSilencePath());
        }

        content.getChildren().add(fields);
        setOnShown(e -> resizeDialogToContent());
    }

    public boolean wasSaved() {
        return saved;
    }

    private void resizeDialogToContent() {
        Platform.runLater(() -> {
            if (getScene() == null) return;

            getScene().getRoot().applyCss();
            getScene().getRoot().layout();
            sizeToScene();

            Rectangle2D visualBounds = Screen.getScreensForRectangle(getX(), getY(), getWidth(), getHeight())
                    .stream()
                    .findFirst()
                    .orElse(Screen.getPrimary())
                    .getVisualBounds();
            double maxHeight = Math.max(400, visualBounds.getHeight() - 40);
            double maxWidth = Math.max(500, visualBounds.getWidth() - 40);

            if (getHeight() > maxHeight) setHeight(maxHeight);
            if (getWidth() > maxWidth) setWidth(maxWidth);
            centerOnScreen();
        });
    }

    @Override
    protected boolean onSave() {
        if (alertType.equals("AIR_RAID")) {
            config.setAudioAirRaidMode(threatLevelMode
                    ? ConfigService.AIR_RAID_AUDIO_MODE_THREAT_LEVEL
                    : ConfigService.AIR_RAID_AUDIO_MODE_GENERAL);
            config.setAudioAirRaidYellowPath(pathYellow.getText());
            config.setAudioAirRaidRedPath(pathRed.getText());
            config.setAudioAirRaidPath(pathStart.getText());
            config.setAudioAirRaidClearPath(pathClear.getText());
            config.setAudioAirRaidErrorPath(pathError.getText());
        } else if (alertType.equals("EMERGENCY")) {
            config.setAudioEmergencyPath(pathStart.getText());
        } else {
            config.setAudioSilencePath(pathStart.getText());
        }
        mainApp.saveConfig();
        saved = true;
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

        Button pickBtn = createCardActionButton(ICON_FOLDER, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
        pickBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Оберіть " + labelText);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо файли (MP3, WAV)", "*.mp3", "*.wav"));
            File file = chooser.showOpenDialog(this);
            if (file != null) {
                field.setText(file.getAbsolutePath());
            }
        });

        Button clearBtn = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
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
