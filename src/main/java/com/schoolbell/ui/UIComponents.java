package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import static com.schoolbell.ui.UIStyles.HEADER_STYLE;
import static com.schoolbell.ui.UIStyles.createSVGIcon;

public class UIComponents {
    public static VBox createSectionHeader(String title, String color, String svgPath) {
        HBox h = new HBox(12, createSVGIcon(svgPath, Color.web(color), 20), new Label(title.toUpperCase()));
        h.setAlignment(Pos.CENTER_LEFT);
        Label l = (Label) h.getChildren().get(1);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436; -fx-letter-spacing: 1px;");
        Region line = new Region();
        line.setPrefHeight(1);
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #dfe6e9;");
        VBox v = new VBox(10, h, line);
        VBox.setVgrow(v, javafx.scene.layout.Priority.NEVER);
        v.setPadding(new Insets(0, 0, 10, 0));
        return v;
    }
}
