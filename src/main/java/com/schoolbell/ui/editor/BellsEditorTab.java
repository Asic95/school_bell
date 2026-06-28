package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.ui.ConfirmationDialog;
import com.schoolbell.ui.ToastService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class BellsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshBells;
    private final List<LessonRowComponent> lessonRows = new ArrayList<>();
    private String currentScheduleName;

    public BellsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    private boolean isUpdating = false;

    private void save() {
        if (isUpdating) return;
        if (currentScheduleName == null) return;
        DaySchedule ds = mainApp.getInternalSchedules().stream()
                .filter(d -> d.getName().equals(currentScheduleName)).findFirst().orElse(null);
        if (ds == null) return;
        
        isUpdating = true;
        try {
            for (int i = 1; i < lessonRows.size(); i++) {
                LessonRowComponent prevRow = lessonRows.get(i - 1);
                LessonRowComponent currRow = lessonRows.get(i);
                DaySchedule.LessonInfo prevInfo = prevRow.getLessonInfo();
                DaySchedule.LessonInfo currInfo = currRow.getLessonInfo();
                
                if (prevInfo != null && currInfo != null) {
                    LocalTime nextStart = prevInfo.end.plusMinutes(prevInfo.breakAfterMinutes);
                    long durationMinutes = java.time.Duration.between(currInfo.start, currInfo.end).toMinutes();
                    if (durationMinutes < 0) {
                        durationMinutes = 0;
                    }
                    LocalTime nextEnd = nextStart.plusMinutes(durationMinutes);
                    currRow.updateTime(nextStart, nextEnd);
                }
            }

            List<DaySchedule.LessonInfo> list = lessonRows.stream()
                    .map(LessonRowComponent::getLessonInfo)
                    .filter(Objects::nonNull)
                    .toList();
                    
            ds.setLessons(new ArrayList<>(list));
            mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
            mainApp.reloadSchedule();
        } finally {
            isUpdating = false;
        }
    }

    private void reindexRows() {
        String[] tones = {COLOR_LAVENDER, COLOR_BLUE_BRIGHT, COLOR_TURQUOISE, COLOR_GOLDEN_ORANGE, COLOR_MAGENTA_SOFT, COLOR_BLUE_VIVID, COLOR_GREEN};
        for (int i = 0; i < lessonRows.size(); i++) {
            lessonRows.get(i).updateIndex(i, tones[i % tones.length]);
        }
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");
        root.getStylesheets().add("data:text/css," + PREMIUM_MENU_STYLE.replace(" ", "%20"));

        HBox header = createPageHeader(
            "РОЗКЛАД ДЗВІНКІВ",
            "Налаштування дзвінків",
            "Створюйте та редагуйте розклади уроків для вашого закладу.",
            ICON_BELL,
            COLOR_SKY,
            null
        );

        HBox contentLayout = new HBox(28); 
        VBox mainContent = new VBox(28); 
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        VBox managementCard = new VBox(14);
        managementCard.setPadding(new Insets(28));
        managementCard.setStyle(SOFT_CARD);

        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox selectorBox = new VBox(7);
        Label selectorLabel = new Label("ОБЕРІТЬ РОЗКЛАД ДЛЯ РЕДАГУВАННЯ");
        selectorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 0.5px;");
        ComboBox<String> selector = new ComboBox<>();
        selector.setPrefWidth(390);
        selector.setStyle(PREMIUM_SELECT_STYLE);
        selector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createSVGIcon(ICON_CALENDAR, Color.web(COLOR_PRIMARY), 16));
                    setText(item);
                    setGraphicTextGap(12);
                    setStyle("-fx-padding: 10 14; -fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + ";");
                }
            }
        });
        selector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createSVGIcon(ICON_CALENDAR, Color.web(COLOR_PRIMARY), 18));
                    setText(item);
                    setGraphicTextGap(12);
                    setStyle("-fx-font-weight: 600; -fx-text-fill: " + COLOR_NAVY + "; -fx-padding: 0 5;");
                }
            }
        });
        selectorBox.getChildren().addAll(selectorLabel, selector);

        Button addBtn = createPrimaryActionButton("ДОДАТИ", ICON_PLUS);
        addBtn.setPrefHeight(48);

        MenuButton editMenuBtn = new MenuButton("РЕДАГУВАТИ");
        editMenuBtn.setGraphic(createSVGIcon(ICON_EDIT, Color.web(COLOR_SLATE), 18));
        editMenuBtn.setGraphicTextGap(10);
        editMenuBtn.setPrefHeight(48);
        String menuBaseStyle = "-fx-background-color: white; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: 900; -fx-font-size: 13px; -fx-padding: 0 24; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 18; -fx-cursor: hand;";
        editMenuBtn.setStyle(menuBaseStyle);
        editMenuBtn.setOnMouseEntered(e -> editMenuBtn.setStyle(menuBaseStyle + "-fx-background-color: " + COLOR_SURFACE_GLASS_START + "; -fx-border-color: " + COLOR_SLATE_MUTED + "; -fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_05 + ", 10, 0, 0, 2);"));
        editMenuBtn.setOnMouseExited(e -> editMenuBtn.setStyle(menuBaseStyle));

        MenuItem miActivate = new MenuItem("Встановити як активний");
        miActivate.setGraphic(createSVGIcon(ICON_CHECK, Color.web(COLOR_SUCCESS), 16));
        
        MenuItem miRename = new MenuItem("Перейменувати");
        miRename.setGraphic(createSVGIcon(ICON_EDIT, Color.web(COLOR_PRIMARY), 16));
        
        MenuItem miDelete = new MenuItem("Видалити");
        miDelete.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 16));

        editMenuBtn.getItems().addAll(miActivate, new SeparatorMenuItem(), miRename, miDelete);

        HBox actions = new HBox(12, addBtn, editMenuBtn);
        actions.setAlignment(Pos.BOTTOM_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(selectorBox, topSpacer, actions);
        managementCard.getChildren().add(topBar);

        VBox editorCard = new VBox(22);
        editorCard.setPadding(new Insets(28));
        editorCard.setStyle(SOFT_CARD);

        HBox sectionHead = new HBox(14);
        sectionHead.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBadge = new VBox(createSVGIcon(ICON_EDIT, Color.web(COLOR_PRIMARY), 22));
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.setPrefSize(54, 54);
        iconBadge.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_SOFT + "); -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-radius: 18;");

        VBox titleCol = new VBox(4);
        Label editorTitle = new Label("РЕДАГУВАННЯ РОЗКЛАДУ");
        editorTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 0.5px;");
        Label editorMainTitle = new Label("Налаштування уроків");
        editorMainTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        titleCol.getChildren().addAll(editorTitle, editorMainTitle);
        
        sectionHead.getChildren().addAll(iconBadge, titleCol);

        VBox rows = new VBox(8);
        VBox.setVgrow(rows, Priority.ALWAYS);

        Button addLessonRowBtn = new Button("ДОДАТИ НОВИЙ УРОК");
        addLessonRowBtn.setGraphic(createSVGIcon(ICON_PLUS, Color.web(COLOR_PRIMARY), 18));
        addLessonRowBtn.setGraphicTextGap(12);
        addLessonRowBtn.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: " + COLOR_BORDER_SOFT + "; -fx-border-width: 1.5; -fx-border-style: dashed; -fx-border-radius: 18; -fx-padding: 15; -fx-font-weight: 800; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-cursor: hand;");
        addLessonRowBtn.setMaxWidth(Double.MAX_VALUE);
        addLessonRowBtn.setOnMouseEntered(e -> addLessonRowBtn.setStyle(addLessonRowBtn.getStyle().replace("white", COLOR_SURFACE_SKY)));
        addLessonRowBtn.setOnMouseExited(e -> addLessonRowBtn.setStyle(addLessonRowBtn.getStyle().replace(COLOR_SURFACE_SKY, "white")));

        addLessonRowBtn.setOnAction(e -> {
            int newIdx = lessonRows.size();
            String[] tones = {COLOR_LAVENDER, COLOR_BLUE_BRIGHT, COLOR_TURQUOISE, COLOR_GOLDEN_ORANGE, COLOR_MAGENTA_SOFT, COLOR_BLUE_VIVID, COLOR_GREEN};
            
            LocalTime lastEnd = LocalTime.of(8, 0);
            if (!lessonRows.isEmpty()) {
                DaySchedule.LessonInfo lastInfo = lessonRows.get(lessonRows.size() - 1).getLessonInfo();
                if (lastInfo != null) {
                    lastEnd = lastInfo.end.plusMinutes(lastInfo.breakAfterMinutes);
                }
            }
            
            DaySchedule.LessonInfo li = new DaySchedule.LessonInfo(lastEnd, lastEnd.plusMinutes(45), 10);
            LessonRowComponent lr = new LessonRowComponent(newIdx, tones[newIdx % tones.length], li, this::save, row -> {
                lessonRows.remove(row);
                rows.getChildren().remove(row);
                reindexRows();
                save();
            });
            lessonRows.add(lr);
            rows.getChildren().add(lr);
            save();
        });

        VBox editorContainer = new VBox(15, rows, addLessonRowBtn);
        HBox.setHgrow(editorContainer, Priority.ALWAYS);

        HBox editorBody = new HBox(14, editorContainer);
        HBox.setHgrow(rows, Priority.ALWAYS);

        editorCard.getChildren().addAll(sectionHead, editorBody);
        
        mainContent.getChildren().addAll(managementCard, editorCard);

        VBox rightColumn = createSideHelpPanel(
                createHelpCard(ICON_CHECK, "Активація", "Оберіть розклад у списку та натисніть РЕДАГУВАТИ -> Встановити як активний, щоб він почав діяти.", COLOR_SUCCESS),
                createHelpCard(ICON_CALENDAR, "Гнучкість", "Можна вести декілька варіантів розкладу і швидко між ними перемикатись.", COLOR_INDIGO_DARK),
                createHelpCard(ICON_CLOCK, "Перерви", "Перерва задається після уроку і впливає на старт наступного.", COLOR_TEAL)
        );

        contentLayout.getChildren().addAll(mainContent, rightColumn);
        root.getChildren().addAll(header, contentLayout);

        miActivate.setOnAction(e -> {
            String selected = selector.getValue();
            if (selected != null) {
                mainApp.getConfigService().setSelectedScheduleName(selected);
                mainApp.saveConfig();
                mainApp.reloadSchedule();
                mainApp.getDashboardView().refreshActiveScheduleLabel();
                ToastService.showSuccess("Розклад '" + selected + "' тепер активний!");
                
                miActivate.setDisable(true);
                miActivate.setText("Вже активовано");
            }
        });

        addBtn.setOnAction(e -> {
            new com.schoolbell.ui.TextInputModalDialog(
                    mainApp.getStage(),
                    "Додати розклад",
                    "Введіть назву для нового розкладу дзвінків",
                    "Новий розклад",
                    "Назва розкладу",
                    name -> {
                        if (mainApp.getInternalSchedules().stream().anyMatch(s -> s.getName().equals(name))) {
                            ToastService.showError("Розклад з такою назвою вже існує!");
                            return;
                        }
                        mainApp.getInternalSchedules().add(new DaySchedule(name));
                        mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                        refreshBells.run();
                        selector.setValue(name);
                        ToastService.showSuccess("Розклад '" + name + "' додано успішно");
                    }
            ).display();
        });

        miRename.setOnAction(e -> {
            String current = selector.getValue();
            if (current == null) return;
            new com.schoolbell.ui.TextInputModalDialog(
                    mainApp.getStage(),
                    "Перейменувати розклад",
                    "Введіть нову назву для поточного розкладу",
                    current,
                    "Нова назва",
                    newName -> {
                        if (mainApp.getInternalSchedules().stream().anyMatch(s -> s.getName().equals(newName) && !newName.equals(current))) {
                            ToastService.showError("Розклад з такою назвою вже існує!");
                            return;
                        }
                        mainApp.getInternalSchedules().stream()
                                .filter(s -> s.getName().equals(current))
                                .findFirst()
                                .ifPresent(s -> s.setName(newName));
                        mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                        refreshBells.run();
                        selector.setValue(newName);
                        ToastService.showSuccess("Розклад перейменовано на '" + newName + "'");
                    }
            ).display();
        });

        miDelete.setOnAction(e -> {
            String current = selector.getValue();
            if (current == null) return;
            if (mainApp.getInternalSchedules().size() <= 1) {
                ToastService.showError("Неможливо видалити останній розклад!");
                return;
            }

            ConfirmationDialog dialog = new ConfirmationDialog(
                    mainApp.getStage(),
                    "Видалення розкладу",
                    "Ви впевнені, що хочете видалити розклад '" + current + "'?",
                    "Ця дія призведе до остаточного видалення всіх уроків та часових інтервалів у цьому розкладі.",
                    "ВИДАЛИТИ РОЗКЛАД"
            );
            
            dialog.setOnHidden(windowEvent -> {
                if (dialog.isConfirmed()) {
                    mainApp.getInternalSchedules().removeIf(s -> s.getName().equals(current));
                    mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                    refreshBells.run();
                    ToastService.showSuccess("Розклад '" + current + "' видалено");
                }
            });
            dialog.display();
        });

        selector.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            currentScheduleName = newV;
            
            boolean isActive = newV.equals(mainApp.getConfigService().getSelectedScheduleName());
            miActivate.setDisable(isActive);
            miActivate.setText(isActive ? "Вже активовано" : "Встановити як активний");

            rows.getChildren().clear();
            lessonRows.clear();
            DaySchedule ds = mainApp.getInternalSchedules().stream()
                    .filter(d -> d.getName().equals(newV))
                    .findFirst().orElse(null);
            if (ds == null) return;

            String[] tones = {COLOR_LAVENDER, COLOR_BLUE_BRIGHT, COLOR_TURQUOISE, COLOR_GOLDEN_ORANGE, COLOR_MAGENTA_SOFT, COLOR_BLUE_VIVID, COLOR_GREEN};
            for (int i = 0; i < ds.getLessons().size(); i++) {
                int idx = i;
                LessonRowComponent lr = new LessonRowComponent(i, tones[i % tones.length], ds.getLessons().get(i), this::save, row -> {
                    lessonRows.remove(row);
                    rows.getChildren().remove(row);
                    reindexRows();
                    save();
                });
                lessonRows.add(lr);
                rows.getChildren().add(lr);
            }
        });

        refreshBells = () -> {
            selector.getItems().setAll(mainApp.getInternalSchedules().stream().map(DaySchedule::getName).toList());
            if (!selector.getItems().isEmpty()) {
                String currentActive = mainApp.getConfigService().getSelectedScheduleName();
                if (currentActive != null && selector.getItems().contains(currentActive)) {
                    selector.setValue(currentActive);
                } else {
                    selector.setValue(selector.getItems().get(0));
                }
            }
        };
        refreshBells.run();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
