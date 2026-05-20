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
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createToggleSwitch;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.ICON_AIR_RAID;
import static com.schoolbell.ui.UIStyles.ICON_CLOCK;
import static com.schoolbell.ui.UIStyles.ICON_FOLDER;
import static com.schoolbell.ui.UIStyles.ICON_LIFEBUOY;
import static com.schoolbell.ui.UIStyles.ICON_MONITOR;
import static com.schoolbell.ui.UIStyles.ICON_MUSIC;
import static com.schoolbell.ui.UIStyles.ICON_VOLUME;

public class EmergencyAlertsPanel {
    private static final String SECTION_CARD =
            "-fx-background-color: rgba(255,255,255,0.96);" +
            "-fx-background-radius: 32;" +
            "-fx-border-color: rgba(226,232,240,0.72);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 32;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 30, 0, 0, 10);";
    private static final String ALERT_CARD =
            "-fx-background-color: linear-gradient(to bottom right, #ffffff, #fbfdff);" +
            "-fx-background-radius: 26;" +
            "-fx-border-color: rgba(226,232,240,0.6);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 26;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 22, 0, 0, 6);";

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

    public EmergencyAlertsPanel(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
    }

    public Node build() {
        VBox section = new VBox(18);
        section.setPadding(new Insets(28));
        section.setStyle(SECTION_CARD);

        Label title = new Label("Конфігурація сигналів");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Налаштуйте параметри сповіщень та оберіть звукові файли для кожного сценарію.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox list = new VBox(18);
        list.getChildren().addAll(
                createAlertCard("Повітряна тривога", "Аудіо • Екран", ICON_AIR_RAID, "#F59E0B", "#FFF7ED", "AIR_RAID",
                        arAudioTg = createToggleSwitch(config.isAudioAirRaidEnabled()),
                        arAudioPath = hiddenField(config.getAudioAirRaidPath()),
                        arVisualTg = createToggleSwitch(config.isVisualAirRaidEnabled())),
                createAlertCard("Екстрена ситуація", "Аудіо • Екран", ICON_LIFEBUOY, "#EF4444", "#FEF2F2", "EMERGENCY",
                        emAudioTg = createToggleSwitch(config.isAudioEmergencyEnabled()),
                        emAudioPath = hiddenField(config.getAudioEmergencyPath()),
                        emVisualTg = createToggleSwitch(config.isVisualEmergencyEnabled())),
                createAlertCard("Хвилина мовчання", "Аудіо • Екран", ICON_CLOCK, "#2563EB", "#EFF6FF", "SILENCE",
                        siAudioTg = createToggleSwitch(config.isAudioSilenceEnabled()),
                        siAudioPath = hiddenField(config.getAudioSilencePath()),
                        siVisualTg = createToggleSwitch(config.isVisualSilenceEnabled()))
        );

        section.getChildren().addAll(title, subtitle, list);
        return section;
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

        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(accent), 26));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(58, 58);
        iconBox.setMinSize(58, 58);
        iconBox.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 18;");

        Label status = createStatusBadge(audioToggle.isSelected() || visualToggle.isSelected());
        Label name = new Label(title);
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label meta = new Label(channels);
        meta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        audioToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));
        visualToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));

        VBox info = new VBox(4, status, name, meta);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(200);
        info.setPrefWidth(210);

        VBox fileCard = createFileCard(pathField);
        fileCard.setMinWidth(200);
        fileCard.setPrefWidth(260);
        HBox.setHgrow(fileCard, Priority.ALWAYS);
        fileCard.setStyle(fileCard.getStyle() + "-fx-cursor: hand;");
        fileCard.setOnMouseClicked(e -> chooseFile(pathField));
        fileCard.setOnMouseEntered(e -> fileCard.setStyle(fileCard.getStyle().replace("-fx-background-color: rgba(248,250,252,0.95);", "-fx-background-color: #f1f5f9;")));
        fileCard.setOnMouseExited(e -> fileCard.setStyle(fileCard.getStyle().replace("-fx-background-color: #f1f5f9;", "-fx-background-color: rgba(248,250,252,0.95);")));
        javafx.scene.control.Tooltip.install(fileCard, new javafx.scene.control.Tooltip("Натисніть, щоб змінити аудіофайл"));

        HBox controls = new HBox(10,
                createToggleSurface("Аудіо", ICON_VOLUME, audioToggle),
                createToggleSurface("Екран", ICON_MONITOR, visualToggle)
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        MenuButton more = new MenuButton();
        more.setGraphic(createSVGIcon("M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z", Color.web("#475569"), 18));
        more.setStyle(
                "-fx-background-color: rgba(248,250,252,0.96);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(226,232,240,0.85);" +
                "-fx-border-radius: 18;" +
                "-fx-padding: 6;" +
                "-fx-cursor: hand;"
        );

        MenuItem testAudio = new MenuItem("Тест Аудіо");
        testAudio.setOnAction(e -> mainApp.getAudioService().playAudioFile(pathField.getText()));

        MenuItem testVisual = new MenuItem("Тест Екрану");
        testVisual.setOnAction(e -> mainApp.getSignalService().setTemporaryAlertType(alertType, 15000));

        javafx.scene.control.SeparatorMenuItem sep = new javafx.scene.control.SeparatorMenuItem();

        MenuItem pick = new MenuItem("Змінити файл");
        pick.setOnAction(e -> chooseFile(pathField));

        MenuItem clear = new MenuItem("Очистити файл");
        clear.setStyle("-fx-text-fill: #ef4444;");
        clear.setOnAction(e -> pathField.setText(""));

        more.getItems().addAll(testAudio, testVisual, sep, pick, clear);

        row.getChildren().addAll(iconBox, info, fileCard, controls, more);
        card.getChildren().add(row);

        applyStatusStyle(status, audioToggle, visualToggle);
        return card;
    }

    private VBox createFileCard(TextField pathField) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
                "-fx-background-color: rgba(248,250,252,0.95);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(219,228,240,0.75);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;"
        );

        Label label = new Label("Аудіофайл");
        label.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(createSVGIcon(ICON_MUSIC, Color.web("#4f46e5"), 18));

        Label fileName = new Label();
        fileName.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        fileName.setTextOverrun(OverrunStyle.ELLIPSIS);
        fileName.setMaxWidth(250);

        Label fileMeta = new Label();
        fileMeta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #94a3b8;");

        Runnable refresh = () -> {
            File file = pathField.getText() == null || pathField.getText().isBlank() ? null : new File(pathField.getText());
            if (file == null || !file.exists()) {
                fileName.setText("Натисніть, щоб додати аудіофайл");
                fileMeta.setText("MP3 або WAV до 50 MB");
            } else {
                fileName.setText(file.getName());
                fileMeta.setText(formatFileSize(file.length()));
            }
        };
        refresh.run();
        pathField.textProperty().addListener((obs, oldVal, newVal) -> refresh.run());

        VBox meta = new VBox(3, fileName, fileMeta);
        meta.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(meta);
        card.getChildren().addAll(label, row);
        return card;
    }

    private HBox createToggleSurface(String text, String iconPath, ToggleButton toggle) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.96);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(226,232,240,0.9);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;"
        );
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        label.setMinWidth(Region.USE_PREF_SIZE);
        box.getChildren().addAll(createSVGIcon(iconPath, Color.web("#334155"), 16), label, toggle);
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
                    "-fx-text-fill: #15803d;" +
                    "-fx-background-color: #ecfdf3;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        } else {
            status.setStyle(
                    "-fx-font-family: 'Inter';" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: 700;" +
                    "-fx-text-fill: #64748b;" +
                    "-fx-background-color: #f1f5f9;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 6 12;"
            );
        }
    }

    private void chooseFile(TextField pathField) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Оберіть аудіофайл");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Аудіо (MP3, WAV)", "*.mp3", "*.wav"));
        File file = chooser.showOpenDialog(mainApp.getStage());
        if (file != null) {
            pathField.setText(file.getAbsolutePath());
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) {
            return "0 KB";
        }
        double mb = bytes / (1024d * 1024d);
        if (mb >= 1) {
            return new DecimalFormat("0.0#").format(mb) + " MB";
        }
        double kb = bytes / 1024d;
        return new DecimalFormat("0").format(kb) + " KB";
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
