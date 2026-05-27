package com.schoolbell.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import static com.schoolbell.ui.UIStyles.*;

/**
 * Premium success dialog for database restoration.
 */
public class RestoreSuccessDialog extends BasePremiumDialog {

    public RestoreSuccessDialog(Stage owner) {
        super(owner,
                "ВІДНОВЛЕННЯ",
                "Відновлення успішне",
                "Базу даних було успішно завантажено з файлу копії.",
                "ЗРОЗУМІЛО",
                500);

        VBox infoBox = new VBox(15);
        infoBox.setPadding(new javafx.geometry.Insets(20));
        infoBox.setStyle("-fx-background-color: " + COLOR_SURFACE_SKY + "; -fx-background-radius: 18; -fx-border-color: " + COLOR_PRIMARY + "40; -fx-border-radius: 18;");
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label infoTitle = new Label("ПОТРІБНЕ ПЕРЕЗАВАНТАЖЕННЯ");
        infoTitle.setStyle("-fx-font-weight: 900; -fx-font-size: 13px; -fx-text-fill: " + COLOR_PRIMARY + ";");
        
        Label infoText = new Label("Для того, щоб нові налаштування та розклад набрали чинності, будь ласка, перезапустіть програму.");
        infoText.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_NAVY + ";");
        infoText.setWrapText(true);

        infoBox.getChildren().addAll(infoTitle, infoText);
        
        content.getChildren().add(infoBox);
        
        // Cleanup footer: remove redundant Cancel and fix icon/text for the primary button
        actions.getChildren().remove(cancelBtn);
        saveBtn.setText("ЗРОЗУМІЛО");
        saveBtn.setGraphic(UIComponents.createSVGIcon(ICON_CHECK, Color.WHITE, 18));
    }

    @Override
    protected boolean onSave() {
        return true;
    }
}
