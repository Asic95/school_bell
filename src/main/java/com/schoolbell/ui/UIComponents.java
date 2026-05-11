package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import javafx.scene.shape.Rectangle;
import java.util.List;

import static com.schoolbell.ui.UIStyles.*;

public class UIComponents {

    public static List<Integer> parseSafe(String val) { 
        try { 
            return List.of(Math.max(0, Integer.parseInt(val))); 
        } catch (Exception e) { 
            return List.of(0); 
        } 
    }

    public static void updatePreview(HBox box, List<Integer> rings, int pause, String hexColor) {
        box.getChildren().clear(); 
        box.setPadding(new Insets(15, 0, 15, 0)); 
        box.setSpacing(8);
        double scale = 30.0;
        
        // Визначаємо градієнт на основі базового кольору
        String gradient = String.format("linear-gradient(to bottom, %s, derive(%s, -20%%))", hexColor, hexColor);
        
        for (int i = 0; i < rings.size(); i++) {
            int d = rings.get(i);
            if (d > 0) {
                VBox block = new VBox(5); 
                block.setAlignment(Pos.CENTER);
                
                Rectangle r = new Rectangle(Math.max(40, d * scale), 42); 
                r.setArcWidth(12); 
                r.setArcHeight(12); 
                r.setStyle("-fx-fill: " + gradient + "; -fx-effect: dropshadow(three-pass-box, " + hexColor + "44, 8, 0, 0, 4);");
                
                Label l = new Label(d + " сек"); 
                l.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #2d3436; -fx-text-transform: uppercase;");
                
                block.getChildren().addAll(r, l); 
                box.getChildren().add(block);
            }
            if (i < rings.size() - 1 && pause > 0) {
                VBox pBlock = new VBox(5); 
                pBlock.setAlignment(Pos.CENTER);
                
                Rectangle p = new Rectangle(Math.max(20, pause * scale), 12); 
                p.setArcWidth(6);
                p.setArcHeight(6);
                p.setFill(Color.web("#dfe6e9"));
                p.setOpacity(0.5);
                
                Label l = new Label(pause + "с"); 
                l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");
                
                pBlock.getChildren().addAll(p, l); 
                box.getChildren().add(pBlock);
            }
        }
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

        // Deterministic color based on name hash
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

    public static Button createPrimaryActionButton(String text, String iconPath) {
        Button btn = new Button(text.toUpperCase());
        if (iconPath != null) {
            btn.setGraphic(createSVGIcon(iconPath, Color.WHITE, 18));
            btn.setGraphicTextGap(12);
        }
        
        String baseStyle = String.format(
            "-fx-background-color: %s; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: 900; " +
            "-fx-font-size: 13px; " +
            "-fx-padding: 12 40; " +
            "-fx-background-radius: 14; " +
            "-fx-cursor: hand;", 
            COLOR_PRIMARY
        );
        
        btn.setStyle(baseStyle);
        
        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle + "-fx-opacity: 0.9; -fx-effect: dropshadow(three-pass-box, rgba(9,132,227,0.3), 15, 0, 0, 5);"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        
        return btn;
    }

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
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox h = new HBox(20, iconBox, textStack, spacer);
        h.setAlignment(Pos.CENTER_LEFT);
        
        if (action != null) {
            h.getChildren().add(action);
        }
        
        Region line = new Region();
        line.setPrefHeight(2);
        line.setStyle("-fx-background-color: #dfe6e9;");
        
        VBox v = new VBox(20, h, line);
        v.setPadding(new Insets(10, 0, 20, 0));
        return v;
    }

    public static VBox createSectionHeader(String title, String subtitle, String color, String svgPath) {
        return createSectionHeader(title, subtitle, color, svgPath, null);
    }

    public static VBox createManagementCard(String title, String description, String iconPath, String iconColor, String bgColor, Runnable action) {
        VBox card = new VBox(16);
        card.setPrefSize(280, 200);
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
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72; -fx-wrap-text: true; -fx-text-alignment: center;");

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
        
        if (hasSlider) {
            Slider slider = new Slider(0, 100, volumeValue);
            slider.setPrefWidth(200);
            slider.setMaxWidth(250);
            slider.getStylesheets().add("data:text/css," + SLIDER_STYLE.replace(" ", "%20"));
            
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int val = newVal.intValue();
                if (valueNode instanceof Label l) {
                    l.setText("Гучність: " + val + "%");
                }
                if (sliderAction != null) {
                    sliderAction.accept(val);
                }
            });

            VBox sliderBox = new VBox(8, slider);
            sliderBox.setPadding(new Insets(5, 0, 0, 0));
            textStack.getChildren().add(sliderBox);
        } else if (actionText != null) {
            Hyperlink link = new Hyperlink(actionText + "  →");
            link.setStyle("-fx-text-fill: " + COLOR_PRIMARY + "; -fx-font-size: 13px; -fx-padding: 5 0 0 0; -fx-underline: false; -fx-font-weight: 800;");
            link.setOnAction(e -> action.run());
            textStack.getChildren().add(link);
        }
        
        layout.getChildren().addAll(iconCircle, textStack);
        card.getChildren().add(layout);
        
        card.setOnMouseEntered(e -> {
            card.setStyle(baseStyle + "-fx-background-color: #fcfcfc; -fx-translate-y: -3;");
            card.setEffect(new DropShadow(javafx.scene.effect.BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.1), 25, 0, 0, 10));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            card.setEffect(new DropShadow(javafx.scene.effect.BlurType.THREE_PASS_BOX, Color.rgb(0,0,0,0.06), 15, 0, 0, 6));
        });
        
        return card;
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
        
        // Icon Container with Glass Effect
        VBox iconBox = new VBox(createSVGIcon(iconPath, Color.WHITE, 42));
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(70, 70);
        iconBox.setStyle("-fx-background-color: rgba(255,255,255,0.15); " +
                          "-fx-background-radius: 20; " +
                          "-fx-border-color: rgba(255,255,255,0.3); " +
                          "-fx-border-width: 1; " +
                          "-fx-border-radius: 20;");
        
        // Text Stack
        VBox text = new VBox(3);
        text.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title); 
        t.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: white; -fx-letter-spacing: 0.5px;");
        
        Label s = new Label(subtext); 
        s.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.7); -fx-font-weight: 800;");
        text.getChildren().addAll(t, s);
        
        HBox leftSide = new HBox(20, iconBox, text);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        
        // Bold SVG Arrow
        String arrowPath = "M4,11V13H16L10.5,18.5L11.92,19.92L19.84,12L11.92,4.08L10.5,5.5L16,11H4Z";
        Node arrowIcon = createSVGIcon(arrowPath, Color.WHITE, 28);
        
        content.setLeft(leftSide);
        content.setRight(arrowIcon);
        BorderPane.setAlignment(arrowIcon, Pos.CENTER);
        
        btn.setGraphic(content);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        // Hover Micro-interactions
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

    public static ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>();
        for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current));
        cb.setPrefWidth(65);
        cb.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 5; -fx-border-color: #dfe6e9; -fx-border-radius: 5;");
        return cb;
    }

    public static VBox createSettingsSection(String title, String color, String svgPath) {
        VBox v = new VBox(8); v.setPadding(new Insets(12)); v.setStyle(DEPTH_2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2d3436;");
        HBox header = new HBox(12, createSVGIcon(svgPath, Color.web(color), 18), titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        v.getChildren().add(header); return v;
    }

    public static HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field); HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); return h;
    }

    public static TextField createStyledField(String val) {
        TextField f = new TextField(val); f.setPrefWidth(70); f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 6; -fx-border-color: #dfe6e9; -fx-border-radius: 6; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 4 7;"); return f;
    }
}
