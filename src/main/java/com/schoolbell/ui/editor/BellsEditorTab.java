package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.DaySchedule;
import com.schoolbell.ui.ToastService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.schoolbell.ui.UIComponents.*;
import static com.schoolbell.ui.UIStyles.*;

public class BellsEditorTab {
    private final MainApp mainApp;
    private Runnable refreshBells;

    public BellsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node createContent() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // --- SAVE BUTTON (Header Action - Standardized) ---
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

        // --- TOOLBAR ---
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(15));
        toolbar.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        VBox selectorBox = new VBox(8);
        Label selectorLabel = new Label("ОБЕРІТЬ РОЗКЛАД ДЛЯ РЕДАГУВАННЯ");
        selectorLabel.setStyle(SUB_HEADER_STYLE + "-fx-font-size: 11px; -fx-text-fill: #636e72;");

        ComboBox<String> selector = new ComboBox<>();
        selector.setPrefWidth(350);
        selector.setPrefHeight(45);
        selector.setStyle(COMBO_STYLE);

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
                    setStyle("-fx-padding: 10 15; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
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
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-padding: 0 5;");
                }
            }
        });

        selectorBox.getChildren().addAll(selectorLabel, selector);

        // Standardized Toolbar Buttons
        Button addBtn = createToolbarButton("Додати", ICON_PLUS, "#2d3436", "white", "#dfe6e9");
        Button renameBtn = createToolbarButton("Редагувати", ICON_EDIT, "#2d3436", "white", "#dfe6e9");
        Button deleteBtn = createToolbarButton("Видалити", ICON_TRASH, "#d63031", "#fff5f5", "#fab1a0");

        HBox actions = new HBox(12, addBtn, renameBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER);
        
        Region spacerToolbar = new Region();
        HBox.setHgrow(spacerToolbar, Priority.ALWAYS);
        toolbar.getChildren().addAll(selectorBox, spacerToolbar, actions);

        // --- EDITOR AREA ---
        VBox editor = new VBox(15);
        editor.setPadding(new Insets(20));
        editor.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        Label editorTitle = new Label("Редагування розкладу");
        editorTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox rows = new VBox(12);
        editor.getChildren().addAll(editorTitle, rows);

        mainContent.getChildren().addAll(toolbar, editor);

        // --- SIDE PANEL (Standardized) ---
        VBox rightColumn = createSideHelpPanel(
            createHelpCard(ICON_CALENDAR, "Гнучкість розкладів", "Ви можете створити декілька варіантів та перемикатися між ними.", "#6c5ce7"),
            createHelpCard(ICON_CLOCK, "Логіка перерв", "Час перерви вказується після відповідного уроку. Це визначає паузу перед початком наступного.", "#00b894"),
            createHelpCard(ICON_SAVE, "Збереження", "Кнопка 'ЗБЕРЕГТИ ЗМІНИ' тепер знаходиться у верхньому правому куті для швидкого доступу.", "#e17055")
        );

        contentLayout.getChildren().addAll(mainContent, rightColumn);
        root.getChildren().addAll(headerArea, contentLayout);

        // Actions
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

        saveBtn.setOnAction(e -> {
            DaySchedule ds = mainApp.getInternalSchedules().stream()
                    .filter(d -> d.getName().equals(selector.getValue())).findFirst().orElse(null);
            if (ds == null) return;
            List<DaySchedule.LessonInfo> list = new ArrayList<>();
            for (Node n : rows.getChildren()) {
                HBox rowCard = (HBox) n;
                HBox timeBlock = (HBox) rowCard.getChildren().get(1);
                VBox startBox = (VBox) timeBlock.getChildren().get(0);
                VBox endBox = (VBox) timeBlock.getChildren().get(2);
                VBox breakBox = (VBox) timeBlock.getChildren().get(4);
                HBox startTime = (HBox) startBox.getChildren().get(1);
                HBox endTime = (HBox) endBox.getChildren().get(1);
                HBox breakTime = (HBox) breakBox.getChildren().get(1);
                int shValue = Integer.parseInt(((ComboBox<String>) startTime.getChildren().get(0)).getValue());
                int smValue = Integer.parseInt(((ComboBox<String>) startTime.getChildren().get(2)).getValue());
                int ehValue = Integer.parseInt(((ComboBox<String>) endTime.getChildren().get(0)).getValue());
                int emValue = Integer.parseInt(((ComboBox<String>) endTime.getChildren().get(2)).getValue());
                int brValue = Integer.parseInt(((TextField) breakTime.getChildren().get(1)).getText());
                list.add(new DaySchedule.LessonInfo(LocalTime.of(shValue, smValue), LocalTime.of(ehValue, emValue), brValue));
            }
            ds.setLessons(list);
            mainApp.getScheduleService().saveInternalSchedules(mainApp.getInternalSchedules());
            ToastService.showSuccess("Зміни в розкладі успішно збережено!");
        });

        selector.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            rows.getChildren().clear();
            DaySchedule ds = mainApp.getInternalSchedules().stream()
                    .filter(d -> d.getName().equals(newV)).findFirst().orElse(null);
            if (ds == null) return;

            String[] colors = {"#a29bfe", "#74b9ff", "#00b894", "#fdcb6e", "#e17055", "#0984e3", "#00cec9"};
            for (int i = 0; i < ds.getLessons().size(); i++) {
                DaySchedule.LessonInfo li = ds.getLessons().get(i);
                ComboBox<String> sh = createTimeCombo(24, li.start != null ? li.start.getHour() : 0);
                ComboBox<String> sm = createTimeCombo(60, li.start != null ? li.start.getMinute() : 0);
                ComboBox<String> eh = createTimeCombo(24, li.end != null ? li.end.getHour() : 0);
                ComboBox<String> em = createTimeCombo(60, li.end != null ? li.end.getMinute() : 0);
                TextField breakF = new TextField(String.valueOf(li.breakAfterMinutes));
                rows.getChildren().add(createLessonCard(i, colors[i % colors.length], sh, sm, eh, em, breakF));
            }
        });

        refreshBells = () -> {
            selector.getItems().setAll(mainApp.getInternalSchedules().stream().map(DaySchedule::getName).toList());
            if (!selector.getItems().isEmpty()) selector.setValue(selector.getItems().get(0));
        };
        refreshBells.run();
        
        ScrollPane mainScroll = new ScrollPane(root);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return mainScroll;
    }

    private Node createLessonCard(int index, String color, ComboBox<String> sh, ComboBox<String> sm, ComboBox<String> eh, ComboBox<String> em, TextField breakF) {
        HBox row = new HBox(40);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 25, 18, 25));
        row.setMinHeight(90);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e9edf3; -fx-border-radius: 16;");
        row.setEffect(new DropShadow(12, Color.rgb(0,0,0,0.04)));

        HBox left = new HBox(15);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMinWidth(120);

        StackPane stripe = new StackPane();
        stripe.setPrefSize(44, 44);
        stripe.setMinSize(44, 44);
        stripe.setMaxSize(44, 44);
        stripe.setStyle("-fx-background-color: derive(" + color + ", 85%); -fx-background-radius: 10;");
        
        Label num = new Label(String.format("%02d", index + 1));
        num.setStyle("-fx-text-fill: derive(" + color + ", -20%); -fx-font-weight: 900; -fx-font-size: 18px;");
        stripe.getChildren().add(num);

        Label title = new Label((index + 1) + " урок");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");
        left.getChildren().addAll(stripe, title);

        HBox timeBlock = new HBox(25);
        timeBlock.setAlignment(Pos.CENTER_LEFT);
        Label dash = new Label("—");
        dash.setStyle("-fx-font-size: 20px; -fx-text-fill: #dfe6e9; -fx-padding: 15 0 0 0;");
        Region separator = new Region();
        separator.setPrefSize(1, 40);
        separator.setStyle("-fx-background-color: #e9edf3;");
        HBox.setMargin(separator, new Insets(0, 10, 0, 10));

        timeBlock.getChildren().addAll(createTimeBox("Початок", sh, sm), dash, createTimeBox("Кінець", eh, em), separator, createBreakBox(breakF));
        row.getChildren().addAll(left, timeBlock);
        return row;
    }

    private VBox createTimeBox(String title, ComboBox<String> h, ComboBox<String> m) {
        Label label = new Label(title.toUpperCase()); 
        label.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        String comboStyle = "-fx-font-size: 15px; -fx-background-color: #f8f9fb; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-font-weight: bold;";
        h.setStyle(comboStyle); h.setPrefWidth(85); h.setPrefHeight(40);
        m.setStyle(comboStyle); m.setPrefWidth(85); m.setPrefHeight(40);
        HBox hBox = new HBox(8, h, new Label(":"), m);
        hBox.setAlignment(Pos.CENTER_LEFT);
        ((Label)hBox.getChildren().get(1)).setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #dfe6e9;");
        return new VBox(6, label, hBox);
    }

    private VBox createBreakBox(TextField breakF) {
        Label label = new Label("ПЕРЕРВА ПІСЛЯ УРОКУ"); 
        label.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 9px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        breakF.setPrefWidth(70); breakF.setPrefHeight(40);
        breakF.setStyle("-fx-font-size: 15px; -fx-background-color: #f8f9fb; -fx-background-radius: 8; -fx-border-color: #dfe6e9; -fx-border-radius: 8; -fx-font-weight: bold; -fx-padding: 0 10;");
        HBox inputArea = new HBox(10, createSVGIcon(ICON_CLOCK, Color.web(COLOR_PRIMARY), 18), breakF, new Label("хв"));
        inputArea.setAlignment(Pos.CENTER_LEFT);
        ((Label)inputArea.getChildren().get(2)).setStyle("-fx-font-weight: bold; -fx-text-fill: #636e72;");
        return new VBox(6, label, inputArea);
    }

    private Button createToolbarButton(String text, String iconPath, String textColor, String bgColor, String borderColor) {
        Button b = new Button(text);
        b.setGraphic(createSVGIcon(iconPath, Color.web(textColor), 16));
        b.setGraphicTextGap(10);
        
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: %s; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 8 20; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 1.2; " +
            "-fx-border-radius: 10; " +
            "-fx-cursor: hand;", 
            bgColor, textColor, borderColor
        );
        
        b.setStyle(baseStyle);
        b.setOnMouseEntered(e -> b.setStyle(baseStyle + "-fx-background-color: derive(" + bgColor + ", -5%); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);"));
        b.setOnMouseExited(e -> b.setStyle(baseStyle));
        
        return b;
    }
}
