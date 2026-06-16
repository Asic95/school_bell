package com.schoolbell.ui;

import com.schoolbell.model.Announcement;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.format.DateTimeFormatter;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createChip;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class AnnouncementCard extends VBox {
    private final Announcement announcement;
    private final Runnable onDelete;
    private final Runnable onEdit;

    public AnnouncementCard(Announcement a, Runnable onEdit, Runnable onDelete) {
        this.announcement = a;
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setPadding(new Insets(25));
        setStyle("-fx-background-color: white; -fx-background-radius: 28; -fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_04 + ", 15, 0, 0, 5);");

        HBox top = new HBox(20);
        top.setAlignment(Pos.TOP_LEFT);

        VBox iconBox = new VBox(createSVGIcon(ICON_BROADCAST, Color.web(COLOR_INDIGO_SOFT), 28));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setMinSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + COLOR_INDIGO_SOFT + "15; -fx-background-radius: 20;");

        VBox info = new VBox(12);
        info.setMinWidth(0); // Allow text wrapping to shrink the block
        Label text = new Label(a.text());
        text.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_TEXT + ";");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        text.setMinWidth(0); // Crucial for text wrapping in HBox

        javafx.scene.layout.FlowPane meta = new javafx.scene.layout.FlowPane();
        meta.setHgap(12);
        meta.setVgap(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getChildren().addAll(
            createChip(a.startDate().format(DateTimeFormatter.ofPattern("dd.MM")) + " - " + a.endDate().format(DateTimeFormatter.ofPattern("dd.MM")), COLOR_PRIMARY, ICON_CALENDAR),
            createChip(a.startTime() + " - " + a.endTime(), COLOR_SUCCESS, ICON_CLOCK),
            createChip(getDaysShortText(a.daysOfWeek()), COLOR_ORANGE, ICON_CALENDAR)
        );

        java.time.LocalDate today = java.time.LocalDate.now();
        boolean isExpired = a.endDate() != null && a.endDate().isBefore(today);
        
        if (!a.isActive()) {
            meta.getChildren().add(createChip("ВИМКНЕНО", COLOR_SLATE, ICON_INFO));
            this.setOpacity(0.7);
        } else if (isExpired) {
            meta.getChildren().add(createChip("ЗАВЕРШЕНО", COLOR_DANGER, ICON_INFO));
            this.setOpacity(0.85);
        }

        info.getChildren().addAll(text, meta);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.TOP_RIGHT);

        Button editBtn = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
        editBtn.setOnAction(e -> onEdit.run());

        Button delBtn = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
        delBtn.setOnAction(e -> onDelete.run());

        actions.getChildren().addAll(editBtn, delBtn);

        top.getChildren().addAll(iconBox, info, actions);
        getChildren().add(top);
    }

    private String getDaysShortText(String days) {
        if (days == null || days.isEmpty()) return "";
        String[] parts = days.split(",");
        String[] dayNames = {"", "ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "НД"};
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            try {
                int d = Integer.parseInt(p.trim());
                if (d >= 1 && d <= 7) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(dayNames[d]);
                }
            } catch (Exception ignored) {}
        }
        return sb.toString();
    }
}
