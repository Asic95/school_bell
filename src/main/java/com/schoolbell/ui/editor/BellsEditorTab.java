package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.ui.ToastService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.schoolbell.ui.CardFactory.createHelpCard;
import static com.schoolbell.ui.CardFactory.createSideHelpPanel;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.ControlFactory.createTimeCombo;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class BellsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshBells;
    private final List<LessonRow> lessonRows = new ArrayList<>();

    public BellsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        Button saveBtn = createPrimaryActionButton("Зберегти зміни", ICON_SAVE);
        HBox header = createPageHeader(
            "РОЗКЛАД ДЗВІНКІВ",
            "Налаштування дзвінків",
            "Створюйте та редагуйте розклади уроків для вашого закладу.",
            ICON_BELL,
            "#0984e3",
            saveBtn
        );

        HBox contentLayout = new HBox(28); // Standard section gap
        VBox mainContent = new VBox(28); // Gap between cards
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // --- CARD 1: MANAGEMENT ---
        VBox managementCard = new VBox(14);
        managementCard.setPadding(new Insets(28));
        managementCard.setStyle(SOFT_CARD); // Uses the centralized style from UIStyles

        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox selectorBox = new VBox(7);
        Label selectorLabel = new Label("ОБЕРІТЬ РОЗКЛАД ДЛЯ РЕДАГУВАННЯ");
        selectorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
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
                    setStyle("-fx-padding: 10 14; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
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
                    setStyle("-fx-font-weight: 600; -fx-text-fill: #0f172a; -fx-padding: 0 5;");
                }
            }
        });
        selectorBox.getChildren().addAll(selectorLabel, selector);

        Button addBtn = createPrimaryActionButton("ДОДАТИ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, COLOR_PURPLE));

        Button renameBtn = new Button("ПЕРЕЙМЕНУВАТИ");
        renameBtn.setGraphic(createSVGIcon(ICON_EDIT, Color.web("#64748b"), 16));
        renameBtn.setGraphicTextGap(10);
        String renameBaseStyle = "-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: 800; -fx-font-size: 13px; -fx-padding: 12 20; -fx-background-radius: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 18; -fx-cursor: hand;";
        renameBtn.setStyle(renameBaseStyle);
        renameBtn.setOnMouseEntered(e -> renameBtn.setStyle(renameBaseStyle + "-fx-background-color: #f8fbff; -fx-border-color: #cbd5e1; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.05), 10, 0, 0, 2);"));
        renameBtn.setOnMouseExited(e -> renameBtn.setStyle(renameBaseStyle));

        Button deleteBtn = new Button("ВИДАЛИТИ");
        deleteBtn.setGraphic(createSVGIcon(ICON_TRASH, Color.web(COLOR_DANGER), 16));
        deleteBtn.setGraphicTextGap(10);
        String deleteBaseStyle = "-fx-background-color: white; -fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: 800; -fx-font-size: 13px; -fx-padding: 12 20; -fx-background-radius: 18; -fx-border-color: #fee2e2; -fx-border-radius: 18; -fx-cursor: hand;";
        deleteBtn.setStyle(deleteBaseStyle);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBaseStyle + "-fx-background-color: #fef2f2; -fx-border-color: " + COLOR_DANGER + "; -fx-effect: dropshadow(three-pass-box, rgba(220,38,38,0.05), 10, 0, 0, 2);"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(deleteBaseStyle));

        HBox actions = new HBox(10, addBtn, renameBtn, deleteBtn);
        actions.setAlignment(Pos.BOTTOM_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(selectorBox, topSpacer, actions);
        managementCard.getChildren().add(topBar);

        // --- CARD 2: EDITOR ---
        VBox editorCard = new VBox(22);
        editorCard.setPadding(new Insets(28));
        editorCard.setStyle(SOFT_CARD);

        HBox sectionHead = new HBox(14);
        sectionHead.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBadge = new VBox(createSVGIcon(ICON_EDIT, Color.web(COLOR_PRIMARY), 22));
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.setPrefSize(54, 54);
        iconBadge.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fbff, #f1f5f9); -fx-background-radius: 18; -fx-border-color: #e2e8f0; -fx-border-radius: 18;");

        VBox titleCol = new VBox(4);
        Label editorTitle = new Label("РЕДАГУВАННЯ РОЗКЛАДУ");
        editorTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Label editorMainTitle = new Label("Налаштування уроків");
        editorMainTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        titleCol.getChildren().addAll(editorTitle, editorMainTitle);
        
        sectionHead.getChildren().addAll(iconBadge, titleCol);

        VBox rows = new VBox(8);
        VBox.setVgrow(rows, Priority.ALWAYS);

        HBox editorBody = new HBox(14, rows);
        HBox.setHgrow(rows, Priority.ALWAYS);

        editorCard.getChildren().addAll(sectionHead, editorBody);
        
        mainContent.getChildren().addAll(managementCard, editorCard);

        VBox rightColumn = createSideHelpPanel(
                createHelpCard(ICON_CALENDAR, "Гнучкість", "Можна вести декілька варіантів розкладу і швидко між ними перемикатись.", "#6c5ce7"),
                createHelpCard(ICON_CLOCK, "Перерви", "Перерва задається після уроку і впливає на старт наступного.", "#00b894"),
                createHelpCard(ICON_SAVE, "Збереження", "Кнопка \"ЗБЕРЕГТИ ЗМІНИ\" знаходиться у верхньому правому куті.", "#e17055")
        );

        contentLayout.getChildren().addAll(mainContent, rightColumn);
        root.getChildren().addAll(header, contentLayout);

        addBtn.setOnAction(e -> {
            new com.schoolbell.ui.TextInputModalDialog(
                    mainApp,
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
            ).show();
        });

        renameBtn.setOnAction(e -> {
            String current = selector.getValue();
            if (current == null) return;
            new com.schoolbell.ui.TextInputModalDialog(
                    mainApp,
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
            ).show();
        });

        deleteBtn.setOnAction(e -> {
            String current = selector.getValue();
            if (current == null) return;
            if (mainApp.getInternalSchedules().size() <= 1) {
                new Alert(Alert.AlertType.WARNING, "Неможливо видалити останній розклад!").show();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Ви впевнені, що хочете видалити '" + current + "'?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(type -> {
                if (type == ButtonType.YES) {
                    mainApp.getInternalSchedules().removeIf(s -> s.getName().equals(current));
                    mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                    refreshBells.run();
                }
            });
        });

        Runnable saveAction = () -> {
            DaySchedule ds = mainApp.getInternalSchedules().stream()
                    .filter(d -> d.getName().equals(selector.getValue())).findFirst().orElse(null);
            if (ds == null) return;
            List<DaySchedule.LessonInfo> list = new ArrayList<>();
            for (LessonRow row : lessonRows) {
                int sh = Integer.parseInt(row.startHour.getValue());
                int sm = Integer.parseInt(row.startMinute.getValue());
                int eh = Integer.parseInt(row.endHour.getValue());
                int em = Integer.parseInt(row.endMinute.getValue());
                int br = Integer.parseInt(row.breakField.getText().trim());
                list.add(new DaySchedule.LessonInfo(LocalTime.of(sh, sm), LocalTime.of(eh, em), br));
            }
            ds.setLessons(list);
            mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
            ToastService.showSuccess("Зміни в розкладі успішно збережено!");
        };
        saveBtn.setOnAction(e -> saveAction.run());

        selector.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            rows.getChildren().clear();
            lessonRows.clear();
            DaySchedule ds = mainApp.getInternalSchedules().stream()
                    .filter(d -> d.getName().equals(newV))
                    .findFirst().orElse(null);
            if (ds == null) return;

            String[] tones = {"#8a6cf6", "#4f9bff", "#00b3c4", "#f08a1b", "#cb6bd1", "#4f7cff", "#27ae60"};
            for (int i = 0; i < ds.getLessons().size(); i++) {
                LessonRow row = createLessonRow(i, tones[i % tones.length], ds.getLessons().get(i));
                lessonRows.add(row);
                rows.getChildren().add(row.root);
            }
        });

        refreshBells = () -> {
            selector.getItems().setAll(mainApp.getInternalSchedules().stream().map(DaySchedule::getName).toList());
            if (!selector.getItems().isEmpty()) {
                selector.setValue(selector.getItems().get(0));
            }
        };
        refreshBells.run();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }

    private LessonRow createLessonRow(int index, String tone, DaySchedule.LessonInfo info) {
        ComboBox<String> sh = createTimeCombo(24, info.start != null ? info.start.getHour() : 0);
        ComboBox<String> sm = createTimeCombo(60, info.start != null ? info.start.getMinute() : 0);
        ComboBox<String> eh = createTimeCombo(24, info.end != null ? info.end.getHour() : 0);
        ComboBox<String> em = createTimeCombo(60, info.end != null ? info.end.getMinute() : 0);
        TextField breakF = new TextField(String.valueOf(info.breakAfterMinutes));

        breakF.setPrefSize(84, 45);
        breakF.setStyle(PREMIUM_FIELD_STYLE + "-fx-font-size: 14px; -fx-padding: 0 12;");

        HBox lessonBox = lessonBox(index, tone);
        VBox startBox = labeledTimeBox("ПОЧАТОК", sh, sm);
        VBox endBox = labeledTimeBox("КІНЕЦЬ", eh, em);
        VBox breakBox = breakLabeledBox(breakF);

        HBox.setHgrow(startBox, Priority.ALWAYS);
        HBox.setHgrow(endBox, Priority.ALWAYS);
        HBox.setHgrow(breakBox, Priority.ALWAYS);
        startBox.setMaxWidth(Double.MAX_VALUE);
        endBox.setMaxWidth(Double.MAX_VALUE);
        breakBox.setMaxWidth(Double.MAX_VALUE);

        Label dash = new Label("—");
        dash.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #cbd5e1; -fx-padding: 14 4 0 4;");

        HBox row = new HBox(15, lessonBox, startBox, dash, endBox, breakBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.03), 8, 0, 0, 2);"
        );

        row.setOnMouseEntered(e -> row.setStyle(row.getStyle() + "-fx-background-color: #f8fafc; -fx-border-color: " + tone + "40;"));
        row.setOnMouseExited(e -> row.setStyle(row.getStyle().replace("-fx-background-color: #f8fafc; -fx-border-color: " + tone + "40;", "")));

        return new LessonRow(row, sh, sm, eh, em, breakF);
    }

    private void styleTimeCombo(ComboBox<String> combo, String style) {
        // Not used anymore as createTimeCombo uses premium style
    }

    private HBox lessonBox(int index, String tone) {
        StackPane badge = new StackPane();
        badge.setMinSize(44, 44);
        badge.setPrefSize(44, 44);
        badge.setMaxSize(44, 44);
        badge.setStyle(
                "-fx-background-color: " + tone + "12;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + tone + "30;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 12;"
        );
        Label num = new Label(String.format("%02d", index + 1));
        num.setStyle("-fx-text-fill: " + tone + "; -fx-font-weight: 900; -fx-font-size: 15px;");
        badge.getChildren().add(num);

        Label lessonText = new Label((index + 1) + " УРОК");
        lessonText.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #0f172a; -fx-letter-spacing: 0.5px;");
        String openBook = "M19,2H14C12.9,2 12,2.9 12,4C12,2.9 11.1,2 10,2H5C3.9,2 3,2.9 3,4V20C3,18.9 3.9,18 5,18H10C11.1,18 12,18.9 12,20C12,18.9 12.9,18 14,18H19C20.1,18 21,18.9 21,20V4C21,2.9 20.1,2 19,2Z";
        HBox box = new HBox(12, badge, createSVGIcon(openBook, Color.web(tone), 22), lessonText);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(160);
        box.setMinWidth(160);
        return box;
    }

    private VBox labeledTimeBox(String labelText, ComboBox<String> h, ComboBox<String> m) {
        Label label = new Label(labelText);
        label.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        HBox box = timeBox(h, m);
        return new VBox(6, label, box);
    }

    private VBox breakLabeledBox(TextField breakF) {
        Label label = new Label("ПЕРЕРВА");
        label.setStyle(HEADER_STYLE + "-fx-font-size: 10px;");
        HBox row = new HBox(8, createSVGIcon(ICON_CLOCK, Color.web("#94a3b8"), 20), breakF, new Label("ХВ"));
        ((Label) row.getChildren().get(2)).setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #64748b;");
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(6, label, row);
    }

    private HBox timeBox(ComboBox<String> h, ComboBox<String> m) {
        Label sep = new Label(":");
        sep.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #c4cfde;");
        HBox box = new HBox(6, h, sep, m);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Button createToolbarButton(String text, String iconPath, String textColor, String bgColor, String borderColor) {
        Button b = new Button(text);
        b.setGraphic(createSVGIcon(iconPath, Color.web(textColor), 16));
        b.setGraphicTextGap(10);
        b.setMinHeight(46);
        b.setPrefHeight(46);
        String baseStyle = String.format(
                "-fx-background-color: %s;" +
                        "-fx-text-fill: %s;" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: %s;" +
                        "-fx-border-width: 1.2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-cursor: hand;",
                bgColor, textColor, borderColor
        );
        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> b.setStyle(baseStyle + "-fx-background-color: derive(" + bgColor + ", -4%); -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.08), 8, 0.2, 0, 2);"));
        b.setOnMouseExited(e -> b.setStyle(baseStyle));
        return b;
    }

    private record LessonRow(
            HBox root,
            ComboBox<String> startHour,
            ComboBox<String> startMinute,
            ComboBox<String> endHour,
            ComboBox<String> endMinute,
            TextField breakField
    ) {
    }
}
