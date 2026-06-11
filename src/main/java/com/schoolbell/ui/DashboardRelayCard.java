package com.schoolbell.ui;

import com.schoolbell.MainApp;
import com.schoolbell.service.ConfigService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static com.schoolbell.ui.UIStyles.*;

public class DashboardRelayCard extends VBox {
    private final Circle relayIndicator;
    private final Label relayStatusLabel;
    private final Label relaySubtext;

    public DashboardRelayCard() {
        super(5);
        relayIndicator = new Circle(8, Color.web(COLOR_DANGER));
        relayStatusLabel = new Label("НЕМАЄ ЗВ'ЯЗКУ");
        relayStatusLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_DANGER + ";");
        relaySubtext = new Label("ПЕРЕВІРТЕ ПІДКЛЮЧЕННЯ");
        relaySubtext.setStyle("-fx-font-size: 11px; -fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");

        HBox statusRow = new HBox(12, relayIndicator, relayStatusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox relayContent = new VBox(4, statusRow, relaySubtext);
        relayContent.setAlignment(Pos.CENTER_LEFT);

        Label relayHeader = new Label("СТАТУС РЕЛЕ");
        relayHeader.setStyle(HEADER_STYLE);

        Region rSpacer = new Region();
        VBox.setVgrow(rSpacer, Priority.ALWAYS);

        getChildren().addAll(relayHeader, rSpacer, relayContent);
        setPadding(new Insets(25));
        setStyle(SOFT_CARD);
    }

    public void update(MainApp mainApp, ConfigService config) {
        if (config.isSimulationMode()) {
            safeSetText(relayStatusLabel, "РЕЖИМ СИМУЛЯЦІЇ");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_INDIGO + ";");
            relayIndicator.setFill(Color.web(COLOR_INDIGO));
            safeSetText(relaySubtext, "ФІЗИЧНЕ РЕЛЕ ВІДКЛЮЧЕНО (ЛОГУВАННЯ)");
        } else if (mainApp.getRelayController().isConnected()) {
            safeSetText(relayStatusLabel, "Підключено");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_SUCCESS + ";");
            relayIndicator.setFill(Color.web(COLOR_SUCCESS));
            safeSetText(relaySubtext, mainApp.getRelayController().getConnectionDetails().toUpperCase());
        } else {
            safeSetText(relayStatusLabel, "Немає зв'язку");
            relayStatusLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + COLOR_DANGER + ";");
            relayIndicator.setFill(Color.web(COLOR_DANGER));

            String errorMsg = "SHELLY".equals(config.getRelayType())
                    ? "ПЕРЕВІРТЕ МЕРЕЖУ ТА ЖИВЛЕННЯ SHELLY"
                    : "ПЕРЕВІРТЕ ПІДКЛЮЧЕННЯ USB КАБЕЛЮ";
            safeSetText(relaySubtext, errorMsg);
        }
    }

    private void safeSetText(Label label, String text) {
        if (label == null || text == null) return;
        if (!text.equals(label.getText())) {
            label.setText(text);
        }
    }
}
