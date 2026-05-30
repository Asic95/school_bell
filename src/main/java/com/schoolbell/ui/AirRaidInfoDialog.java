package com.schoolbell.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Informative dialog shown when Air Raid Automation is enabled.
 */
public class AirRaidInfoDialog extends BasePremiumDialog {

    public AirRaidInfoDialog(Stage owner) {
        super(owner,
                "АВТОМАТИЗАЦІЯ ТРИВОГИ",
                "Важлива інформація",
                "Автоматична система моніторингу сигналів цивільного захисту.",
                "ЗРОЗУМІЛО",
                560);

        VBox infoBox = new VBox(15);
        infoBox.setPadding(new Insets(22));
        infoBox.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 22; -fx-border-color: " + COLOR_PRIMARY + "30; -fx-border-width: 1.5; -fx-border-radius: 22;");
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label infoTitle = new Label("ПЕРЕВАГИ ТА ОСОБЛИВОСТІ");
        infoTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-letter-spacing: 1px;");
        
        Label infoText = new Label("Система автоматично отримує дані про тривоги у вашому регіоні. Це значно спрощує роботу персоналу та забезпечує миттєву реакцію.");
        infoText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + "; -fx-line-spacing: 3;");
        infoText.setWrapText(true);

        infoBox.getChildren().addAll(infoTitle, infoText);

        VBox warningBox = new VBox(12);
        warningBox.setPadding(new Insets(20));
        warningBox.setStyle("-fx-background-color: " + COLOR_AMBER_LIGHT + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_WARNING_AMBER + "40; -fx-border-width: 1; -fx-border-radius: 18;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warnTitle = new Label("ЗВЕРНІТЬ УВАГУ");
        warnTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: " + COLOR_WARNING_AMBER + ";");
        
        Label warnText = new Label("Робота функції залежить від інтернет-з'єднання та сторонніх серверів. " +
                "У разі непередбачуваної помилки зв'язку пролунає спеціальний сигнал 'ПОМИЛКА АВТОМАТИЗАЦІЇ' (налаштовується у параметрах 'Повітряної тривоги' у розділі меню 'Сповіщення').");
        warnText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + "; -fx-line-spacing: 3;");
        warnText.setWrapText(true);

        warningBox.getChildren().addAll(warnTitle, warnText);
        
        content.getChildren().addAll(infoBox, warningBox);
        
        // Cleanup footer and setup countdown
        actions.getChildren().remove(cancelBtn);
        saveBtn.setGraphic(UIComponents.createSVGIcon(ICON_CHECK, Color.WHITE, 18));
        
        setupCountdown(7);
    }

    private void setupCountdown(int seconds) {
        saveBtn.setDisable(true);
        final String baseText = saveBtn.getText();
        
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        for (int i = 0; i <= seconds; i++) {
            final int remaining = seconds - i;
            javafx.animation.KeyFrame frame = new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(i),
                e -> {
                    if (remaining > 0) {
                        saveBtn.setText(baseText + " (" + remaining + ")");
                    } else {
                        saveBtn.setText(baseText);
                        saveBtn.setDisable(false);
                    }
                }
            );
            timeline.getKeyFrames().add(frame);
        }
        timeline.play();
    }

    @Override
    protected boolean onSave() {
        return true;
    }
}
