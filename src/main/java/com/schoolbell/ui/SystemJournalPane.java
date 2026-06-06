package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
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
            "-fx-effect: dropshadow(three-pass-box, " + SHADOW_NAVY_06 + ", 12, 0, 0, 6);" +
            "-fx-border-color: " + BORDER_SLATE_50 + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 28;"
        );

        Label title = new Label("ЖУРНАЛ СИСТЕМНИХ ПОДІЙ");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 1.5px; -fx-text-transform: uppercase;");

        Button exportBtn = new Button("ЕКСПОРТ ЖУРНАЛУ");
        exportBtn.setGraphic(UIComponents.createSVGIcon(ICON_SAVE, Color.WHITE, 16));
        exportBtn.setStyle(PREMIUM_BTN_STYLE + "-fx-font-size: 11px; -fx-padding: 10 20; -fx-background-radius: 14;");
        exportBtn.setOnAction(e -> mainApp.getSystemService().exportLogs((javafx.stage.Stage) getScene().getWindow()));

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(15, title, new javafx.scene.layout.Region() {{ javafx.scene.layout.HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS); }}, exportBtn);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ListView<String> logList = new ListView<>(mainApp.getSystemLogs());
        logList.setPrefHeight(300);
        VBox.setVgrow(logList, Priority.ALWAYS);
        
        logList.setCellFactory(lv -> new ListCell<>() {
            private String lastStyle = "";

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    if (getText() != null) setText(null);
                    if (getGraphic() != null) setGraphic(null);
                    if (!"".equals(lastStyle)) {
                        setStyle("-fx-background-color: transparent;");
                        lastStyle = "";
                    }
                } else {
                    if (!item.equals(getText())) {
                        setText(item);
                    }
                    setFont(javafx.scene.text.Font.font("Inter", 13));
                    setPadding(new Insets(6, 12, 6, 12));
                    
                    String newStyle;
                    String baseStyle = "-fx-background-radius: 8; -fx-margin: 2 0; ";
                    if (item.contains("[ERROR]")) {
                        newStyle = baseStyle + "-fx-background-color: " + COLOR_DANGER_LIGHT + "; -fx-text-fill: " + COLOR_RED_SOFT + "; -fx-font-weight: bold;";
                    } else if (item.contains("[SUCCESS]")) {
                        newStyle = baseStyle + "-fx-background-color: " + COLOR_SUCCESS_LIGHT + "; -fx-text-fill: " + COLOR_GREEN + "; -fx-font-weight: bold;";
                    } else if (item.contains("[WARNING]")) {
                        newStyle = baseStyle + "-fx-background-color: " + COLOR_DANGER_PALE + "; -fx-text-fill: " + COLOR_ORANGE_DARK + "; -fx-font-weight: bold;";
                    } else {
                        newStyle = baseStyle + "-fx-background-color: transparent; -fx-text-fill: " + COLOR_SLATE_STRONG + ";";
                    }

                    if (!newStyle.equals(lastStyle)) {
                        setStyle(newStyle);
                        lastStyle = newStyle;
                    }
                }
            }
        });

        // Clean ListView style
        logList.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        getChildren().addAll(header, logList);
    }
}
