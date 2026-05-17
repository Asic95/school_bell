package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.ScheduleEditorDialog;
import com.schoolbell.ui.ToastService;
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
    private final Locale ukLocale = Locale.of("uk", "UA");

    public SubstitutionsEditorTab(MainApp mainApp, ScheduleEditorDialog parentDialog) {
        this.mainApp = mainApp;
        this.parentDialog = parentDialog;
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox headerArea = createSectionHeader("Керування замінами", "Переглядайте, фільтруйте та керуйте замінами вчителів", "#e67e22", ICON_CLOCK);

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

        String activeStyle = "-fx-background-color: white; -fx-text-fill: #e67e22; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;";
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
        monthPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Month m) {
                if (m == null) return "";
                String name = m.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ukLocale);
                return name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            @Override
            public Month fromString(String s) { return null; }
        });

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

        Label monthL = new Label("Місяць:"); monthL.setStyle(HEADER_STYLE);
        Label yearL = new Label("Рік:"); yearL.setStyle(HEADER_STYLE);
        
        root.getChildren().addAll(header, monthL, monthPicker, yearL, yearPicker, generateBtn);
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
        String mName = month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ukLocale);
        mName = mName.substring(0, 1).toUpperCase() + mName.substring(1);
        fileChooser.setInitialFileName("звіт_замін_" + mName + "_" + year + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        
        File file = fileChooser.showSaveDialog(mainApp.getStage());
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                writer.println("ЗВІТ ПРО ЗАМІНИ ВЧИТЕЛІВ");
                writer.println("Період: " + mName + " " + year);
                writer.println("Згенеровано: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
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

                ToastService.showSuccess("Звіт успішно збережено у файл: " + file.getName());
                } catch (Exception ex) {
                ToastService.showError("Помилка при збереженні файлу: " + ex.getMessage());
                }
                }
                }
    private Node createSubstitutionCard(SubstitutionEntry sub) {
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
            Teacher ot = mainApp.getAcademicService().getAllTeachers().stream().filter(t -> t.id() == original.teacherId()).findFirst().orElse(null);
            Subject os = mainApp.getAcademicService().getAllSubjects().stream().filter(s -> s.id() == original.subjectId()).findFirst().orElse(null);
            
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
        Teacher nt = mainApp.getAcademicService().getAllTeachers().stream().filter(t -> t.id() == sub.teacherId()).findFirst().orElse(null);
        Subject ns = mainApp.getAcademicService().getAllSubjects().stream().filter(s -> s.id() == sub.subjectId()).findFirst().orElse(null);
        
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

        Button editBtn = com.schoolbell.ui.UIComponents.createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
        editBtn.setOnAction(e -> parentDialog.openSubstitutionEditDialog(sub, sub.date(), refreshSubstitutions));

        Button delBtn = com.schoolbell.ui.UIComponents.createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
        delBtn.setOnAction(e -> {
            mainApp.getAcademicService().deleteSubstitution(sub.id());
            refreshSubstitutions.run();
        });

        actions.getChildren().addAll(editBtn, delBtn);
        card.getChildren().addAll(lessonAndClass, mainContent, actions);
        return card;
    }
}
