package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.LessonEditorDialog;
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

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.ControlFactory.createEmptyState;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
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
        root.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        HBox header = createPageHeader(
            "ПЛАНУВАННЯ",
            "Тижневий розклад",
            "Складайте та редагуйте навчальний план для кожного класу окремо.",
            ICON_CALENDAR,
            COLOR_GREEN,
            null
        );
        
        VBox pickerCard = new VBox(22);
        pickerCard.setPadding(new Insets(30));
        pickerCard.setPrefWidth(650);
        pickerCard.setStyle(SOFT_CARD);

        Label pickerLabel = new Label("НАВЧАЛЬНИЙ КЛАС");
        pickerLabel.setStyle(HEADER_STYLE);
        
        ComboBox<SchoolClass> classPicker = new ComboBox<>();
        classPicker.setPromptText("Натисніть тут, щоб обрати клас для редагування...");
        classPicker.setMaxWidth(Double.MAX_VALUE);
        classPicker.setStyle(PREMIUM_SELECT_STYLE);

        VBox iconWrap = new VBox(createSVGIcon(ICON_CLASS, Color.web(COLOR_PRIMARY), 24));
        iconWrap.setAlignment(Pos.CENTER);
        iconWrap.setPrefSize(54, 54);
        iconWrap.setMinSize(54, 54);
        iconWrap.setStyle(ICON_BADGE_STYLE);
        
        HBox row = new HBox(20, iconWrap, classPicker);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(classPicker, Priority.ALWAYS);

        Label note = new Label("Оберіть конкретний клас для налаштування його тижневого розкладу.");
        note.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: " + COLOR_SLATE + ";");
        
        pickerCard.getChildren().addAll(pickerLabel, row, note);

        HBox contentLayout = new HBox(25);
        VBox.setVgrow(contentLayout, Priority.ALWAYS);

        VBox scheduleCard = new VBox(20);
        scheduleCard.setPadding(new Insets(30));
        scheduleCard.setStyle(SOFT_CARD);
        HBox.setHgrow(scheduleCard, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setMaxWidth(Double.MAX_VALUE);

        scheduleCard.getChildren().add(grid);

        HBox helpRow = new HBox(25);
        helpRow.getChildren().addAll(
            createHelpCard(ICON_BOOK, "Парність тижнів", "Для уроків, що чергуються (чисельник/знаменник), оберіть відповідну опцію у вікні редагування.", COLOR_PRIMARY),
            createHelpCard(ICON_INFO, "Швидке редагування", "Натисніть на картку уроку, щоб змінити вчителя або предмет. Натисніть ✕ на картці для швидкого видалення.", COLOR_INDIGO_SOFT)
        );

        root.getChildren().addAll(header, pickerCard, scheduleCard, helpRow);

        final Runnable[] refreshGrid = {null};
        final int[] daysCount = {5};

        refreshGrid[0] = () -> {
            SchoolClass selectedClass = classPicker.getValue();
            scheduleCard.getChildren().clear();
            
            if (selectedClass == null) {
                VBox empty = createEmptyState(ICON_CLASS, "Оберіть клас", "Оберіть конкретний клас зі списку вище, щоб розпочати редагування його розкладу.");
                scheduleCard.setAlignment(Pos.CENTER);
                scheduleCard.getChildren().add(empty);
                return;
            }

            scheduleCard.setAlignment(Pos.TOP_LEFT);
            scheduleCard.getChildren().add(grid);
            grid.getChildren().clear();

            List<ScheduleEntry> entries = mainApp.getAcademicService().getScheduleForClass(selectedClass.id());
            
            // Determine how many days have entries
            int maxDayWithEntry = entries.stream().mapToInt(ScheduleEntry::dayOfWeek).max().orElse(5);
            int currentDays = Math.max(daysCount[0], maxDayWithEntry);
            daysCount[0] = currentDays;

            String[] dayNames = {"ПОНЕДІЛОК", "ВІВТОРОК", "СЕРЕДА", "ЧЕТВЕР", "П'ЯТНИЦЯ", "СУБОТА", "НЕДІЛЯ"};
            
            for (int i = 0; i < currentDays; i++) {
                Label dayLabel = new Label(dayNames[i]);
                dayLabel.setStyle(HEADER_STYLE);
                HBox dayBox = new HBox(dayLabel);
                dayBox.setAlignment(Pos.CENTER_LEFT);
                dayBox.setPadding(new Insets(5, 0, 10, 0));
                grid.add(dayBox, i, 0);
            }
            
            List<Teacher> allTeachers = mainApp.getStaffService().getAllTeachers();
            List<Subject> allSubjects = mainApp.getStaffService().getAllSubjects();
            List<Classroom> allClassrooms = mainApp.getAcademicService().getAllClassrooms();

            for (int d = 1; d <= currentDays; d++) {
                final int dayNum = d;
                List<ScheduleEntry> dayEntries = entries.stream()
                        .filter(e -> e.dayOfWeek() == dayNum)
                        .sorted((e1, e2) -> Integer.compare(e1.lessonNumber(), e2.lessonNumber()))
                        .toList();

                int maxLessonInDay = dayEntries.stream().mapToInt(ScheduleEntry::lessonNumber).max().orElse(0);
                int lessonsToShow = Math.max(maxLessonInDay, 5);

                VBox column = new VBox(20);
                column.setAlignment(Pos.TOP_LEFT);

                for (int l = 1; l <= lessonsToShow; l++) {
                    final int lessonNum = l;
                    List<ScheduleEntry> slotEntries = dayEntries.stream()
                            .filter(e -> e.lessonNumber() == lessonNum)
                            .toList();

                    VBox cardContainer = new VBox(8);
                    cardContainer.setAlignment(Pos.CENTER_LEFT);
                    Label numLabel = new Label(lessonNum + " УРОК");
                    numLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1px;");

                    Node card = createEnhancedCard(selectedClass, dayNum, lessonNum, slotEntries, allTeachers, allSubjects, allClassrooms, refreshGrid[0]);
                    cardContainer.getChildren().addAll(numLabel, card);
                    column.getChildren().add(cardContainer);
                }

                Button addLessonBtn = new Button("+ ДОДАТИ УРОК");
                addLessonBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: 900; -fx-border-color: " + COLOR_SLATE_MUTED + "; -fx-border-radius: 18; -fx-border-style: dashed; -fx-padding: 14 24; -fx-cursor: hand;");
                addLessonBtn.setMaxWidth(Double.MAX_VALUE);
                addLessonBtn.setOnAction(e -> new LessonEditorDialog(mainApp, selectedClass, dayNum, lessonsToShow + 1, 0, allTeachers, allSubjects, refreshGrid[0]).display());
                
                column.getChildren().add(addLessonBtn);
                grid.add(column, d - 1, 1);
            }

            if (currentDays < 7) {
                Button addDayBtn = new Button("+ ДОДАТИ ДЕНЬ");
                addDayBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: 900; -fx-border-color: " + COLOR_SLATE_MUTED + "; -fx-border-radius: 18; -fx-border-style: dashed; -fx-padding: 18 32; -fx-cursor: hand;");
                addDayBtn.setMaxHeight(Double.MAX_VALUE);
                addDayBtn.setPrefWidth(240);
                addDayBtn.setOnAction(e -> {
                    daysCount[0]++;
                    refreshGrid[0].run();
                });
                
                VBox addDayWrapper = new VBox(addDayBtn);
                addDayWrapper.setPadding(new Insets(42, 0, 0, 0)); 
                grid.add(addDayWrapper, currentDays, 1);
            }
        };

        classPicker.valueProperty().addListener((obs, ov, nv) -> {
            daysCount[0] = 5; // Reset to default when changing class
            refreshGrid[0].run();
        });

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

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        
        return scroll;
    }

    private Node createEnhancedCard(SchoolClass cls, int day, int lesson, List<ScheduleEntry> entries, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, Runnable refreshGrid) {
        if (entries.isEmpty()) return createSingleCard(cls, day, lesson, null, teachers, subjects, classrooms, 0, refreshGrid);
        ScheduleEntry allEntry = entries.stream().filter(e -> e.parity() == 0).findFirst().orElse(null);
        if (allEntry != null) return createSingleCard(cls, day, lesson, allEntry, teachers, subjects, classrooms, 0, refreshGrid);
        ScheduleEntry oddEntry = entries.stream().filter(e -> e.parity() == 1).findFirst().orElse(null);
        ScheduleEntry evenEntry = entries.stream().filter(e -> e.parity() == 2).findFirst().orElse(null);
        
        HBox split = new HBox(0); 
        split.setPrefWidth(240);
        Node left = createParityCard(cls, day, lesson, oddEntry, teachers, subjects, classrooms, 1, refreshGrid);
        Node right = createParityCard(cls, day, lesson, evenEntry, teachers, subjects, classrooms, 2, refreshGrid);
        HBox.setHgrow(left, Priority.ALWAYS); 
        HBox.setHgrow(right, Priority.ALWAYS);
        split.getChildren().addAll(left, right); 
        return split;
    }

    private StackPane createParityCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, int parity, Runnable refreshGrid) {
        StackPane card = createSingleCard(cls, day, lesson, entry, teachers, subjects, classrooms, parity, refreshGrid);
        card.setPrefWidth(120);
        VBox content = (VBox) card.getChildren().get(0);
        content.setPadding(new Insets(14, 12, 14, 12));
        Label pLabel = new Label(parity == 1 ? "ЧИСЕЛЬНИК" : "ЗНАМЕННИК");
        pLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: " + (parity == 1 ? COLOR_INDIGO : COLOR_INDIGO_SOFT) + "; -fx-letter-spacing: 0.5px;");
        content.getChildren().add(0, pLabel);
        return card;
    }

    private StackPane createSingleCard(SchoolClass cls, int day, int lesson, ScheduleEntry entry, List<Teacher> teachers, List<Subject> subjects, List<Classroom> classrooms, int parity, Runnable refreshGrid) {
        VBox content = new VBox(8); 
        content.setPadding(new Insets(18, 22, 18, 22)); 
        content.setPrefSize(240, 135); 
        content.setAlignment(Pos.CENTER_LEFT);
        
        Label subLabel = new Label("—"); 
        subLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: " + COLOR_NAVY + ";"); 
        subLabel.setWrapText(true);
        subLabel.setMaxHeight(55);
        
        Label teaLabel = new Label(""); 
        teaLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: 600;"); 
        teaLabel.setWrapText(true);
        teaLabel.setMinHeight(34);
        
        String tempColor = COLOR_SLATE_LIGHT;
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
                roomLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-background-color: " + accentColor + "; -fx-background-radius: 8; -fx-padding: 3 8; -fx-font-weight: 900;");
                HBox roomBox = new HBox(roomLabel);
                roomBox.setAlignment(Pos.CENTER_LEFT);
                content.getChildren().addAll(subLabel, teaLabel, roomBox);
            });
        } else {
            content.getChildren().addAll(subLabel, teaLabel);
        }
        
        StackPane card = new StackPane(content);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 18; -fx-border-width: 1 1 1 6; -fx-border-color: " + COLOR_BORDER_SOFT + " " + COLOR_BORDER_SOFT + " " + COLOR_BORDER_SOFT + " " + accentColor + ";");
        card.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.web(SHADOW_NAVY_08), 15, 0, 0, 5));
        
        Button clearBtn = new Button("✕"); 
        clearBtn.setStyle("-fx-background-color: " + COLOR_ALERT_RED + "; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 900; -fx-background-radius: 99; -fx-padding: 4 8; -fx-cursor: hand;");
        clearBtn.setVisible(false); 
        
        StackPane.setAlignment(clearBtn, Pos.TOP_RIGHT); 
        StackPane.setMargin(clearBtn, new Insets(10));
        clearBtn.setOnAction(e -> { 
            mainApp.getAcademicService().deleteScheduleEntry(cls.id(), day, lesson, parity); 
            refreshGrid.run(); 
            e.consume(); 
        });
        
        card.getChildren().add(clearBtn);
        card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> { 
            card.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 18; -fx-border-color: " + accentColor + "; -fx-border-radius: 18; -fx-border-width: 1 1 1 8;"); 
            if (entry != null) clearBtn.setVisible(true); 
        });
        card.setOnMouseExited(e -> { 
            card.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + " " + COLOR_BORDER_SOFT + " " + COLOR_BORDER_SOFT + " " + accentColor + "; -fx-border-radius: 18; -fx-border-width: 1 1 1 6;"); 
            clearBtn.setVisible(false); 
        });
        card.setOnMouseClicked(e -> new LessonEditorDialog(mainApp, cls, day, lesson, parity, teachers, subjects, refreshGrid).display());
        
        return card;
    }

    private String getSubjectColor(int id) {
        String[] palette = {COLOR_SKY, COLOR_TEAL, COLOR_INDIGO_DARK, COLOR_TANGERINE, COLOR_YELLOW_SOFT, COLOR_PINK, COLOR_TEXT, COLOR_SKY_BRIGHT, COLOR_LIME, COLOR_PINK_LIGHT};
        return palette[Math.abs(id) % palette.length];
    }
}
