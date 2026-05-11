package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.UIComponents.createManagementCard;
import static com.schoolbell.ui.UIComponents.createSectionHeader;
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

        VBox headerArea = createSectionHeader(
                "База школи",
                "Керування академічними даними закладу: вчителі, класи та аудиторії",
                COLOR_PRIMARY,
                ICON_FOLDER
        );

        FlowPane grid = new FlowPane(25, 25);
        grid.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(grid, Priority.ALWAYS);
        
        grid.getChildren().addAll(
            createManagementCard("Вчителі", "Керування списком вчителів та їх спеціалізацією", ICON_PERSON, "#0984e3", "#e3f2fd", () -> mainApp.showEditorTab(1)),
            createManagementCard("Предмети", "Довідник навчальних предметів", ICON_BOOK, "#00b894", "#e8f8f5", () -> mainApp.showEditorTab(2)),
            createManagementCard("Класи", "Список навчальних класів та груп", ICON_CLASS, "#a29bfe", "#f3efff", () -> mainApp.showEditorTab(3)),
            createManagementCard("Аудиторії", "Перелік кабінетів та залів", ICON_ROOM, "#e84393", "#fff0f6", () -> mainApp.showEditorTab(7))
        );
        
        root.getChildren().addAll(headerArea, grid);
        
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return scroll;
    }
}
