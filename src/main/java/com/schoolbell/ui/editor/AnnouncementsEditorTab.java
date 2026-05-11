package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Announcement;
import com.schoolbell.service.AnnouncementService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.UIComponents.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class AnnouncementsEditorTab {
    private final MainApp mainApp;
    private final AnnouncementService announcementService;
    private Runnable refreshList;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private boolean showArchived = false;

    public AnnouncementsEditorTab(MainApp mainApp) {
        this.mainApp = mainApp;
        this.announcementService = new AnnouncementService();
    }

    public Node createContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f8f9fa;");

        VBox headerArea = createSectionHeader("Оголошення для табло", "Створюйте та плануйте показ важливих повідомлень за часом та днями", "#6c5ce7", ICON_BROADCAST);

        HBox actionToolbar = new HBox(20);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("СТВОРИТИ ОГОЛОШЕННЯ");
        String addBtnStyle = "-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-weight: 900; -fx-padding: 10 25; -fx-background-radius: 12; -fx-cursor: hand;";
        addBtn.setStyle(addBtnStyle);
        addBtn.setOnAction(e -> openEditDialog(null));

        // Segmented Toggle for Active/Archive
        HBox toggleGroup = new HBox(0);
        toggleGroup.setStyle("-fx-background-color: #dfe6e9; -fx-background-radius: 12; -fx-padding: 2;");

        ToggleButton activeBtn = new ToggleButton("Активні");
        ToggleButton archiveBtn = new ToggleButton("Архів");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        String activeStyle = "-fx-background-color: white; -fx-text-fill: #6c5ce7; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-background-radius: 10; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;";

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
            refreshList.run();
        });

        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionToolbar.getChildren().addAll(addBtn, spacer, toggleGroup);

        VBox cardsContainer = new VBox(15);
        cardsContainer.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        refreshList = () -> {
            cardsContainer.getChildren().clear();
            List<Announcement> list = announcementService.getAllAnnouncements();
            LocalDate today = LocalDate.now();

            List<Announcement> filtered = list.stream()
                .filter(a -> {
                    boolean isExpired = a.endDate() != null && a.endDate().isBefore(today);
                    boolean isEffectivelyArchived = !a.isActive() || isExpired;
                    return showArchived == isEffectivelyArchived;
                })
                .sorted((a, b) -> {
                    if (showArchived) {
                        LocalDate d1 = a.endDate() != null ? a.endDate() : LocalDate.MAX;
                        LocalDate d2 = b.endDate() != null ? b.endDate() : LocalDate.MAX;
                        return d2.compareTo(d1);
                    } else {
                        LocalDate d1 = a.startDate() != null ? a.startDate() : LocalDate.MIN;
                        LocalDate d2 = b.startDate() != null ? b.startDate() : LocalDate.MIN;
                        return d1.compareTo(d2);
                    }
                })
                .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                VBox empty = new VBox(20);
                empty.setAlignment(Pos.CENTER);
                empty.setPadding(new Insets(60, 0, 60, 0));
                Label emptyIcon = new Label("∅");
                emptyIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: #dfe6e9;");
                Label emptyLabel = new Label(showArchived ? "Архів порожній" : "Немає активних оголошень");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b2bec3;");
                empty.getChildren().addAll(emptyIcon, emptyLabel);
                cardsContainer.getChildren().add(empty);
            } else {
                for (Announcement a : filtered) {
                    cardsContainer.getChildren().add(createAnnouncementCard(a));
                }
            }
        };

        content.getChildren().addAll(headerArea, actionToolbar, scroll);
        refreshList.run();
        return content;
    }

    private Node createAnnouncementCard(Announcement a) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 5); -fx-border-color: #f1f2f6; -fx-border-radius: 20;");

        HBox top = new HBox(20);
        top.setAlignment(Pos.TOP_LEFT);

        VBox iconBox = new VBox(createSVGIcon(ICON_BROADCAST, Color.web(a.isActive() ? "#6c5ce7" : "#95a5a6"), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setStyle("-fx-background-color: " + (a.isActive() ? "#6c5ce715" : "#f1f2f6") + "; -fx-background-radius: 16;");

        VBox info = new VBox(6);
        Label textLabel = new Label(a.text());
        textLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #2d3436;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(800);

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);

        String dateRange = (a.startDate() != null ? a.startDate().format(DATE_FORMATTER) : "...") + " – " + (a.endDate() != null ? a.endDate().format(DATE_FORMATTER) : "...");
        Label dateBadge = createBadge(dateRange, "#0984e3", ICON_CALENDAR);
        
        String timeRange = (a.startTime() != null ? a.startTime() : "...") + " – " + (a.endTime() != null ? a.endTime() : "...");
        Label timeBadge = createBadge(timeRange, "#00b894", ICON_CLOCK);
        
        badges.getChildren().addAll(dateBadge, timeBadge);
        
        if (a.daysOfWeek() != null && !a.daysOfWeek().isEmpty()) {
            badges.getChildren().add(createBadge(getDaysText(a.daysOfWeek()), "#e67e22", ICON_CALENDAR));
        }

        info.getChildren().addAll(textLabel, badges);
        HBox.setHgrow(info, Priority.ALWAYS);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("РЕДАГУВАТИ");
        editBtn.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #636e72; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        editBtn.setOnAction(e -> openEditDialog(a));
        
        Button delBtn = new Button("ВИДАЛИТИ");
        delBtn.setStyle("-fx-background-color: #fff5f5; -fx-text-fill: #ff7675; -fx-font-weight: 900; -fx-font-size: 10px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            announcementService.deleteAnnouncement(a.id());
            refreshList.run();
        });

        actions.getChildren().addAll(editBtn, delBtn);

        top.getChildren().addAll(iconBox, info, actions);
        card.getChildren().add(top);

        return card;
    }

    private Label createBadge(String text, String color, String icon) {
        Label l = new Label(text);
        l.setGraphic(createSVGIcon(icon, Color.web(color), 12));
        l.setGraphicTextGap(6);
        l.setStyle("-fx-background-color: " + color + "15; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 8;");
        return l;
    }

    private String getDaysText(String days) {
        String[] parts = days.split(",");
        String[] dayNames = {"", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"};
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            try {
                int d = Integer.parseInt(p.trim());
                if (d >= 1 && d <= 7) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(dayNames[d]);
                }
            } catch (Exception e) {}
        }
        return sb.toString();
    }

    private void openEditDialog(Announcement a) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(a == null ? "Нове оголошення" : "Редагування оголошення");

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        VBox header = createSectionHeader(a == null ? "Створення" : "Редагування", "Налаштуйте параметри показу", "#6c5ce7", ICON_BROADCAST);

        TextArea textArea = new TextArea(a != null ? a.text() : "");
        textArea.setPromptText("Текст оголошення...");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);
        textArea.setStyle(FIELD_STYLE);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        DatePicker startPicker = new DatePicker(a != null ? a.startDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(a != null ? a.endDate() : LocalDate.now().plusWeeks(1));
        startPicker.setStyle(FIELD_STYLE); endPicker.setStyle(FIELD_STYLE);

        TextField startTimeField = new TextField(a != null && a.startTime() != null ? a.startTime().toString() : "08:00");
        TextField endTimeField = new TextField(a != null && a.endTime() != null ? a.endTime().toString() : "18:00");
        startTimeField.setStyle(FIELD_STYLE); endTimeField.setStyle(FIELD_STYLE);

        HBox daysBox = new HBox(8);
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд"};
        List<CheckBox> dayCbs = new ArrayList<>();
        List<String> activeDays = a != null && a.daysOfWeek() != null ? List.of(a.daysOfWeek().split(",")) : List.of("1","2","3","4","5");
        for (int i = 1; i <= 7; i++) {
            CheckBox cb = new CheckBox(dayNames[i-1]);
            cb.setSelected(activeDays.contains(String.valueOf(i)));
            dayCbs.add(cb);
            daysBox.getChildren().add(cb);
        }

        CheckBox activeCb = new CheckBox("Оголошення активне");
        activeCb.setSelected(a == null || a.isActive());
        activeCb.setStyle("-fx-font-weight: bold;");

        grid.add(new Label("Початок (дата):"), 0, 0); grid.add(startPicker, 1, 0);
        grid.add(new Label("Кінець (дата):"), 0, 1); grid.add(endPicker, 1, 1);
        grid.add(new Label("Початок (час):"), 0, 2); grid.add(startTimeField, 1, 2);
        grid.add(new Label("Кінець (час):"), 0, 3); grid.add(endTimeField, 1, 3);
        grid.add(new Label("Дні тижня:"), 0, 4); grid.add(daysBox, 1, 4);

        Button saveBtn = new Button("ЗБЕРЕГТИ");
        saveBtn.setStyle(BTN_BASE + "-fx-background-color: #27ae60; -fx-padding: 12 50;");
        saveBtn.setOnAction(ev -> {
            String text = textArea.getText().trim();
            if (text.isEmpty()) return;

            String days = dayCbs.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> String.valueOf(dayCbs.indexOf(cb) + 1))
                    .collect(Collectors.joining(","));

            LocalTime st = null, et = null;
            try { st = LocalTime.parse(startTimeField.getText().trim()); } catch (Exception e) {}
            try { et = LocalTime.parse(endTimeField.getText().trim()); } catch (Exception e) {}

            Announcement newA = new Announcement(
                    a != null ? a.id() : 0,
                    text,
                    startPicker.getValue(),
                    endPicker.getValue(),
                    st, et, days,
                    activeCb.isSelected()
            );

            if (a == null) announcementService.addAnnouncement(newA);
            else announcementService.updateAnnouncement(newA);

            refreshList.run();
            stage.close();
        });

        root.getChildren().addAll(header, new Label("ТЕКСТ ПОВІДОМЛЕННЯ:"), textArea, grid, activeCb, new HBox(saveBtn));
        ((HBox)root.getChildren().get(root.getChildren().size()-1)).setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root, 550, 650));
        stage.showAndWait();
    }
}
