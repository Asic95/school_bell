package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.MediaEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Premium dialog for quick media control and event preview.
 */
public class MediaQuickControlDialog extends BasePremiumDialog {
    private final MainApp mainApp;
    private final VBox currentTrackBox;
    private final VBox eventsContainer;
    private final Label currentTrackLabel;
    private final Button stopBtn;
    private javafx.animation.Timeline refreshTimeline;

    public MediaQuickControlDialog(MainApp mainApp) {
        super(mainApp.getStage(), "АУДІО", "УПРАВЛІННЯ МЕДІА",
                "Переглядайте заплановані події та керуйте поточним відтворенням аудіо.", "ЗРОЗУМІЛО", 600);
        this.mainApp = mainApp;

        // Hide redundant cancel button
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);
        saveBtn.setGraphic(UIComponents.createSVGIcon(ICON_CHECK, Color.WHITE, 18));

        // Setup periodic refresh
        setOnShown(e -> {
            refreshTimeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), ev -> updateState()));
            refreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            refreshTimeline.play();
        });
        setOnHidden(e -> {
            if (refreshTimeline != null) refreshTimeline.stop();
        });

        // Current Track Section
        currentTrackBox = new VBox(15);
        currentTrackBox.setPadding(new Insets(20));
        currentTrackBox.setStyle("-fx-background-color: " + COLOR_SURFACE_GLASS_START + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_PRIMARY + "; -fx-border-width: 1.5; -fx-border-radius: 22;");
        currentTrackBox.setAlignment(Pos.CENTER);

        Label playingTitle = new Label("ЗАРАЗ ВІДТВОРЮЄТЬСЯ");
        playingTitle.setStyle(HEADER_STYLE + "-fx-text-fill: " + COLOR_PRIMARY + ";");
        
        currentTrackLabel = new Label("ТИША");
        currentTrackLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        currentTrackLabel.setWrapText(true);
        currentTrackLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        stopBtn = ControlFactory.createSecondaryDialogButton("ЗУПИНИТИ");
        stopBtn.setGraphic(UIComponents.createSVGIcon(ICON_STOP, Color.web(COLOR_DANGER), 18));
        stopBtn.setStyle(stopBtn.getStyle() + "-fx-text-fill: " + COLOR_DANGER + "; -fx-border-color: " + COLOR_DANGER_LIGHT + ";");
        stopBtn.setOnAction(e -> {
            mainApp.getAudioService().stopAll();
            stopBtn.setDisable(true);
            currentTrackLabel.setText("ЗУПИНЯЄТЬСЯ...");
            currentTrackBox.setOpacity(0.8);
        });

        currentTrackBox.getChildren().addAll(playingTitle, currentTrackLabel, stopBtn);

        // Events List Section
        eventsContainer = new VBox(12);
        eventsContainer.setPadding(new Insets(20));
        eventsContainer.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1; -fx-border-radius: 22;");

        ScrollPane scroll = new ScrollPane(eventsContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setPrefHeight(350);

        content.setSpacing(20);
        content.getChildren().addAll(
                currentTrackBox,
                new Label("НАЙБЛИЖЧІ В ЕФІРІ"),
                eventsContainer
        );
        ((Label)content.getChildren().get(1)).setStyle(HEADER_STYLE);

        Button settingsBtn = ControlFactory.createSecondaryDialogButton("НАЛАШТУВАННЯ");
        settingsBtn.setGraphic(UIComponents.createSVGIcon(ICON_SETTINGS, Color.web(COLOR_SLATE), 18));
        settingsBtn.setGraphicTextGap(10);
        settingsBtn.setOnAction(e -> {
            close();
            mainApp.showNotifications();
        });
        addLeftFooterButton(settingsBtn);

        updateState();
    }

    private void updateState() {
        String track = mainApp.getAudioService().getCurrentPlayingTrack();
        if (track != null) {
            currentTrackLabel.setText(track.toUpperCase());
            stopBtn.setVisible(true);
            stopBtn.setManaged(true);
            currentTrackBox.setOpacity(1.0);
        } else {
            currentTrackLabel.setText("АУДІО НЕ ВІДТВОРЮЄТЬСЯ");
            stopBtn.setVisible(false);
            stopBtn.setManaged(false);
            currentTrackBox.setOpacity(0.6);
        }

        eventsContainer.getChildren().clear();
        List<MediaEvent> allEvents = mainApp.getMediaSchedulerService().getEvents();
        List<com.schoolbell.model.BellEntry> schedule = mainApp.getSchedule();
        LocalTime now = LocalTime.now();
        
        java.util.List<MediaEvent> todayPlan = new java.util.ArrayList<>();

        for (MediaEvent e : allEvents) {
            if (!e.isActive()) continue;

            if ("TIME".equals(e.type())) {
                int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue();
                if (e.daysOfWeek().contains(String.valueOf(dayOfWeek))) {
                    todayPlan.add(e);
                }
            } else if ("ONCE".equals(e.type())) {
                if (java.time.LocalDate.now().toString().equals(e.date())) {
                    todayPlan.add(e);
                }
            } else if ("BREAKS".equals(e.type()) && schedule != null) {
                for (int i = 0; i < schedule.size() - 1; i++) {
                    com.schoolbell.model.BellEntry curr = schedule.get(i);
                    com.schoolbell.model.BellEntry nxt = schedule.get(i + 1);
                    if (curr.type().startsWith("OUT") && nxt.type().startsWith("IN")) {
                        LocalTime trigger = calculateTrigger(e, curr.time(), nxt.time());
                        todayPlan.add(new MediaEvent(e.id(), e.name(), e.path(), e.type(), 
                                trigger.toString().substring(0, 5), e.daysOfWeek(), e.date(), 
                                e.isActive(), e.isFolder(), e.durationMinutes(), e.breakAnchor(), e.breakOffset()));
                    }
                }
            }
        }

        List<MediaEvent> futureEvents = todayPlan.stream()
                .filter(e -> LocalTime.parse(e.time()).isAfter(now))
                .sorted((e1, e2) -> e1.time().compareTo(e2.time()))
                .collect(Collectors.toList());

        if (futureEvents.isEmpty()) {
            VBox empty = ControlFactory.createEmptyState(ICON_INFO, "Черга порожня", "На сьогодні більше немає запланованих подій.");
            empty.setPadding(new Insets(20));
            eventsContainer.getChildren().add(empty);
        } else {
            int displayLimit = 4;
            List<MediaEvent> toShow = futureEvents.stream().limit(displayLimit).collect(Collectors.toList());
            
            for (MediaEvent e : toShow) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1; -fx-border-radius: 14;");
                
                Label time = new Label(e.time());
                time.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-min-width: 50;");
                
                VBox text = new VBox(1);
                Label name = new Label(e.name().toUpperCase());
                name.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_NAVY + ";");
                Label typeTag = new Label("BREAKS".equals(e.type()) ? "НА ПЕРЕРВІ" : (e.isFolder() ? "ПАПКА" : "ФАЙЛ"));
                typeTag.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 0.5px;");
                text.getChildren().addAll(name, typeTag);
                
                row.getChildren().addAll(time, text);
                eventsContainer.getChildren().add(row);
            }
            
            if (futureEvents.size() > displayLimit) {
                Label moreLabel = new Label("ЩЕ " + (futureEvents.size() - displayLimit) + " ПОДІЙ ЗАПЛАНОВАНО НА СЬОГОДНІ");
                moreLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 0.5px;");
                moreLabel.setPadding(new Insets(5, 0, 0, 5));
                eventsContainer.getChildren().add(moreLabel);
            }
        }
    }

    private LocalTime calculateTrigger(MediaEvent event, LocalTime breakStart, LocalTime breakEnd) {
        String anchor = event.breakAnchor() != null ? event.breakAnchor() : "START";
        int offset = event.breakOffset();
        long durationSec = java.time.Duration.between(breakStart, breakEnd).getSeconds();

        return switch (anchor) {
            case "START" -> breakStart;
            case "END" -> breakEnd.minusMinutes(offset > 0 ? offset : 2);
            case "MIDDLE" -> breakStart.plusSeconds(durationSec / 2);
            case "OFFSET" -> breakStart.plusMinutes(offset);
            default -> breakStart;
        };
    }

    @Override
    protected boolean onSave() {
        return true;
    }
}
