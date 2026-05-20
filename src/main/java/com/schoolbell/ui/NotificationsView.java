package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.COLOR_BG;
import static com.schoolbell.ui.UIStyles.COLOR_PURPLE;
import static com.schoolbell.ui.UIStyles.COLOR_SUCCESS;
import static com.schoolbell.ui.UIStyles.ICON_INFO;
import static com.schoolbell.ui.UIStyles.ICON_MONITOR;
import static com.schoolbell.ui.UIStyles.ICON_NOTIFICATIONS;
import static com.schoolbell.ui.UIStyles.ICON_VOLUME;

public class NotificationsView {
    private static final String PAGE_BACKGROUND =
            "-fx-background-color: linear-gradient(to bottom right, #f8fbff 0%, #f5f7fb 45%, #edf4ff 100%);";
    private static final String FLOATING_CARD =
            "-fx-background-color: rgba(255,255,255,0.96);" +
            "-fx-background-radius: 28;" +
            "-fx-border-color: rgba(226,232,240,0.7);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 28;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 30, 0, 0, 10);";
    private static final String MICRO_LABEL =
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: #64748b;";
    private static final String TITLE_STYLE =
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 32px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #0f172a;";
    private static final String BODY_STYLE =
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 500;" +
            "-fx-text-fill: #64748b;";
    private static final String SELECT_STYLE =
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 17px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #dbe4f0;" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 1;" +
            "-fx-padding: 11 16;" +
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;";

    private final MainApp mainApp;
    private final ConfigService config;

    private ComboBox<String> deviceCombo;
    private int currentVolumeValue;
    private HBox volumePresetBox;

    private final EmergencyAlertsPanel alertsPanel;
    private final MediaSchedulerPanel mediaPanel;

    public NotificationsView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.alertsPanel = new EmergencyAlertsPanel(mainApp);
        this.mediaPanel = new MediaSchedulerPanel(mainApp);
    }

    public Node build() {
        VBox page = new VBox(28);
        page.setPadding(new Insets(30, 20, 40, 20));
        page.setStyle(PAGE_BACKGROUND);

        page.getChildren().add(buildHeader());

        HBox topCards = new HBox(20);
        VBox deviceCard = createDeviceCard();
        VBox volumeCard = createVolumeCard();
        HBox.setHgrow(deviceCard, Priority.ALWAYS);
        topCards.getChildren().addAll(deviceCard, volumeCard);

        VBox mainCol = new VBox(28, topCards, alertsPanel.build(), mediaPanel.build());
        mainCol.setFillWidth(true);
        HBox.setHgrow(mainCol, Priority.ALWAYS);

        VBox helpPanel = createSideHelpPanel(
                createHelpCard(ICON_VOLUME, "Аудіо", "Швидко змінюйте пристрій відтворення і гучність без зайвих технічних налаштувань.", COLOR_SUCCESS),
                createHelpCard(ICON_MONITOR, "Екран", "Показ розкладу буде тимчасово призупинений, а замість нього буде відображатися текст відповідного сповіщення.", COLOR_PURPLE),
                createHelpCard(ICON_INFO, "Порада", "Для кращого сканування тримайте активними лише сценарії з уже підключеним аудіофайлом.", "#f59e0b")
        );

        HBox content = new HBox(18, mainCol, helpPanel);
        content.setAlignment(Pos.TOP_LEFT);

        page.getChildren().add(content);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: " + COLOR_BG + ";");
        return scroll;
    }

    private HBox buildHeader() {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox badge = new VBox(createSVGIcon(ICON_NOTIFICATIONS, Color.web("#4f46e5"), 24));
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(54, 54);
        badge.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #eef2ff, #dbeafe);" +
                "-fx-background-radius: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.18), 18, 0, 0, 6);"
        );

        Label eyebrow = new Label("Центр керування сповіщеннями");
        eyebrow.setStyle(MICRO_LABEL);
        Label title = new Label("Сигнали та сповіщення");
        title.setStyle(TITLE_STYLE);
        Label subtitle = new Label("Панель для керування екстреними сигналами та автоматичними повідомленнями.");
        subtitle.setStyle(BODY_STYLE);
        subtitle.setWrapText(true);

        VBox text = new VBox(4, eyebrow, title, subtitle);
        text.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = createPrimaryActionButton("Зберегти зміни", ICON_NOTIFICATIONS);
        saveBtn.setOnAction(e -> save());

        header.getChildren().addAll(badge, text, spacer, saveBtn);
        return header;
    }

    private VBox createDeviceCard() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(24));
        card.setMinWidth(380);
        card.setPrefWidth(600);
        card.setStyle(FLOATING_CARD);

        Label label = new Label("Пристрій відтворення");
        label.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: #1e3a8a;"
        );

        deviceCombo = new ComboBox<>();
        deviceCombo.setMaxWidth(Double.MAX_VALUE);
        deviceCombo.setStyle(SELECT_STYLE);
        deviceCombo.setValue(config.getSelectedAudioDeviceName());
        try {
            deviceCombo.getItems().add("Системний за замовчуванням");
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                if (AudioSystem.getMixer(info).isLineSupported(
                        new DataLine.Info(SourceDataLine.class, new AudioFormat(44100, 16, 2, true, false))
                )) {
                    deviceCombo.getItems().add(info.getName());
                }
            }
        } catch (Exception ignored) {
        }

        VBox iconWrap = new VBox(createSVGIcon(ICON_VOLUME, Color.web("#4f46e5"), 24));
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setPrefSize(54, 54);
        iconWrap.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #eef2ff, #ffffff);" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #dbeafe;" +
                "-fx-border-radius: 18;"
        );

        HBox row = new HBox(16, iconWrap, deviceCombo);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(deviceCombo, Priority.ALWAYS);

        Label note = new Label("Оберіть джерело відтворення для всіх сигналів.");
        note.setStyle(BODY_STYLE);

        card.getChildren().addAll(label, row, note);
        return card;
    }

    private VBox createVolumeCard() {
        VBox card = new VBox(18);
        card.setPadding(new Insets(24));
        card.setMinWidth(395);
        card.setPrefWidth(395);
        card.setStyle(FLOATING_CARD);

        Label label = new Label("Загальна гучність сповіщень");
        label.setStyle(
                "-fx-font-family: 'Inter';" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: #1e3a8a;"
        );

        currentVolumeValue = normalizeVolume(config.getSystemVolume());
        volumePresetBox = new HBox(8);
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        volumePresetBox.setStyle(
                "-fx-background-color: rgba(241,245,249,0.92);" +
                "-fx-background-radius: 22;" +
                "-fx-padding: 6;"
        );

        int[] presets = {0, 25, 50, 75, 100};
        for (int preset : presets) {
            Button button = new Button(preset == 0 ? "Вимк" : preset + "%");
            button.setUserData(preset);
            button.setPrefWidth(61);
            button.setPrefHeight(46);
            button.setOnAction(e -> {
                currentVolumeValue = preset;
                updateVolumeStyle();
                mainApp.getAudioService().setVolume(preset);
                mainApp.getSystemService().setWindowsSystemVolume(preset);
            });
            volumePresetBox.getChildren().add(button);
        }

        updateVolumeStyle();
        Label note = new Label("Швидкі пресети гучності.");
        note.setStyle(BODY_STYLE);
        card.getChildren().addAll(label, volumePresetBox, note);
        return card;
    }

    private int normalizeVolume(int value) {
        if (value <= 12) return 0;
        if (value <= 37) return 25;
        if (value <= 62) return 50;
        if (value <= 87) return 75;
        return 100;
    }

    private void updateVolumeStyle() {
        for (Node node : volumePresetBox.getChildren()) {
            if (!(node instanceof Button button)) {
                continue;
            }

            int value = (int) button.getUserData();
            if (value == currentVolumeValue) {
                button.setStyle(
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-color: linear-gradient(to right, #4f46e5, #2563eb);" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.32), 18, 0, 0, 4);"
                );
            } else {
                button.setStyle(
                        "-fx-font-family: 'Inter';" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-text-fill: #334155;" +
                        "-fx-background-color: rgba(255,255,255,0.88);" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;"
                );
            }
        }
    }

    private void save() {
        config.setSelectedAudioDeviceName(deviceCombo.getValue());
        config.setSystemVolume(currentVolumeValue);
        alertsPanel.syncToConfig();
        mainApp.saveConfig();
        ToastService.showSuccess("Налаштування сповіщень збережено");
    }
}
