package com.schoolbell.ui;

import com.schoolbell.model.DaySchedule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.List;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class DashboardNextEventCard extends VBox {
    private final Label nextLessonNumLabel;
    private final Label nextLessonStatusBadge;
    private final Label nextLessonTimeLabel;
    private final Label nextLessonSubjectLabel;

    public DashboardNextEventCard() {
        super(25);
        setPadding(new Insets(30));
        setStyle(SOFT_CARD);

        Label nextHeader = new Label("НАСТУПНА ПОДІЯ");
        nextHeader.setStyle(HEADER_STYLE);

        VBox nextContent = new VBox(15);
        nextContent.setAlignment(Pos.CENTER);

        VBox nextIconCircle = new VBox(createSVGIcon(ICON_CLOCK, Color.web(COLOR_SUCCESS), 40));
        nextIconCircle.setAlignment(Pos.CENTER);
        nextIconCircle.setPrefSize(74, 74);
        nextIconCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_GLASS_END + "); -fx-background-radius: 20;");

        nextLessonNumLabel = new Label("--");
        nextLessonNumLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");

        VBox nextInfoText = new VBox(4);
        nextInfoText.setAlignment(Pos.CENTER);
        nextLessonSubjectLabel = new Label("Очікування...");
        nextLessonSubjectLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        nextLessonTimeLabel = new Label("--:-- – --:--");
        nextLessonTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_SLATE + ";");
        nextInfoText.getChildren().addAll(nextLessonSubjectLabel, nextLessonTimeLabel);

        nextLessonStatusBadge = new Label("ПЕРЕРВА");
        nextLessonStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 5 15; -fx-background-radius: 20;");

        nextContent.getChildren().addAll(nextIconCircle, nextLessonNumLabel, nextInfoText, nextLessonStatusBadge);

        getChildren().addAll(nextHeader, nextContent);
    }

    public void update(LocalTime now, DaySchedule activeDs) {
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
            safeSetText(nextLessonNumLabel, "1 УРОК");
            safeSetText(nextLessonStatusBadge, "ПОЧАТОК ДНЯ");
            safeSetText(nextLessonTimeLabel, firstLi.start + " — " + firstLi.end);
            safeSetText(nextLessonSubjectLabel, "Перше заняття сьогодні");
            found = true;
        } else if (curLessonIdx != -1) {
            if (!isBreak) {
                if (curLessonIdx < activeDs.getLessons().size() - 1) {
                    DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                    safeSetText(nextLessonNumLabel, (curLessonIdx + 2) + " УРОК");
                    safeSetText(nextLessonStatusBadge, "НАСТУПНИЙ");
                    safeSetText(nextLessonTimeLabel, nextLi.start + " — " + nextLi.end);
                    safeSetText(nextLessonSubjectLabel, "Після перерви");
                } else {
                    safeSetText(nextLessonNumLabel, "--");
                    safeSetText(nextLessonStatusBadge, "КІНЕЦЬ");
                    safeSetText(nextLessonSubjectLabel, "Занять більше немає");
                }
                found = true;
            } else {
                DaySchedule.LessonInfo nextLi = activeDs.getLessons().get(curLessonIdx + 1);
                safeSetText(nextLessonNumLabel, (curLessonIdx + 2) + " УРОК");
                safeSetText(nextLessonStatusBadge, "ГОТУЙТЕСЯ");
                safeSetText(nextLessonTimeLabel, nextLi.start + " — " + nextLi.end);
                safeSetText(nextLessonSubjectLabel, "Наступний урок");
                found = true;
            }
        }

        if (!found) {
            safeSetText(nextLessonNumLabel, "--");
            safeSetText(nextLessonStatusBadge, "КІНЕЦЬ");
            safeSetText(nextLessonSubjectLabel, "До завтра!");
        }
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }
}
