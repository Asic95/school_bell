package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static com.schoolbell.ui.UIStyles.*;

public class SystemJournalPane extends VBox {
    private final MainApp mainApp;

    public SystemJournalPane(MainApp mainApp) {
        super(15);
        this.mainApp = mainApp;
        setPadding(new Insets(25));
        setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 28;" +
            "-fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_06 + ", 25, 0, 0, 8);" +
            "-fx-border-color: " + BORDER_SLATE_50 + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 28;"
        );

        Label title = new Label("ЖУРНАЛ СИСТЕМНИХ ПОДІЙ");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE_LIGHT + "; -fx-letter-spacing: 1.2px;");

        ListView<String> logList = new ListView<>(mainApp.getSystemLogs());
        logList.setPrefHeight(300);
        VBox.setVgrow(logList, Priority.ALWAYS);
        
        logList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(item);
                    setFont(javafx.scene.text.Font.font("Inter", 13));
                    setPadding(new Insets(6, 12, 6, 12));
                    
                    String baseStyle = "-fx-background-radius: 8; -fx-margin: 2 0; ";
                    if (item.contains("[ERROR]")) {
                        setStyle(baseStyle + "-fx-background-color: " + COLOR_DANGER_LIGHT + "; -fx-text-fill: " + COLOR_RED_SOFT + "; -fx-font-weight: bold;");
                    } else if (item.contains("[SUCCESS]")) {
                        setStyle(baseStyle + "-fx-background-color: " + COLOR_SUCCESS_LIGHT + "; -fx-text-fill: " + COLOR_GREEN + "; -fx-font-weight: bold;");
                    } else if (item.contains("[WARNING]")) {
                        setStyle(baseStyle + "-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-text-fill: " + COLOR_ORANGE_DARK + "; -fx-font-weight: bold;");
                    } else {
                        setStyle(baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + COLOR_SLATE_STRONG + ";");
                    }
                }
            }
        });

        // Clean ListView style
        logList.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        getChildren().addAll(title, logList);
    }
}
