package com.schoolbell.ui;

import com.schoolbell.model.DaySchedule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.List;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardHeroCard extends VBox {
    private static final String MODERN_PROGRESS_STYLE =
            ".progress-bar { -fx-background-color: transparent; -fx-padding: 0; -fx-background-insets: 0; } " +
            ".progress-bar > .track { -fx-background-color: " + COLOR_SURFACE_SUBTLE + "; -fx-background-radius: 10; -fx-background-insets: 0; } " +
            ".progress-bar > .bar { -fx-background-color: linear-gradient(to right, " + COLOR_PRIMARY + ", " + COLOR_PURPLE + "); -fx-background-radius: 10; -fx-background-insets: 0; }";

    private final Label topActiveScheduleLabel;
    private final Label curLessonNumLabel;
    private final Label curLessonStatusBadge;
    private final Label curLessonSubjectLabel;
    private final Label curLessonTimeLabel;
    private final ProgressBar curLessonProgress;
    private final Label curLessonProgressText;

    private final Label countdownLabel;
    private final Label nextBellTypeLabel;

    public DashboardHeroCard(String initialScheduleName) {
        super(25);
        setPadding(new Insets(30));
        setStyle(SOFT_CARD);

        HBox heroHeader = new HBox(15);
        heroHeader.setAlignment(Pos.CENTER_LEFT);
        Label flowHeader = new Label("ПОТОЧНИЙ СТАН ТА РОЗКЛАД");
        flowHeader.setStyle(HEADER_STYLE);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        topActiveScheduleLabel = new Label(initialScheduleName != null ? initialScheduleName : "Не вибрано");
        topActiveScheduleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-padding: 6 14; -fx-background-radius: 20;");
        heroHeader.getChildren().addAll(flowHeader, headerSpacer, topActiveScheduleLabel);

        HBox currentSessionInfo = new HBox(30);
        currentSessionInfo.setAlignment(Pos.CENTER_LEFT);

        VBox sessionIcon = new VBox(createSVGIcon(ICON_BOOK, Color.web(COLOR_PRIMARY), 48));
        sessionIcon.setAlignment(Pos.CENTER);
        sessionIcon.setPrefSize(84, 84);
        sessionIcon.setStyle(ICON_BADGE_STYLE);

        VBox sessionText = new VBox(6);
        curLessonNumLabel = new Label("--");
        curLessonNumLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        curLessonStatusBadge = new Label("ОЧІКУВАННЯ");
        curLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 4 12; -fx-background-radius: 12;");
        HBox titleRow = new HBox(15, curLessonNumLabel, curLessonStatusBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        curLessonSubjectLabel = new Label("Завантаження даних...");
        curLessonSubjectLabel.setWrapText(true);
        curLessonSubjectLabel.setMaxWidth(400);
        curLessonSubjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_SLATE + ";");
        sessionText.getChildren().addAll(titleRow, curLessonSubjectLabel);

        Region sessionSpacer = new Region();
        HBox.setHgrow(sessionSpacer, Priority.ALWAYS);

        VBox countdownBox = new VBox(2);
        countdownBox.setAlignment(Pos.CENTER_RIGHT);
        Label countdownTitle = new Label("ДО НАСТУПНОГО ДЗВІНКА");
        countdownTitle.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        countdownLabel = new Label("00:00:00");
        countdownLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        nextBellTypeLabel = new Label("—");
        nextBellTypeLabel.setWrapText(true);
        nextBellTypeLabel.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        nextBellTypeLabel.setMaxWidth(220);
        nextBellTypeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE_LIGHT + ";");
        countdownBox.getChildren().addAll(countdownTitle, countdownLabel, nextBellTypeLabel);

        currentSessionInfo.getChildren().addAll(sessionIcon, sessionText, sessionSpacer, countdownBox);

        VBox progressArea = new VBox(12);
        curLessonProgress = new ProgressBar(0);
        curLessonProgress.setMaxWidth(Double.MAX_VALUE);
        curLessonProgress.setPrefHeight(20);
        curLessonProgress.getStylesheets().add("data:text/css," + MODERN_PROGRESS_STYLE.replace(" ", "%20"));

        HBox progressLabels = new HBox();
        curLessonTimeLabel = new Label("--:-- – --:--");
        curLessonTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + ";");
        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);
        curLessonProgressText = new Label("0%");
        curLessonProgressText.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        progressLabels.getChildren().addAll(curLessonTimeLabel, pSpacer, curLessonProgressText);
        progressArea.getChildren().addAll(progressLabels, curLessonProgress);

        getChildren().addAll(heroHeader, currentSessionInfo, progressArea);
    }

    public void update(LocalTime now, DaySchedule activeDs, String countdown, String nextBellType) {
        safeSetText(countdownLabel, countdown);
        safeSetText(nextBellTypeLabel, nextBellType);

        if (activeDs == null) return;
        List<DaySchedule.LessonInfo> lessons = activeDs.getLessons();
        if (lessons.isEmpty()) return;

        int curLessonIdx = -1;
        boolean isBreak = false;
        boolean isBeforeDay = false;

        if (now.isBefore(lessons.get(0).start)) {
            isBeforeDay = true;
        } else {
            for (int i = 0; i < lessons.size(); i++) {
                DaySchedule.LessonInfo li = lessons.get(i);
                if (!now.isBefore(li.start) && now.isBefore(li.end)) {
                    curLessonIdx = i; isBreak = false; break;
                } else if (i < lessons.size() - 1) {
                    DaySchedule.LessonInfo nextLi = lessons.get(i + 1);
                    if (!now.isBefore(li.end) && now.isBefore(nextLi.start)) {
                        curLessonIdx = i; isBreak = true; break;
                    }
                }
            }
        }

        boolean found = false;
        if (isBeforeDay) {
            DaySchedule.LessonInfo firstLi = lessons.get(0);
            safeSetText(curLessonNumLabel, "ПЕРЕД ЗАЙНЯТТЯМИ");
            safeSetText(curLessonStatusBadge, "ОЧІКУВАННЯ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            safeSetText(curLessonTimeLabel, "Початок о " + firstLi.start);
            safeSetText(curLessonSubjectLabel, "Система готова до початку дня");
            safeSetProgress(curLessonProgress, 0);
            safeSetText(curLessonProgressText, "0%");
            found = true;
        } else if (curLessonIdx != -1) {
            DaySchedule.LessonInfo li = activeDs.getLessons().get(curLessonIdx);
            if (!isBreak) {
                safeSetText(curLessonNumLabel, (curLessonIdx + 1) + " УРОК");
                safeSetText(curLessonStatusBadge, "ТРИВАЄ");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_PRIMARY + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                safeSetText(curLessonTimeLabel, li.start + " — " + li.end);
                safeSetText(curLessonSubjectLabel, "Йде навчальний процес");

                long total = java.time.Duration.between(li.start, li.end).toSeconds();
                long elapsed = java.time.Duration.between(li.start, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                safeSetProgress(curLessonProgress, p);
                safeSetText(curLessonProgressText, (int)(p * 100) + "% ЗАВЕРШЕНО");
                found = true;
            } else {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                safeSetText(curLessonNumLabel, "ПЕРЕРВА");
                safeSetText(curLessonStatusBadge, "ВІДПОЧИНОК");
                curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_WARNING + "; -fx-padding: 3 10; -fx-background-radius: 6;");
                safeSetText(curLessonTimeLabel, li.end + " — " + nextLi.start);
                safeSetText(curLessonSubjectLabel, "Час для відпочинку та підготовки");

                long total = java.time.Duration.between(li.end, nextLi.start).toSeconds();
                long elapsed = java.time.Duration.between(li.end, now).toSeconds();
                double p = Math.max(0, Math.min(1.0, (double) elapsed / total));
                safeSetProgress(curLessonProgress, p);
                safeSetText(curLessonProgressText, (int)(p * 100) + "% МИНУЛО");
                found = true;
            }
        }

        if (!found) {
            safeSetText(curLessonNumLabel, "--");
            safeSetText(curLessonStatusBadge, "КІНЕЦЬ");
            curLessonStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_NEUTRAL + "; -fx-padding: 3 10; -fx-background-radius: 6;");
            safeSetText(curLessonSubjectLabel, "Навчальний день завершено");
            safeSetProgress(curLessonProgress, 0);
            safeSetText(curLessonProgressText, "0%");
        }
    }

    public void refreshScheduleName(String name) {
        safeSetText(topActiveScheduleLabel, name != null ? name : "Не вибрано");
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }

    private void safeSetProgress(ProgressBar bar, double progress) {
        if (bar == null) return;
        if (Math.abs(bar.getProgress() - progress) > 0.001) {
            bar.setProgress(progress);
        }
    }
}
