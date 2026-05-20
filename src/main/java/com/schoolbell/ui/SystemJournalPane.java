package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 25, 0, 0, 8);" +
            "-fx-border-color: rgba(226, 232, 240, 0.5);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 28;"
        );

        Label title = new Label("ЖУРНАЛ СИСТЕМНИХ ПОДІЙ");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1.2px;");

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
                    setPadding(new Insets(6, 10, 6, 10));
                    
                    if (item.contains("[ERROR]")) {
                        setTextFill(Color.web("#e74c3c"));
                        setStyle("-fx-background-color: #fff5f5; -fx-background-radius: 6; -fx-font-weight: bold;");
                    } else if (item.contains("[SUCCESS]")) {
                        setTextFill(Color.web("#27ae60"));
                        setStyle("-fx-background-color: #f0fff4; -fx-background-radius: 6; -fx-font-weight: bold;");
                    } else if (item.contains("[WARNING]")) {
                        setTextFill(Color.web("#f39c12"));
                        setStyle("-fx-background-color: #fffaf0; -fx-background-radius: 6; -fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.web("#334155"));
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        // Hide ListView background and border
        logList.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0; -fx-control-inner-background: transparent;");

        getChildren().addAll(title, logList);
    }
}
