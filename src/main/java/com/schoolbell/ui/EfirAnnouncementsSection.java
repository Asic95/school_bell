package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.model.Announcement;
import com.schoolbell.service.AnnouncementService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.schoolbell.ui.ControlFactory.createEmptyState;
import static com.schoolbell.ui.ControlFactory.createSmallPrimaryActionButton;
import static com.schoolbell.ui.UIStyles.*;

public class EfirAnnouncementsSection extends VBox {
    private final MainApp mainApp;
    private final AnnouncementService announcementService;
    private final VBox announcementsContainer;
    private boolean showArchivedAnnouncements = false;

    public EfirAnnouncementsSection(MainApp mainApp) {
        super(25);
        this.mainApp = mainApp;
        this.announcementService = new AnnouncementService();
        this.announcementsContainer = new VBox(15);

        setPadding(new Insets(28));
        setStyle(SOFT_CARD);

        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(4);
        Label eyebrow = new Label("ОГОЛОШЕННЯ");
        eyebrow.setStyle(HEADER_STYLE + "-fx-font-size: 11px;");
        Label title = new Label("Стрічка ефіру");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        titleBox.getChildren().addAll(eyebrow, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter Toggle Group
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
                showArchivedAnnouncements = false;
            } else {
                activeBtn.setStyle(PREMIUM_TOGGLE_INACTIVE);
                archiveBtn.setStyle(PREMIUM_TOGGLE_ACTIVE);
                showArchivedAnnouncements = true;
            }
            refreshAnnouncements();
        });
        toggleGroup.getChildren().addAll(activeBtn, archiveBtn);

        Button addBtn = createSmallPrimaryActionButton("СТВОРИТИ", ICON_PLUS);
        addBtn.setOnAction(e -> openEditDialog(null));

        HBox actionsRow = new HBox(20, toggleGroup, addBtn);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(titleBox, spacer, actionsRow);

        announcementsContainer.setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(header, announcementsContainer);

        refreshAnnouncements();
    }

    public void refreshAnnouncements() {
        List<Announcement> filtered = announcementService.getAllAnnouncements().stream()
                .filter(a -> a.isActive() != showArchivedAnnouncements)
                .toList();
        
        if (filtered.isEmpty()) {
            String title = showArchivedAnnouncements ? "Архів порожній" : "Оголошень немає";
            String sub = showArchivedAnnouncements ? "Тут з'являтимуться оголошення, термін дії яких минув." : "Натисніть 'СТВОРИТИ', щоб додати перше інформаційне повідомлення.";
            announcementsContainer.getChildren().setAll(createEmptyState(ICON_INFO, title, sub));
        } else {
            updateContainer(announcementsContainer, filtered, a -> new AnnouncementCard(a, () -> openEditDialog(a), () -> {
                announcementService.deleteAnnouncement(a.id());
                refreshAnnouncements();
            }));
        }
    }

    private <T> void updateContainer(Pane container, List<T> items, java.util.function.Function<T, Node> mapper) {
        container.getChildren().setAll(items.stream().map(mapper).toList());
    }

    private void openEditDialog(Announcement a) {
        new AnnouncementEditorDialog(mainApp.getStage(), announcementService, a, this::refreshAnnouncements).display();
    }
}
