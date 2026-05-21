package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.ScheduleEditorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.LayoutUtils.createAvatar;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class SubstitutionCard {
    private final MainApp mainApp;
    private final ScheduleEditorDialog parentDialog;
    private final Runnable refreshSubstitutions;

    public SubstitutionCard(MainApp mainApp, ScheduleEditorDialog parentDialog, Runnable refreshSubstitutions) {
        this.mainApp = mainApp;
        this.parentDialog = parentDialog;
        this.refreshSubstitutions = refreshSubstitutions;
    }

    public Node build(SubstitutionEntry sub) {
        HBox card = new HBox(25);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle(SOFT_CARD + "-fx-border-color: #f1f2f6; -fx-border-radius: 24;");

        // Lesson & Class Badge Group
        VBox lessonAndClass = new VBox(10);
        lessonAndClass.setAlignment(Pos.CENTER);
        
        VBox lessonBox = new VBox();
        lessonBox.setAlignment(Pos.CENTER);
        lessonBox.setPrefSize(50, 50);
        lessonBox.setStyle("-fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-background-radius: 14; -fx-effect: dropshadow(three-pass-box, rgba(9,132,227,0.1), 5, 0, 0, 2);");
        Label lessonNum = new Label(String.valueOf(sub.lessonNumber()));
        lessonNum.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        Label lessonText = new Label("УРОК");
        lessonText.setStyle("-fx-font-size: 8px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 0.5px;");
        lessonBox.getChildren().addAll(lessonNum, lessonText);

        SchoolClass cls = mainApp.getAcademicService().getAllClasses().stream().filter(c -> c.id() == sub.classId()).findFirst().orElse(null);
        Label classBadge = new Label(cls != null ? cls.name() : "?");
        classBadge.setStyle("-fx-background-color: " + COLOR_PURPLE_LIGHT + "; -fx-text-fill: " + COLOR_PURPLE + "; -fx-padding: 4 12; -fx-background-radius: 10; -fx-font-weight: 900; -fx-font-size: 11px; -fx-border-color: " + COLOR_PURPLE + "20; -fx-border-radius: 10;");
        
        lessonAndClass.getChildren().addAll(lessonBox, classBadge);

        // Find Original Teacher and Subject from Schedule
        int dayOfWeek = sub.date().getDayOfWeek().getValue();
        List<ScheduleEntry> schedule = mainApp.getAcademicService().getScheduleForClass(sub.classId());
        ScheduleEntry original = schedule.stream()
                .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == sub.lessonNumber())
                .findFirst().orElse(null);

        HBox mainContent = new HBox(30);
        mainContent.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // --- ORIGINAL BLOCK ---
        VBox fromBox = new VBox(8);
        fromBox.setAlignment(Pos.CENTER_LEFT);
        Label fromLabel = new Label("БУЛО (ЗА РОЗКЛАДОМ)");
        fromLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 0.5px;");
        
        HBox fromInfo = new HBox(12);
        fromInfo.setAlignment(Pos.CENTER_LEFT);
        if (original != null) {
            Teacher ot = mainApp.getStaffService().getAllTeachers().stream().filter(t -> t.id() == original.teacherId()).findFirst().orElse(null);
            Subject os = mainApp.getStaffService().getAllSubjects().stream().filter(s -> s.id() == original.subjectId()).findFirst().orElse(null);
            
            VBox otStack = new VBox(2);
            Label otName = new Label(ot != null ? ot.name() : "Невідомий");
            otName.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: #636e72;");
            Label osName = new Label(os != null ? os.name().toUpperCase() : "?");
            osName.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #95a5a6;");
            otStack.getChildren().addAll(otName, osName);
            
            fromInfo.getChildren().addAll(createAvatar(ot != null ? ot.name() : "?", 36), otStack);
        } else {
            Label noOrig = new Label("ВІЛЬНИЙ УРОК");
            noOrig.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-opacity: 0.5;");
            fromInfo.getChildren().add(noOrig);
        }
        fromBox.getChildren().addAll(fromLabel, fromInfo);

        // --- DECORATIVE TRANSITION ---
        VBox transitionBox = new VBox();
        transitionBox.setAlignment(Pos.CENTER);
        Node arrow = createSVGIcon("M14,16.94V12.94H5.08L5.05,10.93H14V6.94L19,11.94L14,16.94Z", Color.web("#e67e22"), 24);
        arrow.setStyle("-fx-effect: dropshadow(three-pass-box, #e67e2230, 10, 0, 0, 0);");
        transitionBox.getChildren().add(arrow);

        // --- REPLACEMENT BLOCK ---
        VBox toBox = new VBox(8);
        toBox.setAlignment(Pos.CENTER_LEFT);
        Label toLabel = new Label("СТАЛО (ЗАМІНА)");
        toLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #e67e22; -fx-letter-spacing: 0.5px;");
        
        HBox toInfo = new HBox(12);
        toInfo.setAlignment(Pos.CENTER_LEFT);
        Teacher nt = mainApp.getStaffService().getAllTeachers().stream().filter(t -> t.id() == sub.teacherId()).findFirst().orElse(null);
        Subject ns = mainApp.getStaffService().getAllSubjects().stream().filter(s -> s.id() == sub.subjectId()).findFirst().orElse(null);
        
        VBox ntStack = new VBox(2);
        Label ntName = new Label(nt != null ? nt.name() : "Немає вчителя");
        ntName.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label nsName = new Label(ns != null ? ns.name().toUpperCase() : "ЗАМІНА");
        nsName.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #e67e22;");
        ntStack.getChildren().addAll(ntName, nsName);

        // Add classroom badge
        if (sub.classroomId() > 0) {
            String roomName = mainApp.getClassroomName(sub.classroomId());
            Label roomBadge = new Label(roomName);
            roomBadge.setGraphic(createSVGIcon(ICON_ROOM, Color.WHITE, 10));
            roomBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #e67e22; -fx-background-radius: 6; -fx-padding: 2 8; -fx-font-weight: 900;");
            HBox roomWrapper = new HBox(roomBadge);
            roomWrapper.setPadding(new Insets(2, 0, 0, 0));
            ntStack.getChildren().add(roomWrapper);
        }
        
        toInfo.getChildren().addAll(createAvatar(nt != null ? nt.name() : "?", 36), ntStack);
        toBox.getChildren().addAll(toLabel, toInfo);

        mainContent.getChildren().addAll(fromBox, transitionBox, toBox);

        // Action Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
        editBtn.setOnAction(e -> new com.schoolbell.ui.SubstitutionEditorDialog(mainApp, sub, sub.date(), refreshSubstitutions).show());

        Button delBtn = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
        delBtn.setOnAction(e -> {
            mainApp.getAcademicService().deleteSubstitution(sub.id());
            refreshSubstitutions.run();
        });

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(lessonAndClass, mainContent, actions);
        return card;
    }
}
