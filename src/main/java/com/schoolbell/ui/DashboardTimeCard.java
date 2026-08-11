package com.schoolbell.ui;

import com.schoolbell.service.NtpService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
    private final Label timeSubtext;
    private final Circle ntpDot;
    private final HBox statusContainer;

    public DashboardTimeCard() {
        super(5);
        currentTimeLabel = new Label("00:00:00");
        currentTimeLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_TEXT + ";");
        
        timeSubtext = new Label("СИНХРОНІЗАЦІЯ...");
        timeSubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_SLATE + "; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        
        ntpDot = new Circle(4, Color.web(COLOR_SLATE));
        
        statusContainer = new HBox(8, ntpDot, timeSubtext);
        statusContainer.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("ПОТОЧНИЙ ЧАС");
        headerLabel.setStyle(HEADER_STYLE);
        
        getChildren().addAll(headerLabel, currentTimeLabel, statusContainer);
        setPadding(new Insets(25));
        setStyle(SOFT_CARD);
    }

    public void update(LocalTime now) {
        String text = now.format(HH_MM_SS);
        if (!text.equals(currentTimeLabel.getText())) {
            currentTimeLabel.setText(text);
        }
    }

    public void updateSyncStatus(NtpService.SyncResult result) {
        if (result.isSynced()) {
            ntpDot.setFill(Color.web(COLOR_SUCCESS));
            timeSubtext.setText("NTP СИНХРОНІЗОВАНО");
            timeSubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
            
            String tooltipText = "Сервер: " + result.getServer() + "\nЗсув часу: " + result.getOffsetMs() + " мс";
            Tooltip.install(statusContainer, new Tooltip(tooltipText));
        } else {
            ntpDot.setFill(Color.web(COLOR_WARNING_AMBER));
            timeSubtext.setText("СИСТЕМНИЙ ЧАС");
            timeSubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: " + COLOR_WARNING_AMBER + "; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
            
            Tooltip.install(statusContainer, new Tooltip("Неможливо з'єднатися з NTP серверами.\nВикористовується системний час ПК."));
        }
    }
}
