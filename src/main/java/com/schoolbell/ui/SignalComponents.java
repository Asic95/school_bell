package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

import static com.schoolbell.ui.UIStyles.*;

public class SignalComponents {

    public static List<Integer> parseSafe(String val) { 
        try { 
            return List.of(Math.max(0, Integer.parseInt(val))); 
        } catch (Exception e) { 
            return List.of(0); 
        } 
    }

    public static void updatePreview(HBox box, String title, List<Integer> rings, int pause, String hexColor) {
        box.getChildren().clear(); 
        box.setPadding(new Insets(5, 0, 5, 0)); 
        box.setSpacing(8);
        box.setAlignment(Pos.CENTER_LEFT);
        
        if (title != null && !title.isEmpty()) {
            Label titleLbl = new Label(title + ": ");
            titleLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-min-width: 120;");
            box.getChildren().add(titleLbl);
        }
        
        double scale = 20.0;
        String gradient = String.format("linear-gradient(to bottom, %s, derive(%s, -20%%))", hexColor, hexColor);
        
        for (int i = 0; i < rings.size(); i++) {
            int d = rings.get(i);
            if (d > 0) {
                VBox block = new VBox(5); 
                block.setAlignment(Pos.CENTER);
                
                Rectangle r = new Rectangle(Math.max(30, d * scale), 30); 
                r.setArcWidth(10); 
                r.setArcHeight(10); 
                r.setStyle("-fx-fill: " + gradient + "; -fx-effect: dropshadow(three-pass-box, " + hexColor + "44, 8, 0, 0, 4);");
                
                Label l = new Label(d + "с"); 
                l.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXT + ";");
                
                block.getChildren().addAll(r, l); 
                box.getChildren().add(block);
            }
            if (i < rings.size() - 1 && pause > 0) {
                VBox pBlock = new VBox(5); 
                pBlock.setAlignment(Pos.CENTER);
                
                Rectangle p = new Rectangle(Math.max(15, pause * scale), 8); 
                p.setArcWidth(4);
                p.setArcHeight(4);
                p.setFill(Color.web("#dfe6e9"));
                p.setOpacity(0.5);
                
                Label l = new Label(pause + "с"); 
                l.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");
                
                pBlock.getChildren().addAll(p, l); 
                box.getChildren().add(pBlock);
            }
        }
    }

    public static javafx.scene.control.Spinner<Integer> createStyledSpinner(int min, int max, int initial) {
        javafx.scene.control.Spinner<Integer> s = new javafx.scene.control.Spinner<>(min, max, initial);
        s.setEditable(true);
        s.setPrefWidth(90);
        s.getStylesheets().add("data:text/css," + MODERN_SPINNER_STYLE.replace(" ", "%20"));
        return s;
    }
}
