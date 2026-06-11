package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardInfoRow extends StackPane {
    private final MainApp mainApp;
    private final ConfigService config;

    private Label activeScheduleValue;
    private Label volPercentLabel;
    private HBox volumeSegmentBox;
    private Label brStatusLabel;
    private Label mediaValue;

    private int currentVolumeValue;

    public DashboardInfoRow(MainApp mainApp) {
        this.mainApp = mainApp;
        this.config = mainApp.getConfigService();
        this.currentVolumeValue = normalizeVolume(config.getSystemVolume());

        Region bg = new Region();
        bg.setStyle(SOFT_CARD);
        
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(20, 20, 20, 20));

        HBox schSection = createScheduleSection();
        HBox volSection = createVolumeSection();
        HBox brSection = createBroadcastSection();
        HBox mediaSection = createMediaSection();

        bar.getChildren().addAll(
            schSection, createDivider(),
            volSection, createDivider(),
            brSection, createDivider(),
            mediaSection
        );

        getChildren().addAll(bg, bar);
    }

    private HBox createScheduleSection() {
        activeScheduleValue = new Label(config.getSelectedScheduleName() != null ? config.getSelectedScheduleName() : "Не вибрано");
        activeScheduleValue.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");

        Button changeBtn = createActionButton("Змінити", COLOR_PRIMARY, () -> new ScheduleQuickSelectorDialog(mainApp).display());

        VBox details = new VBox(10, activeScheduleValue, changeBtn);
        details.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(14,
                createSectionTitle("Активний розклад"),
                createSectionBody(ICON_CALENDAR, COLOR_PRIMARY, details)
        );
        return createSectionWrapper(texts, COLOR_PRIMARY);
    }

    private HBox createVolumeSection() {
        volPercentLabel = new Label(currentVolumeValue + "%");
        volPercentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE + ";");

        volumeSegmentBox = new HBox(4);
        volumeSegmentBox.setAlignment(Pos.CENTER_LEFT);
        
        int[] presets = {0, 20, 40, 60, 80, 100};
        for (int p : presets) {
            Region segment = new Region();
            segment.setPrefSize(30, 11);
            segment.setUserData(p);
            segment.setCursor(javafx.scene.Cursor.HAND);
            segment.setOnMouseClicked(e -> {
                currentVolumeValue = p;
                safeSetText(volPercentLabel, p + "%");
                updateVolumeStyle();
                config.setSystemVolume(p);
                mainApp.getSystemService().setWindowsSystemVolume(p);
                mainApp.saveConfig();
            });
            volumeSegmentBox.getChildren().add(segment);
        }
        updateVolumeStyle();

        HBox volSegmentsWithLabel = new HBox(12, volumeSegmentBox, volPercentLabel);
        volSegmentsWithLabel.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(volSegmentsWithLabel);
        details.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(14,
                createSectionTitle("Гучність системи"),
                createSectionBody(ICON_VOLUME, COLOR_SUCCESS, details)
        );
        return createSectionWrapper(content, COLOR_SUCCESS);
    }

    private HBox createBroadcastSection() {
        brStatusLabel = new Label(config.isBroadcastEnabled() ? "Активно" : "Вимкнено");
        brStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + (config.isBroadcastEnabled() ? COLOR_SUCCESS : COLOR_DANGER) + ";");

        Button openBtn = createActionButton("Відкрити", COLOR_INDIGO_SOFT, () -> mainApp.getHostServices().showDocument("http://localhost:" + config.getBroadcastPort()));

        VBox details = new VBox(10, brStatusLabel, openBtn);
        details.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(14,
                createSectionTitle("Трансляція"),
                createSectionBody(ICON_MONITOR, COLOR_INDIGO_SOFT, details)
        );
        return createSectionWrapper(texts, COLOR_INDIGO_SOFT);
    }

    private HBox createMediaSection() {
        mediaValue = new Label("Очікування...");
        mediaValue.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        mediaValue.setWrapText(false);
        mediaValue.setEllipsisString("...");
        mediaValue.setMaxWidth(260);

        Button manageBtn = createActionButton("Керувати", COLOR_TANGERINE, () -> new MediaQuickControlDialog(mainApp).display());

        VBox details = new VBox(10, mediaValue, manageBtn);
        details.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(14,
                createSectionTitle("Медіа-ефір"),
                createSectionBody(ICON_AIRPLAY, COLOR_TANGERINE, details)
        );
        updateMedia();

        return createSectionWrapper(texts, COLOR_TANGERINE);
    }

    private HBox createSectionWrapper(Node content, String accentColor) {
        HBox wrapper = new HBox(content);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(10, 22, 10, 22));
        wrapper.setStyle("-fx-background-radius: 20;");
        
        wrapper.setOnMouseEntered(e -> wrapper.setStyle("-fx-background-color: " + accentColor + "08; -fx-background-radius: 20;"));
        wrapper.setOnMouseExited(e -> wrapper.setStyle("-fx-background-color: transparent; -fx-background-radius: 20;"));
        
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private Region createDivider() {
        Region d = new Region();
        d.setPrefWidth(1);
        d.setMinHeight(92);
        d.setMaxHeight(92);
        d.setStyle("-fx-background-color: " + COLOR_BORDER_SOFT + ";");
        return d;
    }

    private Label createSectionTitle(String text) {
        Label title = new Label(text.toUpperCase());
        title.setStyle(HEADER_STYLE);
        return title;
    }

    private HBox createSectionBody(String icon, String color, Node content) {
        HBox body = new HBox(16, createIconBadge(icon, color), content);
        body.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
        return body;
    }

    private VBox createIconBadge(String icon, String color) {
        VBox badge = new VBox(createSVGIcon(icon, Color.web(color), 42));
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(72, 72);
        badge.setMinSize(72, 72);
        badge.setMaxSize(72, 72);
        badge.setStyle(ICON_BADGE_STYLE);
        return badge;
    }

    private Button createActionButton(String text, String accentColor, Runnable action) {
        Button b = new Button(text);
        String normalStyle = "-fx-background-color: " + accentColor + "12; "
                + "-fx-background-radius: 14; "
                + "-fx-border-color: " + accentColor + "35; "
                + "-fx-border-radius: 14; "
                + "-fx-padding: 6 13; "
                + "-fx-text-fill: " + accentColor + "; "
                + "-fx-font-size: 12px; "
                + "-fx-font-weight: 900; "
                + "-fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + accentColor + "; "
                + "-fx-background-radius: 14; "
                + "-fx-border-color: " + accentColor + "; "
                + "-fx-border-radius: 14; "
                + "-fx-padding: 6 13; "
                + "-fx-text-fill: white; "
                + "-fx-font-size: 12px; "
                + "-fx-font-weight: 900; "
                + "-fx-cursor: hand;";
        b.setStyle(normalStyle);
        b.setOnAction(e -> action.run());
        b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
        b.setOnMouseExited(e -> b.setStyle(normalStyle));
        return b;
    }

    private void updateVolumeStyle() {
        if (volumeSegmentBox == null) return;
        for (Node n : volumeSegmentBox.getChildren()) {
            if (n instanceof Region r) {
                int val = (int) r.getUserData();
                boolean isActive = val <= currentVolumeValue && currentVolumeValue > 0;
                if (val == 0 && currentVolumeValue == 0) isActive = true;

                String baseStyle = "-fx-background-radius: 4; ";
                if (isActive) {
                    r.setStyle(baseStyle + "-fx-background-color: " + COLOR_SUCCESS + ";");
                } else {
                    r.setStyle(baseStyle + "-fx-background-color: " + COLOR_BORDER_SOFT + ";");
                }
            }
        }
    }

    public void updateSchedule(String name) {
        safeSetText(activeScheduleValue, name != null ? name : "Не вибрано");
    }

    public void updateVolume(int volume) {
        currentVolumeValue = normalizeVolume(volume);
        safeSetText(volPercentLabel, currentVolumeValue + "%");
        updateVolumeStyle();
    }

    public void updateBroadcast() {
        boolean active = config.isBroadcastEnabled();
        safeSetText(brStatusLabel, active ? "Активно" : "Вимкнено");
        brStatusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + (active ? COLOR_SUCCESS : COLOR_DANGER) + ";");
    }

    public void updateMedia() {
        String currentTrack = mainApp.getAudioService().getCurrentPlayingTrack();
        if (currentTrack != null) {
            safeSetText(mediaValue, currentTrack);
            mediaValue.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TANGERINE + ";");
        } else {
            MediaEvent nextEvent = mainApp.getMediaSchedulerService().getNextEvent();
            if (nextEvent != null) {
                safeSetText(mediaValue, nextEvent.time() + " — " + nextEvent.name());
            } else {
                safeSetText(mediaValue, "Подій немає");
            }
            mediaValue.setStyle("-fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        }
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
}
