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
import static com.schoolbell.ui.UIStyles.*;

public class SchoolView {
    private final MainApp mainApp;

    public SchoolView(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public Node build() {
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        HBox header = createPageHeader(
            "АКАДЕМІЧНІ ДАНІ",
            "База школи",
            "Керування академічними даними закладу: вчителі, предмети, класи та аудиторії.",
            ICON_FOLDER,
            COLOR_PRIMARY,
            null
        );

        FlowPane grid = new FlowPane(25, 25);
        grid.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(grid, Priority.ALWAYS);
        
        grid.getChildren().addAll(
            createManagementCard("Вчителі", "Керування списком вчителів та їх спеціалізацією", ICON_PERSON, COLOR_SKY, COLOR_BLUE_LIGHT, () -> mainApp.showEditorTab(1)),
            createManagementCard("Предмети", "Довідник навчальних предметів", ICON_BOOK, COLOR_TEAL, COLOR_GREEN_LIGHT, () -> mainApp.showEditorTab(2)),
            createManagementCard("Класи", "Список навчальних класів та груп", ICON_CLASS, COLOR_PURPLE, COLOR_PURPLE_LIGHT, () -> mainApp.showEditorTab(3)),
            createManagementCard("Аудиторії", "Перелік кабінетів та залів", ICON_ROOM, COLOR_CYAN, COLOR_CYAN_LIGHT, () -> mainApp.showEditorTab(7))
        );
        
        root.getChildren().addAll(header, grid);
        
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
