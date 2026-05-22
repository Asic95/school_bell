package com.schoolbell.ui;

import com.schoolbell.model.Announcement;
import com.schoolbell.service.AnnouncementService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIStyles.*;

import javafx.stage.StageStyle;

public class AnnouncementEditorDialog {
    private final AnnouncementService announcementService;
    private final Runnable onSave;

    public AnnouncementEditorDialog(AnnouncementService announcementService, Runnable onSave) {
        this.announcementService = announcementService;
        this.onSave = onSave;
    }

    public void show(Announcement a) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(28);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD + "-fx-background-radius: 32; -fx-border-radius: 32; -fx-border-width: 2; -fx-border-color: #e2e8f0;");
        root.setPrefWidth(650);

        // --- SIMPLIFIED HEADER (NO ICON BOX) ---
        VBox headerText = new VBox(2);
        Label eb = new Label((a == null ? "СТВОРЕННЯ" : "РЕДАГУВАННЯ").toUpperCase());
        eb.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 1.2px;");
        Label t = new Label("Параметри оголошення");
        t.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label s = new Label("Налаштуйте текст та розклад відображення повідомлення.");
        s.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: #64748b;");
        headerText.getChildren().addAll(eb, t, s);

        TextArea textArea = new TextArea(a != null ? a.text() : "");
        textArea.setPromptText("Введіть текст оголошення тут...");
        textArea.setPrefRowCount(4);
        textArea.setWrapText(true);
        
        String textAreaBaseStyle = 
            "-fx-font-family: 'Inter'; -fx-font-size: 15px; " +
            "-fx-control-inner-background: white; " +
            "-fx-background-color: white; " +
            "-fx-background-radius: 14; " +
            "-fx-border-color: #e2e8f0; " +
            "-fx-border-radius: 14; " +
            "-fx-padding: 8; " +
            "-fx-text-box-border: transparent; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;";
            
        textArea.setStyle(textAreaBaseStyle);
        
        textArea.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                textArea.setStyle(textAreaBaseStyle + "-fx-border-color: #4f46e5; -fx-control-inner-background: #f8faff; -fx-background-color: #f8faff;");
            } else {
                textArea.setStyle(textAreaBaseStyle);
            }
        });

        // Period Section
        DatePicker startPicker = new DatePicker(a != null ? a.startDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(a != null ? a.endDate() : LocalDate.now().plusWeeks(1));
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);

        HBox dateRow = new HBox(15, 
            createLabeledField("ДАТА ПОЧАТКУ", startPicker),
            createLabeledField("ДАТА ЗАВЕРШЕННЯ", endPicker)
        );

        // Time Section
        LocalTime st = a != null && a.startTime() != null ? a.startTime() : LocalTime.of(8, 0);
        LocalTime et = a != null && a.endTime() != null ? et = a.endTime() : LocalTime.of(18, 0);
        
        ComboBox<String> startH = createTimeCombo(24, st.getHour());
        ComboBox<String> startM = createTimeCombo(60, st.getMinute());
        ComboBox<String> endH = createTimeCombo(24, et.getHour());
        ComboBox<String> endM = createTimeCombo(60, et.getMinute());

        Label startHL = new Label("год.");
        Label startML = new Label("хв.");
        Label endHL = new Label("год.");
        Label endML = new Label("хв.");
        
        String labelStyle = "-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: normal;";
        startHL.setStyle(labelStyle); startML.setStyle(labelStyle);
        endHL.setStyle(labelStyle); endML.setStyle(labelStyle);

        HBox startTimeBox = new HBox(8, startH, startHL, startM, startML);
        startTimeBox.setAlignment(Pos.CENTER_LEFT);
        HBox endTimeBox = new HBox(8, endH, endHL, endM, endML);
        endTimeBox.setAlignment(Pos.CENTER_LEFT);

        HBox timeRow = new HBox(25, 
            createLabeledField("ЧАС ПОЧАТКУ", startTimeBox),
            createLabeledField("ЧАС ЗАВЕРШЕННЯ", endTimeBox)
        );

        // Days Section
        HBox daysBox = new HBox(18);
        daysBox.setAlignment(Pos.CENTER_LEFT);
        String[] dayNames = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "НД"};
        List<CheckBox> dayCbs = new ArrayList<>();
        List<String> activeDays = a != null && a.daysOfWeek() != null ? List.of(a.daysOfWeek().split(",")) : List.of("1","2","3","4","5");
        for (int i = 1; i <= 7; i++) {
            CheckBox cb = new CheckBox(dayNames[i-1]);
            cb.setSelected(activeDays.contains(String.valueOf(i)));
            dayCbs.add(cb);
            daysBox.getChildren().add(cb);
        }

        VBox daysSection = createLabeledField("ДНІ ТИЖНЯ", daysBox);

        // --- FLATTENED SCHEDULE SECTION (NO INNER BLOCK) ---
        VBox scheduleSettings = new VBox(22);
        Label scheduleHeader = new Label("РОЗКЛАД ПОКАЗУ");
        scheduleHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        scheduleSettings.getChildren().addAll(scheduleHeader, dateRow, timeRow, daysSection);

        CheckBox activeCb = new CheckBox("Це оголошення зараз активне");
        activeCb.setSelected(a == null || a.isActive());

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ОГОЛОШЕННЯ", ICON_SAVE);
        saveBtn.setOnAction(ev -> {
            String text = textArea.getText().trim();
            if (text.isEmpty()) {
                ToastService.showError("Текст оголошення не може бути порожнім");
                return;
            }

            String days = dayCbs.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> String.valueOf(dayCbs.indexOf(cb) + 1))
                    .collect(Collectors.joining(","));

            LocalTime startT = LocalTime.of(Integer.parseInt(startH.getValue()), Integer.parseInt(startM.getValue()));
            LocalTime endT = LocalTime.of(Integer.parseInt(endH.getValue()), Integer.parseInt(endM.getValue()));

            Announcement newA = new Announcement(
                    a != null ? a.id() : 0,
                    text,
                    startPicker.getValue(),
                    endPicker.getValue(),
                    startT, endT, days,
                    activeCb.isSelected()
            );

            if (a == null) announcementService.addAnnouncement(newA);
            else announcementService.updateAnnouncement(newA);

            onSave.run();
            stage.close();
            ToastService.showSuccess("Оголошення збережено успішно");
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(headerText, createLabeledField("ТЕКСТ ПОВІДОМЛЕННЯ", textArea), scheduleSettings, activeCb, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
            "data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"),
            "data:text/css," + MODERN_CHECKBOX_STYLE.replace(" ", "%20")
        );
        stage.setScene(scene);
        stage.showAndWait();
    }
}
