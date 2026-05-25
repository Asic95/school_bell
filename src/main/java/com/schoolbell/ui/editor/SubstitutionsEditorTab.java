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
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
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
            COLOR_ORANGE,
            null
        );

        // Filter & Search Bar
        HBox actionToolbar = new HBox(20);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = createPrimaryActionButton("НОВА ЗАМІНА", ICON_PLUS);
        addBtn.setOnAction(e -> new SubstitutionEditorDialog(mainApp, null, LocalDate.now(), refreshSubstitutions).show());

        Button reportBtn = createPrimaryActionButton("ЗВІТ", ICON_SAVE);
        reportBtn.setStyle(reportBtn.getStyle().replace(COLOR_PRIMARY, COLOR_PURPLE));
        reportBtn.setOnAction(e -> new SubstitutionReportDialog(mainApp, reportService).show());

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук за вчителем або класом...");
        searchField.setPrefWidth(320);
        searchField.setStyle(PREMIUM_FIELD_STYLE);
        searchField.textProperty().addListener((o, ov, nv) -> {
            searchText = nv.toLowerCase();
            refreshSubstitutions.run();
        });

        HBox toggleGroup = new HBox(0);
        toggleGroup.setAlignment(Pos.CENTER);
        toggleGroup.setStyle(PREMIUM_TOGGLE_CONTAINER);
        
        ToggleButton activeBtn = new ToggleButton("Активні");
        ToggleButton archiveBtn = new ToggleButton("Архів");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        activeBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
        archiveBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);

        group.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == activeBtn) {
                activeBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
                archiveBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);
                showArchived = false;
            } else {
                activeBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);
                archiveBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
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
                empty.setPadding(new Insets(100, 0, 100, 0));
                empty.setMinWidth(900);
                
                Node icon = createSVGIcon(ICON_INFO, Color.web(COLOR_WHITE_MUTED_BORDER), 64);
                Label emptyLabel = new Label(showArchived ? "Архів замін порожній" : "Немає активних замін");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_ICON_MUTED + ";");
                Label subLabel = new Label(showArchived ? "Тут з'являтимуться заміни, термін дії яких минув" : "Натисніть 'НОВА ЗАМІНА', щоб внести оперативні зміни");
                subLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + ";");
                
                empty.getChildren().addAll(icon, emptyLabel, subLabel);
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
