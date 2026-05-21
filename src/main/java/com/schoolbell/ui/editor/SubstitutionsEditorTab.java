package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.*;
import com.schoolbell.ui.ScheduleEditorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

import com.schoolbell.ui.SubstitutionEditorDialog;
import com.schoolbell.ui.SubstitutionReportDialog;

public class SubstitutionsEditorTab {
    private final MainApp mainApp;
    private final ScheduleEditorDialog parentDialog;
    private final SubstitutionReportService reportService;
    private Runnable refreshSubstitutions;
    private String searchText = "";
    private boolean showArchived = false;
    private final Locale ukLocale = Locale.of("uk", "UA");

    public SubstitutionsEditorTab(MainApp mainApp, ScheduleEditorDialog parentDialog) {
        this.mainApp = mainApp;
        this.parentDialog = parentDialog;
        this.reportService = new SubstitutionReportService(mainApp);
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        HBox header = createPageHeader(
            "ОПЕРАТИВНІ ЗМІНИ",
            "Керування замінами",
            "Переглядайте, фільтруйте та керуйте замінами вчителів у реальному часі.",
            ICON_CLOCK,
            "#e67e22",
            null
        );

        // Filter & Search Bar
        HBox actionToolbar = new HBox(20);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("НОВА ЗАМІНА");
        addBtn.setGraphic(createSVGIcon(ICON_PLUS, Color.WHITE, 16));
        addBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_SUCCESS + "; -fx-padding: 10 20;");
        addBtn.setOnAction(e -> new SubstitutionEditorDialog(mainApp, null, LocalDate.now(), refreshSubstitutions).show());

        Button reportBtn = new Button("ЗВІТ");
        reportBtn.setGraphic(createSVGIcon(ICON_SAVE, Color.WHITE, 16));
        reportBtn.setStyle(BTN_BASE + "-fx-background-color: " + COLOR_PURPLE + "; -fx-padding: 10 20;");
        reportBtn.setOnAction(e -> new SubstitutionReportDialog(mainApp, reportService).show());

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук за вчителем або класом...");
        searchField.setPrefWidth(300);
        searchField.setStyle(FIELD_STYLE);
        searchField.textProperty().addListener((o, ov, nv) -> {
            searchText = nv.toLowerCase();
            refreshSubstitutions.run();
        });

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

        SubstitutionCard cardBuilder = new SubstitutionCard(mainApp, parentDialog, () -> refreshSubstitutions.run());

        refreshSubstitutions = () -> {
            contentList.getChildren().clear();
            List<SubstitutionEntry> allSubs = mainApp.getAcademicService().getAllSubstitutions();
            LocalDate today = LocalDate.now();

            List<SubstitutionEntry> filtered = allSubs.stream()
                .filter(sub -> showArchived ? sub.date().isBefore(today) : !sub.date().isBefore(today))
                .filter(sub -> {
                    if (searchText.isEmpty()) return true;
                    Teacher t = mainApp.getStaffService().getAllTeachers().stream().filter(tea -> tea.id() == sub.teacherId()).findFirst().orElse(null);
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
                    contentList.getChildren().add(cardBuilder.build(sub));
                }
            }
        };

        root.getChildren().addAll(header, actionToolbar, scroll);
        refreshSubstitutions.run();
        return root;
    }
}
