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

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.createPrimaryActionButton;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIComponents.createSVGIcon;
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

        Button addBtn = createPrimaryActionButton("СТВОРИТИ ОГОЛОШЕННЯ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, "#6c5ce7"));
        addBtn.setOnAction(e -> openEditDialog(null));

        // Segmented Toggle for Active/Archive
        HBox toggleGroup = new HBox(0);
        toggleGroup.setStyle("-fx-background-color: #f1f2f6; -fx-background-radius: 14; -fx-padding: 3; -fx-effect: inset 0 1 2 rgba(0,0,0,0.1);");

        ToggleButton activeBtn = new ToggleButton("АКТИВНІ");
        ToggleButton archiveBtn = new ToggleButton("АРХІВ");
        ToggleGroup group = new ToggleGroup();
        activeBtn.setToggleGroup(group);
        archiveBtn.setToggleGroup(group);
        activeBtn.setSelected(true);

        String activeStyle = "-fx-background-color: white; -fx-text-fill: #6c5ce7; -fx-background-radius: 11; -fx-font-weight: 900; -fx-padding: 8 25; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-background-radius: 11; -fx-font-weight: 900; -fx-padding: 8 25; -fx-cursor: hand;";

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

        VBox cardsContainer = new VBox(20);
        cardsContainer.setPadding(new Insets(10));

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
                empty.setPadding(new Insets(80, 0, 80, 0));
                Node emptyIcon = createSVGIcon(ICON_INFO, Color.web("#dfe6e9"), 64);
                Label emptyLabel = new Label(showArchived ? "Архів оголошень порожній" : "Немає активних оголошень");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #b2bec3;");
                empty.getChildren().addAll(emptyIcon, emptyLabel);
                cardsContainer.getChildren().add(empty);
            } else {
                for (Announcement a : filtered) {
                    cardsContainer.getChildren().add(createAnnouncementCard(a));
                }
            }
        };

        content.getChildren().addAll(headerArea, actionToolbar, cardsContainer);
        refreshList.run();
        return content;
    }

    private Node createAnnouncementCard(Announcement a) {
        VBox card = new VBox(20);
        card.setStyle(SOFT_CARD + "-fx-padding: 25; -fx-border-color: #f1f2f6; -fx-border-radius: 20;");

        HBox top = new HBox(25);
        top.setAlignment(Pos.TOP_LEFT);

        VBox iconBox = new VBox(createSVGIcon(ICON_BROADCAST, Color.web(a.isActive() ? "#6c5ce7" : COLOR_TEXT_DIM), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setMinSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + (a.isActive() ? "#6c5ce715" : "#f1f2f6") + "; -fx-background-radius: 18;");

        VBox info = new VBox(10);
        Label textLabel = new Label(a.text());
        textLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 16px; -fx-text-fill: #2d3436;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(800);

        FlowPane badges = new FlowPane(12, 10);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.setPrefWrapLength(700); // Trigger wrap earlier for better readability on laptops

        String dateRange = (a.startDate() != null ? a.startDate().format(DATE_FORMATTER) : "...") + " – " + (a.endDate() != null ? a.endDate().format(DATE_FORMATTER) : "...");
        Label dateBadge = createBadge(dateRange, COLOR_PRIMARY, ICON_CALENDAR);
        
        String timeRange = (a.startTime() != null ? a.startTime() : "...") + " – " + (a.endTime() != null ? a.endTime() : "...");
        Label timeBadge = createBadge(timeRange, COLOR_SUCCESS, ICON_CLOCK);
        
        badges.getChildren().addAll(dateBadge, timeBadge);
        
        if (a.daysOfWeek() != null && !a.daysOfWeek().isEmpty()) {
            badges.getChildren().add(createBadge(getDaysText(a.daysOfWeek()).toUpperCase(), "#e67e22", ICON_CALENDAR));
        }

        info.getChildren().addAll(textLabel, badges);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.TOP_RIGHT);
        
        Button editBtn = createCardActionButton(ICON_EDIT, "#f1f2f6", COLOR_PRIMARY);
        editBtn.setOnAction(e -> openEditDialog(a));
        
        Button delBtn = createCardActionButton(ICON_TRASH, "#fff5f5", COLOR_DANGER);
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
        l.setGraphicTextGap(8);
        l.setStyle("-fx-background-color: " + color + "10; -fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 5 12; -fx-background-radius: 10; -fx-border-color: " + color + "25; -fx-border-radius: 10;");
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

        Label startDL = new Label("Початок (дата):"); startDL.setStyle(HEADER_STYLE);
        grid.add(startDL, 0, 0); grid.add(startPicker, 1, 0);
        
        Label endDL = new Label("Кінець (дата):"); endDL.setStyle(HEADER_STYLE);
        grid.add(endDL, 0, 1); grid.add(endPicker, 1, 1);
        
        Label startTL = new Label("Початок (час):"); startTL.setStyle(HEADER_STYLE);
        grid.add(startTL, 0, 2); grid.add(startTimeField, 1, 2);
        
        Label endTL = new Label("Кінець (час):"); endTL.setStyle(HEADER_STYLE);
        grid.add(endTL, 0, 3); grid.add(endTimeField, 1, 3);
        
        Label daysL = new Label("Дні тижня:"); daysL.setStyle(HEADER_STYLE);
        grid.add(daysL, 0, 4); grid.add(daysBox, 1, 4);

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

        Label textL = new Label("ТЕКСТ ПОВІДОМЛЕННЯ:");
        textL.setStyle(HEADER_STYLE);
        root.getChildren().addAll(header, textL, textArea, grid, activeCb, new HBox(saveBtn));
        ((HBox)root.getChildren().get(root.getChildren().size()-1)).setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 550, 720);
        scene.getStylesheets().add("data:text/css," + MODERN_DATE_PICKER_STYLE.replace(" ", "%20"));
        stage.setScene(scene);
        stage.showAndWait();
    }
}
