package com.schoolbell.ui;

import com.schoolbell.MainApp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Base64;

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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(15, title, spacer, exportBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        ListView<String> logList = new ListView<>(mainApp.getSystemLogs());
        logList.setPrefHeight(320);
        VBox.setVgrow(logList, Priority.ALWAYS);

        Label placeholder = new Label("Поки що немає зареєстрованих системних подій");
        placeholder.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE_LIGHT + ";");
        logList.setPlaceholder(placeholder);

        logList.setCellFactory(lv -> new ListCell<>() {
            private final HBox row = new HBox(6);
            private final Label timeLabel = new Label();
            private final Label badgeLabel = new Label();
            private final Label messageLabel = new Label();

            {
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5, 10, 5, 10));

                timeLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_SLATE + ";");
                timeLabel.setMinWidth(130);
                timeLabel.setPrefWidth(130);

                badgeLabel.setAlignment(Pos.CENTER);
                badgeLabel.setMinWidth(72);
                badgeLabel.setPrefWidth(72);

                messageLabel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: " + COLOR_NAVY + ";");
                messageLabel.setWrapText(true);
                HBox.setHgrow(messageLabel, Priority.ALWAYS);

                row.getChildren().addAll(timeLabel, badgeLabel, messageLabel);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);

                    String timestamp = "";
                    String level = "INFO";
                    String message = item;

                    int firstClose = item.indexOf(']');
                    if (item.startsWith("[") && firstClose > 0) {
                        timestamp = item.substring(1, firstClose).trim();
                        int secondOpen = item.indexOf('[', firstClose);
                        if (secondOpen > 0) {
                            int secondClose = item.indexOf(']', secondOpen);
                            if (secondClose > 0) {
                                level = item.substring(secondOpen + 1, secondClose).trim();
                                message = item.substring(secondClose + 1).trim();
                            }
                        }
                    }

                    timeLabel.setText(timestamp);
                    messageLabel.setText(message);

                    String badgeText;
                    String badgeStyle;
                    String msgStyle = "-fx-font-family: 'Inter'; -fx-font-size: 13px;";

                    switch (level.toUpperCase()) {
                        case "ERROR" -> {
                            badgeText = "ПОМИЛКА";
                            badgeStyle = "-fx-background-color: " + COLOR_DANGER_LIGHT + "; -fx-text-fill: " + COLOR_DANGER + "; -fx-border-color: " + COLOR_DANGER_BORDER + ";";
                            msgStyle += " -fx-font-weight: 700; -fx-text-fill: " + COLOR_DANGER + ";";
                        }
                        case "WARNING" -> {
                            badgeText = "УВАГА";
                            badgeStyle = "-fx-background-color: #fffbeb; -fx-text-fill: " + COLOR_WARNING + "; -fx-border-color: #fde68a;";
                            msgStyle += " -fx-font-weight: 700; -fx-text-fill: " + COLOR_WARNING + ";";
                        }
                        case "SUCCESS" -> {
                            badgeText = "УСПІХ";
                            badgeStyle = "-fx-background-color: " + COLOR_SUCCESS_LIGHT + "; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-border-color: #bbf7d0;";
                            msgStyle += " -fx-font-weight: 600; -fx-text-fill: " + COLOR_SUCCESS + ";";
                        }
                        default -> {
                            badgeText = "ІНФО";
                            badgeStyle = "-fx-background-color: " + COLOR_SURFACE_SOFT + "; -fx-text-fill: " + COLOR_SLATE + "; -fx-border-color: " + COLOR_BORDER_SOFT + ";";
                            msgStyle += " -fx-font-weight: 500; -fx-text-fill: " + COLOR_NAVY + ";";
                        }
                    }

                    badgeLabel.setText(badgeText);
                    badgeLabel.setStyle(
                        "-fx-font-family: 'Inter'; -fx-font-size: 10px; -fx-font-weight: 900; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1; " +
                        "-fx-padding: 2 6; " + badgeStyle
                    );

                    messageLabel.setStyle(msgStyle);

                    setGraphic(row);
                }
            }
        });

        // Modern Soft UI styling for ListView
        String listCss =
            ".list-view { -fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0; } " +
            ".list-cell { -fx-background-color: transparent; -fx-padding: 2 4; } " +
            ".list-cell:filled:hover { -fx-background-color: rgba(241, 245, 249, 0.6); -fx-background-radius: 10; } " +
            ".list-cell:filled:selected { -fx-background-color: #f1f5f9; -fx-background-radius: 10; }";
        logList.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString(listCss.getBytes()));

        getChildren().addAll(header, logList);
    }
}
