package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class CardFactory {

    public static VBox createManagementCard(String title, String description, String iconPath, String iconColor, String bgColor, Runnable action) {
        VBox card = new VBox(16);
        card.setMinWidth(280);
        card.setPrefWidth(280);
        card.setMinHeight(200);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);");
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.QUALITY);

        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(iconColor), 44));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(80, 80);
        iconBox.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 24;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2d3436;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72; -fx-line-spacing: 1.2px;");

        card.getChildren().addAll(iconBox, titleLabel, descLabel);
        card.setOnMouseClicked(e -> action.run());
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 5);"));
        
        return card;
    }

    public static VBox createSmallInfoCard(String title, Node valueNode, String actionText, Runnable action, String iconPath, String circleColor, String iconColor, boolean hasSlider, int volumeValue, java.util.function.Consumer<Integer> sliderAction) {
        VBox card = new VBox();
        card.setPadding(new Insets(25, 30, 25, 30));
        card.setMinWidth(400);
        card.setMinHeight(145);
        card.setMaxWidth(Double.MAX_VALUE);
        String baseStyle = "-fx-background-color: white; -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 6); -fx-border-color: #f1f2f6; -fx-border-width: 1.5; -fx-border-radius: 24;";
        card.setStyle(baseStyle);
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        HBox layout = new HBox(30);
        layout.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(layout, Priority.ALWAYS);
        
        VBox iconCircle = new VBox(createSVGIcon(iconPath, Color.web(iconColor), 42));
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.setPrefSize(82, 82);
        iconCircle.setMinSize(82, 82);
        iconCircle.setStyle("-fx-background-color: " + circleColor + "; -fx-background-radius: 22; -fx-effect: dropshadow(three-pass-box, " + iconColor + "22, 12, 0, 0, 5);");
        
        VBox textStack = new VBox(6);
        textStack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textStack, Priority.ALWAYS);
        
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-letter-spacing: 1.2px; -fx-text-transform: uppercase;");
        
        if (valueNode instanceof Label l) {
            l.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        }
        
        textStack.getChildren().addAll(t, valueNode);
        
        if (actionText != null && action != null) {
            Hyperlink link = new Hyperlink(actionText + "  →");
            link.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 13px; -fx-padding: 5 0 0 0; -fx-underline: false; -fx-font-weight: 800;");
            link.setFocusTraversable(false);
            link.setOnAction(e -> action.run());
            textStack.getChildren().add(link);
        }
        
        layout.getChildren().addAll(iconCircle, textStack);
        card.getChildren().add(layout);
        
        card.setOnMouseEntered(e -> {
            card.setStyle(baseStyle + "-fx-background-color: #fcfcfc;");
            card.setEffect(new DropShadow(javafx.scene.effect.BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.08), 20, 0, 0, 8));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            card.setEffect(new DropShadow(javafx.scene.effect.BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.06), 15, 0, 0, 6));
        });
        
        return card;
    }

    public static VBox createMiniLessonCard(String header, Label lessonLabel, Label statusBadge, String color, Label timeLabel, Label subjectLabel, Label roomLabel, String iconPath) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle(SOFT_CARD);
        
        Label h = new Label(header); h.setStyle(HEADER_STYLE);
        
        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(color), 28));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(50, 50);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 50;");
        
        VBox info = new VBox(2);
        lessonLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        statusBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-padding: 2 6; -fx-background-radius: 4;");
        HBox titleBox = new HBox(12, lessonLabel, statusBadge); titleBox.setAlignment(Pos.CENTER_LEFT);
        
        timeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        subjectLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        roomLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        
        info.getChildren().addAll(titleBox, new HBox(10, timeLabel, new Label("|"), roomLabel), subjectLabel);
        ((Label)((HBox)info.getChildren().get(1)).getChildren().get(1)).setStyle("-fx-text-fill: #dfe6e9;");
        
        content.getChildren().addAll(iconBox, info);
        card.getChildren().addAll(h, content);
        return card;
    }

    public static VBox createDetailedLessonCard(String header, Label lessonLabel, Label statusBadge, String color, Label timeLabel, Label subjectLabel, Label roomLabel, String iconPath, ProgressBar progress, Label progressText) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle(SOFT_CARD);
        
        Label h = new Label(header); h.setStyle(HEADER_STYLE);
        
        HBox content = new HBox(20);
        content.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(color), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 50;");
        
        VBox info = new VBox(4);
        lessonLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        statusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: white; -fx-background-color: " + color + "; -fx-padding: 3 10; -fx-background-radius: 6;");
        HBox titleBox = new HBox(15, lessonLabel, statusBadge); titleBox.setAlignment(Pos.CENTER_LEFT);
        
        timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT_DIM + ";");
        subjectLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        roomLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-weight: bold;");
        
        HBox metaBox = new HBox(12, timeLabel, new Label("|"), roomLabel);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        ((Label)metaBox.getChildren().get(1)).setStyle("-fx-text-fill: #dfe6e9;");
        
        VBox progressBox = new VBox(5, progress, new HBox(progressText));
        ((HBox)progressBox.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);
        
        info.getChildren().addAll(titleBox, metaBox, subjectLabel, progressBox);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        content.getChildren().addAll(iconBox, info);
        card.getChildren().addAll(h, content);
        return card;
    }

    public static VBox createHelpCard(String iconPath, String title, String description, String iconColor) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #f1f2f6; -fx-border-width: 1.5; -fx-border-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4);");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(iconColor), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-color: " + iconColor + "15; -fx-background-radius: 10;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        
        header.getChildren().addAll(iconBox, titleLabel);
        
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72; -fx-line-spacing: 1.2px;");
        
        card.getChildren().addAll(header, descLabel);
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 16; -fx-border-color: #dfe6e9; -fx-border-width: 1.5; -fx-border-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 15, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #f1f2f6; -fx-border-width: 1.5; -fx-border-radius: 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4);"));
        
        return card;
    }

    public static VBox createSideHelpPanel(Node... cards) {
        VBox panel = new VBox(15);
        panel.setPrefWidth(320);
        panel.setMinWidth(320);
        
        Label header = new Label("ДОВІДКА ТА ПОРАДИ");
        header.setStyle(HEADER_STYLE + "-fx-padding: 0 0 5 10;");
        
        panel.getChildren().add(header);
        panel.getChildren().addAll(cards);
        return panel;
    }

    public static Button createCardActionButton(String iconPath, String hoverBg, String hoverBorder) {
        Button btn = new Button();
        btn.setGraphic(createSVGIcon(iconPath, Color.web(COLOR_NEUTRAL), 18));
        btn.setPrefSize(40, 40);
        btn.setMinSize(40, 40);
        
        String baseStyle = "-fx-background-color: rgba(248,250,252,0.96); " +
                           "-fx-background-radius: 999; " +
                           "-fx-border-color: rgba(226,232,240,0.85); " +
                           "-fx-border-width: 1; " +
                           "-fx-border-radius: 999; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 10, 0, 0, 3);";
        
        btn.setStyle(baseStyle);
        
        btn.setOnMouseEntered(e -> {
            btn.setStyle(baseStyle + 
                "-fx-background-color: " + hoverBg + "; " +
                "-fx-border-color: " + hoverBorder + "; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.16), 14, 0, 0, 4);");
            if (hoverBorder.equals(COLOR_DANGER)) {
                btn.setGraphic(createSVGIcon(iconPath, Color.web(COLOR_DANGER), 18));
            } else {
                btn.setGraphic(createSVGIcon(iconPath, Color.web(COLOR_PRIMARY), 18));
            }
        });
        
        btn.setOnMouseExited(e -> {
            btn.setStyle(baseStyle);
            btn.setGraphic(createSVGIcon(iconPath, Color.web(COLOR_NEUTRAL), 18));
        });
        
        return btn;
    }
}
