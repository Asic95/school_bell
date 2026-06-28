package com.schoolbell.ui.editor;

import com.schoolbell.model.DaySchedule;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.function.Consumer;

import static com.schoolbell.ui.ControlFactory.createTimeCombo;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

/**
 * A modular UI component representing a single lesson row in the Bells Editor.
 */
public class LessonRowComponent extends HBox {

    private final ComboBox<String> sh;
    private final ComboBox<String> sm;
    private final ComboBox<String> eh;
    private final ComboBox<String> em;
    private final TextField breakF;

    private final StackPane badge;
    private final Label numLabel;
    private final Label lessonTextLabel;

    private String currentTone;

    public LessonRowComponent(int index, String tone, DaySchedule.LessonInfo info, Runnable onSave, Consumer<LessonRowComponent> onDelete) {
        super(15);
        this.currentTone = tone;

        this.sh = createTimeCombo(24, info.start != null ? info.start.getHour() : 0);
        this.sm = createTimeCombo(60, info.start != null ? info.start.getMinute() : 0);
        this.eh = createTimeCombo(24, info.end != null ? info.end.getHour() : 0);
        this.em = createTimeCombo(60, info.end != null ? info.end.getMinute() : 0);
        this.breakF = new TextField(String.valueOf(info.breakAfterMinutes));

        // Listeners
        sh.setOnAction(e -> onSave.run());
        sm.setOnAction(e -> onSave.run());
        eh.setOnAction(e -> onSave.run());
        em.setOnAction(e -> onSave.run());
        breakF.focusedProperty().addListener((obs, ov, nv) -> { if (!nv) onSave.run(); });

        breakF.setPrefSize(90, 45);
        breakF.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 15px; -fx-padding: 0 12; -fx-alignment: CENTER;");

        // Badge Setup
        VBox badgeContent = new VBox(-2);
        badgeContent.setAlignment(Pos.CENTER);
        
        this.lessonTextLabel = new Label("УРОК");
        this.numLabel = new Label(String.format("%02d", index + 1));
        
        badgeContent.getChildren().addAll(lessonTextLabel, numLabel);

        this.badge = new StackPane(badgeContent);
        badge.setMinSize(74, 58);
        badge.setPrefSize(74, 58);
        badge.setMaxSize(74, 58);

        HBox lessonBox = new HBox(badge);
        lessonBox.setAlignment(Pos.CENTER_LEFT);
        lessonBox.setPrefWidth(90);
        lessonBox.setMinWidth(90);

        // --- UNIFIED EDITOR BLOCK ---
        Label dash = new Label("—");
        dash.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_PALE + "; -fx-padding: 15 0 0 0;");

        HBox timeRange = new HBox(12, 
            labeledTimeBox("ПОЧАТОК", sh, sm), 
            dash, 
            labeledTimeBox("КІНЕЦЬ", eh, em)
        );
        timeRange.setAlignment(Pos.CENTER_LEFT);

        // Vertical Separator
        javafx.scene.layout.Region vSep = new javafx.scene.layout.Region();
        vSep.setPrefSize(1, 35);
        vSep.setMinSize(1, 35);
        vSep.setStyle("-fx-background-color: " + COLOR_BORDER_SOFT + "; -fx-opacity: 0.6;");
        VBox vSepContainer = new VBox(vSep);
        vSepContainer.setAlignment(Pos.BOTTOM_CENTER);
        vSepContainer.setPadding(new Insets(0, 0, 5, 0));

        VBox breakBox = breakLabeledBox(breakF);

        HBox editorBlock = new HBox(30, timeRange, vSepContainer, breakBox);
        editorBlock.setAlignment(Pos.CENTER_LEFT);

        // Spacer to push delete button to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button delBtn = com.schoolbell.ui.CardFactory.createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
        delBtn.setOnAction(e -> onDelete.accept(this));

        // Row Assembly
        this.getChildren().addAll(lessonBox, editorBlock, spacer, delBtn);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(14, 20, 14, 20));
        
        applyBaseStyle();
        updateIndex(index, tone);

        // Hover Effects
        this.setOnMouseEntered(e -> this.setStyle(this.getStyle() + "-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-border-color: " + currentTone + "40;"));
        this.setOnMouseExited(e -> applyBaseStyle());
    }

    private void applyBaseStyle() {
        this.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1; -fx-border-radius: 18; -fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_03 + ", 8, 0, 0, 2);");
    }

    public void updateIndex(int index, String tone) {
        this.currentTone = tone;
        numLabel.setText(String.format("%02d", index + 1));
        numLabel.setStyle("-fx-text-fill: " + tone + "; -fx-font-weight: 900; -fx-font-size: 19px;");
        lessonTextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + tone + "; -fx-opacity: 0.8;");
        badge.setStyle("-fx-background-color: " + tone + "12; -fx-background-radius: 16; -fx-border-color: " + tone + "30; -fx-border-width: 1.5; -fx-border-radius: 16;");
    }

    public DaySchedule.LessonInfo getLessonInfo() {
        try {
            int h1 = Integer.parseInt(sh.getValue());
            int m1 = Integer.parseInt(sm.getValue());
            int h2 = Integer.parseInt(eh.getValue());
            int m2 = Integer.parseInt(em.getValue());
            int br = Integer.parseInt(breakF.getText().trim());
            return new DaySchedule.LessonInfo(LocalTime.of(h1, m1), LocalTime.of(h2, m2), br);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateTime(LocalTime start, LocalTime end) {
        if (start != null) {
            sh.setValue(String.format("%02d", start.getHour()));
            sm.setValue(String.format("%02d", start.getMinute()));
        }
        if (end != null) {
            eh.setValue(String.format("%02d", end.getHour()));
            em.setValue(String.format("%02d", end.getMinute()));
        }
    }

    private VBox labeledTimeBox(String labelText, ComboBox<String> h, ComboBox<String> m) {
        Label label = new Label(labelText);
        label.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        Label sep = new Label(":");
        sep.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE_PALE + ";");
        HBox box = new HBox(6, h, sep, m);
        box.setAlignment(Pos.CENTER_LEFT);
        return new VBox(5, label, box);
    }

    private VBox breakLabeledBox(TextField breakF) {
        Label label = new Label("ПЕРЕРВА");
        label.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        HBox row = new HBox(10, createSVGIcon(ICON_CLOCK, Color.web(COLOR_SLATE_LIGHT), 18), breakF, new Label("ХВ"));
        ((Label) row.getChildren().get(2)).setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + ";");
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(5, label, row);
    }
}
