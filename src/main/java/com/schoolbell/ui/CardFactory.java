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
        VBox card = new VBox(20);
        card.setMinWidth(300);
        card.setPrefWidth(300);
        card.setMinHeight(240);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);
        card.setStyle(SOFT_CARD + "-fx-cursor: hand;");
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.QUALITY);

        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(COLOR_PRIMARY), 44));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(84, 84);
        iconBox.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_GLASS_END + "); -fx-background-radius: 22;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_SLATE + "; -fx-line-spacing: 1.2px;");

        card.getChildren().addAll(iconBox, titleLabel, descLabel);
        card.setOnMouseClicked(e -> action.run());
        card.setOnMouseEntered(e -> card.setStyle(SOFT_CARD + "-fx-cursor: hand; -fx-background-color: " + COLOR_SURFACE_ELEVATED + "; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.12), 40, 0, 0, 15);"));
        card.setOnMouseExited(e -> card.setStyle(SOFT_CARD + "-fx-cursor: hand;"));
        
        return card;
    }

    public static VBox createSmallInfoCard(String title, Node valueNode, String actionText, Runnable action, String iconPath, String circleColor, String iconColor, boolean hasSlider, int volumeValue, java.util.function.Consumer<Integer> sliderAction) {
        VBox card = new VBox();
        card.setPadding(new Insets(28, 32, 28, 32));
        card.setMinWidth(420);
        card.setMinHeight(160);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(SOFT_CARD);
        card.setCache(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);
        
        HBox layout = new HBox(30);
        layout.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(layout, Priority.ALWAYS);
        
        VBox iconCircle = new VBox(createSVGIcon(iconPath, Color.web(iconColor), 42));
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.setPrefSize(84, 84);
        iconCircle.setMinSize(84, 84);
        iconCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, " + COLOR_SURFACE_GLASS_START + ", " + COLOR_SURFACE_GLASS_END + "); -fx-background-radius: 22; -fx-effect: dropshadow(three-pass-box, " + iconColor + "15, 12, 0, 0, 5);");
        
        VBox textStack = new VBox(8);
        textStack.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textStack, Priority.ALWAYS);
        
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_SLATE + "; -fx-letter-spacing: 1.5px; -fx-text-transform: uppercase;");
        
        if (valueNode instanceof Label l) {
            l.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_NAVY + ";");
        }
        
        textStack.getChildren().addAll(t, valueNode);
        
        if (actionText != null && action != null) {
            Hyperlink link = new Hyperlink(actionText + "  →");
            link.setStyle("-fx-text-fill: " + COLOR_INDIGO + "; -fx-font-size: 14px; -fx-padding: 5 0 0 0; -fx-underline: false; -fx-font-weight: 900;");
            link.setFocusTraversable(false);
            link.setOnAction(e -> action.run());
            textStack.getChildren().add(link);
        }
        
        layout.getChildren().addAll(iconCircle, textStack);
        card.getChildren().add(layout);
        
        card.setOnMouseEntered(e -> card.setStyle(SOFT_CARD + "-fx-background-color: " + COLOR_SURFACE_ELEVATED + "; -fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.12), 40, 0, 0, 15);"));
        card.setOnMouseExited(e -> card.setStyle(SOFT_CARD));
        
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
        ((Label)((HBox)info.getChildren().get(1)).getChildren().get(1)).setStyle("-fx-text-fill: " + COLOR_WHITE_MUTED_BORDER + ";");
        
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
        ((Label)metaBox.getChildren().get(1)).setStyle("-fx-text-fill: " + COLOR_WHITE_MUTED_BORDER + ";");
        
        VBox progressBox = new VBox(5, progress, new HBox(progressText));
        ((HBox)progressBox.getChildren().get(1)).setAlignment(Pos.CENTER_RIGHT);
        
        info.getChildren().addAll(titleBox, metaBox, subjectLabel, progressBox);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        content.getChildren().addAll(iconBox, info);
        card.getChildren().addAll(h, content);
        return card;
    }

    public static VBox createHelpCard(String iconPath, String title, String description, String iconColor) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(24));
        card.setStyle(SOFT_CARD);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.web(iconColor), 20));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle("-fx-background-color: " + iconColor + "10; -fx-background-radius: 12;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + ";");
        
        header.getChildren().addAll(iconBox, titleLabel);
        
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + COLOR_SLATE + "; -fx-line-spacing: 1.2px;");
        
        card.getChildren().addAll(header, descLabel);
        
        card.setOnMouseEntered(e -> card.setStyle(SOFT_CARD + "-fx-background-color: " + COLOR_SURFACE_ELEVATED + "; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(SOFT_CARD));
        
        return card;
    }

    public static VBox createSideHelpPanel(Node... cards) {
        VBox panel = new VBox(20);
        panel.setPrefWidth(280);
        panel.setMinWidth(280);
        
        Label header = new Label("ДОВІДКА ТА ПОРАДИ");
        header.setStyle(HEADER_STYLE + "-fx-padding: 0 0 5 10;");
        
        panel.getChildren().add(header);
        panel.getChildren().addAll(cards);
        return panel;
    }

    public static Button createCardActionButton(String iconPath, String hoverBg, String hoverBorder, Runnable action) {
        Button btn = createCardActionButton(iconPath, hoverBg, hoverBorder);
        btn.setOnAction(e -> action.run());
        return btn;
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

            // Визначаємо, чи це кнопка видалення (містить "danger" або відповідні стилі)
            boolean isDanger = hoverBorder.contains(COLOR_DANGER) || hoverBorder.contains(COLOR_DANGER_LIGHT) || iconPath.contains("trash");

            if (isDanger) {
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
