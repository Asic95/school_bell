package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.util.List;

import static com.schoolbell.ui.ControlFactory.*;
import static com.schoolbell.ui.UIStyles.*;

public class SubstitutionEditorDialog extends Stage {
    private final MainApp mainApp;
    private final SubstitutionEntry entry;
    private final Runnable onSave;

    private DatePicker datePicker;
    private Spinner<Integer> lessonSpinner;
    private ComboBox<SchoolClass> classCombo;
    private ComboBox<Teacher> teacherCombo;
    private ComboBox<Subject> subjectCombo;
    private ComboBox<Classroom> classroomCombo;

    public SubstitutionEditorDialog(MainApp mainApp, SubstitutionEntry entry, LocalDate defaultDate, Runnable onSave) {
        this.mainApp = mainApp;
        this.entry = entry;
        this.onSave = onSave;

        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        initOwner(mainApp.getStage());

        VBox root = new VBox(25);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD + "-fx-background-radius: 32; -fx-border-radius: 32; -fx-border-width: 2; -fx-border-color: #e2e8f0;");
        root.setPrefWidth(650); // Increased width

        Label title = new Label(entry == null ? "Нова заміна" : "Редагування заміни");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + ";");
        Label subtitle = new Label("Оберіть параметри для автоматичної заміни в розкладі.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #64748b;");

        VBox headerBox = new VBox(4, title, subtitle);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(18);
        grid.setAlignment(Pos.CENTER_LEFT);
        
        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setPrefWidth(180); // Increased label column width
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        datePicker = new DatePicker(entry != null ? entry.date() : defaultDate);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        grid.add(createLabel("ДАТА"), 0, 0);
        grid.add(datePicker, 1, 0);

        lessonSpinner = new Spinner<>(1, 10, entry != null ? entry.lessonNumber() : 1);
        lessonSpinner.setEditable(true);
        lessonSpinner.setMaxWidth(120);
        grid.add(createLabel("УРОК №"), 0, 1);
        grid.add(lessonSpinner, 1, 1);

        classCombo = new ComboBox<>();
        classCombo.getItems().addAll(mainApp.getAcademicService().getAllClasses());
        classCombo.setMaxWidth(Double.MAX_VALUE);
        classCombo.setStyle(COMBO_STYLE + "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 12; -fx-border-radius: 12;");
        grid.add(createLabel("КЛАС"), 0, 2);
        grid.add(classCombo, 1, 2);

        teacherCombo = new ComboBox<>();
        teacherCombo.getItems().addAll(mainApp.getStaffService().getAllTeachers());
        teacherCombo.setMaxWidth(Double.MAX_VALUE);
        teacherCombo.setStyle(COMBO_STYLE + "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 12; -fx-border-radius: 12;");
        grid.add(createLabel("ВЧИТЕЛЬ"), 0, 3);
        grid.add(teacherCombo, 1, 3);

        subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(mainApp.getStaffService().getAllSubjects());
        subjectCombo.setMaxWidth(Double.MAX_VALUE);
        subjectCombo.setStyle(COMBO_STYLE + "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 12; -fx-border-radius: 12;");
        grid.add(createLabel("ПРЕДМЕТ"), 0, 4);
        grid.add(subjectCombo, 1, 4);

        classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(mainApp.getAcademicService().getAllClassrooms());
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setStyle(COMBO_STYLE + "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-background-radius: 12; -fx-border-radius: 12;");
        classroomCombo.setPromptText("Оберіть кабінет");
        grid.add(createLabel("КАБІНЕТ"), 0, 5);
        grid.add(classroomCombo, 1, 5);

        teacherCombo.setOnAction(e -> {
            Teacher selected = teacherCombo.getValue();
            if (selected != null) {
                List<Subject> teacherSubjects = mainApp.getStaffService().getSubjectsForTeacher(selected.id());
                Subject currentSub = subjectCombo.getValue();
                subjectCombo.getItems().setAll(teacherSubjects);
                if (currentSub != null && teacherSubjects.contains(currentSub)) {
                    subjectCombo.setValue(currentSub);
                }
            }
        });

        if (entry != null) {
            classCombo.getItems().stream().filter(c -> c.id() == entry.classId()).findFirst().ifPresent(classCombo::setValue);
            mainApp.getStaffService().getAllTeachers().stream().filter(t -> t.id() == entry.teacherId()).findFirst().ifPresent(t -> {
                teacherCombo.setValue(t);
                List<Subject> ts = mainApp.getStaffService().getSubjectsForTeacher(t.id());
                subjectCombo.getItems().setAll(ts);
                mainApp.getStaffService().getAllSubjects().stream().filter(s -> s.id() == entry.subjectId()).findFirst().ifPresent(subjectCombo::setValue);
            });
            mainApp.getAcademicService().getAllClassrooms().stream().filter(c -> c.id() == entry.classroomId()).findFirst().ifPresent(classroomCombo::setValue);
        }

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("СКАСУВАТИ");
        cancelBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 14; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ ЗАМІНУ", ICON_SAVE);
        saveBtn.setOnAction(ev -> {
            if (saveSubstitution()) {
                close();
            }
        });

        actions.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(headerBox, grid, actions);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(
            "data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"),
            "data:text/css," + MODERN_SPINNER_STYLE.replace(" ", "%20")
        );
        setScene(scene);
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px;");
        return lbl;
    }

    private boolean saveSubstitution() {
        if (classCombo.getValue() == null || teacherCombo.getValue() == null || subjectCombo.getValue() == null) {
            ToastService.showError("Заповніть всі обов'язкові поля (клас, вчитель, предмет)");
            return false;
        }

        int classroomId = classroomCombo.getValue() != null ? classroomCombo.getValue().id() : 0;
        if (entry != null && (entry.classId() != classCombo.getValue().id() || !entry.date().equals(datePicker.getValue()) || entry.lessonNumber() != lessonSpinner.getValue())) {
            mainApp.getAcademicService().deleteSubstitution(entry.id());
        }
        
        mainApp.getAcademicService().saveSubstitution(
                classCombo.getValue().id(),
                datePicker.getValue(),
                lessonSpinner.getValue(),
                teacherCombo.getValue().id(),
                subjectCombo.getValue().id(),
                classroomId
        );
        
        if (onSave != null) onSave.run();
        ToastService.showSuccess("Заміну збережено успішно");
        return true;
    }
}
