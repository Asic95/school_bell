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

        Label title = new Label("Конфігурація екстрених сигналів");
        title.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label subtitle = new Label("Кожен сценарій оформлений як окрема card-поверхня з компактними діями та читабельними станами.");
        subtitle.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox list = new VBox(18);
        list.getChildren().addAll(
                createAlertCard("Повітряна тривога", "Аудіо • Екран", ICON_AIR_RAID, "#F59E0B", "#FFF7ED",
                        arAudioTg = createToggleSwitch(config.isAudioAirRaidEnabled()),
                        arAudioPath = hiddenField(config.getAudioAirRaidPath()),
                        arVisualTg = createToggleSwitch(config.isVisualAirRaidEnabled())),
                createAlertCard("Екстрена ситуація", "Аудіо • Екран", ICON_LIFEBUOY, "#EF4444", "#FEF2F2",
                        emAudioTg = createToggleSwitch(config.isAudioEmergencyEnabled()),
                        emAudioPath = hiddenField(config.getAudioEmergencyPath()),
                        emVisualTg = createToggleSwitch(config.isVisualEmergencyEnabled())),
                createAlertCard("Хвилина мовчання", "Аудіо • Екран", ICON_CLOCK, "#2563EB", "#EFF6FF",
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
        iconBox.setPrefSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + iconBg + "; -fx-background-radius: 20;");

        Label status = createStatusBadge(audioToggle.isSelected() || visualToggle.isSelected());
        Label name = new Label(title);
        name.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label meta = new Label(channels);
        meta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        audioToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));
        visualToggle.selectedProperty().addListener((obs, oldVal, newVal) -> applyStatusStyle(status, audioToggle, visualToggle));

        VBox info = new VBox(6, status, name, meta);
        info.setAlignment(Pos.CENTER_LEFT);
        info.setMinWidth(220);
        info.setPrefWidth(235);

        VBox fileCard = createFileCard(pathField);
        fileCard.setMinWidth(250);
        fileCard.setPrefWidth(300);
        HBox.setHgrow(fileCard, Priority.ALWAYS);

        HBox controls = new HBox(10,
                createToggleSurface("Аудіо", ICON_VOLUME, audioToggle),
                createToggleSurface("Екран", ICON_MONITOR, visualToggle)
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        Button preview = createCardActionButton("M8,5V19L19,12L8,5Z", "#EEF2FF", "#C7D2FE");
        preview.setOnAction(e -> mainApp.getAudioService().playAudioFile(pathField.getText()));

        Button browse = createCardActionButton(ICON_FOLDER, "#EFF6FF", "#BFDBFE");
        browse.setOnAction(e -> chooseFile(pathField));

        MenuButton more = new MenuButton();
        more.setGraphic(createSVGIcon("M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z", Color.web("#475569"), 18));
        more.setStyle(
                "-fx-background-color: rgba(248,250,252,0.96);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(226,232,240,0.85);" +
                "-fx-border-radius: 18;" +
                "-fx-padding: 4;" +
                "-fx-cursor: hand;"
        );
        MenuItem pick = new MenuItem("Оберіть аудіофайл");
        pick.setOnAction(e -> chooseFile(pathField));
        MenuItem clear = new MenuItem("Очистити файл");
        clear.setOnAction(e -> pathField.setText(""));
        more.getItems().addAll(pick, clear);

        HBox actions = new HBox(10, preview, browse, more);
        actions.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(iconBox, info, fileCard, controls, actions);
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
        fileName.setMaxWidth(180);

        Label fileMeta = new Label();
        fileMeta.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #94a3b8;");

        Runnable refresh = () -> {
            File file = pathField.getText() == null || pathField.getText().isBlank() ? null : new File(pathField.getText());
            if (file == null || !file.exists()) {
                fileName.setText("Додайте аудіофайл");
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
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 14, 12, 14));
        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.96);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: rgba(226,232,240,0.9);" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;"
        );
        Label label = new Label(text);
        label.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
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
