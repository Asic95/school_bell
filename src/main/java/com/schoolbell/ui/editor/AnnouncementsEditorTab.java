package com.schoolbell.ui.editor;

import com.schoolbell.MainApp;
import com.schoolbell.model.Announcement;
import com.schoolbell.service.AnnouncementService;
import com.schoolbell.ui.AnnouncementEditorDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.schoolbell.ui.CardFactory.createCardActionButton;
import static com.schoolbell.ui.ControlFactory.*;
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
        content.setStyle("-fx-background-color: " + COLOR_SURFACE_CANVAS + ";");

        VBox headerArea = createSectionHeader("Оголошення для табло", "Створюйте та плануйте показ важливих повідомлень за часом та днями", COLOR_INDIGO_DARK, ICON_BROADCAST);

        HBox actionToolbar = new HBox(20);
        actionToolbar.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = createPrimaryActionButton("СТВОРИТИ ОГОЛОШЕННЯ", ICON_PLUS);
        addBtn.setStyle(addBtn.getStyle().replace(COLOR_PRIMARY, COLOR_INDIGO_DARK));
        addBtn.setOnAction(e -> openEditDialog(null));

        // Segmented Toggle for Active/Archive
        HBox toggleGroup = new HBox(0);
        toggleGroup.setStyle(PREMIUM_TOGGLE_CONTAINER);

        ToggleButton activeBtn = new ToggleButton("АКТИВНІ");
        ToggleButton archiveBtn = new ToggleButton("АРХІВ");
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
            refreshList.run();
        });

        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionToolbar.getChildren().addAll(addBtn, spacer, toggleGroup);

        VBox cardsContainer = new VBox(20);
        cardsContainer.setPadding(new Insets(10));
        
        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

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
                String title = showArchived ? "Архів оголошень порожній" : "Немає активних оголошень";
                String sub = showArchived ? "Тут з'являтимуться оголошення, термін дії яких минув." : "Натисніть 'СТВОРИТИ ОГОЛОШЕННЯ', щоб додати перше повідомлення.";
                cardsContainer.getChildren().add(createEmptyState(ICON_INFO, title, sub));
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
        VBox card = new VBox(20);
        card.setStyle(SOFT_CARD + "-fx-padding: 28;");

        HBox top = new HBox(25);
        top.setAlignment(Pos.TOP_LEFT);

        VBox iconBox = new VBox(createSVGIcon(ICON_BROADCAST, Color.web(a.isActive() ? COLOR_PRIMARY : COLOR_ZINC_500), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setMinSize(64, 64);
        iconBox.setStyle(ICON_BADGE_STYLE);

        VBox info = new VBox(12);
        Label textLabel = new Label(a.text());
        textLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 17px; -fx-text-fill: " + COLOR_NAVY + ";");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(800);

        FlowPane badges = new FlowPane(12, 10);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.setPrefWrapLength(700);

        String dateRange = (a.startDate() != null ? a.startDate().format(DATE_FORMATTER) : "...") + " – " + (a.endDate() != null ? a.endDate().format(DATE_FORMATTER) : "...");
        Label dateBadge = createBadge(dateRange, COLOR_PRIMARY, ICON_CALENDAR);
        
        String timeRange = (a.startTime() != null ? a.startTime() : "...") + " – " + (a.endTime() != null ? a.endTime() : "...");
        Label timeBadge = createBadge(timeRange, COLOR_SUCCESS, ICON_CLOCK);
        
        badges.getChildren().addAll(dateBadge, timeBadge);
        
        if (a.daysOfWeek() != null && !a.daysOfWeek().isEmpty()) {
            badges.getChildren().add(createBadge(getDaysText(a.daysOfWeek()).toUpperCase(), COLOR_ORANGE, ICON_CALENDAR));
        }

        info.getChildren().addAll(textLabel, badges);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.TOP_RIGHT);
        
        Button editBtn = createCardActionButton(ICON_EDIT, COLOR_SURFACE_SUBTLE, COLOR_PRIMARY);
        editBtn.setOnAction(e -> openEditDialog(a));
        
        Button delBtn = createCardActionButton(ICON_TRASH, COLOR_DANGER_LIGHT, COLOR_DANGER);
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
        l.setStyle("-fx-background-color: " + color + "10; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 6 12; -fx-background-radius: 12; -fx-border-color: " + color + "25; -fx-border-radius: 12;");
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
        new AnnouncementEditorDialog(mainApp.getStage(), announcementService, a, refreshList).display();
    }
}
