package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class ScheduleEditorDialog {
    private final MainApp mainApp;
    
    private Runnable refreshBells;
    private Runnable refreshTeachers;
    private Runnable refreshSubjects;
    private Runnable refreshClasses;
    private Runnable refreshWeekly;

    public ScheduleEditorDialog(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void show(Stage owner) {
        Stage dialog = new Stage(); dialog.initModality(Modality.WINDOW_MODAL); dialog.initOwner(owner); dialog.setTitle("Редактор розкладів");
        VBox root = new VBox(0); root.setStyle("-fx-background-color: #f1f2f6;");
        TabPane tabPane = new TabPane();
        tabPane.getStylesheets().add("data:text/css," + TAB_STYLE);

        tabPane.getTabs().add(createBellsTab());
        tabPane.getTabs().add(createTeachersTab());
        tabPane.getTabs().add(createSubjectsTab());
        tabPane.getTabs().add(createClassesTab());
        tabPane.getTabs().add(createWeeklyScheduleTab());

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null) return;
            if (newTab.getText().contains("ДЗВІНКИ") && refreshBells != null) refreshBells.run();
            if (newTab.getText().contains("ВЧИТЕЛІ") && refreshTeachers != null) refreshTeachers.run();
            if (newTab.getText().contains("ПРЕДМЕТИ") && refreshSubjects != null) refreshSubjects.run();
            if (newTab.getText().contains("КЛАСИ") && refreshClasses != null) refreshClasses.run();
            if (newTab.getText().contains("РОЗКЛАД") && refreshWeekly != null) refreshWeekly.run();
        });

        root.getChildren().add(tabPane);
        dialog.setScene(new Scene(root, 1100, 850));
        dialog.showAndWait();
    }

    private Tab createBellsTab() {
        Tab tab = new Tab("🔔 ДЗВІНКИ"); tab.setClosable(false);
        VBox content = new VBox(20); content.setPadding(new Insets(25)); content.setStyle("-fx-background-color: white;");
        content.setAlignment(Pos.TOP_CENTER);
        
        VBox headerArea = createSectionHeader("Налаштування часу дзвінків", "#e17055", ICON_BELL);
        ComboBox<String> scheduleSelector = new ComboBox<>();
        scheduleSelector.setPromptText("Оберіть розклад для редагування"); scheduleSelector.setPrefWidth(550); scheduleSelector.setStyle(COMBO_STYLE);
        
        refreshBells = () -> {
            String current = scheduleSelector.getValue();
            scheduleSelector.getItems().setAll(mainApp.getInternalSchedules().stream().map(DaySchedule::getName).toList());
            if (current != null && scheduleSelector.getItems().contains(current)) scheduleSelector.setValue(current);
        };

        Button addBtn = new Button("ДОДАТИ НОВИЙ"); addBtn.setStyle("-fx-background-radius: 10; -fx-padding: 8 20;");
        addBtn.setOnAction(e -> {
            TextInputDialog tid = new TextInputDialog(); tid.setTitle("Новий розклад"); tid.setHeaderText("Введіть назву:");
            tid.showAndWait().ifPresent(name -> { if (mainApp.getInternalSchedules().stream().noneMatch(ds -> ds.getName().equalsIgnoreCase(name))) {
                mainApp.getInternalSchedules().add(new DaySchedule(name)); refreshBells.run(); scheduleSelector.setValue(name);
            }});
        });

        Button renameBtn = new Button("ПЕРЕЙМЕНУВАТИ"); renameBtn.setStyle("-fx-background-radius: 10; -fx-padding: 8 20;"); renameBtn.setDisable(true);
        renameBtn.setOnAction(e -> {
            String curr = scheduleSelector.getValue(); if (curr == null) return;
            TextInputDialog tid = new TextInputDialog(curr); tid.setTitle("Перейменування"); tid.setHeaderText("Нова назва:");
            tid.showAndWait().ifPresent(newName -> { if (!newName.equals(curr) && mainApp.getInternalSchedules().stream().noneMatch(ds -> ds.getName().equalsIgnoreCase(newName))) {
                mainApp.getInternalSchedules().stream().filter(d -> d.getName().equals(curr)).findFirst().ifPresent(d -> d.setName(newName));
                refreshBells.run(); scheduleSelector.setValue(newName);
                mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules()); mainApp.refreshScheduleOptions();
            }});
        });

        Button deleteBtn = new Button("ВИДАЛИТИ"); deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 20;");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> {
            String name = scheduleSelector.getValue(); if (name != null) {
                new Alert(Alert.AlertType.CONFIRMATION, "Видалити '" + name + "'?", ButtonType.YES, ButtonType.NO).showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) { mainApp.getInternalSchedules().removeIf(ds -> ds.getName().equals(name)); refreshBells.run(); mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules()); mainApp.refreshScheduleOptions(); }
                });
            }
        });

        VBox editorContainer = new VBox(0);
        editorContainer.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #dcdde1;");
        editorContainer.setMaxWidth(650);
        
        Button saveBtn = new Button("ЗБЕРЕГТИ ПАРАМЕТРИ ДЗВІНКІВ");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 80; -fx-background-radius: 12;");
        saveBtn.setDisable(true);

        scheduleSelector.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) { saveBtn.setDisable(true); renameBtn.setDisable(true); deleteBtn.setDisable(true); return; }
            saveBtn.setDisable(false); renameBtn.setDisable(false); deleteBtn.setDisable(false);
            editorContainer.getChildren().clear();
            DaySchedule ds = mainApp.getInternalSchedules().stream().filter(d -> d.getName().equals(newV)).findFirst().get();
            HBox header = new HBox(10, new Label("УРОК"), new Label("ПОЧАТОК"), new Region(), new Label("КІНЕЦЬ"));
            header.setPadding(new Insets(15, 20, 15, 20)); header.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15 15 0 0; -fx-border-color: transparent transparent #dcdde1 transparent;");
            ((Label)header.getChildren().get(0)).setStyle(HEADER_STYLE); ((Label)header.getChildren().get(0)).setPrefWidth(60);
            ((Label)header.getChildren().get(1)).setStyle(HEADER_STYLE);
            ((Label)header.getChildren().get(3)).setStyle(HEADER_STYLE);
            HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS); editorContainer.getChildren().add(header);
            
            for (int i = 0; i < 7; i++) {
                final int curIdx = i; DaySchedule.LessonInfo li = ds.getLessons().get(i);
                ComboBox<String> sh = mainApp.createTimeCombo(24, li.start != null ? li.start.getHour() : 8);
                ComboBox<String> sm = mainApp.createTimeCombo(60, li.start != null ? li.start.getMinute() : 0);
                ComboBox<String> eh = mainApp.createTimeCombo(24, li.end != null ? li.end.getHour() : 8);
                ComboBox<String> em = mainApp.createTimeCombo(60, li.end != null ? li.end.getMinute() : 45);
                Label lessonNum = new Label((i + 1) + " урок"); lessonNum.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;"); lessonNum.setPrefWidth(60);
                HBox lessonRow = new HBox(8, lessonNum, sh, new Label(":"), sm, new Region(), eh, new Label(":"), em);
                lessonRow.setAlignment(Pos.CENTER_LEFT); lessonRow.setPadding(new Insets(10, 20, 10, 20));
                if (i % 2 != 0) lessonRow.setStyle("-fx-background-color: #fafafa;");
                HBox.setHgrow(lessonRow.getChildren().get(4), Priority.ALWAYS);
                editorContainer.getChildren().add(lessonRow);
                if (i < 6) {
                    TextField breakF = new TextField(String.valueOf(li.breakAfterMinutes)); breakF.setPrefWidth(45); breakF.setAlignment(Pos.CENTER);
                    breakF.setStyle("-fx-font-size: 11px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 10; -fx-border-color: #bdc3c7; -fx-border-radius: 10; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 2 5;");
                    Label bLabel = new Label("перерва"); bLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #0984e3; -fx-font-weight: bold;");
                    HBox breakRow = new HBox(8, new Region(), bLabel, breakF, new Label("хв"), new Region());
                    breakRow.setAlignment(Pos.CENTER); breakRow.setPadding(new Insets(2, 0, 2, 0)); breakRow.setStyle("-fx-background-color: rgba(9, 132, 227, 0.05);");
                    editorContainer.getChildren().add(breakRow);
                }
            }
        });

        saveBtn.setOnAction(e -> {
            String name = scheduleSelector.getValue(); DaySchedule ds = mainApp.getInternalSchedules().stream().filter(d -> d.getName().equals(name)).findFirst().get();
            List<DaySchedule.LessonInfo> newLessons = new ArrayList<>();
            try {
                int rowIdx = 1;
                for (int i = 0; i < 7; i++) {
                    HBox lRow = (HBox) editorContainer.getChildren().get(rowIdx);
                    ComboBox<String> sh = (ComboBox<String>) lRow.getChildren().get(1); ComboBox<String> sm = (ComboBox<String>) lRow.getChildren().get(3);
                    ComboBox<String> eh = (ComboBox<String>) lRow.getChildren().get(5); ComboBox<String> em = (ComboBox<String>) lRow.getChildren().get(7);
                    int bVal = 0; if (i < 6) { bVal = Integer.parseInt(((TextField)((HBox)editorContainer.getChildren().get(rowIdx+1)).getChildren().get(2)).getText()); }
                    newLessons.add(new DaySchedule.LessonInfo(LocalTime.of(Integer.parseInt(sh.getValue()), Integer.parseInt(sm.getValue())), LocalTime.of(Integer.parseInt(eh.getValue()), Integer.parseInt(em.getValue())), bVal));
                    rowIdx += 2;
                }
                ds.setLessons(newLessons); mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules()); mainApp.refreshScheduleOptions();
                mainApp.addLog("Розклад '" + name + "' збережено.", "SUCCESS");
            } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Помилка збереження!").show(); }
        });

        HBox actions = new HBox(12, addBtn, renameBtn, new Region(), deleteBtn); actions.setMaxWidth(650); HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);
        VBox mainV = new VBox(20, headerArea, scheduleSelector, actions, editorContainer, saveBtn);
        mainV.setPadding(new Insets(25)); mainV.setStyle("-fx-background-color: white;");
        mainV.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(mainV); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        refreshBells.run(); tab.setContent(scroll);
        return tab;
    }

    private Tab createTeachersTab() {
        Tab tab = new Tab("👨‍🏫 ВЧИТЕЛІ"); tab.setClosable(false);
        VBox content = new VBox(20); content.setPadding(new Insets(25)); content.setStyle("-fx-background-color: white;");
        VBox headerArea = createSectionHeader("Керування викладацьким складом", "#0984e3", ICON_PERSON);
        TextField addField = new TextField(); addField.setPromptText("Введіть ПІБ вчителя (наприклад: Поліщук Олена Василівна)..."); 
        addField.setStyle(COMBO_STYLE); addField.setPrefWidth(550);
        Button addBtn = new Button("ДОДАТИ ВЧИТЕЛЯ"); addBtn.setStyle("-fx-background-color: #0984e3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        FlowPane cardsContainer = new FlowPane(20, 20); cardsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(cardsContainer); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshTeachers = () -> {
            cardsContainer.getChildren().clear();
            List<Subject> allSubs = mainApp.getAcademicService().getAllSubjects();
            for (Teacher t : mainApp.getAcademicService().getAllTeachers()) {
                VBox card = new VBox(12); card.getStyleClass().add("teacher-card"); card.setPrefWidth(320);
                HBox header = new HBox(5); header.setAlignment(Pos.CENTER_LEFT);
                StackPane nameStack = new StackPane();
                Label nameLabel = new Label(t.name()); nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;"); nameLabel.setWrapText(true); nameLabel.setMaxWidth(260);
                TextField nameEdit = new TextField(t.name()); nameEdit.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); nameEdit.setVisible(false);
                nameStack.getChildren().addAll(nameLabel, nameEdit); StackPane.setAlignment(nameLabel, Pos.CENTER_LEFT); HBox.setHgrow(nameStack, Priority.ALWAYS);
                nameLabel.setOnMouseClicked(e -> { nameLabel.setVisible(false); nameEdit.setVisible(true); nameEdit.requestFocus(); });
                nameEdit.focusedProperty().addListener((obs, ov, nv) -> { 
                    if (!nv) { if (!nameEdit.getText().equals(t.name()) && !nameEdit.getText().isEmpty()) { mainApp.getAcademicService().updateTeacher(t.id(), nameEdit.getText()); refreshTeachers.run(); } else { nameEdit.setVisible(false); nameLabel.setVisible(true); } }
                });
                Button del = new Button("✕"); del.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff7675; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 16px;");
                del.setMinWidth(Region.USE_PREF_SIZE);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteTeacher(t.id()); refreshTeachers.run(); });
                header.getChildren().addAll(nameStack, del);
                FlowPane chips = new FlowPane(6, 6);
                for (Subject sub : mainApp.getAcademicService().getSubjectsForTeacher(t.id())) {
                    Label chip = new Label(sub.name() + " ✕"); chip.getStyleClass().add("subject-chip");
                    chip.setOnMouseClicked(e -> { mainApp.getAcademicService().unlinkTeacherFromSubject(t.id(), sub.id()); refreshTeachers.run(); });
                    chips.getChildren().add(chip);
                }
                ComboBox<Subject> picker = new ComboBox<>(); picker.setPromptText("+ додати предмет"); picker.getItems().setAll(allSubs); picker.setMaxWidth(Double.MAX_VALUE);
                picker.setStyle("-fx-font-size: 11px; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d1e3ff;");
                picker.setOnAction(e -> { if (picker.getValue() != null) { mainApp.getAcademicService().linkTeacherToSubject(t.id(), picker.getValue().id()); refreshTeachers.run(); } });
                card.getChildren().addAll(header, chips, picker); cardsContainer.getChildren().add(card);
            }
        };

        addField.textProperty().addListener((obs, ov, nv) -> refreshTeachers.run());

        addBtn.setOnAction(e -> { 
            if (!addField.getText().isEmpty()) { 
                mainApp.getAcademicService().addTeacher(addField.getText().trim()); 
                addField.clear(); 
                refreshTeachers.run(); 
            } 
        });
        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshTeachers.run(); tab.setContent(content);
        return tab;
    }

    private Tab createSubjectsTab() {
        Tab tab = new Tab("📚 ПРЕДМЕТИ"); tab.setClosable(false);
        VBox content = new VBox(20); content.setPadding(new Insets(25)); content.setStyle("-fx-background-color: white;");
        VBox headerArea = createSectionHeader("Перелік навчальних дисциплін", "#00b894", ICON_BOOK);
        TextField addField = new TextField(); addField.setPromptText("Введіть назву предмета..."); addField.setStyle(COMBO_STYLE); addField.setPrefWidth(550);
        Button addBtn = new Button("ДОДАТИ ПРЕДМЕТ"); addBtn.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");
        FlowPane subjectsContainer = new FlowPane(15, 15); subjectsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(subjectsContainer); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshSubjects = () -> {
            subjectsContainer.getChildren().clear();
            for (Subject s : mainApp.getAcademicService().getAllSubjects()) {
                VBox card = new VBox(8); card.getStyleClass().add("subject-card"); card.setPrefWidth(240);
                HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
                TextField edit = new TextField(s.name()); edit.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0;"); HBox.setHgrow(edit, Priority.ALWAYS);
                edit.focusedProperty().addListener((obs, ov, nv) -> { if (!nv && !edit.getText().equals(s.name()) && !edit.getText().isEmpty()) { mainApp.getAcademicService().updateSubject(s.id(), edit.getText()); refreshSubjects.run(); } });
                Button del = new Button("✕"); del.setStyle("-fx-text-fill: #ff7675; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 16px;");
                del.setMinWidth(Region.USE_PREF_SIZE);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteSubject(s.id()); refreshSubjects.run(); });
                header.getChildren().addAll(createSVGIcon(ICON_BOOK, Color.web("#00b894"), 16), edit, del);
                card.getChildren().add(header); subjectsContainer.getChildren().add(card);
            }
        };

        addBtn.setOnAction(e -> { if (!addField.getText().isEmpty()) { mainApp.getAcademicService().addSubject(addField.getText()); addField.clear(); refreshSubjects.run(); } });
        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshSubjects.run(); tab.setContent(content);
        return tab;
    }

    private Tab createClassesTab() {
        Tab tab = new Tab("🏫 КЛАСИ"); tab.setClosable(false);
        VBox content = new VBox(20); content.setPadding(new Insets(25)); content.setStyle("-fx-background-color: white;");
        VBox headerArea = createSectionHeader("Шкільні класи та паралелі", "#6c5ce7", ICON_CLASS);
        TextField addField = new TextField(); addField.setPromptText("Назва класу (напр. 5-А)..."); addField.setStyle(COMBO_STYLE); addField.setPrefWidth(550);
        Button addBtn = new Button("ДОДАТИ КЛАС"); addBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8;");
        FlowPane listContainer = new FlowPane(15, 15); listContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(listContainer); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshClasses = () -> {
            listContainer.getChildren().clear();
            for (SchoolClass c : mainApp.getAcademicService().getAllClasses()) {
                VBox card = new VBox(10); card.getStyleClass().add("class-card"); card.setPrefWidth(120);
                HBox box = new HBox(5); box.setAlignment(Pos.CENTER_LEFT);
                TextField edit = new TextField(c.name()); edit.setPrefWidth(60); edit.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 0;"); HBox.setHgrow(edit, Priority.ALWAYS);
                edit.focusedProperty().addListener((obs, ov, nv) -> { if (!nv && !edit.getText().equals(c.name()) && !edit.getText().isEmpty()) { mainApp.getAcademicService().updateClass(c.id(), edit.getText()); refreshClasses.run(); } });
                Button del = new Button("✕"); del.setStyle("-fx-text-fill: #ff7675; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 16px;");
                del.setMinWidth(Region.USE_PREF_SIZE);
                del.setOnAction(e -> { mainApp.getAcademicService().deleteClass(c.id()); refreshClasses.run(); });
                box.getChildren().addAll(edit, del); card.getChildren().add(box); listContainer.getChildren().add(card);
            }
        };
        addBtn.setOnAction(e -> { if (!addField.getText().isEmpty()) { mainApp.getAcademicService().addClass(addField.getText()); addField.clear(); refreshClasses.run(); } });
        content.getChildren().addAll(headerArea, new HBox(15, addField, addBtn), scroll);
        refreshClasses.run(); tab.setContent(content);
        return tab;
    }

    private Tab createWeeklyScheduleTab() {
        Tab tab = new Tab("📅 РОЗКЛАД КЛАСІВ");
        tab.setClosable(false);

        VBox root = new VBox(25);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white;");

        VBox headerArea = createSectionHeader("Тижневий розклад занять", "#27ae60", ICON_CALENDAR);

        // Enhanced Class Picker Area
        VBox pickerArea = new VBox(8);
        Label pickerLabel = new Label("ОБЕРІТЬ КЛАС ДЛЯ РЕДАГУВАННЯ");
        pickerLabel.setStyle(HEADER_STYLE);
        
        ComboBox<SchoolClass> classPicker = new ComboBox<>();
        classPicker.setPromptText("Натисніть тут, щоб обрати клас...");
        classPicker.setPrefWidth(450);
        classPicker.setStyle(COMBO_STYLE);
        
        HBox pickerBox = new HBox(12, createSVGIcon(ICON_CLASS, Color.web("#6c5ce7"), 18), classPicker);
        pickerBox.setAlignment(Pos.CENTER_LEFT);
        
        pickerArea.getChildren().addAll(pickerLabel, pickerBox);

        // Info banner for parity
        HBox infoBanner = new HBox(10);
        infoBanner.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 10 15; -fx-background-radius: 10; -fx-border-color: #dfe6e9; -fx-border-radius: 10;");
        infoBanner.setAlignment(Pos.CENTER_LEFT);
        Label infoText = new Label("Порада: Для уроків, що чергуються, оберіть 'Парний/Непарний тиждень' у параметрах заняття.");
        infoText.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        infoBanner.getChildren().addAll(createSVGIcon(ICON_BOOK, Color.web("#0984e3"), 14), infoText);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(10, 0, 10, 0));

        final Runnable[] refreshGrid = {null};
        refreshGrid[0] = () -> {
            SchoolClass selectedClass = classPicker.getValue();
            grid.getChildren().clear();
            if (selectedClass == null) return;

            String[] days = {"ПОНЕДІЛОК", "ВІВТОРОК", "СЕРЕДА", "ЧЕТВЕР", "П'ЯТНИЦЯ"};
            for (int i = 0; i < 5; i++) {
                Label dayLabel = new Label(days[i]);
                dayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #636e72; -fx-font-size: 11px;");
                HBox dayBox = new HBox(dayLabel);
                dayBox.setAlignment(Pos.CENTER);
                dayBox.setPadding(new Insets(5, 0, 10, 0));
                grid.add(dayBox, i, 0);
            }

            List<ScheduleEntry> entries = mainApp.getAcademicService().getScheduleForClass(selectedClass.id());
            List<Teacher> allTeachers = mainApp.getAcademicService().getAllTeachers();
            List<Subject> allSubjects = mainApp.getAcademicService().getAllSubjects();
            
            for (int d = 1; d <= 5; d++) {
                final int dayNum = d;
                List<ScheduleEntry> dayEntries = entries.stream()
                        .filter(e -> e.dayOfWeek() == dayNum)
                        .sorted((e1, e2) -> Integer.compare(e1.lessonNumber(), e2.lessonNumber()))
                        .toList();

                int maxLessonInDay = dayEntries.stream()
                        .mapToInt(ScheduleEntry::lessonNumber)
                        .max().orElse(0);
                int lessonsToShow = Math.max(maxLessonInDay, 5);
                
                VBox column = new VBox(15);
                column.setAlignment(Pos.TOP_CENTER);
                
                for (int l = 1; l <= lessonsToShow; l++) {
                    final int lessonNum = l;
                    List<ScheduleEntry> slotEntries = dayEntries.stream()
                            .filter(e -> e.lessonNumber() == lessonNum)
                            .toList();

                    VBox cardContainer = new VBox(2);
                    cardContainer.setAlignment(Pos.CENTER_LEFT);
                    
                    Label numLabel = new Label(lessonNum + " урок");
                    numLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
                    
                    Node card = createEnhancedCard(selectedClass, dayNum, lessonNum, slotEntries, allTeachers, allSubjects, refreshGrid[0]);
                    cardContainer.getChildren().addAll(numLabel, card);
                    column.getChildren().add(cardContainer);
                }

                Button addLessonBtn = new Button("+ ДОДАТИ УРОК");
                addLessonBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0984e3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #0984e3; -fx-border-radius: 8; -fx-border-style: dashed; -fx-padding: 8 20;");
                addLessonBtn.setMaxWidth(Double.MAX_VALUE);
                addLessonBtn.setCursor(Cursor.HAND);
                addLessonBtn.setOnAction(e -> {
                    int targetLesson = lessonsToShow + 1;
                    openEditDialog(selectedClass, dayNum, targetLesson, 0, allTeachers, allSubjects, refreshGrid[0]);
                });
                
                column.getChildren().add(addLessonBtn);
                grid.add(column, d - 1, 1);
            }
        };

        classPicker.valueProperty().addListener((obs, ov, nv) -> refreshGrid[0].run());
        
        refreshWeekly = () -> {
            SchoolClass currentSelection = classPicker.getValue();
            List<SchoolClass> allSelected = mainApp.getAcademicService().getAllClasses();
            classPicker.getItems().setAll(allSelected);
            if (currentSelection != null) {
                allSelected.stream()
                        .filter(c -> c.id() == currentSelection.id())
                        .findFirst()
                        .ifPresent(classPicker::setValue);
            }
            refreshGrid[0].run();
        };

        classPicker.getItems().setAll(mainApp.getAcademicService().getAllClasses());

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.getChildren().addAll(headerArea, pickerArea, infoBanner, scroll);
        tab.setContent(root);
        return tab;
    }

    private Node createEnhancedCard(SchoolClass cls, int day, int lesson, List<ScheduleEntry> entries, 
                                        List<Teacher> teachers, List<Subject> subjects, Runnable refreshGrid) {
        
        if (entries.isEmpty()) {
            return createSingleCard(cls, day, lesson, null, teachers, subjects, 0, refreshGrid);
        }

        ScheduleEntry allEntry = entries.stream().filter(e -> e.parity() == 0).findFirst().orElse(null);
        if (allEntry != null) {
            return createSingleCard(cls, day, lesson, allEntry, teachers, subjects, 0, refreshGrid);
        }

        ScheduleEntry oddEntry = entries.stream().filter(e -> e.parity() == 1).findFirst().orElse(null);
        ScheduleEntry evenEntry = entries.stream().filter(e -> e.parity() == 2).findFirst().orElse(null);

        HBox split = new HBox(0);
        split.setPrefWidth(165);
        Node left = createParityCard(cls, day, lesson, oddEntry, teachers, subjects, 1, refreshGrid);
        Node right = createParityCard(cls, day, lesson, evenEntry, teachers, subjects, 2, refreshGrid);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        split.getChildren().addAll(left, right);
        return split;
    }

    private StackPane createParityCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, 
                                      List<Teacher> teachers, List<Subject> subjects, int parity, Runnable refreshGrid) {
        StackPane card = createSingleCard(cls, day, lesson, entry, teachers, subjects, parity, refreshGrid);
        card.setPrefWidth(82);
        
        VBox content = (VBox) card.getChildren().get(0);
        content.setPadding(new Insets(8, 5, 8, 5));
        Label pLabel = new Label(parity == 1 ? "НЕПАРНИЙ" : "ПАРНИЙ");
        pLabel.setStyle("-fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: " + (parity == 1 ? "#0984e3" : "#6c5ce7") + ";");
        content.getChildren().add(0, pLabel);
        
        return card;
    }

    private StackPane createSingleCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, 
                                        List<Teacher> teachers, List<Subject> subjects, int parity, Runnable refreshGrid) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(12, 15, 12, 15));
        content.setPrefSize(165, 80);
        content.setAlignment(Pos.CENTER_LEFT);

        Label subLabel = new Label("—");
        subLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #2d3436;");
        subLabel.setWrapText(true);

        Label teaLabel = new Label("");
        teaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #636e72; -fx-font-style: italic;");
        teaLabel.setWrapText(true);

        String tempColor = "#dfe6e9";
        if (entry != null) {
            Subject s = subjects.stream().filter(x -> x.id() == entry.subjectId()).findFirst().orElse(null);
            Teacher t = teachers.stream().filter(x -> x.id() == entry.teacherId()).findFirst().orElse(null);
            if (s != null) {
                subLabel.setText(s.name());
                tempColor = getSubjectColor(s.id());
            }
            if (t != null) teaLabel.setText(t.name());
        }
        final String accentColor = tempColor;

        content.getChildren().addAll(subLabel, teaLabel);

        StackPane card = new StackPane(content);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dcdde1; -fx-border-radius: 12; -fx-border-width: 1 1 1 4; -fx-border-color: #dcdde1 #dcdde1 #dcdde1 " + accentColor + ";");
        card.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.05), 5, 0, 0, 2));

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: #ff7675; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 5;");
        clearBtn.setVisible(false);
        clearBtn.setCursor(Cursor.HAND);
        StackPane.setAlignment(clearBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(clearBtn, new Insets(5));
        
        clearBtn.setOnAction(e -> {
            mainApp.getAcademicService().deleteScheduleEntry(cls.id(), day, lesson, parity);
            refreshGrid.run();
            e.consume();
        });

        card.getChildren().add(clearBtn);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 12; -fx-border-color: " + accentColor + "; -fx-border-radius: 12; -fx-border-width: 1 1 1 6;");
            if (entry != null) clearBtn.setVisible(true);
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #dcdde1 #dcdde1 #dcdde1 " + accentColor + "; -fx-border-radius: 12; -fx-border-width: 1 1 1 4;");
            clearBtn.setVisible(false);
        });

        card.setOnMouseClicked(e -> openEditDialog(cls, day, lesson, parity, teachers, subjects, refreshGrid));
        
        return card;
    }

    private String getSubjectColor(int id) {
        String[] palette = {"#0984e3", "#00b894", "#6c5ce7", "#e17055", "#fdcb6e", "#e84393", "#2d3436", "#17c0eb", "#3ae374", "#ffb8b8"};
        return palette[Math.abs(id) % palette.length];
    }

    private void openEditDialog(SchoolClass schoolClass, int day, int lesson, int parity,
                                List<Teacher> allTeachers, List<Subject> allSubjects, Runnable refreshGrid) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Редагування уроку");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: white;");

        VBox header = createSectionHeader("Параметри заняття", "#0984e3", ICON_BOOK);
        
        ComboBox<Teacher> teacherBox = new ComboBox<>();
        teacherBox.setPromptText("Оберіть викладача...");
        teacherBox.getItems().setAll(allTeachers);
        teacherBox.setPrefWidth(400);
        teacherBox.setStyle(COMBO_STYLE);

        ComboBox<Subject> subjectBox = new ComboBox<>();
        subjectBox.setPromptText("Оберіть предмет...");
        subjectBox.setPrefWidth(400);
        subjectBox.setStyle(COMBO_STYLE);

        ComboBox<String> parityBox = new ComboBox<>();
        parityBox.getItems().addAll("Кожен тиждень", "Непарний тиждень", "Парний тиждень");
        parityBox.setValue(parity == 1 ? "Непарний тиждень" : parity == 2 ? "Парний тиждень" : "Кожен тиждень");
        parityBox.setPrefWidth(400);
        parityBox.setStyle(COMBO_STYLE);

        teacherBox.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                subjectBox.getItems().setAll(mainApp.getAcademicService().getSubjectsForTeacher(nv.id()));
            }
        });

        // Initialize values if exist
        List<ScheduleEntry> current = mainApp.getAcademicService().getScheduleForClass(schoolClass.id());
        current.stream().filter(e -> e.dayOfWeek() == day && e.lessonNumber() == lesson && (parity == 0 || e.parity() == parity))
                .findFirst().ifPresent(e -> {
            teacherBox.getItems().stream().filter(t -> t.id() == e.teacherId()).findFirst().ifPresent(teacherBox::setValue);
            subjectBox.getItems().stream().filter(s -> s.id() == e.subjectId()).findFirst().ifPresent(subjectBox::setValue);
            parityBox.setValue(e.parity() == 1 ? "Непарний тиждень" : e.parity() == 2 ? "Парний тиждень" : "Кожен тиждень");
        });

        Button saveBtn = new Button("ЗБЕРЕГТИ ЗМІНИ");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 10;");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (teacherBox.getValue() != null && subjectBox.getValue() != null) {
                int selectedParity = switch (parityBox.getValue()) {
                    case "Непарний тиждень" -> 1;
                    case "Парний тиждень" -> 2;
                    default -> 0;
                };
                mainApp.getAcademicService().saveScheduleEntry(schoolClass.id(), day, lesson, 
                                teacherBox.getValue().id(), subjectBox.getValue().id(), selectedParity);
                refreshGrid.run();
                dialog.close();
            }
        });

        Button deleteBtn = new Button("ВИДАЛИТИ УРОК");
        deleteBtn.setStyle("-fx-background-color: #ff7675; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 40; -fx-background-radius: 10;");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> {
            int selectedParity = switch (parityBox.getValue()) {
                case "Непарний тиждень" -> 1;
                case "Парний тиждень" -> 2;
                default -> 0;
            };
            mainApp.getAcademicService().deleteScheduleEntry(schoolClass.id(), day, lesson, selectedParity);
            refreshGrid.run();
            dialog.close();
        });

        VBox fields = new VBox(15, 
            new VBox(5, new Label("ВИКЛАДАЧ"), teacherBox),
            new VBox(5, new Label("НАВЧАЛЬНА ДИСЦИПЛІНА"), subjectBox),
            new VBox(5, new Label("ПЕРІОДИЧНІСТЬ"), parityBox)
        );
        fields.getChildren().forEach(n -> {
            if (n instanceof VBox vb) ((Label)vb.getChildren().get(0)).setStyle(HEADER_STYLE);
        });

        root.getChildren().addAll(header, fields, saveBtn, deleteBtn);
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }
}
