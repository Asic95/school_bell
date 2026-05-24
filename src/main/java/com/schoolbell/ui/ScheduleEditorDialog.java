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

import javafx.stage.StageStyle;

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

        List<Classroom> allClasses = mainApp.getAcademicService().getAllClassrooms();

        VBox root = new VBox(28);
        root.setPadding(new Insets(35));
        root.setStyle(SOFT_CARD);
        root.setPrefWidth(550);

        // --- MODERN PREMIUM HEADER ---
        VBox headerText = new VBox(8);
        Label eb = new Label("РЕДАГУВАННЯ УРОКУ");
        eb.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        Label t = new Label(schoolClass.name());
        t.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label s = new Label("Налаштуйте вчителя, предмет та кабінет для уроку №" + lesson);
        s.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-text-fill: #64748b;");
        headerText.getChildren().addAll(eb, t, s);

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
        
        // Fix for empty items artifact in dropdown
        subjectCombo.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems != null) {
                subjectCombo.setVisibleRowCount(Math.min(newItems.size(), 10));
            }
        });

        ComboBox<Classroom> classroomCombo = new ComboBox<>();
        classroomCombo.getItems().addAll(allClasses);
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
                    mainApp.getStaffService().getAllTeachers().stream().filter(teach -> teach.id() == e.teacherId()).findFirst().ifPresent(teach -> {
                        teacherCombo.setValue(teach);
                        List<Subject> ts = mainApp.getStaffService().getSubjectsForTeacher(teach.id());
                        subjectCombo.getItems().setAll(ts);
                        mainApp.getStaffService().getAllSubjects().stream().filter(subj -> subj.id() == e.subjectId()).findFirst().ifPresent(subjectCombo::setValue);
                    });
                    allClasses.stream().filter(cl -> cl.id() == e.classroomId()).findFirst().ifPresent(classroomCombo::setValue);
                });

        fields.getChildren().addAll(
            createLabeledField("ВЧИТЕЛЬ", teacherCombo),
            createLabeledField("ПРЕДМЕТ", subjectCombo),
            createLabeledField("КАБІНЕТ", classroomCombo),
            createLabeledField("ТИЖДЕНЬ", parityCombo)
        );

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("СКАСУВАТИ");
        String cancelStyle = "-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 18; -fx-cursor: hand;";
        cancelBtn.setStyle(cancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(cancelStyle + "-fx-background-color: #f1f2f6;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(cancelStyle));
        cancelBtn.setOnAction(e -> stage.close());

        Button deleteBtn = new Button("ВИДАЛИТИ");
        String deleteStyle = "-fx-background-color: white; -fx-text-fill: #dc2626; -fx-font-weight: 800; -fx-padding: 12 24; -fx-background-radius: 18; -fx-cursor: hand; -fx-border-color: #fee2e2; -fx-border-radius: 18;";
        deleteBtn.setStyle(deleteStyle);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteStyle + "-fx-background-color: #fef2f2; -fx-border-color: " + COLOR_DANGER + ";"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(deleteStyle));
        deleteBtn.setOnAction(ev -> {
            int selectedParity = parityCombo.getSelectionModel().getSelectedIndex();
            mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, selectedParity);
            refreshGrid.run();
            stage.close();
            ToastService.showSuccess("Урок видалено");
        });

        Button saveBtn = ControlFactory.createPrimaryActionButton("ЗБЕРЕГТИ", ICON_SAVE);
        saveBtn.setStyle(PREMIUM_BTN_STYLE);
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

    private VBox createLabeledField(String labelText, Node field) {
        VBox box = new VBox(8);
        Label label = new Label(labelText);
        label.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        box.getChildren().addAll(label, field);
        return box;
    }
}
