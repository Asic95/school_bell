package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Classroom;
import com.schoolbell.model.SchoolClass;
import com.schoolbell.model.Subject;
import com.schoolbell.model.Teacher;
import com.schoolbell.ui.editor.AnnouncementsEditorTab;
import com.schoolbell.ui.editor.BellsEditorTab;
import com.schoolbell.ui.editor.ClassesEditorTab;
import com.schoolbell.ui.editor.ClassroomsEditorTab;
import com.schoolbell.ui.editor.SubjectsEditorTab;
import com.schoolbell.ui.editor.SubstitutionsEditorTab;
import com.schoolbell.ui.editor.TeachersEditorTab;
import com.schoolbell.ui.editor.WeeklyScheduleEditorTab;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

import static com.schoolbell.ui.ControlFactory.createDangerDialogButton;
import static com.schoolbell.ui.ControlFactory.createDialogHeader;
import static com.schoolbell.ui.ControlFactory.createDialogRoot;
import static com.schoolbell.ui.ControlFactory.createLabeledField;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createSecondaryDialogButton;
import static com.schoolbell.ui.UIStyles.ICON_SAVE;
import static com.schoolbell.ui.UIStyles.MODERN_CHECKBOX_STYLE;
import static com.schoolbell.ui.UIStyles.MODERN_DATE_PICKER_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_BTN_STYLE;
import static com.schoolbell.ui.UIStyles.PREMIUM_SELECT_STYLE;

public class ScheduleEditorDialog {
    private final MainApp mainApp;

    private final BellsEditorTab bellsTab;
    private final TeachersEditorTab teachersTab;
    private final SubjectsEditorTab subjectsTab;
    private final ClassesEditorTab classesTab;
    private final WeeklyScheduleEditorTab weeklyTab;
    private final SubstitutionsEditorTab substitutionsTab;
    private final AnnouncementsEditorTab announcementsTab;
    private final ClassroomsEditorTab classroomsTab;

    public ScheduleEditorDialog(MainApp mainApp) {
        this.mainApp = mainApp;
        this.bellsTab = new BellsEditorTab(mainApp);
        this.teachersTab = new TeachersEditorTab(mainApp);
        this.subjectsTab = new SubjectsEditorTab(mainApp);
        this.classesTab = new ClassesEditorTab(mainApp);
        this.weeklyTab = new WeeklyScheduleEditorTab(mainApp, this);
        this.substitutionsTab = new SubstitutionsEditorTab(mainApp, this);
        this.announcementsTab = new AnnouncementsEditorTab(mainApp);
        this.classroomsTab = new ClassroomsEditorTab(mainApp);
    }

    public Node createTabContent(int index) {
        return switch (index) {
            case 0 -> bellsTab.createContent();
            case 1 -> teachersTab.createContent();
            case 2 -> subjectsTab.createContent();
            case 3 -> classesTab.createContent();
            case 4 -> weeklyTab.createContent();
            case 5 -> substitutionsTab.createContent();
            case 6 -> announcementsTab.createContent();
            case 7 -> classroomsTab.createContent();
            default -> new Label("Unknown Tab");
        };
    }

    public void openEditDialog(SchoolClass schoolClass, int day, int lesson, int parity,
                               List<Teacher> allTeachers, List<Subject> allSubjects, Runnable refreshGrid) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        List<Classroom> allClassrooms = mainApp.getAcademicService().getAllClassrooms();

        VBox root = createDialogRoot(550);
        VBox headerText = createDialogHeader(
                "Редагування уроку",
                schoolClass.name(),
                "Налаштуйте вчителя, предмет та кабінет для уроку №" + lesson
        );

        VBox fields = new VBox(20);
        fields.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Teacher> teacherCombo = new ComboBox<>();
        teacherCombo.getItems().addAll(mainApp.getStaffService().getAllTeachers());
        teacherCombo.setPromptText("Оберіть вчителя");
        teacherCombo.setMaxWidth(Double.MAX_VALUE);
        teacherCombo.setStyle(PREMIUM_SELECT_STYLE);

        ComboBox<Subject> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(mainApp.getStaffService().getAllSubjects());
        subjectCombo.setPromptText("Оберіть предмет");
        subjectCombo.setMaxWidth(Double.MAX_VALUE);
        subjectCombo.setStyle(PREMIUM_SELECT_STYLE);
        subjectCombo.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems != null) {
                subjectCombo.setVisibleRowCount(Math.min(newItems.size(), 10));
            }
        });

        ComboBox<Classroom> classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(allClassrooms);
        classroomCombo.setPromptText("Оберіть кабінет");
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setStyle(PREMIUM_SELECT_STYLE);

        ComboBox<String> parityCombo = new ComboBox<>();
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

        fields.getChildren().addAll(
                createLabeledField("ВЧИТЕЛЬ", teacherCombo),
                createLabeledField("ПРЕДМЕТ", subjectCombo),
                createLabeledField("КАБІНЕТ", classroomCombo),
                createLabeledField("ТИЖДЕНЬ", parityCombo)
        );

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = createSecondaryDialogButton("СКАСУВАТИ");
        cancelBtn.setOnAction(e -> stage.close());

        Button deleteBtn = createDangerDialogButton("ВИДАЛИТИ");
        deleteBtn.setOnAction(ev -> {
            int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
            mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, selectedParity);
            refreshGrid.run();
            stage.close();
            ToastService.showSuccess("Урок видалено");
        });

        Button saveBtn = createPrimaryActionButton("ЗБЕРЕГТИ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
        saveBtn.setOnAction(ev -> {
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
                refreshGrid.run();
                stage.close();
                ToastService.showSuccess("Розклад оновлено");
            } else {
                ToastService.showError("Оберіть вчителя та предмет");
            }
        });

        actions.getChildren().addAll(cancelBtn, deleteBtn, saveBtn);
        root.getChildren().addAll(headerText, fields, actions);

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
