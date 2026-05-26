package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.Subject;
import com.schoolbell.model.Teacher;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

import static com.schoolbell.ui.ControlFactory.createDangerDialogButton;
import static com.schoolbell.ui.ControlFactory.createLabeledField;
import static com.schoolbell.ui.UIStyles.PREMIUM_SELECT_STYLE;

public class LessonEditorDialog extends BasePremiumDialog {
    private final MainApp mainApp;
    private final SchoolClass schoolClass;
    private final int day;
    private final int lesson;
    private final int parity;
    private final Runnable refreshGrid;

    private ComboBox<Teacher> teacherCombo;
    private ComboBox<Subject> subjectCombo;
    private ComboBox<Classroom> classroomCombo;
    private ComboBox<String> parityCombo;

    public LessonEditorDialog(MainApp mainApp, SchoolClass schoolClass, int day, int lesson, int parity,
                              List<Teacher> allTeachers, List<Subject> allSubjects, Runnable refreshGrid) {
        super(mainApp.getStage(),
                "Редагування уроку",
                schoolClass.name(),
                "Налаштуйте вчителя, предмет та кабінет для уроку №" + lesson,
                "ЗБЕРЕГТИ",
                550);

        this.mainApp = mainApp;
        this.schoolClass = schoolClass;
        this.day = day;
        this.lesson = lesson;
        this.parity = parity;
        this.refreshGrid = refreshGrid;

        List<Classroom> allClassrooms = mainApp.getAcademicService().getAllClassrooms();

        teacherCombo = new ComboBox<>();
        teacherCombo.getItems().addAll(mainApp.getStaffService().getAllTeachers());
        teacherCombo.setPromptText("Оберіть вчителя");
        teacherCombo.setMaxWidth(Double.MAX_VALUE);
        teacherCombo.setStyle(PREMIUM_SELECT_STYLE);

        subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(mainApp.getStaffService().getAllSubjects());
        subjectCombo.setPromptText("Оберіть предмет");
        subjectCombo.setMaxWidth(Double.MAX_VALUE);
        subjectCombo.setStyle(PREMIUM_SELECT_STYLE);
        subjectCombo.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems != null) {
                subjectCombo.setVisibleRowCount(Math.min(newItems.size(), 10));
            }
        });

        classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(allClassrooms);
        classroomCombo.setPromptText("Оберіть кабінет");
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setStyle(PREMIUM_SELECT_STYLE);

        parityCombo = new ComboBox<>();
        parityCombo.getItems().addAll("Всі тижні", "Чисельник (непарний)", "Знаменник (парний)");
        parityCombo.getSelectionModel().select(parity);
        parityCombo.setMaxWidth(Double.MAX_VALUE);
        parityCombo.setStyle(PREMIUM_SELECT_STYLE);

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

        mainApp.getAcademicService().getScheduleForClass(schoolClass.id()).stream()
                .filter(entry -> entry.dayOfWeek() == day && entry.lessonNumber() == lesson && (entry.parity() == parity || entry.parity() == 0))
                .findFirst()
                .ifPresent(entry -> {
                    mainApp.getStaffService().getAllTeachers().stream()
                            .filter(teacher -> teacher.id() == entry.teacherId())
                            .findFirst()
                            .ifPresent(teacher -> {
                                teacherCombo.setValue(teacher);
                                List<Subject> teacherSubjects = mainApp.getStaffService().getSubjectsForTeacher(teacher.id());
                                subjectCombo.getItems().setAll(teacherSubjects);
                                mainApp.getStaffService().getAllSubjects().stream()
                                        .filter(subject -> subject.id() == entry.subjectId())
                                        .findFirst()
                                        .ifPresent(subjectCombo::setValue);
                            });
                    allClassrooms.stream().filter(room -> room.id() == entry.classroomId()).findFirst().ifPresent(classroomCombo::setValue);
                });

        VBox fields = new VBox(20);
        fields.setAlignment(Pos.CENTER_LEFT);
        fields.getChildren().addAll(
                createLabeledField("ВЧИТЕЛЬ", teacherCombo),
                createLabeledField("ПРЕДМЕТ", subjectCombo),
                createLabeledField("КАБІНЕТ", classroomCombo),
                createLabeledField("ТИЖДЕНЬ", parityCombo)
        );

        content.getChildren().add(fields);

        Button deleteBtn = createDangerDialogButton("ВИДАЛИТИ");
        deleteBtn.setOnAction(ev -> {
            int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
            mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, selectedParity);
            if (refreshGrid != null) refreshGrid.run();
            close();
            ToastService.showSuccess("Урок видалено");
        });
        addLeftFooterButton(deleteBtn);
    }

    @Override
    protected boolean onSave() {
        if (teacherCombo.getValue() != null && subjectCombo.getValue() != null) {
            int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
            int classroomId = classroomCombo.getValue() != null ? classroomCombo.getValue().id() : 0;
            if (selectedParity != parity) {
                mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, parity);
            }
            mainApp.getAcademicService().saveScheduleEntry(
                    schoolClass.id(),
                    day,
                    lesson,
                    teacherCombo.getValue().id(),
                    subjectCombo.getValue().id(),
                    classroomId,
                    selectedParity
            );
            if (refreshGrid != null) refreshGrid.run();
            ToastService.showSuccess("Розклад оновлено");
            return true;
        } else {
            ToastService.showError("Оберіть вчителя та предмет");
            return false;
        }
    }
}
