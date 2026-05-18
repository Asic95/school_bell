package com.schoolbell.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class LayoutUtils {

    public static VBox createSectionHeader(String title, String subtitle, String color, String svgPath, Node action) {
        VBox iconBox = new VBox(createSVGIcon(svgPath, Color.web(color), 32));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 16;");

        VBox textStack = new VBox(4);
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 20px; -fx-text-fill: #2d3436; -fx-letter-spacing: 1.5px;");
        
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #636e72;");
        
        textStack.getChildren().addAll(titleLabel, subLabel);
        textStack.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox h = new HBox(20, iconBox, textStack, spacer);
        h.setAlignment(Pos.CENTER_LEFT);
        
        if (action != null) {
            h.getChildren().add(action);
        }
        
        Region line = new Region();
        line.setPrefHeight(2);
        line.setStyle("-fx-background-color: #dfe6e9;");
        
        VBox v = new VBox(20, h, line);
        v.setPadding(new javafx.geometry.Insets(10, 0, 20, 0));
        return v;
    }

    public static VBox createSectionHeader(String title, String subtitle, String color, String svgPath) {
        return createSectionHeader(title, subtitle, color, svgPath, null);
    }

    public static StackPane createAvatar(String name, double size) {
        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            for (String part : parts) {
                if (!part.isEmpty()) initials += part.substring(0, 1).toUpperCase();
                if (initials.length() >= 2) break;
            }
        }
        if (initials.isEmpty()) initials = "?";

        int hash = name != null ? name.hashCode() : 0;
        String[] colors = {"#74b9ff", "#55efc4", "#fab1a0", "#a29bfe", "#ffeaa7", "#81ecec"};
        String bgColor = colors[Math.abs(hash) % colors.length];

        StackPane avatar = new StackPane();
        avatar.setPrefSize(size, size);
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        avatar.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 50%;");

        Label label = new Label(initials);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size / 2.5) + "px;");
        
        avatar.getChildren().add(label);
        return avatar;
    }

    public static javafx.scene.shape.Line createSeparator() {
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 100, 0);
        line.setStroke(Color.web("#dfe6e9"));
        line.setStrokeWidth(1.5);
        return line;
    }
}
