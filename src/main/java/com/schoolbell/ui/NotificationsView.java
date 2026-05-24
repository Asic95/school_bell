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
import static com.schoolbell.ui.UIStyles.*;

public class NotificationsView {
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
        page.setStyle("-fx-background-color: " + COLOR_BG + ";");

        page.getChildren().add(buildHeader());

        HBox topCards = new HBox(25);
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
                createHelpCard(ICON_INFO, "Порада", "Для кращого сканування тримайте активними лише сценарії з уже підключеним аудіофайлом.", COLOR_WARNING_AMBER)
        );

        HBox content = new HBox(28, mainCol, helpPanel);
        content.setAlignment(Pos.TOP_LEFT);

        page.getChildren().add(content);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private HBox buildHeader() {
        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ЗМІНИ", ICON_NOTIFICATIONS);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(e -> save());

        return ControlFactory.createPageHeader(
            "ПОВІДОМЛЕННЯ ТА ЗВУК",
            "Сигнали та сповіщення",
            "Керування екстреними сигналами, фоновою музикою та вибором аудіопристроїв.",
            ICON_NOTIFICATIONS,
            COLOR_INDIGO,
            saveBtn
        );
    }

    private VBox createDeviceCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setMinWidth(380);
        card.setStyle(SOFT_CARD);

        Label label = new Label("ПРИСТРІЙ ВІДТВОРЕННЯ");
        label.setStyle(HEADER_STYLE);

        deviceCombo = new ComboBox<>();
        deviceCombo.setMaxWidth(Double.MAX_VALUE);
        deviceCombo.setStyle(PREMIUM_SELECT_STYLE);
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
        } catch (Exception ignored) {}

        VBox iconWrap = new VBox(createSVGIcon(ICON_VOLUME, Color.web(COLOR_PRIMARY), 24));
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setPrefSize(54, 54);
        iconWrap.setStyle(ICON_BADGE_STYLE);

        HBox row = new HBox(20, iconWrap, deviceCombo);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(deviceCombo, Priority.ALWAYS);

        Label note = new Label("Оберіть джерело аудіосигналу для всіх типів дзвінків та трансляцій.");
        note.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");

        card.getChildren().addAll(label, row, note);
        return card;
    }

    private VBox createVolumeCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setMinWidth(420);
        card.setPrefWidth(420);
        card.setStyle(SOFT_CARD);

        Label label = new Label("ЗАГАЛЬНА ГУЧНІСТЬ");
        label.setStyle(HEADER_STYLE);

        currentVolumeValue = normalizeVolume(config.getSystemVolume());
        volumePresetBox = new HBox(8);
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        volumePresetBox.setStyle(PREMIUM_TOGGLE_CONTAINER);

        int[] presets = {0, 25, 50, 75, 100};
        for (int preset : presets) {
            Button button = new Button(preset == 0 ? "ВИМК" : preset + "%");
            button.setUserData(preset);
            button.setPrefWidth(85);
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
        Label note = new Label("Використовуйте пресети для швидкого налаштування.");
        note.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");
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
            if (!(node instanceof Button button)) continue;

            int value = (int) button.getUserData();
            if (value == currentVolumeValue) {
                button.setStyle(PREMIUM_TOGGLE_ACTIVE);
            } else {
                button.setStyle(PREMIUM_TOGGLE_INACTIVE);
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
