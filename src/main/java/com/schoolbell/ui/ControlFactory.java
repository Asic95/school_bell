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
        f.setPrefWidth(70); 
        f.setAlignment(Pos.CENTER);
        f.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 6; -fx-border-color: #dfe6e9; -fx-border-radius: 6; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 4 7;"); 
        return f;
    }

    public static ComboBox<String> createTimeCombo(int max, int current) {
        ComboBox<String> cb = new ComboBox<>();
        for (int i = 0; i < max; i++) cb.getItems().add(String.format("%02d", i));
        cb.setValue(String.format("%02d", current));
        cb.setPrefWidth(65);
        cb.setStyle("-fx-font-family: 'Monospaced'; -fx-font-weight: bold; -fx-background-color: white; -fx-background-insets: 0; -fx-background-radius: 5; -fx-border-color: #dfe6e9; -fx-border-radius: 5;");
        return cb;
    }

    public static HBox createFieldRow(String label, TextField field) {
        HBox h = new HBox(10, new Label(label), new Region(), field); 
        HBox.setHgrow(h.getChildren().get(1), Priority.ALWAYS);
        h.setAlignment(Pos.CENTER_LEFT); 
        return h;
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
