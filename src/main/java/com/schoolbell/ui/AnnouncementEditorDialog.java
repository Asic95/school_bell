package com.schoolbell.ui;

import com.schoolbell.model.Announcement;
import com.schoolbell.service.AnnouncementService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.ControlFactory.createDialogHeader;
import static com.schoolbell.ui.ControlFactory.createDialogRoot;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createSecondaryDialogButton;
import static com.schoolbell.ui.UIStyles.COLOR_INDIGO;
import static com.schoolbell.ui.UIStyles.COLOR_NAVY;
import static com.schoolbell.ui.UIStyles.COLOR_SLATE;
import static com.schoolbell.ui.UIStyles.COLOR_SURFACE_BRAND;
import static com.schoolbell.ui.UIStyles.HEADER_STYLE;
import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.MODERN_CHECKBOX_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_BTN_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_FIELD_STYLE;

public class AnnouncementEditorDialog {
    private final AnnouncementService announcementService;
    private final Runnable onSave;

    public AnnouncementEditorDialog(AnnouncementService announcementService, Runnable onSave) {
        this.announcementService = announcementService;
        this.onSave = onSave;
    }

    public void show(Announcement announcement) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = createDialogRoot(650);
        VBox headerText = createDialogHeader(
                announcement == null ? "Створення" : "Редагування",
                "Параметри оголошення",
                "Налаштуйте текст та розклад відображення повідомлення."
        );

        TextArea textArea = new TextArea(announcement != null ? announcement.text() : "");
        textArea.setPromptText("Введіть текст оголошення тут...");
        textArea.setPrefRowCount(4);
        textArea.setWrapText(true);

        String textAreaBaseStyle = PREMIUM_FIELD_STYLE + "-fx-padding: 0;";
        textArea.setStyle(textAreaBaseStyle);
        textArea.skinProperty().addListener((obs, oldSkin, newSkin) -> updateInternalStyles(textArea, false));
        textArea.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                textArea.setStyle(textAreaBaseStyle + "-fx-border-color: " + COLOR_INDIGO + "; -fx-background-color: " + COLOR_SURFACE_BRAND + ";");
                updateInternalStyles(textArea, true);
            } else {
                textArea.setStyle(textAreaBaseStyle);
                updateInternalStyles(textArea, false);
            }
        });

        DatePicker startPicker = new DatePicker(announcement != null ? announcement.startDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(announcement != null ? announcement.endDate() : LocalDate.now().plusWeeks(1));
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);

        HBox dateRow = new HBox(15,
                createLabeledField("ДАТА ПОЧАТКУ", startPicker),
                createLabeledField("ДАТА ЗАВЕРШЕННЯ", endPicker)
        );

        LocalTime startTime = announcement != null && announcement.startTime() != null
                ? announcement.startTime()
                : LocalTime.of(8, 0);
        LocalTime endTime = announcement != null && announcement.endTime() != null
                ? announcement.endTime()
                : LocalTime.of(18, 0);

        ComboBox<String> startHour = ControlFactory.createTimeCombo(24, startTime.getHour());
        ComboBox<String> startMinute = ControlFactory.createTimeCombo(60, startTime.getMinute());
        ComboBox<String> endHour = ControlFactory.createTimeCombo(24, endTime.getHour());
        ComboBox<String> endMinute = ControlFactory.createTimeCombo(60, endTime.getMinute());

        String timeUnitStyle = "-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: 800;";
        Label startHourLabel = new Label("год.");
        Label startMinuteLabel = new Label("хв.");
        Label endHourLabel = new Label("год.");
        Label endMinuteLabel = new Label("хв.");
        startHourLabel.setStyle(timeUnitStyle);
        startMinuteLabel.setStyle(timeUnitStyle);
        endHourLabel.setStyle(timeUnitStyle);
        endMinuteLabel.setStyle(timeUnitStyle);

        HBox startTimeBox = new HBox(8, startHour, startHourLabel, startMinute, startMinuteLabel);
        startTimeBox.setAlignment(Pos.CENTER_LEFT);
        HBox endTimeBox = new HBox(8, endHour, endHourLabel, endMinute, endMinuteLabel);
        endTimeBox.setAlignment(Pos.CENTER_LEFT);

        HBox timeRow = new HBox(25,
                createLabeledField("ЧАС ПОЧАТКУ", startTimeBox),
                createLabeledField("ЧАС ЗАВЕРШЕННЯ", endTimeBox)
        );

        HBox daysBox = new HBox(12);
        daysBox.setAlignment(Pos.CENTER_LEFT);
        String[] dayNames = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "НД"};

        List<CheckBox> dayCheckboxes = new ArrayList<>();
        List<String> activeDays = announcement != null && announcement.daysOfWeek() != null
                ? List.of(announcement.daysOfWeek().split(","))
                : List.of("1", "2", "3", "4", "5");

        for (int i = 1; i <= 7; i++) {
            CheckBox cb = new CheckBox(dayNames[i - 1]);
            cb.setSelected(activeDays.contains(String.valueOf(i)));
            dayCheckboxes.add(cb);
            daysBox.getChildren().add(cb);
        }

        VBox daysSection = createLabeledField("ДНІ ТИЖНЯ", daysBox);

        VBox scheduleSettings = new VBox(22);
        Label scheduleHeader = new Label("РОЗКЛАД ПОКАЗУ");
        scheduleHeader.setStyle(HEADER_STYLE);
        scheduleSettings.getChildren().addAll(scheduleHeader, dateRow, timeRow, daysSection);

        CheckBox activeCheckbox = new CheckBox("ЦЕ ОГОЛОШЕННЯ ЗАРАЗ АКТИВНЕ");
        activeCheckbox.setSelected(announcement == null || announcement.isActive());
        activeCheckbox.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + ";");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = createSecondaryDialogButton("СКАСУВАТИ");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ОГОЛОШЕННЯ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(ev -> {
            String text = textArea.getText().trim();
            if (text.isEmpty()) {
                ToastService.showError("Текст оголошення не може бути порожнім");
                return;
            }

            String days = dayCheckboxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> String.valueOf(dayCheckboxes.indexOf(cb) + 1))
                    .collect(Collectors.joining(","));

            LocalTime newStartTime = LocalTime.of(Integer.parseInt(startHour.getValue()), Integer.parseInt(startMinute.getValue()));
            LocalTime newEndTime = LocalTime.of(Integer.parseInt(endHour.getValue()), Integer.parseInt(endMinute.getValue()));

            Announcement updated = new Announcement(
                    announcement != null ? announcement.id() : 0,
                    text,
                    startPicker.getValue(),
                    endPicker.getValue(),
                    newStartTime,
                    newEndTime,
                    days,
                    activeCheckbox.isSelected()
            );

            if (announcement == null) {
                announcementService.addAnnouncement(updated);
            } else {
                announcementService.updateAnnouncement(updated);
            }

            onSave.run();
            stage.close();
            ToastService.showSuccess("Оголошення збережено успішно");
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(
                headerText,
                createLabeledField("ТЕКСТ ПОВІДОМЛЕННЯ", textArea),
                scheduleSettings,
                activeCheckbox,
                actions
        );

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
                "data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"),
                "data:text/css," + MODERN_CHECKBOX_STYLE.replace(" ", "%20")
        );
        stage.setScene(scene);
        textArea.applyCss();
        textArea.layout();
        stage.showAndWait();
    }

    private void updateInternalStyles(TextArea textArea, boolean focused) {
        String background = focused ? COLOR_SURFACE_BRAND : "white";
        Region content = (Region) textArea.lookup(".content");
        if (content != null) {
            content.setStyle("-fx-background-color: transparent; -fx-padding: 15;");
        }
        Region viewport = (Region) textArea.lookup(".viewport");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: transparent;");
        }
        Region scrollPane = (Region) textArea.lookup(".scroll-pane");
        if (scrollPane != null) {
            scrollPane.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 17; -fx-background-insets: 0; -fx-padding: 0;");
        }
    }

    private VBox createLabeledField(String labelText, Node field) {
        VBox box = new VBox(8);
        Label label = new Label(labelText);
        label.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        box.getChildren().addAll(label, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return box;
    }
}
