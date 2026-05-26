package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.Subject;
import com.schoolbell.model.SubstitutionEntry;
import com.schoolbell.model.Teacher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
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

import static com.schoolbell.ui.ControlFactory.createDialogHeader;
import static com.schoolbell.ui.ControlFactory.createDialogRoot;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createSecondaryDialogButton;
import static com.schoolbell.ui.UIStyles.HEADER_STYLE;
import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_SPINNER_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_BTN_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_SELECT_STYLE;

public class SubstitutionEditorDialog extends BasePremiumDialog {
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
        super(mainApp.getStage(),
                entry == null ? "Створення" : "Редагування",
                entry == null ? "Нова заміна" : "Редагування заміни",
                "Оберіть параметри для автоматичної заміни в розкладі.",
                "ЗБЕРЕГТИ ЗАМІНУ");

        this.mainApp = mainApp;
        this.entry = entry;
        this.onSave = onSave;

        GridPane grid = new GridPane();
        grid.setHgap(25);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
        labelCol.setPrefWidth(180);
        javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        datePicker = new DatePicker(entry != null ? entry.date() : defaultDate);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        grid.add(createLabel("ДАТА"), 0, 0);
        grid.add(datePicker, 1, 0);

        lessonSpinner = new Spinner<>(1, 15, entry != null ? entry.lessonNumber() : 1);
        lessonSpinner.setEditable(true);
        lessonSpinner.setMaxWidth(120);
        grid.add(createLabel("УРОК №"), 0, 1);
        grid.add(lessonSpinner, 1, 1);

        classCombo = new ComboBox<>();
        classCombo.getItems().addAll(mainApp.getAcademicService().getAllClasses());
        classCombo.setMaxWidth(Double.MAX_VALUE);
        classCombo.setStyle(PREMIUM_SELECT_STYLE);
        grid.add(createLabel("КЛАС"), 0, 2);
        grid.add(classCombo, 1, 2);

        teacherCombo = new ComboBox<>();
        teacherCombo.getItems().addAll(mainApp.getStaffService().getAllTeachers());
        teacherCombo.setMaxWidth(Double.MAX_VALUE);
        teacherCombo.setStyle(PREMIUM_SELECT_STYLE);
        grid.add(createLabel("ВЧИТЕЛЬ"), 0, 3);
        grid.add(teacherCombo, 1, 3);

        subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(mainApp.getStaffService().getAllSubjects());
        subjectCombo.setMaxWidth(Double.MAX_VALUE);
        subjectCombo.setStyle(PREMIUM_SELECT_STYLE);
        grid.add(createLabel("ПРЕДМЕТ"), 0, 4);
        grid.add(subjectCombo, 1, 4);

        classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(mainApp.getAcademicService().getAllClassrooms());
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setStyle(PREMIUM_SELECT_STYLE);
        classroomCombo.setPromptText("Оберіть кабінет");
        grid.add(createLabel("КАБІНЕТ"), 0, 5);
        grid.add(classroomCombo, 1, 5);

        teacherCombo.setOnAction(e -> {
            Teacher selected = teacherCombo.getValue();
            if (selected != null) {
                List<Subject> teacherSubjects = mainApp.getStaffService().getSubjectsForTeacher(selected.id());
                Subject currentSubject = subjectCombo.getValue();
                subjectCombo.getItems().setAll(teacherSubjects);
                if (currentSubject != null && teacherSubjects.contains(currentSubject)) {
                    subjectCombo.setValue(currentSubject);
                }
            }
        });

        if (entry != null) {
            classCombo.getItems().stream().filter(c -> c.id() == entry.classId()).findFirst().ifPresent(classCombo::setValue);
            mainApp.getStaffService().getAllTeachers().stream().filter(t -> t.id() == entry.teacherId()).findFirst().ifPresent(teacher -> {
                teacherCombo.setValue(teacher);
                List<Subject> teacherSubjects = mainApp.getStaffService().getSubjectsForTeacher(teacher.id());
                subjectCombo.getItems().setAll(teacherSubjects);
                mainApp.getStaffService().getAllSubjects().stream().filter(s -> s.id() == entry.subjectId()).findFirst().ifPresent(subjectCombo::setValue);
            });
            mainApp.getAcademicService().getAllClassrooms().stream().filter(c -> c.id() == entry.classroomId()).findFirst().ifPresent(classroomCombo::setValue);
        }

        content.getChildren().add(grid);
    }

    @Override
    protected boolean onSave() {
        if (classCombo.getValue() == null || teacherCombo.getValue() == null || subjectCombo.getValue() == null) {
            ToastService.showError("Заповніть всі обов'язкові поля (клас, вчитель, предмет)");
            return false;
        }

        int classroomId = classroomCombo.getValue() != null ? classroomCombo.getValue().id() : 0;
        if (entry != null && (entry.classId() != classCombo.getValue().id()
                || !entry.date().equals(datePicker.getValue())
                || entry.lessonNumber() != lessonSpinner.getValue())) {
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

        if (onSave != null) {
            onSave.run();
        }
        ToastService.showSuccess("Зміну збережено успішно");
        return true;
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        return lbl;
    }
}
