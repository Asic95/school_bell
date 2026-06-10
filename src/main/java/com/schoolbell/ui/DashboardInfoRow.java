package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.CardFactory.createSmallInfoCard;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardInfoRow extends HBox {
    private final MainApp mainApp;
    private final ConfigService config;

    private final Label activeScheduleValue;
    private final Label volStatusLabel;
    private final HBox volumePresetBox;
    private final Label brStatusLabel;
    private final Label mediaValue;

    private int currentVolumeValue;

    public DashboardInfoRow(MainApp mainApp) {
        super(20);
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        setAlignment(Pos.CENTER_LEFT);

        // Schedule Card
        activeScheduleValue = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        applyInfoValueStyle(activeScheduleValue, COLOR_NAVY);
        VBox schCard = createSmallInfoCard("АКТИВНИЙ РОЗКЛАД", activeScheduleValue, "Змінити", () -> new ScheduleQuickSelectorDialog(mainApp).display(), ICON_CALENDAR, COLOR_BLUE_LIGHT, COLOR_PRIMARY, true, 0, null);

        // Volume Card
        currentVolumeValue = normalizeVolume(config.getSystemVolume());
        volStatusLabel = new Label(currentVolumeValue + "%");
        applyInfoValueStyle(volStatusLabel, COLOR_NAVY);

        volumePresetBox = new HBox(0);
        volumePresetBox.setAlignment(Pos.CENTER_LEFT);
        volumePresetBox.setMaxWidth(Region.USE_PREF_SIZE);
        volumePresetBox.setStyle(PREMIUM_TOGGLE_CONTAINER + "-fx-padding: 2;");

        int[] presets = {0, 20, 40, 60, 80, 100};
        for (int p : presets) {
            Button pb = new Button(p == 0 ? "0" : p + "");
            pb.setPrefHeight(28);
            pb.setMinWidth(38);
            pb.setUserData(p);
            pb.setOnAction(e -> {
                currentVolumeValue = p;
                safeSetText(volStatusLabel, p + "%");
                updateVolumeStyle();
                config.setSystemVolume(p);
                mainApp.getSystemService().setWindowsSystemVolume(p);
                mainApp.saveConfig();
            });
            volumePresetBox.getChildren().add(pb);
        }
        updateVolumeStyle();

        VBox volContent = new VBox(5, volStatusLabel, volumePresetBox);
        VBox volCard = createSmallInfoCard("СИСТЕМНА ГУЧНІСТЬ", volContent, null, null, ICON_VOLUME, COLOR_GREEN_LIGHT, COLOR_SUCCESS, true, 0, null);

        // Broadcast Card
        brStatusLabel = new Label(config.isBroadcastEnabled() ? "Увімкнено" : "Вимкнено");
        applyInfoValueStyle(brStatusLabel, COLOR_NAVY);
        VBox brCard = createSmallInfoCard("ТРАНСЛЯЦІЯ ДАШБОРДУ", brStatusLabel, "Відкрити в браузері", () -> mainApp.getHostServices().showDocument("http://localhost:" + (config.getBroadcastPort())), ICON_MONITOR, COLOR_PURPLE_LIGHT, COLOR_INDIGO_SOFT, true, 0, null);

        // Media Card
        mediaValue = new Label("Очікування...");
        applyInfoValueStyle(mediaValue, COLOR_NAVY);
        VBox mediaCard = createSmallInfoCard("МЕДІА-ЕФІР", mediaValue, "Управління", () -> new MediaQuickControlDialog(mainApp).display(), ICON_AIRPLAY, COLOR_TANGERINE_LIGHT, COLOR_TANGERINE, true, 0, null);

        getChildren().addAll(schCard, volCard, brCard, mediaCard);
        for (Node n : getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    public void updateSchedule(String name) {
        safeSetText(activeScheduleValue, name != null ? name : "Не вибрано");
    }

    public void updateVolume(int volume) {
        currentVolumeValue = normalizeVolume(volume);
        safeSetText(volStatusLabel, currentVolumeValue + "%");
        updateVolumeStyle();
    }

    public void updateBroadcast() {
        safeSetText(brStatusLabel, config.isBroadcastEnabled() ? "Увімкнено" : "Вимкнено");
    }

    public void updateMedia() {
        String currentTrack = mainApp.getAudioService().getCurrentPlayingTrack();
        if (currentTrack != null) {
            safeSetText(mediaValue, "ЗАРАЗ: " + currentTrack.toUpperCase());
            applyInfoValueStyle(mediaValue, COLOR_TANGERINE);
        } else {
            MediaEvent nextEvent = mainApp.getMediaSchedulerService().getNextEvent();
            if (nextEvent != null) {
                safeSetText(mediaValue, nextEvent.time() + " — " + nextEvent.name().toUpperCase());
            } else {
                safeSetText(mediaValue, "ПОДІЙ НЕ ЗАПЛАНОВАНО");
            }
            applyInfoValueStyle(mediaValue, COLOR_NAVY);
        }
    }

    private void applyInfoValueStyle(Label label, String color) {
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");
        label.setWrapText(true);
        label.setMaxWidth(200);
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }

    private int normalizeVolume(int value) {
        if (value <= 10) return 0;
        if (value <= 30) return 20;
        if (value <= 50) return 40;
        if (value <= 70) return 60;
        if (value <= 90) return 80;
        return 100;
    }

    private void updateVolumeStyle() {
        if (volumePresetBox == null) return;
        for (Node n : volumePresetBox.getChildren()) {
            if (n instanceof Button b) {
                int val = (int) b.getUserData();
                if (val == currentVolumeValue) {
                    b.setStyle(PREMIUM_TOGGLE_ACTIVE);
                    b.setOnMouseEntered(null);
                    b.setOnMouseExited(null);
                } else {
                    b.setStyle(PREMIUM_TOGGLE_INACTIVE);
                    b.setOnMouseEntered(e -> b.setStyle(PREMIUM_TOGGLE_INACTIVE + "-fx-background-color: " + TR_WHITE_08 + ";"));
                    b.setOnMouseExited(e -> b.setStyle(PREMIUM_TOGGLE_INACTIVE));
                }
            }
        }
    }
}
