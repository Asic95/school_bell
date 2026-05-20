package com.schoolbell.ui;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import static com.schoolbell.ui.UIComponents.createSVGIcon;
import static com.schoolbell.ui.UIStyles.*;

public class ControlFactory {

    public static HBox createPageHeader(String eyebrow, String title, String subtitle, String icon, String iconColor, Node action) {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox badge = new VBox(createSVGIcon(icon, Color.web(iconColor), 24));
        badge.setAlignment(Pos.CENTER);
        badge.setPrefSize(54, 54);
        badge.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #eef2ff, #dbeafe);" +
                "-fx-background-radius: 18;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.18), 18, 0, 0, 6);"
        );

        VBox text = new VBox(2);
        Label eb = new Label(eyebrow.toUpperCase());
        eb.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 1.2px;");

        Label t = new Label(title);
        t.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label s = new Label(subtitle);
        s.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: #64748b;");
        s.setWrapText(true);

        text.getChildren().addAll(eb, t, s);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(badge, text, spacer);
        if (action != null) header.getChildren().add(action);

        return header;
    }

    public static Button createPrimaryActionButton(String text, String iconPath) {
        Button btn = new Button(text);
        if (iconPath != null) {
            btn.setGraphic(createSVGIcon(iconPath, Color.WHITE, 18));
            btn.setGraphicTextGap(10);
        }
        
        String baseStyle = 
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: linear-gradient(to right, #4f46e5, #2563eb);" +
            "-fx-background-radius: 18;" +
            "-fx-padding: 14 24;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.2), 18, 0, 0, 6);";
        
        btn.setStyle(baseStyle);
        
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + 
            "-fx-background-color: linear-gradient(to right, #4338ca, #1d4ed8);" +
            "-fx-effect: dropshadow(three-pass-box, rgba(79,70,229,0.35), 24, 0, 0, 8);"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        
        return btn;
    }

    public static ToggleButton createToggleSwitch(boolean initialState) {
        ToggleButton btn = new ToggleButton();
        btn.setSelected(initialState);
        btn.setPrefSize(46, 28);
        btn.setMinSize(46, 28);
        
        Circle thumb = new Circle(10, Color.WHITE);
        thumb.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(15, 23, 42, 0.16)));
        
        StackPane container = new StackPane(thumb);
        container.setPrefSize(46, 28);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(0, 4, 0, 4));
        
        btn.setGraphic(container);
        
        Runnable updateStyle = () -> {
            if (btn.isSelected()) {
                container.setStyle(
                    "-fx-background-color: linear-gradient(to right, #22c55e, #16a34a);" +
                    "-fx-background-radius: 999;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.28), 14, 0, 0, 2);"
                );
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(18);
                tt.play();
            } else {
                container.setStyle("-fx-background-color: #dbe2ea; -fx-background-radius: 999;");
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(0);
                tt.play();
            }
        };
        
        btn.selectedProperty().addListener((obs, oldVal, newVal) -> updateStyle.run());
        updateStyle.run();
        
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");
        return btn;
    }

    public static TextField createStyledField(String val) {
        TextField f = new TextField(val); 
        f.setStyle(
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 11 16;" +
            "-fx-text-fill: #0f172a;"
        );
        
        f.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                f.setStyle(f.getStyle() + "-fx-border-color: #4f46e5; -fx-background-color: #f8faff;");
            } else {
                f.setStyle(f.getStyle().replace("-fx-border-color: #4f46e5; -fx-background-color: #f8faff;", ""));
            }
        });
        return f;
    }

    public static ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>();
        for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current));
        cb.setPrefWidth(90);
        cb.setPrefHeight(45);
        cb.setStyle(
            "-fx-font-family: 'Inter';" +
            "-fx-font-weight: 600;" + // Changed from 700 to 600 to match DatePicker
            "-fx-font-size: 14px;" +   // Match standard input size
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 0 0 0 12;" +
            "-fx-text-fill: #0f172a;"
        );
        return cb;
    }

    public static HBox createStatusBadge(String icon, String color, String tag, String label, Label value) {
        HBox badge = new HBox(16);
        badge.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 22));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(52, 52);
        iconBox.setStyle("-fx-background-color: " + color + "12; -fx-background-radius: 16;");

        VBox text = new VBox(1);
        Label tagLabel = new Label(tag);
        tagLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_ZINC_500 + "; -fx-letter-spacing: 0.8px;");

        HBox valueLine = new HBox(8);
        valueLine.setAlignment(Pos.BASELINE_LEFT);
        Label mainLabel = new Label(label);
        mainLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + COLOR_ZINC_500 + ";");
        value.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: " + COLOR_TEXT + ";");
        valueLine.getChildren().addAll(mainLabel, value);

        text.getChildren().addAll(tagLabel, valueLine);

        badge.getChildren().addAll(iconBox, text);
        return badge;
    }

    public static Label createChip(String text, String color, String icon) {
        Label l = new Label(text);
        if (icon != null) {
            l.setGraphic(createSVGIcon(icon, Color.web(color), 12));
            l.setGraphicTextGap(8);
        }
        l.setStyle(
                "-fx-background-color: " + color + "10;" +
                "-fx-text-fill: " + color + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 700;" +
                "-fx-padding: 6 14;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: " + color + "25;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );
        return l;
    }

    public static HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field); 
        HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); 
        return h;
    }

    public static VBox createModernSettingsGroup(String title, String icon, String color, Node content) {
        VBox section = new VBox(22);
        section.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 28;" +
            "-fx-padding: 28;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.06), 25, 0, 0, 8);" +
            "-fx-border-color: rgba(226, 232, 240, 0.5);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 28;"
        );
        HBox.setHgrow(section, Priority.ALWAYS);

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox(createSVGIcon(icon, Color.web(color), 22));
        iconBox.setPrefSize(52, 52);
        iconBox.setMinSize(52, 52);
        iconBox.setStyle("-fx-background-color: " + color + "10; -fx-background-radius: 16; -fx-alignment: CENTER;");

        VBox titleBlock = new VBox(1);
        Label tagLabel = new Label("ПАРАМЕТРИ");
        tagLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        titleBlock.getChildren().addAll(tagLabel, titleLabel);

        header.getChildren().addAll(iconBox, titleBlock);
        section.getChildren().addAll(header, content);
        return section;
    }

    public static VBox createModernSettingsGroup(String title, String icon, Node content) {
        return createModernSettingsGroup(title, icon, COLOR_PRIMARY, content);
    }

    public static VBox createLabeledField(String label, Node field) {
        VBox group = new VBox(8);
        if (field instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        group.getChildren().addAll(l, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return group;
    }

    public static VBox createSettingsSection(String title, String color, String svgPath) {
        VBox v = new VBox(8); 
        v.setPadding(new Insets(12)); 
        v.setStyle(DEPTH_2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        HBox header = new HBox(12, createSVGIcon(svgPath, Color.web(color), 18), titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().add(header); 
        return v;
    }

    public static Button createActionButton(String title, String subtext, String iconPath, String gradient) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(25, 30, 25, 30));
        btn.setCache(true);
        btn.setCacheHint(javafx.scene.CacheHint.QUALITY);
        
        btn.getProperties().put("baseGradient", gradient);
        applyActionButtonStyle(btn, false);
        
        BorderPane content = new BorderPane();
        
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.WHITE, 42));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(70, 70);
        iconBox.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                          "-fx-background-radius: 20; " +
                          "-fx-border-color: rgba(255,255,255,0.3); " +
                          "-fx-border-width: 1; " +
                          "-fx-border-radius: 20;");
        
        VBox text = new VBox(3);
        text.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title); 
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: white; -fx-letter-spacing: 0.5px;");
        
        Label s = new Label(subtext); 
        s.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7); -fx-font-weight: 800;");
        text.getChildren().addAll(t, s);
        
        HBox leftSide = new HBox(20, iconBox, text);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        
        String arrowPath = "M4,11V13H16L10.5,18.5L11.92,19.92L19.84,12L11.92,4.08L10.5,5.5L16,11H4Z";
        Node arrowIcon = createSVGIcon(arrowPath, Color.WHITE, 28);
        
        content.setLeft(leftSide);
        content.setRight(arrowIcon);
        BorderPane.setAlignment(arrowIcon, Pos.CENTER);
        
        btn.setGraphic(content);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        btn.setOnMouseEntered(e -> {
            applyActionButtonStyle(btn, true);
            btn.setTranslateY(-5);
            iconBox.setStyle(iconBox.getStyle().replace("0.15", "0.28"));
            arrowIcon.setTranslateX(8);
            arrowIcon.setScaleX(1.1);
            arrowIcon.setScaleY(1.1);
        });
        
        btn.setOnMouseExited(e -> {
            applyActionButtonStyle(btn, false);
            btn.setTranslateY(0);
            iconBox.setStyle(iconBox.getStyle().replace("0.28", "0.15"));
            arrowIcon.setTranslateX(0);
            arrowIcon.setScaleX(1.0);
            arrowIcon.setScaleY(1.0);
        });
        
        btn.setOnMousePressed(e -> btn.setTranslateY(-1));
        
        return btn;
    }

    private static void applyActionButtonStyle(Button btn, boolean hover) {
        String gradient = (String) btn.getProperties().getOrDefault("baseGradient", GRADIENT_PRIMARY);
        String style = "-fx-background-color: " + gradient + "; " +
                          "-fx-background-radius: 24; " +
                          "-fx-cursor: hand; " +
                          "-fx-border-color: rgba(255,255,255,0.2); " +
                          "-fx-border-width: 1.5; " +
                          "-fx-border-radius: 24;";
        if (hover) {
            style += "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 35, 0, 0, 18);";
        } else {
            style += "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 20, 0, 0, 10);";
        }
        btn.setStyle(style);
    }

    public static void updateActionButton(Button btn, String title, String subtext, String iconPath, String gradient) {
        BorderPane content = (BorderPane) btn.getGraphic();
        HBox leftSide = (HBox) content.getLeft();
        VBox iconBox = (VBox) leftSide.getChildren().get(0);
        VBox textStack = (VBox) leftSide.getChildren().get(1);
        
        Label t = (Label) textStack.getChildren().get(0);
        Label s = (Label) textStack.getChildren().get(1);
        
        if (!t.getText().equals(title)) {
            t.setText(title);
            s.setText(subtext);
            iconBox.getChildren().clear();
            iconBox.getChildren().add(createSVGIcon(iconPath, Color.WHITE, 42));
            
            btn.getProperties().put("baseGradient", gradient);
            applyActionButtonStyle(btn, false);
        }
    }
}
