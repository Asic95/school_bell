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
        VBox headerArea = createSectionHeader(
                "Налаштування дзвінків",
                "Створюйте та редагуйте розклади уроків для вашого закладу",
                "#0984e3",
                ICON_BELL,
                saveBtn
        );

        HBox contentLayout = new HBox(25);
        VBox mainContent = new VBox(20);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        VBox workbench = new VBox(14);
        workbench.setPadding(new Insets(18));
        workbench.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #e6edf7;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 18;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 18, 0.16, 0, 4);"
        );

        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox selectorBox = new VBox(7);
        Label selectorLabel = new Label("ОБЕРІТЬ РОЗКЛАД ДЛЯ РЕДАГУВАННЯ");
        selectorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #6b7a90;");
        ComboBox<String> selector = new ComboBox<>();
        selector.setPrefWidth(390);
        selector.setPrefHeight(46);
        selector.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-color: #f8fbff;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dbe6f4;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 0 8;"
        );
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
                    setStyle("-fx-padding: 10 14; -fx-font-weight: 700; -fx-text-fill: #223249;");
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
                    setStyle("-fx-font-weight: 700; -fx-text-fill: #223249; -fx-padding: 0 5;");
                }
            }
        });
        selectorBox.getChildren().addAll(selectorLabel, selector);

        Button addBtn = createToolbarButton("Додати", ICON_PLUS, "#ffffff", "#6f63f6", "#6f63f6");
        Button renameBtn = createToolbarButton("Перейменувати", ICON_EDIT, "#3a4f70", "#ffffff", "#dfe6f2");
        Button deleteBtn = createToolbarButton("Видалити", ICON_TRASH, "#ef3f3f", "#ffffff", "#f5c7c7");
        HBox actions = new HBox(10, addBtn, renameBtn, deleteBtn);
        actions.setAlignment(Pos.BOTTOM_RIGHT);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(selectorBox, topSpacer, actions);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #e8eef8;");

        HBox sectionHead = new HBox(10);
        sectionHead.setAlignment(Pos.CENTER_LEFT);
        StackPane dot = new StackPane();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color: #4f7cff; -fx-background-radius: 99;");
        Label editorTitle = new Label("РЕДАГУВАННЯ РОЗКЛАДУ");
        editorTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #2a3c56;");
        Label editorSub = new Label("Оновлюйте час уроків і тривалість перерв в одному потоці");
        editorSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #8393a8;");
        sectionHead.getChildren().addAll(dot, editorTitle, editorSub);

        VBox rows = new VBox(8);
        VBox.setVgrow(rows, Priority.ALWAYS);

        HBox editorBody = new HBox(14, rows);
        HBox.setHgrow(rows, Priority.ALWAYS);

        workbench.getChildren().addAll(topBar, divider, sectionHead, editorBody);
        mainContent.getChildren().add(workbench);

        VBox rightColumn = createSideHelpPanel(
                createHelpCard(ICON_CALENDAR, "Гнучкість", "Можна вести декілька варіантів розкладу і швидко між ними перемикатись.", "#6c5ce7"),
                createHelpCard(ICON_CLOCK, "Перерви", "Перерва задається після уроку і впливає на старт наступного.", "#00b894"),
                createHelpCard(ICON_SAVE, "Збереження", "Кнопка \"ЗБЕРЕГТИ ЗМІНИ\" знаходиться у верхньому правому куті.", "#e17055")
        );

        contentLayout.getChildren().addAll(mainContent, rightColumn);
        root.getChildren().addAll(headerArea, contentLayout);

        addBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Новий розклад");
            dialog.setTitle("Додати розклад");
            dialog.setHeaderText("Введіть назву для нового розкладу");
            dialog.showAndWait().ifPresent(name -> {
                if (mainApp.getInternalSchedules().stream().anyMatch(s -> s.getName().equals(name))) {
                    new Alert(Alert.AlertType.ERROR, "Розклад з такою назвою вже існує!").show();
                    return;
                }
                mainApp.getInternalSchedules().add(new DaySchedule(name));
                mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                refreshBells.run();
                selector.setValue(name);
            });
        });

        renameBtn.setOnAction(e -> {
            String current = selector.getValue();
            if (current == null) return;
            TextInputDialog dialog = new TextInputDialog(current);
            dialog.setTitle("Перейменувати розклад");
            dialog.setHeaderText("Введіть нову назву для розкладу");
            dialog.showAndWait().ifPresent(newName -> {
                mainApp.getInternalSchedules().stream()
                        .filter(s -> s.getName().equals(current))
                        .findFirst()
                        .ifPresent(s -> s.setName(newName));
                mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
                refreshBells.run();
                selector.setValue(newName);
            });
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

        String comboStyle = "-fx-font-size: 13px; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dbe5f2; -fx-border-radius: 8; -fx-font-weight: 500;";
        styleTimeCombo(sh, comboStyle);
        styleTimeCombo(sm, comboStyle);
        styleTimeCombo(eh, comboStyle);
        styleTimeCombo(em, comboStyle);
        breakF.setPrefSize(72, 36);
        breakF.setStyle("-fx-font-size: 13px; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dbe5f2; -fx-border-radius: 8; -fx-font-weight: 700; -fx-padding: 0 8;");

        HBox lessonBox = lessonBox(index, tone);
        VBox startBox = labeledTimeBox("Початок", sh, sm);
        VBox endBox = labeledTimeBox("Кінець", eh, em);
        VBox breakBox = breakLabeledBox(breakF);

        HBox.setHgrow(startBox, Priority.ALWAYS);
        HBox.setHgrow(endBox, Priority.ALWAYS);
        HBox.setHgrow(breakBox, Priority.ALWAYS);
        startBox.setMaxWidth(Double.MAX_VALUE);
        endBox.setMaxWidth(Double.MAX_VALUE);
        breakBox.setMaxWidth(Double.MAX_VALUE);

        Label dash = new Label("—");
        dash.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #c2cadd; -fx-padding: 14 4 0 4;");

        HBox row = new HBox(10, lessonBox, startBox, dash, endBox, breakBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e6edf7;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
        );

        return new LessonRow(row, sh, sm, eh, em, breakF);
    }

    private void styleTimeCombo(ComboBox<String> combo, String style) {
        combo.setStyle(style);
        combo.setPrefSize(80, 36);
    }

    private HBox lessonBox(int index, String tone) {
        StackPane badge = new StackPane();
        badge.setMinSize(40, 40);
        badge.setPrefSize(40, 40);
        badge.setMaxSize(40, 40);
        badge.setStyle(
                "-fx-background-color: derive(" + tone + ", 94%);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: derive(" + tone + ", 82%);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;"
        );
        Label num = new Label(String.format("%02d", index + 1));
        num.setStyle("-fx-text-fill: derive(" + tone + ", -20%); -fx-font-weight: 900; -fx-font-size: 14px;");
        badge.getChildren().add(num);

        Label lessonText = new Label((index + 1) + " урок");
        lessonText.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #2b3b52;");
        String openBook = "M19,2H14C12.9,2 12,2.9 12,4C12,2.9 11.1,2 10,2H5C3.9,2 3,2.9 3,4V20C3,18.9 3.9,18 5,18H10C11.1,18 12,18.9 12,20C12,18.9 12.9,18 14,18H19C20.1,18 21,18.9 21,20V4C21,2.9 20.1,2 19,2Z";
        HBox box = new HBox(10, badge, createSVGIcon(openBook, Color.web(tone), 20), lessonText);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(140);
        box.setMinWidth(140);
        return box;
    }

    private VBox labeledTimeBox(String labelText, ComboBox<String> h, ComboBox<String> m) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #6b7893;");
        HBox box = timeBox(h, m);
        return new VBox(4, label, box);
    }

    private VBox breakLabeledBox(TextField breakF) {
        Label label = new Label("Перерва після уроку");
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #6b7893;");
        HBox row = new HBox(8, createSVGIcon(ICON_CLOCK, Color.web("#9aa9c1"), 20), breakF, new Label("хв"));
        ((Label) row.getChildren().get(2)).setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #5d6f87;");
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4, label, row);
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
