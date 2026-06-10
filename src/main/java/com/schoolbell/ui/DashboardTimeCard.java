package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.schoolbell.ui.UIStyles.*;

public class DashboardTimeCard extends VBox {
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Label currentTimeLabel;

    public DashboardTimeCard() {
        super(5);
        currentTimeLabel = new Label("00:00:00");
        currentTimeLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        
        Label timeSubtext = new Label("NTP СИНХРОНІЗОВАНО");
        timeSubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        
        Circle ntpDot = new Circle(4, Color.web(COLOR_SUCCESS));
        
        Label headerLabel = new Label("ПОТОЧНИЙ ЧАС");
        headerLabel.setStyle(HEADER_STYLE);
        
        getChildren().addAll(headerLabel, currentTimeLabel, new HBox(8, ntpDot, timeSubtext));
        setPadding(new Insets(25));
        setStyle(SOFT_CARD);
    }

    public void update(LocalTime now) {
        String text = now.format(HH_MM_SS);
        if (!text.equals(currentTimeLabel.getText())) {
            currentTimeLabel.setText(text);
        }
    }
}
