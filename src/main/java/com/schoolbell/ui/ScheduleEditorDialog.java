package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.editor.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

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
        stage.setTitle("Редагування уроку: " + schoolClass.name());

        List<Classroom> allClasses = mainApp.getAcademicService().getAllClassrooms();

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox header = createSectionHeader("Редагування уроку", schoolClass.name() + " | Урок " + lesson, COLOR_PRIMARY, ICON_EDIT);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(15);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(130);
        col1.setPrefWidth(130);
        grid.getColumnConstraints().addAll(col1, new ColumnConstraints());

        ComboBox<Teacher> teacherCombo = new ComboBox<>();
        teacherCombo.getItems().addAll(mainApp.getStaffService().getAllTeachers());
        teacherCombo.setPromptText("Оберіть вчителя");
        teacherCombo.setMaxWidth(Double.MAX_VALUE);
        teacherCombo.setStyle(COMBO_STYLE);

        ComboBox<Subject> subjectCombo = new ComboBox<>();
        subjectCombo.getItems().addAll(mainApp.getStaffService().getAllSubjects());
        subjectCombo.setPromptText("Оберіть предмет");
        subjectCombo.setMaxWidth(Double.MAX_VALUE);
        subjectCombo.setStyle(COMBO_STYLE);

        ComboBox<Classroom> classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(allClasses);
        classroomCombo.setPromptText("Оберіть кабінет");
        classroomCombo.setMaxWidth(Double.MAX_VALUE);
        classroomCombo.setStyle(COMBO_STYLE);

        ComboBox<String> parityCombo = new ComboBox<>();
        parityCombo.getItems().addAll("Всі тижні", "Чисельник (непарний)", "Знаменник (парний)");
        parityCombo.getSelectionModel().select(parity);
        parityCombo.setMaxWidth(Double.MAX_VALUE);
        parityCombo.setStyle(COMBO_STYLE);

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

        // Pre-select current values
        mainApp.getAcademicService().getScheduleForClass(schoolClass.id()).stream()
                .filter(e -> e.dayOfWeek() == day && e.lessonNumber() == lesson && (e.parity() == parity || e.parity() == 0))
                .findFirst().ifPresent(e -> {
                    mainApp.getStaffService().getAllTeachers().stream().filter(t -> t.id() == e.teacherId()).findFirst().ifPresent(t -> {
                        teacherCombo.setValue(t);
                        List<Subject> ts = mainApp.getStaffService().getSubjectsForTeacher(t.id());
                        subjectCombo.getItems().setAll(ts);
                        mainApp.getStaffService().getAllSubjects().stream().filter(s -> s.id() == e.subjectId()).findFirst().ifPresent(subjectCombo::setValue);
                    });
                    allClasses.stream().filter(c -> c.id() == e.classroomId()).findFirst().ifPresent(classroomCombo::setValue);
                });

        Label teacherL = new Label("Вчитель:"); teacherL.setStyle(HEADER_STYLE);
        grid.add(teacherL, 0, 0);
        grid.add(teacherCombo, 1, 0);
        
        Label subjectL = new Label("Предмет:"); subjectL.setStyle(HEADER_STYLE);
        grid.add(subjectL, 0, 1);
        grid.add(subjectCombo, 1, 1);
        
        Label classroomL = new Label("Кабінет:"); classroomL.setStyle(HEADER_STYLE);
        grid.add(classroomL, 0, 2);
        grid.add(classroomCombo, 1, 2);
        
        Label weekL = new Label("Тиждень:"); weekL.setStyle(HEADER_STYLE);
        grid.add(weekL, 0, 3);
        grid.add(parityCombo, 1, 3);

        Button saveBtn = new Button("ЗБЕРЕГТИ");
        saveBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 10 30;");
        saveBtn.setOnAction(ev -> {
            if (teacherCombo.getValue() != null && subjectCombo.getValue() != null) {
                int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
                int classroomId = classroomCombo.getValue() != null ? classroomCombo.getValue().id() : 0;
                if (selectedParity != parity) {
                    mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, parity);
                }
                mainApp.getAcademicService().saveScheduleEntry(
                        schoolClass.id(), day, lesson,
                        teacherCombo.getValue().id(), subjectCombo.getValue().id(), classroomId, selectedParity
                );
                refreshGrid.run();
                stage.close();
            }
        });

        Button deleteBtn = new Button("ВИДАЛИТИ");
        deleteBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_DANGER + "; -fx-padding: 10 30;");
        deleteBtn.setOnAction(ev -> {
            int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
            mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, selectedParity);
            refreshGrid.run();
            stage.close();
        });

        HBox buttons = new HBox(15, saveBtn, deleteBtn);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(header, grid, buttons);
        Scene scene = new Scene(root, 400, 450);
        scene.getStylesheets().add("data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"));
        scene.getStylesheets().add("data:text/css," + MODERN_SPINNER_STYLE.replace(" ", "%20"));
        stage.setScene(scene);
        stage.showAndWait();
    }
}
