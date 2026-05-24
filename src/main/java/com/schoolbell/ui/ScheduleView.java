package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.CardFactory.createManagementCard;
import static com.schoolbell.ui.ControlFactory.createPageHeader;
import static com.schoolbell.ui.LayoutUtils.createSectionHeader;
import static com.schoolbell.ui.UIStyles.*;

public class ScheduleView {
    private final MainApp mainApp;

    public ScheduleView(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        HBox header = createPageHeader(
            "РОЗКЛАД ТА ГРАФІКИ",
            "Розклад занять",
            "Керування тижневими графіками, щоденними замінами та часом дзвінків.",
            ICON_CALENDAR,
            COLOR_GREEN,
            null
        );

        FlowPane grid = new FlowPane(25, 25);
        grid.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(grid, Priority.ALWAYS);
        
        grid.getChildren().addAll(
            createManagementCard("Тижневий розклад", "Основний графік занять для всіх класів", ICON_CALENDAR, COLOR_GREEN, COLOR_GREEN_SOFT, () -> mainApp.showEditorTab(4)),
            createManagementCard("Заміни та зміни", "Оперативні зміни в розкладі на конкретні дати", ICON_CLOCK, COLOR_ORANGE, COLOR_ORANGE_SOFT, () -> mainApp.showEditorTab(5)),
            createManagementCard("Час дзвінків", "Налаштування сітки годин (уроки та перерви)", ICON_BELL, COLOR_SKY, COLOR_BLUE_LIGHT, () -> mainApp.showEditorTab(0))
        );
        
        root.getChildren().addAll(header, grid);
        
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
