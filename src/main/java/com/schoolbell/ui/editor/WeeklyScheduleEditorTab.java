package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.ScheduleEditorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class WeeklyScheduleEditorTab {
    private final MainApp mainApp;
    private final ScheduleEditorDialog parentDialog;
    private Runnable refreshWeekly;

    public WeeklyScheduleEditorTab(MainApp mainApp, ScheduleEditorDialog parentDialog) {
        this.mainApp = mainApp;
        this.parentDialog = parentDialog;
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Тижневий розклад занять", "Складайте навчальний план для кожного класу", "#27ae60", ICON_CALENDAR);
        
        HBox pickerCard = new HBox(20);
        pickerCard.setPadding(new Insets(20, 25, 20, 25));
        pickerCard.setAlignment(Pos.CENTER_LEFT);
        pickerCard.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 10, 0, 0, 4); -fx-border-color: #f1f2f6; -fx-border-radius: 16;");

        Label pickerLabel = new Label("ОБЕРІТЬ КЛАС:");
        pickerLabel.setStyle(HEADER_STYLE);
        
        ComboBox<SchoolClass> classPicker = new ComboBox<>();
        classPicker.setPromptText("Натисніть тут, щоб обрати клас для редагування...");
        classPicker.setPrefWidth(450);
        classPicker.setStyle(COMBO_STYLE);
        
        HBox pickerBox = new HBox(12, createSVGIcon(ICON_CLASS, Color.web("#6c5ce7"), 18), classPicker);
        pickerBox.setAlignment(Pos.CENTER_LEFT);
        
        pickerCard.getChildren().addAll(pickerLabel, pickerBox);

        HBox contentLayout = new HBox(25);
        VBox.setVgrow(contentLayout, Priority.ALWAYS);

        VBox scheduleCard = new VBox(15);
        scheduleCard.setPadding(new Insets(25));
        scheduleCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 15, 0, 0, 5); -fx-border-color: #f1f2f6; -fx-border-radius: 20;");
        HBox.setHgrow(scheduleCard, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        scheduleCard.getChildren().add(scroll);

        VBox rightColumn = createSideHelpPanel(
            createHelpCard(ICON_BOOK, "Парність тижнів", "Для уроків, що чергуються (чисельник/знаменник), оберіть відповідну опцію у вікні редагування.", "#0984e3"),
            createHelpCard(ICON_INFO, "Швидке редагування", "Натисніть на картку уроку, щоб змінити вчителя або предмет. Натисніть ✕ на картці для швидкого видалення.", "#6c5ce7")
        );

        contentLayout.getChildren().addAll(scheduleCard, rightColumn);
        root.getChildren().addAll(headerArea, pickerCard, contentLayout);

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
                dayBox.setAlignment(Pos.CENTER_LEFT);
                dayBox.setPadding(new Insets(5, 0, 10, 0));
                grid.add(dayBox, i, 0);
            }
            
            List<ScheduleEntry> entries = mainApp.getAcademicService().getScheduleForClass(selectedClass.id());
            List<Teacher> allTeachers = mainApp.getAcademicService().getAllTeachers();
            List<Subject> allSubjects = mainApp.getAcademicService().getAllSubjects();
            List<Classroom> allClassrooms = mainApp.getAcademicService().getAllClassrooms();

            for (int d = 1; d <= 5; d++) {
                final int dayNum = d;
                List<ScheduleEntry> dayEntries = entries.stream()
                        .filter(e -> e.dayOfWeek() == dayNum)
                        .sorted((e1, e2) -> Integer.compare(e1.lessonNumber(), e2.lessonNumber()))
                        .toList();

                int maxLessonInDay = dayEntries.stream().mapToInt(ScheduleEntry::lessonNumber).max().orElse(0);
                int lessonsToShow = Math.max(maxLessonInDay, 5);

                VBox column = new VBox(15);
                column.setAlignment(Pos.TOP_LEFT);

                for (int l = 1; l <= lessonsToShow; l++) {
                    final int lessonNum = l;
                    List<ScheduleEntry> slotEntries = dayEntries.stream()
                            .filter(e -> e.lessonNumber() == lessonNum)
                            .toList();

                    VBox cardContainer = new VBox(2);
                    cardContainer.setAlignment(Pos.CENTER_LEFT);
                    Label numLabel = new Label(lessonNum + " урок");
                    numLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");

                    Node card = createEnhancedCard(selectedClass, dayNum, lessonNum, slotEntries, allTeachers, allSubjects, allClassrooms, refreshGrid[0]);
                    cardContainer.getChildren().addAll(numLabel, card);
                    column.getChildren().add(cardContainer);
                }

                Button addLessonBtn = new Button("+ ДОДАТИ УРОК");
                addLessonBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0984e3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #0984e3; -fx-border-radius: 8; -fx-border-style: dashed; -fx-padding: 8 20;");
                addLessonBtn.setMaxWidth(Double.MAX_VALUE);
                addLessonBtn.setCursor(Cursor.HAND);
                addLessonBtn.setOnAction(e -> parentDialog.openEditDialog(selectedClass, dayNum, lessonsToShow + 1, 0, allTeachers, allSubjects, refreshGrid[0]));
                
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
                allSelected.stream().filter(c -> c.id() == currentSelection.id()).findFirst().ifPresent(classPicker::setValue);
            }
            refreshGrid[0].run();
        };

        classPicker.getItems().setAll(mainApp.getAcademicService().getAllClasses());
        refreshGrid[0].run();

        return root;
    }

    private Node createEnhancedCard(SchoolClass cls, int day, int lesson, List<ScheduleEntry> entries, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, Runnable refreshGrid) {
        if (entries.isEmpty()) return createSingleCard(cls, day, lesson, null, teachers, subjects, classrooms, 0, refreshGrid);
        ScheduleEntry allEntry = entries.stream().filter(e -> e.parity() == 0).findFirst().orElse(null);
        if (allEntry != null) return createSingleCard(cls, day, lesson, allEntry, teachers, subjects, classrooms, 0, refreshGrid);
        ScheduleEntry oddEntry = entries.stream().filter(e -> e.parity() == 1).findFirst().orElse(null);
        ScheduleEntry evenEntry = entries.stream().filter(e -> e.parity() == 2).findFirst().orElse(null);
        
        HBox split = new HBox(0); 
        split.setPrefWidth(200);
        Node left = createParityCard(cls, day, lesson, oddEntry, teachers, subjects, classrooms, 1, refreshGrid);
        Node right = createParityCard(cls, day, lesson, evenEntry, teachers, subjects, classrooms, 2, refreshGrid);
        HBox.setHgrow(left, Priority.ALWAYS); 
        HBox.setHgrow(right, Priority.ALWAYS);
        split.getChildren().addAll(left, right); 
        return split;
    }

    private StackPane createParityCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, int parity, Runnable refreshGrid) {
        StackPane card = createSingleCard(cls, day, lesson, entry, teachers, subjects, classrooms, parity, refreshGrid);
        card.setPrefWidth(100);
        VBox content = (VBox) card.getChildren().get(0);
        content.setPadding(new Insets(10, 8, 10, 8));
        Label pLabel = new Label(parity == 1 ? "ЧИСЕЛЬНИК" : "ЗНАМЕННИК");
        pLabel.setStyle("-fx-font-size: 7px; -fx-font-weight: 900; -fx-text-fill: " + (parity == 1 ? "#0984e3" : "#6c5ce7") + "; -fx-letter-spacing: 0.5px;");
        content.getChildren().add(0, pLabel);
        return card;
    }

    private StackPane createSingleCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, int parity, Runnable refreshGrid) {
        VBox content = new VBox(4); 
        content.setPadding(new Insets(12, 15, 12, 15)); 
        content.setPrefSize(200, 95); 
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label subLabel = new Label("—"); 
        subLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;"); 
        subLabel.setWrapText(true);
        subLabel.setMaxHeight(40);
        
        Label teaLabel = new Label(""); 
        teaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #636e72;"); 
        teaLabel.setWrapText(true);
        
        String tempColor = "#dfe6e9";
        if (entry != null) {
            Subject s = subjects.stream().filter(x -> x.id() == entry.subjectId()).findFirst().orElse(null);
            Teacher t = teachers.stream().filter(x -> x.id() == entry.teacherId()).findFirst().orElse(null);
            if (s != null) { subLabel.setText(s.name()); tempColor = getSubjectColor(s.id()); }
            if (t != null) teaLabel.setText(t.name());
        }
        final String accentColor = tempColor;
        
        if (entry != null && entry.classroomId() > 0) {
            classrooms.stream().filter(c -> c.id() == entry.classroomId()).findFirst().ifPresent(c -> {
                Label roomLabel = new Label(c.name());
                roomLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: " + accentColor + "; -fx-background-radius: 4; -fx-padding: 1 4;");
                HBox roomBox = new HBox(roomLabel);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                content.getChildren().addAll(subLabel, teaLabel, roomBox);
            });
        } else {
            content.getChildren().addAll(subLabel, teaLabel);
        }
        
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
        card.setOnMouseClicked(e -> parentDialog.openEditDialog(cls, day, lesson, parity, teachers, subjects, refreshGrid));
        
        return card;
    }

    private String getSubjectColor(int id) {
        String[] palette = {"#0984e3", "#00b894", "#6c5ce7", "#e17055", "#fdcb6e", "#e84393", "#2d3436", "#17c0eb", "#3ae374", "#ffb8b8"};
        return palette[Math.abs(id) % palette.length];
    }
}
