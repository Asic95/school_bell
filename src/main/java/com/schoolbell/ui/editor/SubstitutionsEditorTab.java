package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.ScheduleEditorDialog;
import com.schoolbell.ui.UIComponents;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.schoolbell.ui.UIComponents.createAvatar;
import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class SubstitutionsEditorTab {
    private final MainApp mainApp;
    private final ScheduleEditorDialog parentDialog;
    private Runnable refreshSubstitutions;
    private String searchText = "";
    private boolean showArchived = false;
    private final Locale ukLocale = new Locale("uk", "UA");

    public SubstitutionsEditorTab(MainApp mainApp, ScheduleEditorDialog parentDialog) {
        this.mainApp = mainApp;
        this.parentDialog = parentDialog;
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox headerArea = createSectionHeader("Керування замінами", "Переглядайте, фільтруйте та керуйте замінами вчителів", COLOR_PRIMARY, ICON_CALENDAR);

        // Filter & Search Bar
        HBox actionToolbar = new HBox(20);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("НОВА ЗАМІНА");
        addBtn.setGraphic(createSVGIcon(ICON_PLUS, Color.WHITE, 16));
        addBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 10 20;");
        addBtn.setOnAction(e -> parentDialog.openSubstitutionEditDialog(null, LocalDate.now(), refreshSubstitutions));

        Button reportBtn = new Button("ЗВІТ");
        reportBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 16));
        reportBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_PURPLE + "; -fx-padding: 10 20;");
        reportBtn.setOnAction(e -> showReportDialog());

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук за вчителем або класом...");
        searchField.setPrefWidth(300);
        searchField.setStyle(FIELD_STYLE);
        searchField.textProperty().addListener((o, ov, nv) -> {
            searchText = nv.toLowerCase();
            refreshSubstitutions.run();
        });

        // Segmented Toggle for Active/Archive
        HBox toggleGroup = new HBox(0);
        toggleGroup.setStyle("-fx-background-color: #dfe6e9; -fx-background-radius: 12; -fx-padding: 2;");
        
        ToggleButton activeBtn = new ToggleButton("Активні");
        ToggleButton archiveBtn = new ToggleButton("Архів");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        String activeStyle = "-fx-background-color: white; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: " + COLOR_NEUTRAL + "; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;";

        activeBtn.setStyle(activeStyle);
        archiveBtn.setStyle(inactiveStyle);

        group.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == activeBtn) {
                activeBtn.setStyle(activeStyle);
                archiveBtn.setStyle(inactiveStyle);
                showArchived = false;
            } else {
                activeBtn.setStyle(inactiveStyle);
                archiveBtn.setStyle(activeStyle);
                showArchived = true;
            }
            refreshSubstitutions.run();
        });

        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionToolbar.getChildren().addAll(addBtn, reportBtn, spacer, searchField, toggleGroup);

        VBox contentList = new VBox(15);
        contentList.setPadding(new Insets(5, 5, 20, 5));
        
        ScrollPane scroll = new ScrollPane(contentList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        refreshSubstitutions = () -> {
            contentList.getChildren().clear();
            List<SubstitutionEntry> allSubs = mainApp.getAcademicService().getAllSubstitutions();
            LocalDate today = LocalDate.now();

            List<SubstitutionEntry> filtered = allSubs.stream()
                .filter(sub -> showArchived ? sub.date().isBefore(today) : !sub.date().isBefore(today))
                .filter(sub -> {
                    if (searchText.isEmpty()) return true;
                    Teacher t = mainApp.getAcademicService().getAllTeachers().stream().filter(tea -> tea.id() == sub.teacherId()).findFirst().orElse(null);
                    SchoolClass cls = mainApp.getAcademicService().getAllClasses().stream().filter(c -> c.id() == sub.classId()).findFirst().orElse(null);
                    String teacherName = t != null ? t.name().toLowerCase() : "";
                    String className = cls != null ? cls.name().toLowerCase() : "";
                    return teacherName.contains(searchText) || className.contains(searchText);
                })
                .sorted((a, b) -> showArchived ? b.date().compareTo(a.date()) : a.date().compareTo(b.date()))
                .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                VBox empty = new VBox(20);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(100, 0, 0, 0));
                Label emptyIcon = new Label("∅");
                emptyIcon.setStyle("-fx-font-size: 64px; -fx-text-fill: #dfe6e9;");
                Label emptyLabel = new Label(showArchived ? "Архів порожній" : "Немає активних замін");
                emptyLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #b2bec3;");
                empty.getChildren().addAll(emptyIcon, emptyLabel);
                contentList.getChildren().add(empty);
            } else {
                LocalDate lastDate = null;
                DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", ukLocale);
                for (SubstitutionEntry sub : filtered) {
                    if (lastDate == null || !lastDate.equals(sub.date())) {
                        Label dateHeader = new Label(sub.date().format(headerFormatter).toUpperCase());
                        dateHeader.setStyle(HEADER_STYLE + "-fx-padding: 10 0 5 10;");
                        contentList.getChildren().add(dateHeader);
                        lastDate = sub.date();
                    }
                    contentList.getChildren().add(createSubstitutionCard(sub));
                }
            }
        };

        root.getChildren().addAll(headerArea, actionToolbar, scroll);
        refreshSubstitutions.run();
        return root;
    }

    private void showReportDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Генерація звіту замін");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white;");

        Label header = new Label("ОБЕРІТЬ ПЕРІОД");
        header.setStyle(HEADER_STYLE);

        ComboBox<Month> monthPicker = new ComboBox<>();
        monthPicker.getItems().addAll(Month.values());
        monthPicker.setValue(LocalDate.now().getMonth());
        monthPicker.setStyle(COMBO_STYLE);
        monthPicker.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> yearPicker = new Spinner<>(2024, 2030, LocalDate.now().getYear());
        yearPicker.setEditable(true);
        yearPicker.setStyle(FIELD_STYLE);
        yearPicker.setMaxWidth(Double.MAX_VALUE);

        Button generateBtn = new Button("ЗГЕНЕРУВАТИ ТА ЗБЕРЕГТИ");
        generateBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 12;");
        generateBtn.setMaxWidth(Double.MAX_VALUE);

        generateBtn.setOnAction(e -> {
            generateReport(monthPicker.getValue(), yearPicker.getValue());
            stage.close();
        });

        root.getChildren().addAll(header, new Label("Місяць:"), monthPicker, new Label("Рік:"), yearPicker, generateBtn);
        stage.setScene(new Scene(root, 350, 350));
        stage.show();
    }

    private void generateReport(Month month, int year) {
        List<SubstitutionEntry> allSubs = mainApp.getAcademicService().getAllSubstitutions();
        List<SubstitutionEntry> filtered = allSubs.stream()
                .filter(s -> s.date().getMonth() == month && s.date().getYear() == year)
                .sorted((a, b) -> a.date().compareTo(b.date()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("За вказаний період замін не знайдено.");
            alert.show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти звіт");
        fileChooser.setInitialFileName("report_substitutions_" + year + "_" + month.getValue() + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        File file = fileChooser.showSaveDialog(mainApp.getStage());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                writer.println("ЗВІТ ПРО ЗАМІНИ ВЧИТЕЛІВ");
                writer.println("Період: " + month.name() + " " + year);
                writer.println("Згенеровано: " + LocalDate.now());
                writer.println("==========================================");
                writer.println();

                // Group by teacher
                Map<String, List<SubstitutionEntry>> byTeacher = new TreeMap<>();
                for (SubstitutionEntry s : filtered) {
                    Teacher t = mainApp.getAcademicService().getAllTeachers().stream().filter(tea -> tea.id() == s.teacherId()).findFirst().orElse(null);
                    String name = t != null ? t.name() : "Невідомий вчитель";
                    byTeacher.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(s);
                }

                for (Map.Entry<String, List<SubstitutionEntry>> entry : byTeacher.entrySet()) {
                    writer.println("ВЧИТЕЛЬ: " + entry.getKey());
                    writer.println("Кількість замін: " + entry.getValue().size());
                    writer.println("------------------------------------------");
                    for (SubstitutionEntry s : entry.getValue()) {
                        SchoolClass c = mainApp.getAcademicService().getAllClasses().stream().filter(cl -> cl.id() == s.classId()).findFirst().orElse(null);
                        Subject sub = mainApp.getAcademicService().getAllSubjects().stream().filter(x -> x.id() == s.subjectId()).findFirst().orElse(null);
                        String roomName = s.classroomId() > 0 ? mainApp.getClassroomName(s.classroomId()) : "—";
                        writer.printf("  %s | Урок %d | Клас: %s | Предмет: %s | Каб: %s%n", 
                            s.date().format(DateTimeFormatter.ofPattern("dd.MM (EEE)", ukLocale)),
                            s.lessonNumber(),
                            (c != null ? c.name() : "?"),
                            (sub != null ? sub.name() : "Заміна"),
                            roomName
                        );
                    }
                    writer.println();
                }

                writer.println("==========================================");
                writer.println("Всього замін у звіті: " + filtered.size());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("Звіт успішно збережено у файл: " + file.getName());
                alert.show();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Помилка при збереженні файлу: " + ex.getMessage());
                alert.show();
            }
        }
    }

    private Node createSubstitutionCard(SubstitutionEntry sub) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 25, 15, 25));
        card.setStyle(SOFT_CARD);

        // Lesson Badge
        VBox lessonBox = new VBox();
        lessonBox.setAlignment(Pos.CENTER);
        lessonBox.setPrefSize(45, 45);
        lessonBox.setStyle("-fx-background-color: " + COLOR_BLUE_LIGHT + "; -fx-background-radius: 12;");
        Label lessonNum = new Label(String.valueOf(sub.lessonNumber()));
        lessonNum.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_PRIMARY + ";");
        Label lessonText = new Label("УРОК");
        lessonText.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_PRIMARY + ";");
        lessonBox.getChildren().addAll(lessonNum, lessonText);

        // Find Original Teacher and Subject from Schedule
        int dayOfWeek = sub.date().getDayOfWeek().getValue();
        List<ScheduleEntry> schedule = mainApp.getAcademicService().getScheduleForClass(sub.classId());
        ScheduleEntry original = schedule.stream()
                .filter(e -> e.dayOfWeek() == dayOfWeek && e.lessonNumber() == sub.lessonNumber())
                .findFirst().orElse(null);

        VBox comparisonBox = new VBox(8);
        comparisonBox.setAlignment(Pos.CENTER_LEFT);

        // Original info
        HBox originalRow = new HBox(10);
        originalRow.setAlignment(Pos.CENTER_LEFT);
        originalRow.setOpacity(0.6);
        if (original != null) {
            Teacher ot = mainApp.getAcademicService().getAllTeachers().stream().filter(t -> t.id() == original.teacherId()).findFirst().orElse(null);
            Subject os = mainApp.getAcademicService().getAllSubjects().stream().filter(s -> s.id() == original.subjectId()).findFirst().orElse(null);
            
            Label otLabel = new Label(ot != null ? ot.name() : "Невідомий");
            otLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + COLOR_NEUTRAL + ";");
            Label osLabel = new Label("(" + (os != null ? os.name() : "?") + ")");
            osLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
            originalRow.getChildren().addAll(createAvatar(ot != null ? ot.name() : "?", 24), otLabel, osLabel);
        } else {
            Label noOrig = new Label("Вікно / Новий урок");
            noOrig.setStyle("-fx-font-style: italic; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
            originalRow.getChildren().add(noOrig);
        }

        // Arrow Icon
        HBox arrowBox = new HBox(createSVGIcon("M7.41,8.58L12,13.17L16.59,8.58L18,10L12,16L6,10L7.41,8.58Z", Color.web(COLOR_NEUTRAL), 16));
        arrowBox.setPadding(new Insets(0, 0, 0, 30));

        // Replacement info
        HBox replacementRow = new HBox(10);
        replacementRow.setAlignment(Pos.CENTER_LEFT);
        Teacher nt = mainApp.getAcademicService().getAllTeachers().stream().filter(t -> t.id() == sub.teacherId()).findFirst().orElse(null);
        Subject ns = mainApp.getAcademicService().getAllSubjects().stream().filter(s -> s.id() == sub.subjectId()).findFirst().orElse(null);
        
        Label ntLabel = new Label(nt != null ? nt.name() : "Немає вчителя");
        ntLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: " + COLOR_TEXT + ";");
        Label nsLabel = new Label("(" + (ns != null ? ns.name() : "Заміна") + ")");
        nsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-weight: bold;");
        
        replacementRow.getChildren().addAll(createAvatar(nt != null ? nt.name() : "?", 32), ntLabel, nsLabel);

        // Add classroom info if exists
        if (sub.classroomId() > 0) {
            String roomName = mainApp.getClassroomName(sub.classroomId());
            Label roomLabel = new Label(roomName);
            roomLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: " + COLOR_PRIMARY + "; -fx-background-radius: 6; -fx-padding: 2 8; -fx-font-weight: bold;");
            HBox roomBox = new HBox(createSVGIcon(ICON_ROOM, Color.WHITE, 12), roomLabel);
            roomBox.setSpacing(5);
            roomBox.setAlignment(Pos.CENTER_LEFT);
            replacementRow.getChildren().add(roomBox);
        }

        comparisonBox.getChildren().addAll(originalRow, arrowBox, replacementRow);

        // Class Badge
        SchoolClass cls = mainApp.getAcademicService().getAllClasses().stream().filter(c -> c.id() == sub.classId()).findFirst().orElse(null);
        Label classBadge = new Label(cls != null ? cls.name() : "?");
        classBadge.setStyle("-fx-background-color: " + COLOR_PURPLE_LIGHT + "; -fx-text-fill: " + COLOR_PURPLE + "; -fx-padding: 5 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action Buttons
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button();
        editBtn.setGraphic(createSVGIcon(ICON_EDIT, Color.web(COLOR_NEUTRAL), 18));
        editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;");
        editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #f1f2f6; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;"));
        editBtn.setOnAction(e -> parentDialog.openSubstitutionEditDialog(sub, sub.date(), refreshSubstitutions));

        Button delBtn = new Button();
        delBtn.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 18));
        delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;");
        delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #fff5f5; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;"));
        delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8; -fx-background-radius: 10;"));
        delBtn.setOnAction(e -> {
            mainApp.getAcademicService().deleteSubstitution(sub.id());
            refreshSubstitutions.run();
        });

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(lessonBox, comparisonBox, spacer, classBadge, actions);
        return card;
    }
}
